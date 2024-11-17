package com.example.blowscrolling.components

import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.example.blowscrolling.data.Post
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PostList(postList: List<Post>, voteList: MutableList<Int>, postPositions: MutableList<kotlin.Pair<Int, LayoutCoordinates?>>, scroll: Boolean, scrollUp: Boolean, modifier: Modifier = Modifier) {
    val scrollState = rememberLazyListState()

    Log.d("POSTLIST", "Active Scrolling: $scroll")

    LaunchedEffect(scroll) {
        delay(200L)
        while(scroll) {
            launch {
                if(scrollUp)
                    scrollState.animateScrollBy(-1000f, tween(1000, 0, easing = LinearEasing))
                else
                    scrollState.animateScrollBy(1000f, tween(1000, 0, easing = LinearEasing))
            }
            delay(1000L)
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                postPositions.clear()
                return super.onPreScroll(available, source)
            }
        }
    }

    LazyColumn(state = scrollState, modifier = modifier.nestedScroll(nestedScrollConnection)) {
        postPositions.clear()
        itemsIndexed(postList) { index, post ->

            PostCard(
                post = post,
                selection = voteList[index],
                onUpVoteClick = {
                    if(it)
                        voteList[index] = 1
                    else
                        voteList[index] = 0
                    voteList[index]
                },
                onDownVoteClick = {
                    if(it)
                        voteList[index] = 2
                    else
                        voteList[index] = 0
                    voteList[index]
                },
                modifier = Modifier
                    .padding(start = 15.dp, end = 15.dp, top = 12.dp, bottom = 12.dp)
                    .fillMaxWidth()
                    .onGloballyPositioned {
                        //Log.d("POSTLIST", "$index GLOBALLY POSITIONED: ${it.positionInWindow().y}")
                        //parentPositions[index] = it.positionInWindow().y
                        //if(parentPositions.size <= index)
                        postPositions.add(Pair(index, it))
                        //else
                        //parentPositions[index] = Pair(index, it)
                    },
            )
        }
    }
}