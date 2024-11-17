package com.example.blowscrolling.data

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState

data class PostResponse(val data: Posts)

data class Posts(val children: List<Post>)

data class Post(val data: Data)

data class Data(
    val subreddit: String,
    val author: String,
    val title: String,
    val ups: Int,
    val downs: Int,
    val thumbnail: String,
    val is_video: Boolean,
    val url: String,
    val is_reddit_media_domain: Boolean
)

data class PostCardData(
    val post: Post,
    var voteSelection: Int
)