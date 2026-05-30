package co.booknook.core.common.network

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val bookibaDispatcher: BookibaDispatchers)

enum class BookibaDispatchers {
    Default,
    IO,
    Main
}
