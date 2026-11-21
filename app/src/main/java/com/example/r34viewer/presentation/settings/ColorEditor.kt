package com.example.r34viewer.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.r34viewer.domain.model.AppColors
import kotlin.math.abs
import kotlin.math.roundToInt

val ColorPresets = listOf(
    "#AAE5A4",
    "#64B5F6",
    "#B39DDB",
    "#F48FB1",
    "#EF5350",
    "#FFB74D",
    "#FFF176",
    "#4DD0E1",
    "#E0E0E0",
    "#424242"
)

@Composable
fun ColorEditor(
    title: String,
    value: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var hexText by remember(value) { mutableStateOf(value ?: "") }
    val color = hexToColor(hexText) ?: Color.Gray
    val hsv = color.toHsv()

    fun commit(newHex: String) {
        hexText = newHex
        onValueChange(newHex)
    }

    fun updateComponent(index: Int, raw: String) {
        val component = raw.toIntOrNull() ?: return
        if (component !in 0..255) return
        val components = intArrayOf(
            (color.red * 255f).roundToInt(),
            (color.green * 255f).roundToInt(),
            (color.blue * 255f).roundToInt()
        )
        components[index] = component
        commit(colorToHex(components[0], components[1], components[2]))
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Swatch(color = color, borderColor = MaterialTheme.colorScheme.outline, size = 36.dp)
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = value ?: "Predeterminado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ColorPresets.forEach { preset ->
                val presetColor = hexToColor(preset) ?: Color.Gray
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(presetColor)
                        .border(
                            width = if (value == preset) 2.dp else 1.dp,
                            color = if (value == preset) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { commit(preset) }
                )
            }
        }

        OutlinedTextField(
            value = hexText,
            onValueChange = { input ->
                hexText = input
                AppColors.normalizeHex(input)?.let(onValueChange)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Hexadecimal (#RRGGBB)") },
            singleLine = true
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ComponentField(
                label = "R",
                value = (color.red * 255f).roundToInt(),
                onChange = { updateComponent(0, it) },
                modifier = Modifier.weight(1f)
            )
            ComponentField(
                label = "G",
                value = (color.green * 255f).roundToInt(),
                onChange = { updateComponent(1, it) },
                modifier = Modifier.weight(1f)
            )
            ComponentField(
                label = "B",
                value = (color.blue * 255f).roundToInt(),
                onChange = { updateComponent(2, it) },
                modifier = Modifier.weight(1f)
            )
        }

        Text("Matiz", style = MaterialTheme.typography.labelMedium)
        Slider(
            value = hsv.hue,
            valueRange = 0f..360f,
            onValueChange = { hue ->
                val newColor = hsv.copy(hue = hue).toColor()
                commit(colorToHex(newColor))
            }
        )
    }
}

@Composable
private fun ComponentField(
    label: String,
    value: Int,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = onChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
private fun Swatch(color: Color, borderColor: Color, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(0.dp)
    )
}

private fun hexToColor(hex: String?): Color? {
    val raw = AppColors.normalizeHex(hex)?.removePrefix("#") ?: return null
    val value = raw.toLongOrNull(16) ?: return null
    return Color((0xFF000000L or value).toInt())
}

private fun colorToHex(color: Color): String = colorToHex(
    (color.red * 255f).roundToInt(),
    (color.green * 255f).roundToInt(),
    (color.blue * 255f).roundToInt()
)

private fun colorToHex(red: Int, green: Int, blue: Int): String =
    "#%02X%02X%02X".format(red.coerceIn(0, 255), green.coerceIn(0, 255), blue.coerceIn(0, 255))

private data class Hsv(val hue: Float, val saturation: Float, val value: Float)

private fun Color.toHsv(): Hsv {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min
    val hue = when {
        delta == 0f -> 0f
        max == red -> (60f * (((green - blue) / delta) % 2f) + 360f) % 360f
        max == green -> 60f * ((blue - red) / delta) + 120f
        else -> 60f * ((red - green) / delta) + 240f
    }
    return Hsv(hue, if (max == 0f) 0f else delta / max, max)
}

private fun Hsv.toColor(): Color {
    val hue = ((hue % 360f) + 360f) % 360f / 60f
    val chroma = value * saturation
    val x = chroma * (1f - abs(hue % 2f - 1f))
    val match = value - chroma
    val (red, green, blue) = when (hue.toInt()) {
        0 -> Triple(chroma, x, 0f)
        1 -> Triple(x, chroma, 0f)
        2 -> Triple(0f, chroma, x)
        3 -> Triple(0f, x, chroma)
        4 -> Triple(x, 0f, chroma)
        else -> Triple(chroma, 0f, x)
    }
    return Color(red + match, green + match, blue + match)
}
