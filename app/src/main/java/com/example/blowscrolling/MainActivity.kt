package com.example.blowscrolling

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.blowscrolling.api.PostRetriever
import com.example.blowscrolling.components.PostList
import com.example.blowscrolling.data.Post
import com.example.blowscrolling.data.PostResponse
import com.example.blowscrolling.data.Posts
import com.example.blowscrolling.ui.theme.BlowScrollingTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

data class BlowEvent(var start: Long, var end: Long)

class MainActivity : ComponentActivity() {

    private lateinit var sensorManager: SensorManager
    private lateinit var barometerSensor: Sensor

    private var coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)

    private val blowEventListener = FilteredBlowListener()//BlowEventListener()
    private val postRetriever = PostRetriever()
    private val postList = mutableStateListOf<Post>()
    private var voteList = mutableStateListOf<Int>()//mutableStateListOf<PostCardData>()
    private var postPositions = mutableListOf<Pair<Int, LayoutCoordinates?>>()
    private val clicks = ArrayDeque<Long>()
    private var buttonHeld = false

    private var scrollDirUp by mutableStateOf(false)
    private var activeScrolling by mutableStateOf(false)
    private var activeBlowingState by mutableStateOf(false)
    private val blowGapMS = 500L
    private val blows = ArrayDeque<BlowEvent>()
    private var activeBlowing = false

    private var cursorX = 0f
    private var cursorY = 0f

    init {
        blowEventListener.registerOnBlowCallback {
            activeBlowing = true
            activeBlowingState = true
            val time = System.currentTimeMillis()

            if(!blows.isEmpty() && time - blows.last().end > blowGapMS)
                blows.clear()
            blows.addLast(BlowEvent(time, -1))

            if(blows.size == 1) {
                coroutineScope.launch {
                    delay(blowGapMS)
                    val t2 = System.currentTimeMillis()
                    if (blows.size == 1 && blows.last().end == -1L && t2 - blows.last().start > blowGapMS)
                        activeScrolling = true
                }
            }
        }

        blowEventListener.registerOnReleaseCallback {
            activeBlowing = false
            activeBlowingState = false
            activeScrolling = false
            blows.last().end = System.currentTimeMillis()

            if(blows.size == 2) {
                coroutineScope.launch {
                    delay(blowGapMS)
                    val t2 = System.currentTimeMillis()
                    if(blows.size == 2 && t2 - blows.last().end > blowGapMS)
                            scrollDirUp = !scrollDirUp
                }
            }
            else if(blows.size == 3) {
                coroutineScope.launch {
                    delay(blowGapMS)
                    val t2 = System.currentTimeMillis()
                    if(blows.size == 3 && t2 - blows.last().end > blowGapMS)
                        upvoteCursorItem()
                }
            }
            else if(blows.size == 4) {
                coroutineScope.launch {
                    delay(blowGapMS)
                    val time = System.currentTimeMillis()
                    if(blows.size == 4 && time - blows.last().end > blowGapMS)
                        downvoteCursorItem()
                }
            }
        }
    }

    private val apiCallback = object : Callback<PostResponse> {
        override fun onFailure(call: Call<PostResponse>?, t: Throwable?) {
            Log.d("MainActivity", "Problem calling Reddit API {${t?.message}}")
        }

        override fun onResponse(call: Call<PostResponse>?, response: Response<PostResponse>?) {
            response?.isSuccessful.let {
                val resultData = PostResponse(response?.body()?.data ?: Posts(emptyList()))
                postList.addAll(resultData.data.children.toMutableList())
                for(post in resultData.data.children) {
                    voteList.add(0)
                }
                //for (post in postList) {
                    //postPositions.add(0f)
                //}
            }
        }
    }

    protected var networkConnected: Boolean = false
    private var networkCallbackRegistered: Boolean = false

    private val networkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        .build()

    private var networkCallback = object : ConnectivityManager.NetworkCallback() {
        // network is available for use
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            networkConnected = true
            Log.d("NETWORK", "onAvailable Called!")
        }

        // Network capabilities have changed for the network
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            val unmetered = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)

            Log.d("NETWORK", "onCapabilitiesChanged Called!")
        }

        // lost network connection
        override fun onLost(network: Network) {
            super.onLost(network)
            networkConnected = false
            Log.d("NETWORK", "onLost Called!")
        }
    }

    @SuppressLint("UnusedBoxWithConstraintsScope")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)?.let { barometerSensor = it }

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.requestNetwork(networkRequest, networkCallback)
        networkCallbackRegistered = true

        Log.d("NETWORK", "Network Connected: " + networkConnected)

        //sensorManager.registerListener(blowEventListener, barometerSensor, SensorManager.SENSOR_DELAY_FASTEST)


        setContent {
            BlowScrollingTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RedditApp(activeBlowing = activeBlowingState, activeScrolling = activeScrolling)
                }
            }
        }

        if (networkConnected) {
            postRetriever.getRedditdevPosts(apiCallback)
        } else {
            AlertDialog.Builder(this).setTitle("No Internet Connection")
                .setMessage("Please check your internet connection and try again")
                .setPositiveButton(android.R.string.ok) { _, _ -> }
                .setIcon(android.R.drawable.ic_dialog_alert).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if(!networkCallbackRegistered) {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.requestNetwork(networkRequest, networkCallback)
            networkCallbackRegistered = true
        }
        sensorManager.registerListener(blowEventListener, barometerSensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    override fun onPause() {
        super.onPause()
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.unregisterNetworkCallback(networkCallback)
        networkCallbackRegistered = false
        this.sensorManager.unregisterListener(blowEventListener)
        blowEventListener.resetFilter()
    }

    private fun upvoteCursorItem() {
        for(pPair in postPositions) {
            if(pPair.second != null && pPair.second!!.isAttached) {
                val coords = pPair.second!!.positionInWindow()
                val bounds = pPair.second!!.boundsInWindow()
                if (cursorY > coords.y && coords.y + bounds.height > cursorY) {
                    if (voteList[pPair.first] != 1)
                        voteList[pPair.first] = 1
                    else
                        voteList[pPair.first] = 0
                    break
                }
            }
        }
    }

    private fun downvoteCursorItem() {
        for(pPair in postPositions) {
            if(pPair.second != null && pPair.second!!.isAttached) {
                val coords = pPair.second!!.positionInWindow()
                val bounds = pPair.second!!.boundsInWindow()
                 if (cursorY > coords.y && coords.y + bounds.height > cursorY) {
                    if (voteList[pPair.first] != 2)
                        voteList[pPair.first] = 2
                    else
                        voteList[pPair.first] = 0
                    break
                }
            }
        }
    }

    @Composable
    fun CreateList(scroll: Boolean, scrollUp: Boolean, activeBlowing: Boolean, modifier: Modifier = Modifier) {
        var color = MaterialTheme.colorScheme.background
        if(activeBlowing)
            color = Color.Green

        val layoutDirection = LocalLayoutDirection.current
        Surface(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(
                    start = WindowInsets.safeDrawing
                        .asPaddingValues()
                        .calculateStartPadding(layoutDirection),
                    end = WindowInsets.safeDrawing
                        .asPaddingValues()
                        .calculateEndPadding(layoutDirection)
                ),
        ) {
            PostList(
                postList = postList,
                voteList = voteList,
                postPositions = postPositions,
                scroll = scroll,
                scrollUp = scrollUp,
                modifier = Modifier.background(color)
            )
        }
    }

    @Composable
    fun RedditApp(activeBlowing: Boolean, activeScrolling: Boolean, modifier: Modifier = Modifier) {
        var lastClick by rememberSaveable { mutableLongStateOf(0) }
        val coroutineScope = rememberCoroutineScope()

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(1f)
        ) {
            val maxHeight = this.maxHeight
            val cursorHeight = 30.dp
            CreateList(scroll = activeScrolling, scrollUp = scrollDirUp, activeBlowing = activeBlowing, modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight())
            Column(modifier = Modifier
                .padding(start = 50.dp, end = 50.dp, bottom = maxHeight / 2 - cursorHeight / 2)
                .fillMaxWidth()
                .height(cursorHeight)
                .align(Alignment.BottomCenter)
            ){
                Cursor(
                    Modifier
                        .align(alignment = Alignment.CenterHorizontally)
                        .onGloballyPositioned {
                            cursorX = it.positionInWindow().x
                            cursorY = it.positionInWindow().y
                        }
                )
            }
            Column(modifier = Modifier
                .padding(start = 50.dp, end = 50.dp, bottom = 60.dp)
                .fillMaxWidth()
                .height(70.dp)
                .align(Alignment.BottomCenter)
            ){
                HoverButton(onClick = {
                        buttonHeld = true
                        val time = System.currentTimeMillis()
                        if (time - lastClick <= 400) {
                            clicks.addLast(time)
                        } else {
                            clicks.clear()
                            clicks.addLast(time)
                        }

                        if(clicks.size == 1) {
                            coroutineScope.launch {
                                delay(400)
                                if(buttonHeld && clicks.size == 1 && System.currentTimeMillis() - clicks.last() > 400)
                                    this@MainActivity.activeScrolling = true
                            }
                        }
                        else if(clicks.size == 2) {
                            coroutineScope.launch {
                                delay(400)
                                if(clicks.size == 2 && System.currentTimeMillis() - clicks.last() > 400) {
                                    scrollDirUp = !scrollDirUp
                                    if(buttonHeld)
                                        this@MainActivity.activeScrolling = true
                                }
                            }
                        }
                        else if(clicks.size == 3) {
                            coroutineScope.launch {
                                delay(400)
                                if(clicks.size == 3 && System.currentTimeMillis() - clicks.last() > 400)
                                    upvoteCursorItem()
                            }
                        }
                        else if(clicks.size == 4) {
                            coroutineScope.launch {
                                delay(400)
                                if(clicks.size == 4 && System.currentTimeMillis() - clicks.last() > 400)
                                    downvoteCursorItem()
                            }
                        }
                        Log.d("HOVERBUTTON", "Last: $lastClick, Current: $time, Difference: ${time - lastClick}, ScollUp: $scrollDirUp")
                        for (i in 0..<postPositions.size) {
                            Log.d("HOVERBUTTON", "$i: ${postPositions[i].first}, ${postPositions[i].second?.let {postPositions[i].second!!.positionInWindow().y} ?: run {"null"}}")
                        }
                        lastClick = time
                        //this@MainActivity.activeScrolling = true
                    },
                    onRelease = {
                        buttonHeld = false
                        this@MainActivity.activeScrolling = false
                        val time = System.currentTimeMillis()
                        //lastClick = time
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                )
            }
        }
    }
}

fun <T> SnapshotStateList<T>.swapList(newList: List<T>) {
    clear()
    addAll(newList)
}


@Composable
fun HoverButton(onClick: () -> Unit, onRelease: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var currentStateTxt by remember { mutableStateOf("Not Pressed") }

    if(isPressed) {
        LaunchedEffect(Unit) {
            currentStateTxt = "Pressed"
            onClick()
        }

        DisposableEffect(Unit) {
            onDispose{
                currentStateTxt = "Released"
                onRelease()
            }
        }
    }
    Button(onClick = {  }, interactionSource = interactionSource, modifier = modifier) {
        Text(text = currentStateTxt)
    }
}

@Composable
fun Cursor(modifier: Modifier = Modifier){
    Canvas(modifier = modifier.size(100.dp), onDraw = {
        drawCircle(color = Color.Red)
    })
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BlowScrollingTheme {
    }
}