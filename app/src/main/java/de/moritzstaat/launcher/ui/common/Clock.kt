package de.moritzstaat.launcher.ui.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDateTime

/**
 * Current wall clock time, updated by the system's minute tick.
 *
 * A ticker coroutine would either drift or have to poll every second; ACTION_TIME_TICK fires
 * exactly on the minute and costs nothing while the screen is off.
 */
@Composable
fun rememberCurrentDateTime(): State<LocalDateTime> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(LocalDateTime.now()) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                state.value = LocalDateTime.now()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_LOCALE_CHANGED)
        }
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        state.value = LocalDateTime.now()
        onDispose { context.unregisterReceiver(receiver) }
    }

    return state
}

/** True when the user picked 24 hour time in the system settings. */
@Composable
fun rememberIs24Hour(): Boolean {
    val context = LocalContext.current
    var is24Hour by remember { mutableStateOf(android.text.format.DateFormat.is24HourFormat(context)) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                context ?: return
                is24Hour = android.text.format.DateFormat.is24HourFormat(context)
            }
        }
        context.registerReceiver(
            receiver,
            IntentFilter(Intent.ACTION_TIME_CHANGED),
            Context.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }
    return is24Hour
}
