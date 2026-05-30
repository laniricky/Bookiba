package co.booknook.core.network.auth

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Placeholder for real token logic from DataStore or memory
        val token = "placeholder_token" 
        
        val newRequest = originalRequest.newBuilder()
            .apply {
                if (token.isNotEmpty()) {
                    header("Authorization", "Bearer $token")
                }
            }
            .build()

        return chain.proceed(newRequest)
    }
}
