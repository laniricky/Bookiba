package co.booknook.core.network.api

import co.booknook.core.network.model.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BookibaApi {

    @GET("books/featured")
    suspend fun getFeaturedBooks(): NetworkBooksResponse

    @GET("books/staff-pick")
    suspend fun getStaffPickBooks(): NetworkBooksResponse

    @GET("books")
    suspend fun getBooks(
        @Query("search") search: String? = null,
        @Query("genre") genre: String? = null,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): NetworkBooksResponse

    @GET("books/{id}")
    suspend fun getBookDetails(
        @Path("id") bookId: String
    ): NetworkBook

    @POST("checkout")
    suspend fun checkout(
        @Body request: NetworkCheckoutRequest
    ): NetworkCheckoutResponse

    @POST("auth/login")
    suspend fun login(
        @Body request: NetworkLoginRequest
    ): NetworkAuthResponse

    @POST("auth/register")
    suspend fun register(
        @Body request: NetworkRegisterRequest
    ): NetworkAuthResponse

    @GET("user/profile")
    suspend fun getUserProfile(): NetworkUserProfileResponse
}
