package de.moritzstaat.launcher.data.media

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
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

    private val audioManager = appContext.getSystemService(AudioManager::class.java)

    /**
     * Name of the audio output music would go to right now, or null when that is the phone
     * itself.
     *
     * Android exposes no "current media route" for another app's session, so the connected
     * devices are ranked instead: a Bluetooth or wired output is where the sound is going if one
     * is attached. Good enough for a label, and it never lies about the phone speaker.
     */
    fun currentOutputName(): String? {
        val devices = runCatching {
            audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)?.toList()
        }.getOrNull().orEmpty()

        val device = RANKED_TYPES.firstNotNullOfOrNull { type ->
            devices.firstOrNull { it.type == type }
        } ?: return null

        val name = device.productName?.toString()?.trim()
        return name?.takeIf { it.isNotBlank() && !it.equals(android.os.Build.MODEL, true) }
            ?: fallbackName(device.type)
    }

    private fun fallbackName(type: Int): String? = when (type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLE_HEADSET -> "Bluetooth"
        AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Kabel"
        AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> "USB"
        else -> null
    }

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

    private companion object {
        /** Most specific first: a headset beats a speaker when both are attached. */
        val RANKED_TYPES = listOf(
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        )
    }
}

private fun Intent.isConnectEvent(): Boolean = when (action) {
    AudioManager.ACTION_HEADSET_PLUG -> getIntExtra(EXTRA_HEADSET_STATE, 0) == 1
    BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED ->
        getIntExtra(BluetoothProfile.EXTRA_STATE, -1) == BluetoothProfile.STATE_CONNECTED

    else -> false
}

/** ACTION_HEADSET_PLUG carries "state": 0 unplugged, 1 plugged in. */
private const val EXTRA_HEADSET_STATE = "state"
