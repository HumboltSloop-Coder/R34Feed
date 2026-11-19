package com.example.r34viewer.data.remote.api

import com.example.r34viewer.data.remote.dto.AutocompleteDto
import com.example.r34viewer.data.remote.dto.PostDto
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

interface Rule34ApiService {

    @GET("index.php")
    suspend fun getPosts(
        @Query("page") page: String = "dapi",
        @Query("s") s: String = "post",
        @Query("q") q: String = "index",
        @Query("json") json: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("pid") pid: Int = 0,
        @Query("tags") tags: String = ""
    ): List<PostDto>

    @GET("autocomplete.php")
    suspend fun autocompleteTags(
        @Query("q") query: String
    ): List<AutocompleteDto>

    @GET("index.php")
    suspend fun getTagDetails(
        @Query("page") page: String = "dapi",
        @Query("s") s: String = "tag",
        @Query("q") q: String = "index",
        @Query("json") json: Int = 1,
        @Query("name_pattern") namePattern: String,
        @Query("limit") limit: Int = 10
    ): ResponseBody

    companion object {
        const val BASE_URL = "https://api.rule34.xxx/"
    }
}
