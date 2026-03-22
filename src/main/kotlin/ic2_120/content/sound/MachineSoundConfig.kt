package ic2_120.content.sound

import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier

/**
 * 声音类型枚举
 */
enum class SoundType {
    /** 循环音（如电解机、发电机） */
    LOOP,
    /** 操作音（如打粉机、压缩机） */
    OPERATE,
    /** 启动/停止音（如电炉、感应炉） */
    START_STOP,
    /** 无声音 */
    NONE
}

/**
 * 机器声音配置
 *
 * @property soundType 声音类型
 * @property loopSound 循环音事件
 * @property loopVolume 循环音音量
 * @property loopPitch 循环音音调
 * @property loopIntervalTicks 循环音播放间隔
 * @property startSound 启动音事件
 * @property startVolume 启动音音量
 * @property startPitch 启动音音调
 * @property stopSound 停止音事件
 * @property stopVolume 停止音音量
 * @property stopPitch 停止音音调
 * @property operateSound 操作音事件
 * @property operateVolume 操作音音量
 * @property operatePitch 操作音音调
 * @property operateIntervalTicks 操作音播放间隔
 * @property interruptSound 中断音事件
 * @property interruptVolume 中断音音量
 * @property interruptPitch 中断音音调
 * @property overloadSound 过载音事件
 * @property overloadVolume 过载音音量
 * @property overloadPitch 过载音音调
 */
data class MachineSoundConfig(
    val soundType: SoundType = SoundType.NONE,

    // 循环音配置
    val loopSound: SoundEvent? = null,
    val loopVolume: Float = 1.0f,
    val loopPitch: Float = 1.0f,
    val loopIntervalTicks: Int = 20, // 默认 1 秒

    // 启动/停止音配置
    val startSound: SoundEvent? = null,
    val startVolume: Float = 1.0f,
    val startPitch: Float = 1.0f,
    val stopSound: SoundEvent? = null,
    val stopVolume: Float = 1.0f,
    val stopPitch: Float = 1.0f,

    // 操作音配置
    val operateSound: SoundEvent? = null,
    val operateVolume: Float = 1.0f,
    val operatePitch: Float = 1.0f,
    val operateIntervalTicks: Int = 20,

    // 中断音配置
    val interruptSound: SoundEvent? = null,
    val interruptVolume: Float = 1.0f,
    val interruptPitch: Float = 1.0f,

    // 过载音配置
    val overloadSound: SoundEvent? = null,
    val overloadVolume: Float = 1.0f,
    val overloadPitch: Float = 1.0f
) {
    fun hasStartSound() = startSound != null
    fun hasStopSound() = stopSound != null
    fun hasLoopSound() = loopSound != null
    fun hasOperateSound() = operateSound != null
    fun hasInterruptSound() = interruptSound != null
    fun hasOverloadSound() = overloadSound != null

    companion object {
        /**
         * 创建循环音配置（用于发电机、电解机等）
         */
        fun loop(
            soundId: String,
            volume: Float = 0.5f,
            pitch: Float = 1.0f,
            intervalTicks: Int = 20
        ) = MachineSoundConfig(
            soundType = SoundType.LOOP,
            loopSound = SoundEvent.of(Identifier("ic2", soundId)),
            loopVolume = volume,
            loopPitch = pitch,
            loopIntervalTicks = intervalTicks
        )

        /**
         * 创建操作音配置（用于打粉机、压缩机等）
         */
        fun operate(
            soundId: String,
            volume: Float = 0.5f,
            pitch: Float = 1.0f,
            intervalTicks: Int = 20
        ) = MachineSoundConfig(
            soundType = SoundType.OPERATE,
            operateSound = SoundEvent.of(Identifier("ic2", soundId)),
            operateVolume = volume,
            operatePitch = pitch,
            operateIntervalTicks = intervalTicks
        )

        /**
         * 创建启动/停止音配置（用于电炉、感应炉等）
         */
        fun startStop(
            startSoundId: String,
            stopSoundId: String,
            volume: Float = 0.5f,
            pitch: Float = 1.0f,
            loopSoundId: String? = null,
            loopIntervalTicks: Int = 20
        ) = MachineSoundConfig(
            soundType = if (loopSoundId != null) SoundType.LOOP else SoundType.START_STOP,
            startSound = SoundEvent.of(Identifier("ic2", startSoundId)),
            stopSound = SoundEvent.of(Identifier("ic2", stopSoundId)),
            startVolume = volume,
            stopVolume = volume,
            startPitch = pitch,
            stopPitch = pitch,
            loopSound = loopSoundId?.let { SoundEvent.of(Identifier("ic2", it)) },
            loopIntervalTicks = loopIntervalTicks
        )

        /**
         * 创建无声音配置
         */
        fun none() = MachineSoundConfig(soundType = SoundType.NONE)
    }
}
