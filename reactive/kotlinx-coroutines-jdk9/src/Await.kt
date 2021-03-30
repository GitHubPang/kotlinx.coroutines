/*
 * Copyright 2016-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.coroutines.jdk9

import java.util.concurrent.*
import org.reactivestreams.FlowAdapters
import kotlinx.coroutines.reactive.*

/**
 * Awaits for the first value from the given publisher without blocking a thread and
 * returns the resulting value or throws the corresponding exception if this publisher had produced error.
 *
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this function
 * immediately resumes with [CancellationException].
 *
 * @throws NoSuchElementException if publisher does not emit any value
 */
public suspend fun <T> Flow.Publisher<T>.awaitFirst(): T = FlowAdapters.toPublisher(this).awaitFirst()

/**
 * Awaits for the first value from the given observable or the [default] value if none is emitted without blocking a
 * thread and returns the resulting value or throws the corresponding exception if this observable had produced error.
 *
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this function
 * immediately resumes with [CancellationException].
 */
public suspend fun <T> Flow.Publisher<T>.awaitFirstOrDefault(default: T): T =
        FlowAdapters.toPublisher(this).awaitFirstOrDefault(default)

/**
 * Awaits for the first value from the given observable or `null` value if none is emitted without blocking a
 * thread and returns the resulting value or throws the corresponding exception if this observable had produced error.
 *
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this function
 * immediately resumes with [CancellationException].
 */
public suspend fun <T> Flow.Publisher<T>.awaitFirstOrNull(): T? =
        FlowAdapters.toPublisher(this).awaitFirstOrNull()

/**
 * Awaits for the first value from the given observable or call [defaultValue] to get a value if none is emitted without blocking a
 * thread and returns the resulting value or throws the corresponding exception if this observable had produced error.
 *
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this function
 * immediately resumes with [CancellationException].
 */
public suspend fun <T> Flow.Publisher<T>.awaitFirstOrElse(defaultValue: () -> T): T =
        FlowAdapters.toPublisher(this).awaitFirstOrElse(defaultValue)

/**
 * Awaits for the last value from the given publisher without blocking a thread and
 * returns the resulting value or throws the corresponding exception if this publisher had produced error.
 *
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this function
 * immediately resumes with [CancellationException].
 *
 * @throws NoSuchElementException if publisher does not emit any value
 */
public suspend fun <T> Flow.Publisher<T>.awaitLast(): T =
        FlowAdapters.toPublisher(this).awaitLast()

/**
 * Awaits for the single value from the given publisher without blocking a thread and
 * returns the resulting value or throws the corresponding exception if this publisher had produced error.
 *
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this function
 * immediately resumes with [CancellationException].
 *
 * @throws NoSuchElementException if publisher does not emit any value
 * @throws IllegalArgumentException if publisher emits more than one value
 */
public suspend fun <T> Flow.Publisher<T>.awaitSingle(): T =
        FlowAdapters.toPublisher(this).awaitSingle()

/**
 * Awaits the single value from the given observable, or returns the [default] value if none is emitted, without
 * blocking the thread, and returns the resulting value, or, if this observable has produced an error, throws the
 * corresponding exception.
 *
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while the suspending function is waiting, this
 * function immediately cancels its [Flow.Subscription] and resumes with [CancellationException].
 *
 * @throws NoSuchElementException if the publisher does not emit any value
 * @throws IllegalArgumentException if the publisher emits more than one value
 */
public suspend fun <T> Flow.Publisher<T>.awaitSingleOrDefault(default: T): T =
    FlowAdapters.toPublisher(this).awaitSingleOrDefault(default)

/**
 * Awaits the single value from the given observable without blocking the thread and returns the resulting value, or, if
 * this observable has produced an error, throws the corresponding exception. If more than one value or none were
 * produced by the publisher, `null` is returned.
 *
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while the suspending function is waiting, this
 * function immediately cancels its [Flow.Subscription] and resumes with [CancellationException].
 */
public suspend fun <T> Flow.Publisher<T>.awaitSingleOrNull(): T? =
    FlowAdapters.toPublisher(this).awaitSingleOrNull()

/**
 * Awaits the single value from the given observable, or calls [defaultValue] to get a value if none is emitted, without
 * blocking the thread, and returns the resulting value, or, if this observable has produced an error, throws the
 * corresponding exception.
 *
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while the suspending function is waiting, this
 * function immediately cancels its [Flow.Subscription] and resumes with [CancellationException].
 *
 * @throws IllegalArgumentException if the publisher emits more than one value
 */
public suspend fun <T> Flow.Publisher<T>.awaitSingleOrElse(defaultValue: () -> T): T =
    FlowAdapters.toPublisher(this).awaitSingleOrElse(defaultValue)