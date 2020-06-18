package at.robbert.upkeep

import com.pengrad.telegrambot.Callback
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Chat
import com.pengrad.telegrambot.model.User
import com.pengrad.telegrambot.request.BaseRequest
import com.pengrad.telegrambot.response.BaseResponse
import kotlinx.coroutines.future.await
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.lang.Exception
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

@Suppress("FINITE_BOUNDS_VIOLATION_IN_JAVA")
data class RequestResponseBase<T : BaseRequest<T, R>, R : BaseResponse>(val request: T, val response: R)

fun <T : BaseRequest<T, R>, R : BaseResponse> TelegramBot.execute(
    request: T, failureCallback: (T, IOException) -> Unit = { r, e ->
        log.error("Request $r failed:")
        e.printStackTrace()
    }, responseCallback: RequestResponseBase<T, R>.(R) -> Unit
) {
    val callback: Callback<T, R> = object : Callback<T, R> {
        override fun onFailure(request: T, e: IOException) {
            failureCallback(request, e)
        }

        override fun onResponse(request: T, response: R) {
            RequestResponseBase(request, response).responseCallback(response)
        }

    }
    this.execute(request, callback)
}

suspend fun <T : BaseRequest<T, R>, R : BaseResponse> TelegramBot.awaitExecution(request: T): RequestResponseBase<T, R> {
    return executeLater(request).await()
}

fun <T : BaseRequest<T, R>, R : BaseResponse> TelegramBot.executeLater(request: T): CompletableFuture<RequestResponseBase<T, R>> {
    val future = CompletableFuture<RequestResponseBase<T, R>>()

    val callback: Callback<T, R> = object : Callback<T, R> {
        override fun onFailure(request: T, e: IOException) {
            future.completeExceptionally(e)
        }

        override fun onResponse(request: T, response: R) {
            future.complete(RequestResponseBase(request, response))
        }

    }
    this.execute(request, callback)

    return future
}

fun Chat.fullName() = firstName() + " " + lastName()
fun User.fullName() = firstName() + " " + lastName()

private val loggers = ConcurrentHashMap<Class<*>, Logger>()
val <T : Any> T.log: Logger
    get() = loggers.getOrPut(this::class.java) {
        LoggerFactory.getLogger(this::class.java)
    }
