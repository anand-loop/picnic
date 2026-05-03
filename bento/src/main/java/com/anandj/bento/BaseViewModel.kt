// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.bento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Base ViewModel for the MVI pattern.
 *
 * @param S State type, emitted via [state].
 * @param E Effect type for one-shot events (navigation, toasts, etc.), emitted via [effects].
 * @param A Action type, a sealed class of user intents dispatched via [sendAction].
 */
abstract class BaseViewModel<S, E, A>(initialState: S) : ViewModel() {
    private val _state = MutableStateFlow(initialState)

    /** The current UI state. */
    val state: StateFlow<S> = _state.asStateFlow()

    /** Snapshot of the current state, for use inside subclass logic. */
    protected val currentState: S get() = _state.value

    private val _effects = Channel<E>(Channel.UNLIMITED)

    /**
     * One-shot side effects to be consumed by the UI exactly once. Intended for a
     * **single collector**; two collectors will race for events.
     */
    val effects: Flow<E> = _effects.receiveAsFlow()

    private val actionsChannel = Channel<A>(Channel.UNLIMITED)
    private val collectorStarted = AtomicBoolean(false)

    /** Enqueues [action] to be processed sequentially by [onAction]. Thread-safe. */
    fun sendAction(action: A) {
        if (collectorStarted.compareAndSet(false, true)) startActionCollector()
        actionsChannel.trySend(action)
    }

    /** Atomically updates [state] using [block]. */
    protected fun updateState(block: (S) -> S) {
        _state.update(block)
    }

    /**
     * Called for each action dispatched via [sendAction]. Actions are processed
     * serially — the next action waits until this call returns (suspensions included).
     * Launch into [viewModelScope] from here if you want fan-out concurrency.
     */
    protected abstract suspend fun onAction(action: A)

    /**
     * Invoked when [onAction] throws (excluding [CancellationException], which is
     * always re-thrown to respect structured concurrency). Default implementation
     * swallows the throwable so the action pipeline stays alive. Override to log,
     * rethrow in debug builds, or forward as an [E].
     */
    protected open fun onActionError(
        action: A,
        throwable: Throwable,
    ) {
        // no-op by default
    }

    /** Emits a one-shot [effect] to the UI. Preserves call order. */
    protected fun sendEffect(effect: E) {
        _effects.trySend(effect)
    }

    private fun startActionCollector() {
        viewModelScope.launch {
            actionsChannel.receiveAsFlow().collect { action ->
                try {
                    onAction(action)
                } catch (ce: CancellationException) {
                    throw ce
                } catch (t: Throwable) {
                    onActionError(action, t)
                }
            }
        }
    }
}
