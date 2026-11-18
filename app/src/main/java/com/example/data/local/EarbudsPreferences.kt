package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AncMode
import com.example.data.model.AudioCodec
import com.example.data.model.TouchAction
import com.example.data.model.TouchGestureConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.earbudsDataStore: DataStore<Preferences> by preferencesDataStore(name = "aether_buds_prefs")

data class StoredPreferences(
    val ancMode: AncMode = AncMode.ANC_MAX,
    val ancDepth: Int = 85,
    val voiceFocusBoost: Boolean = true,
    val callSidetone: Boolean = true,
    val selectedProfileId: String = "preset_signature",
    val activeBandsDb: List<Float> = listOf(1.5f, 2.0f, 1.0f, 0.0f, -0.5f, 0.5f, 2.0f, 3.0f, 2.5f, 1.5f),
    val bassBoostPercent: Int = 28,
    val spatialAudio: Boolean = true,
    val headTracking: Boolean = false,
    val gamingLowLatency: Boolean = false,
    val audioCodec: AudioCodec = AudioCodec.LDAC,
    val inEarDetection: Boolean = true,
    val autoAnswerCalls: Boolean = false,
    val gestures: TouchGestureConfig = TouchGestureConfig(),
    val firmwareVersion: String = "v4.2.1",
    val exclusiveLockEnabled: Boolean = true,
    val lostFreezeActive: Boolean = false,
    val boundBudsName: String = "Aether Buds Pro",
    val boundBudsAddress: String = "E4:90:7E:A1:4C:02",
    val isHardwareBtBound: Boolean = false,
    val pinHash: String = ""
)

class EarbudsPreferences(private val context: Context) {

    private object Keys {
        val ANC_MODE = stringPreferencesKey("anc_mode")
        val ANC_DEPTH = intPreferencesKey("anc_depth")
        val VOICE_FOCUS = booleanPreferencesKey("voice_focus")
        val SIDETONE = booleanPreferencesKey("call_sidetone")
        val SELECTED_PROFILE_ID = stringPreferencesKey("selected_profile_id")
        val ACTIVE_BANDS_CSV = stringPreferencesKey("active_bands_csv")
        val BASS_BOOST = intPreferencesKey("bass_boost")
        val SPATIAL_AUDIO = booleanPreferencesKey("spatial_audio")
        val HEAD_TRACKING = booleanPreferencesKey("head_tracking")
        val GAMING_LATENCY = booleanPreferencesKey("gaming_latency")
        val AUDIO_CODEC = stringPreferencesKey("audio_codec")
        val IN_EAR_DETECTION = booleanPreferencesKey("in_ear_detection")
        val AUTO_ANSWER = booleanPreferencesKey("auto_answer")
        val GESTURE_L_SINGLE = stringPreferencesKey("g_l_single")
        val GESTURE_L_DOUBLE = stringPreferencesKey("g_l_double")
        val GESTURE_L_TRIPLE = stringPreferencesKey("g_l_triple")
        val GESTURE_L_HOLD = stringPreferencesKey("g_l_hold")
        val GESTURE_R_SINGLE = stringPreferencesKey("g_r_single")
        val GESTURE_R_DOUBLE = stringPreferencesKey("g_r_double")
        val GESTURE_R_TRIPLE = stringPreferencesKey("g_r_triple")
        val GESTURE_R_HOLD = stringPreferencesKey("g_r_hold")
        val FIRMWARE_VERSION = stringPreferencesKey("firmware_version")
        val EXCLUSIVE_LOCK = booleanPreferencesKey("exclusive_lock")
        val LOST_FREEZE = booleanPreferencesKey("lost_freeze")
        val BOUND_BUDS_NAME = stringPreferencesKey("bound_buds_name")
        val BOUND_BUDS_ADDRESS = stringPreferencesKey("bound_buds_address")
        val IS_HARDWARE_BT_BOUND = booleanPreferencesKey("is_hardware_bt_bound")
        val PIN_HASH = stringPreferencesKey("pin_hash")
    }

    val preferencesFlow: Flow<StoredPreferences> = context.earbudsDataStore.data.map { prefs ->
        val defaultBands = listOf(1.5f, 2.0f, 1.0f, 0.0f, -0.5f, 0.5f, 2.0f, 3.0f, 2.5f, 1.5f)
        val parsedBands = prefs[Keys.ACTIVE_BANDS_CSV]
            ?.split(",")
            ?.mapNotNull { it.toFloatOrNull() }
            ?.takeIf { it.size == 10 }
            ?: defaultBands

        StoredPreferences(
            ancMode = enumOrDefault(prefs[Keys.ANC_MODE], AncMode.ANC_MAX),
            ancDepth = prefs[Keys.ANC_DEPTH] ?: 85,
            voiceFocusBoost = prefs[Keys.VOICE_FOCUS] ?: true,
            callSidetone = prefs[Keys.SIDETONE] ?: true,
            selectedProfileId = prefs[Keys.SELECTED_PROFILE_ID] ?: "preset_signature",
            activeBandsDb = parsedBands,
            bassBoostPercent = prefs[Keys.BASS_BOOST] ?: 28,
            spatialAudio = prefs[Keys.SPATIAL_AUDIO] ?: true,
            headTracking = prefs[Keys.HEAD_TRACKING] ?: false,
            gamingLowLatency = prefs[Keys.GAMING_LATENCY] ?: false,
            audioCodec = enumOrDefault(prefs[Keys.AUDIO_CODEC], AudioCodec.LDAC),
            inEarDetection = prefs[Keys.IN_EAR_DETECTION] ?: true,
            autoAnswerCalls = prefs[Keys.AUTO_ANSWER] ?: false,
            gestures = TouchGestureConfig(
                leftSingle = enumOrDefault(prefs[Keys.GESTURE_L_SINGLE], TouchAction.PLAY_PAUSE),
                leftDouble = enumOrDefault(prefs[Keys.GESTURE_L_DOUBLE], TouchAction.PREV_TRACK),
                leftTriple = enumOrDefault(prefs[Keys.GESTURE_L_TRIPLE], TouchAction.VOLUME_DOWN),
                leftHold = enumOrDefault(prefs[Keys.GESTURE_L_HOLD], TouchAction.CYCLE_ANC),
                rightSingle = enumOrDefault(prefs[Keys.GESTURE_R_SINGLE], TouchAction.PLAY_PAUSE),
                rightDouble = enumOrDefault(prefs[Keys.GESTURE_R_DOUBLE], TouchAction.NEXT_TRACK),
                rightTriple = enumOrDefault(prefs[Keys.GESTURE_R_TRIPLE], TouchAction.VOLUME_UP),
                rightHold = enumOrDefault(prefs[Keys.GESTURE_R_HOLD], TouchAction.VOICE_ASSISTANT)
            ),
            firmwareVersion = prefs[Keys.FIRMWARE_VERSION] ?: "v4.2.1",
            exclusiveLockEnabled = prefs[Keys.EXCLUSIVE_LOCK] ?: true,
            lostFreezeActive = prefs[Keys.LOST_FREEZE] ?: false,
            boundBudsName = prefs[Keys.BOUND_BUDS_NAME] ?: "Aether Buds Pro",
            boundBudsAddress = prefs[Keys.BOUND_BUDS_ADDRESS] ?: "E4:90:7E:A1:4C:02",
            isHardwareBtBound = prefs[Keys.IS_HARDWARE_BT_BOUND] ?: false,
            pinHash = prefs[Keys.PIN_HASH] ?: ""
        )
    }

    suspend fun setAncMode(mode: AncMode) {
        context.earbudsDataStore.edit { it[Keys.ANC_MODE] = mode.name }
    }

    suspend fun setAncDepth(depth: Int) {
        context.earbudsDataStore.edit { it[Keys.ANC_DEPTH] = depth.coerceIn(0, 100) }
    }

    suspend fun setVoiceFocusBoost(enabled: Boolean) {
        context.earbudsDataStore.edit { it[Keys.VOICE_FOCUS] = enabled }
    }

    suspend fun setCallSidetone(enabled: Boolean) {
        context.earbudsDataStore.edit { it[Keys.SIDETONE] = enabled }
    }

    suspend fun setEqState(profileId: String, bands: List<Float>, bassBoost: Int) {
        context.earbudsDataStore.edit {
            it[Keys.SELECTED_PROFILE_ID] = profileId
            it[Keys.ACTIVE_BANDS_CSV] = bands.joinToString(",")
            it[Keys.BASS_BOOST] = bassBoost.coerceIn(0, 100)
        }
    }

    suspend fun setSpatialAudio(enabled: Boolean) {
        context.earbudsDataStore.edit {
            it[Keys.SPATIAL_AUDIO] = enabled
            if (!enabled) it[Keys.HEAD_TRACKING] = false
        }
    }

    suspend fun setHeadTracking(enabled: Boolean) {
        context.earbudsDataStore.edit { it[Keys.HEAD_TRACKING] = enabled }
    }

    suspend fun setGamingLowLatency(enabled: Boolean) {
        context.earbudsDataStore.edit { it[Keys.GAMING_LATENCY] = enabled }
    }

    suspend fun setAudioCodec(codec: AudioCodec) {
        context.earbudsDataStore.edit { it[Keys.AUDIO_CODEC] = codec.name }
    }

    suspend fun setInEarDetection(enabled: Boolean) {
        context.earbudsDataStore.edit { it[Keys.IN_EAR_DETECTION] = enabled }
    }

    suspend fun setAutoAnswerCalls(enabled: Boolean) {
        context.earbudsDataStore.edit { it[Keys.AUTO_ANSWER] = enabled }
    }

    suspend fun updateTouchGestures(config: TouchGestureConfig) {
        context.earbudsDataStore.edit {
            it[Keys.GESTURE_L_SINGLE] = config.leftSingle.name
            it[Keys.GESTURE_L_DOUBLE] = config.leftDouble.name
            it[Keys.GESTURE_L_TRIPLE] = config.leftTriple.name
            it[Keys.GESTURE_L_HOLD] = config.leftHold.name
            it[Keys.GESTURE_R_SINGLE] = config.rightSingle.name
            it[Keys.GESTURE_R_DOUBLE] = config.rightDouble.name
            it[Keys.GESTURE_R_TRIPLE] = config.rightTriple.name
            it[Keys.GESTURE_R_HOLD] = config.rightHold.name
        }
    }

    suspend fun setFirmwareVersion(version: String) {
        context.earbudsDataStore.edit { it[Keys.FIRMWARE_VERSION] = version }
    }

    suspend fun setExclusiveLockEnabled(enabled: Boolean) {
        context.earbudsDataStore.edit { it[Keys.EXCLUSIVE_LOCK] = enabled }
    }

    suspend fun setLostFreezeActive(active: Boolean) {
        context.earbudsDataStore.edit { it[Keys.LOST_FREEZE] = active }
    }

    suspend fun setBoundEarbuds(name: String, address: String, isHardwareBacked: Boolean) {
        context.earbudsDataStore.edit {
            it[Keys.BOUND_BUDS_NAME] = name
            it[Keys.BOUND_BUDS_ADDRESS] = address
            it[Keys.IS_HARDWARE_BT_BOUND] = isHardwareBacked
        }
    }

    suspend fun saveSecurityPin(pin: String) {
        context.earbudsDataStore.edit { it[Keys.PIN_HASH] = hashPin(pin) }
    }

    fun verifyPinHash(pin: String, storedHash: String): Boolean {
        if (storedHash.isEmpty()) return false
        return hashPin(pin) == storedHash
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(("aether_salt_v1:$pin").toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, default: T): T {
        if (raw == null) return default
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
    }
}
