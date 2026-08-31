package de.moritzstaat.launcher.data.media

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/**
 * Fires whenever an audio output is plugged in or connects over Bluetooth.
 *
 * Both broadcasts are protected and only delivered to receivers registered at runtime, so this
 * is a flow rather than a manifest entry. It carries no state of its own: the home screen
 * decides how long the music apps stay visible.
 */
class AudioOutputRepository(context: Context) {

    private val appContext = context.applicationContext

    /** Emits once per connect event. Disconnects are ignored. */
    val outputConnected: Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.isConnectEvent() == true) trySend(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
        }
        appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        awaitClose { runCatching { appContext.unregisterReceiver(receiver) } }
    }.conflate()
}

private fun Intent.isConnectEvent(): Boolean = when (action) {
    AudioManager.ACTION_HEADSET_PLUG -> getIntExtra(EXTRA_HEADSET_STATE, 0) == 1
    BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED ->
        getIntExtra(BluetoothProfile.EXTRA_STATE, -1) == BluetoothProfile.STATE_CONNECTED

    else -> false
}

/** ACTION_HEADSET_PLUG carries "state": 0 unplugged, 1 plugged in. */
private const val EXTRA_HEADSET_STATE = "state"
