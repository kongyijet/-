package com.example.audio

data class RingtoneItem(
    val id: String,
    val name: String,
    val description: String,
    val iconName: String
)

object RingtoneCatalog {
    val RINGTONES = listOf(
        RingtoneItem(
            id = "classic_digital",
            name = "经典数字闹钟",
            description = "清脆明快的数字电子蜂鸣声",
            iconName = "alarm"
        ),
        RingtoneItem(
            id = "morning_birds",
            name = "清晨鸟鸣",
            description = "轻快优美的清晨自然鸟语",
            iconName = "nature"
        ),
        RingtoneItem(
            id = "gentle_chime",
            name = "优雅和弦",
            description = "温和舒缓的马林巴琴音律",
            iconName = "music_note"
        ),
        RingtoneItem(
            id = "energetic_beat",
            name = "活力电音",
            description = "节奏欢快的电子元气旋律",
            iconName = "bolt"
        ),
        RingtoneItem(
            id = "soft_piano",
            name = "柔和钢琴",
            description = "温暖平缓的治愈钢琴音阶",
            iconName = "piano"
        ),
        RingtoneItem(
            id = "radar_alert",
            name = "雷达警报",
            description = "穿透力强的现代雷达脉冲",
            iconName = "sensors"
        )
    )

    fun getRingtoneById(id: String): RingtoneItem {
        return RINGTONES.find { it.id == id } ?: RINGTONES.first()
    }
}
