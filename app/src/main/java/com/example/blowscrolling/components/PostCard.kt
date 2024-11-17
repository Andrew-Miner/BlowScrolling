package com.example.blowscrolling.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.blowscrolling.data.Post

@Composable
fun PostCard(post: Post, selection: Int?, onUpVoteClick: ((Boolean) -> Int)?, onDownVoteClick: ((Boolean) -> Int)?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)//RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    Card(
        modifier = modifier
            .border(border = BorderStroke(2.dp, Color.Black), shape = shape)
        ,
        shape = shape
    ) {
        Column(
            modifier = Modifier
                .background(Color(0xffcdd1e4))
                .fillMaxWidth()
        ) {
            if(post.data.is_reddit_media_domain && !post.data.is_video) {
                Column(modifier = Modifier
                    .heightIn(0.dp, 200.dp)
                ) {
                    val strokeWidth = with(LocalDensity.current) { 2.dp.toPx() }
                    AsyncImage(
                        model = post.data.url,
                        contentDescription = post.data.url,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            //.padding(top = 16.dp, start = 4.dp)
                            .fillMaxWidth()
                            .drawWithContent {
                                drawContent()

                                val y = size.height - strokeWidth / 2f

                                drawLine(
                                    Color.Black,
                                    Offset(0f, y),
                                    Offset(size.width, y),
                                    strokeWidth
                                )
                            }
                    )
                }
            }
            Row {
                VoteGroup(
                    selection = selection,
                    onUpVoteClick = onUpVoteClick,
                    onDownVoteClick = onDownVoteClick,
                    modifier = Modifier
                        .padding(top = 16.dp, start = 8.dp)
                        .align(Alignment.CenterVertically))
                Text(
                    text = post.data.title,
                    fontSize = 24.sp,
                    modifier = Modifier
                        .padding(top = 16.dp, start = 16.dp)
                        .align(Alignment.CenterVertically),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            Text(
                text = "Author: ${post.data.author}",
                fontSize = 20.sp,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
        }
    }
}