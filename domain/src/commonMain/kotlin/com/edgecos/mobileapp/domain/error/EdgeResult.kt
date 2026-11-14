package com.edgecos.mobileapp.domain.error

/**
 * EdgeCOS 统一结果包装。
 *
 * 用法：
 * ```
 * val result: EdgeResult<User> = authRepository.login(...)
 * result.onSuccess { user -> ... }
 *      .onFailure { error -> ... }
 * ```
 */
sealed interface EdgeResult<out T> {
    data class Success<T>(val data: T) : EdgeResult<T>
    data class Failure(val error: EdgeError) : EdgeResult<Nothing>
}

/** 成功的快捷构造 */
fun <T> success(data: T): EdgeResult<T> = EdgeResult.Success(data)

/** 失败的快捷构造 */
fun failure(error: EdgeError): EdgeResult<Nothing> = EdgeResult.Failure(error)

/** Result 操作扩展 */
inline fun <T> EdgeResult<T>.onSuccess(block: (T) -> Unit): EdgeResult<T> {
    if (this is EdgeResult.Success) block(data)
    return this
}

inline fun <T> EdgeResult<T>.onFailure(block: (EdgeError) -> Unit): EdgeResult<T> {
    if (this is EdgeResult.Failure) block(error)
    return this
}

inline fun <T, R> EdgeResult<T>.map(block: (T) -> R): EdgeResult<R> = when (this) {
    is EdgeResult.Success -> EdgeResult.Success(block(data))
    is EdgeResult.Failure -> this
}

inline fun <T> EdgeResult<T>.getOrNull(): T? = (this as? EdgeResult.Success)?.data

inline fun <T> EdgeResult<T>.getOrThrow(): T = when (this) {
    is EdgeResult.Success -> data
    is EdgeResult.Failure -> throw error
}

/** 把可能抛出异常的代码块包装为 EdgeResult（用于协程异常捕获） */
inline fun <T> runCatchingEdge(block: () -> T): EdgeResult<T> = try {
    EdgeResult.Success(block())
} catch (e: EdgeError) {
    EdgeResult.Failure(e)
} catch (e: Throwable) {
    EdgeResult.Failure(EdgeError.Unknown(e.message ?: "未知错误", e))
}
