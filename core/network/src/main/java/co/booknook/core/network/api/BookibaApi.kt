package co.booknook.core.network.api

import co.booknook.core.network.model.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BookibaApi {

    @GET("home.php")
    suspend fun getHome(): NetworkHomeResponse

    @GET("books.php")
    suspend fun getBooks(
        @Query("q") query: String? = null,
        @Query("category") category: String? = null
    ): NetworkBooksResponse

    @GET("books.php")
    suspend fun getBookDetails(
        @Query("id") bookId: String
    ): NetworkSingleBookResponse

    @POST("checkout.php")
    suspend fun checkout(
        @Body request: NetworkCheckoutRequest
    ): NetworkCheckoutResponse
}
