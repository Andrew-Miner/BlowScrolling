package com.example.blowscrolling.api

import com.example.blowscrolling.data.PostResponse
import retrofit2.http.GET
import retrofit2.Call

interface RedditService {
    //@GET("/r/redditdev.json?limit=100")
    @GET("/r/all.json?limit=100")
    //@GET("/r/ProgrammerHumor.json?limit=50")
    fun retrieveRedditdevPosts(): Call<PostResponse>
}