package com.example.blowscrolling.data

import android.util.Log
import com.google.gson.Gson
import java.net.URL


class Request(private val url: String) {
    fun run(): PostResponse {
        val repoListJsonStr = URL(url).readText()
        Log.d(javaClass.simpleName, repoListJsonStr)
        return Gson().fromJson(repoListJsonStr, PostResponse::class.java)
    }
}