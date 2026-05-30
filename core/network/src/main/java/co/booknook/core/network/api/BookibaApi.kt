package co.booknook.core.network.api

import co.booknook.core.network.model.NetworkBook
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface BookibaApi {

    @GET("books/featured")
    suspend fun getFeaturedBooks(): List<NetworkBook>

    @GET("books")
    suspend fun getBooks(
        @Query("query") query: String? = null,
        @Query("category") category: String? = null,
        @Query("page") page: Int = 1
    ): List<NetworkBook>

    @GET("books/{id}")
    suspend fun getBookDetails(
        @Path("id") bookId: String
    ): NetworkBook

    @GET("reels")
    suspend fun getReels(): List<String> // Placeholder for Reel model list
}
