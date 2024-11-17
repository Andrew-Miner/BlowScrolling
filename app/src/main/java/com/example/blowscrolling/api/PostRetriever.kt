package com.example.blowscrolling.api

import com.example.blowscrolling.data.PostResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Callback

class PostRetriever {
    private val service: RedditService

    companion object {
        const val BASE_URL = "https://www.reddit.com/"
    }

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        service = retrofit.create(RedditService::class.java)
    }

    fun getRedditdevPosts(callback: Callback<PostResponse>) {
        val call = service.retrieveRedditdevPosts()
        call.enqueue(callback)
    }
}