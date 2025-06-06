package com.example.faceswapapp

import androidx.compose.ui.geometry.Offset

enum class StickerType { HAT, GLASSES, MUSTACHE }

data class Sticker(
    val id: String,
    val label: String,
    val type: StickerType,
    val resId: Int,
    // Anchor points for more precise placement (null if not needed for that sticker type)
    val leftAnchor: Offset? = null,
    val rightAnchor: Offset? = null
)

data class PlacedSticker(
    val sticker: Sticker,
    var x: Float = 0f,
    var y: Float = 0f,
    var scale: Float = 1f,
    var isSelected: Boolean = false,
    var rotation: Float = 0f
)

val availableStickers = listOf(
    Sticker(
        id = "hat",
        label = "Cappello",
        type = StickerType.HAT,
        resId = R.drawable.sticker_hat
    ),
    Sticker(
        id = "glasses",
        label = "Occhiali",
        type = StickerType.GLASSES,
        resId = R.drawable.sticker_glasses,
        leftAnchor = Offset(0.276f, 0.466f),
        rightAnchor = Offset(0.778f, 0.466f)
    ),
    Sticker(
        id = "mustache",
        label = "Baffi",
        type = StickerType.MUSTACHE,
        resId = R.drawable.sticker_mustache
    )
)