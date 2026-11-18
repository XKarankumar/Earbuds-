package com.example.data.hardware

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.core.content.ContextCompat
import com.example.data.model.BondedAudioDevice
import com.example.data.model.RingTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import kotlin.math.PI
import kotlin.math.sin

class HardwareBridge(private val context: Context) {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val HOST_KEY_ALIAS = "aether_host_ec_v1"
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val audioManager: AudioManager? =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var ringJob: Job? = null

    fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    fun hasBluetoothRadio(): Boolean = bluetoothAdapter != null

    fun getHostDeviceModel(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
    }

    fun getOrCreateHostKeyFingerprint(forceRotate: Boolean = false): String {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            if (forceRotate && keyStore.containsAlias(HOST_KEY_ALIAS)) {
                keyStore.deleteEntry(HOST_KEY_ALIAS)
            }
            if (!keyStore.containsAlias(HOST_KEY_ALIAS)) {
                val generator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_EC,
                    KEYSTORE_PROVIDER
                )
                val spec = KeyGenParameterSpec.Builder(
                    HOST_KEY_ALIAS,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                )
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .build()
                generator.initialize(spec)
                generator.generateKeyPair()
            }
            val cert = keyStore.getCertificate(HOST_KEY_ALIAS)
            val encoded = cert?.publicKey?.encoded ?: getHostDeviceModel().toByteArray()
            val digest = MessageDigest.getInstance("SHA-256").digest(encoded)
            digest.take(8).joinToString(":") { "%02X".format(it) }
        } catch (_: Exception) {
            val fallback = MessageDigest.getInstance("SHA-256")
                .digest("${Build.BRAND}:${Build.MODEL}:${Build.HARDWARE}".toByteArray())
            fallback.take(8).joinToString(":") { "%02X".format(it) }
        }
    }

    @SuppressLint("MissingPermission")
    fun getBondedAudioDevices(): List<BondedAudioDevice> {
        val adapter = bluetoothAdapter ?: return emptyList()
        if (!hasBluetoothPermission() || !adapter.isEnabled) {
            return emptyList()
        }
        val activeBtAddresses = getActiveAudioBluetoothAddresses()
        return runCatching {
            adapter.bondedDevices.orEmpty()
                .filter { device ->
                    val major = device.bluetoothClass?.majorDeviceClass
                    major == BluetoothClass.Device.Major.AUDIO_VIDEO ||
                        major == BluetoothClass.Device.Major.WEARABLE ||
                        major == BluetoothClass.Device.Major.UNCATEGORIZED
                }
                .map { device ->
                    val safeName = device.name?.takeIf { it.isNotBlank() } ?: "Bluetooth Audio Device"
                    val isConnected = activeBtAddresses.contains(device.address) || isDeviceAclConnected(device)
                    BondedAudioDevice(
                        name = safeName,
                        address = device.address,
                        isConnected = isConnected,
                        isHardwareBacked = true,
                        deviceClassLabel = classifyBluetoothDevice(device)
                    )
                }
                .sortedByDescending { it.isConnected }
        }.getOrDefault(emptyList())
    }

    private fun getActiveAudioBluetoothAddresses(): Set<String> {
        val manager = audioManager ?: return emptySet()
        return runCatching {
            manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .filter {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                            it.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
                }
                .mapNotNull { info ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.address else null
                }
                .toSet()
        }.getOrDefault(emptySet())
    }

    private fun isDeviceAclConnected(device: BluetoothDevice): Boolean {
        return try {
            val method = device.javaClass.getMethod("isConnected")
            (method.invoke(device) as? Boolean) == true
        } catch (_: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun classifyBluetoothDevice(device: BluetoothDevice): String {
        return when (device.bluetoothClass?.deviceClass) {
            BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET,
            BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES -> "Wireless Earbuds / Headset"
            BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER -> "Bluetooth Speaker"
            else -> "BLE / Classic Audio"
        }
    }

    fun applyEqualizerAndBass(bandsDb: List<Float>, bassBoostPercent: Int) {
        try {
            if (equalizer == null) {
                equalizer = Equalizer(0, 0).apply { enabled = true }
            }
            val eq = equalizer ?: return
            val bandCount = eq.numberOfBands.toInt()
            val minLevel = eq.bandLevelRange[0]
            val maxLevel = eq.bandLevelRange[1]
            for (i in 0 until bandCount) {
                val mappedIndex = ((i.toFloat() / bandCount.coerceAtLeast(1)) * bandsDb.size)
                    .toInt()
                    .coerceIn(0, bandsDb.lastIndex)
                val db = bandsDb[mappedIndex].coerceIn(-12f, 12f)
                val millibels = (db * 100f).toInt().coerceIn(minLevel.toInt(), maxLevel.toInt())
                eq.setBandLevel(i.toShort(), millibels.toShort())
            }
            if (bassBoost == null) {
                bassBoost = BassBoost(0, 0).apply { enabled = true }
            }
            val strength = ((bassBoostPercent / 100f) * 1000f).toInt().coerceIn(0, 1000)
            bassBoost?.setStrength(strength.toShort())
        } catch (_: Exception) {
            // Device OEM/emulator restricts global session 0 AudioEffect; DSP state remains persisted
        }
    }

    fun startFindBudsTone(target: RingTarget, scope: CoroutineScope, onFinished: () -> Unit) {
        stopFindBudsTone()
        if (target == RingTarget.NONE) {
            onFinished()
            return
        }
        ringJob = scope.launch(Dispatchers.IO) {
            val sampleRate = 44100
            val pulseSamples = (sampleRate * 0.25).toInt()
            val stereoShorts = ShortArray(pulseSamples * 2)
            val leftGain = if (target == RingTarget.LEFT || target == RingTarget.BOTH) 0.7f else 0f
            val rightGain = if (target == RingTarget.RIGHT || target == RingTarget.BOTH) 0.7f else 0f

            for (i in 0 until pulseSamples) {
                val envelope = sin(PI * i / pulseSamples)
                val freq = if ((i / 2200) % 2 == 0) 2400.0 else 3100.0
                val sample = (sin(2.0 * PI * freq * i / sampleRate) * envelope * 22000.0).toInt()
                stereoShorts[i * 2] = (sample * leftGain).toInt().coerceIn(-32767, 32767).toShort()
                stereoShorts[i * 2 + 1] = (sample * rightGain).toInt().coerceIn(-32767, 32767).toShort()
            }

            var track: AudioTrack? = null
            try {
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(minBuf, stereoShorts.size * 2))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(stereoShorts, 0, stereoShorts.size)
                repeat(8) {
                    if (!isActive) return@repeat
                    track.stop()
                    track.reloadStaticData()
                    track.play()
                    delay(450)
                }
            } catch (_: Exception) {
                // Gracefully exit if audio hardware unavailable
            } finally {
                runCatching {
                    track?.stop()
                    track?.release()
                }
                onFinished()
            }
        }
    }

    fun stopFindBudsTone() {
        ringJob?.cancel()
        ringJob = null
    }

    fun release() {
        stopFindBudsTone()
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        equalizer = null
        bassBoost = null
    }
}
