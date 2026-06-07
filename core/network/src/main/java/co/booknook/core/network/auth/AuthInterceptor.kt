package co.booknook.core.network.auth

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Supplies the Bearer token for every outgoing request.
 * The token is injected at construction time so this interceptor
 * remains stateless. [NetworkModule] is responsible for providing a
 * fresh instance whenever the token changes (e.g. on login/logout).
 *
 * Pass an empty string to send unauthenticated requests.
 */
class AuthInterceptor(private val token: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().apply {
            if (token.isNotBlank()) {
                header("Authorization", "Bearer $token")
            }
        }.build()
        return chain.proceed(request)
    }
}
