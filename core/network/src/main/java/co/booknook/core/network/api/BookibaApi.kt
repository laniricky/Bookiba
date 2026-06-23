package co.booknook.core.network.api

import co.booknook.core.network.model.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Header

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

    @GET("books/suggestions")
    suspend fun getBookSuggestions(
        @Query("q") query: String
    ): Map<String, List<String>>

    @GET("books/genres")
    suspend fun getGenres(): Map<String, List<String>>

    @GET("books/{id}")
    suspend fun getBookDetails(
        @Path("id") bookId: String
    ): NetworkBook

    @POST("orders")
    suspend fun createOrder(
        @Body request: NetworkCheckoutRequest
    ): NetworkCheckoutResponse

    @GET("orders")
    suspend fun getOrders(): NetworkOrdersResponse

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

    @GET("wishlist")
    suspend fun getWishlist(): NetworkBooksResponse

    @POST("wishlist")
    suspend fun addToWishlist(
        @Body request: NetworkWishlistRequest
    )

    @DELETE("wishlist/{bookId}")
    suspend fun removeFromWishlist(
        @Path("bookId") bookId: String
    )


    @GET("reels")
    suspend fun getReels(): List<NetworkReel>

    @GET("banners")
    suspend fun getBanners(): NetworkBannersResponse

    @GET("books/{bookId}/reviews")
    suspend fun getReviews(
        @Path("bookId") bookId: String
    ): NetworkReviewsResponse

    @POST("books/{bookId}/reviews")
    suspend fun submitReview(
        @Path("bookId") bookId: String,
        @Body request: NetworkSubmitReviewRequest
    ): Map<String, String>

    @GET("addresses")
    suspend fun getAddresses(
        @Header("Authorization") token: String
    ): NetworkAddressesResponse

    @POST("addresses")
    suspend fun createAddress(
        @Header("Authorization") token: String,
        @Body request: NetworkCreateAddressRequest
    ): Map<String, String>

    @retrofit2.http.PUT("addresses/{id}")
    suspend fun updateAddress(
        @Path("id") addressId: String,
        @Header("Authorization") token: String,
        @Body request: NetworkUpdateAddressRequest
    ): Map<String, String>

    @DELETE("addresses/{id}")
    suspend fun deleteAddress(
        @Path("id") addressId: String,
        @Header("Authorization") token: String
    ): Map<String, String>

    @GET("editorials")
    suspend fun getEditorials(): NetworkEditorialsResponse

    @GET("themes")
    suspend fun getThemes(): NetworkThemesResponse

    @DELETE("user/account")
    suspend fun deleteAccount(
        @Header("Authorization") token: String
    ): Map<String, String>

    companion object {
        const val BASE_URL = "https://bookiba-backend.onrender.com"
    }
}
