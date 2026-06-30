package io.github.itsmelissadev.swiftsense.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ColorOption(val name: String, val color: Color)

val PresetColors = listOf(
    ColorOption("White", Color(0xFFFFFFFF)),
    ColorOption("Gray", Color(0xFF9E9E9E)),
    ColorOption("Red", Color(0xFFF44336)),
    ColorOption("Pink", Color(0xFFE91E63)),
    ColorOption("Purple", Color(0xFF9C27B0)),
    ColorOption("Deep Purple", Color(0xFF673AB7)),
    ColorOption("Indigo", Color(0xFF3F51B5)),
    ColorOption("Blue", Color(0xFF2196F3)),
    ColorOption("Light Blue", Color(0xFF03A9F4)),
    ColorOption("Cyan", Color(0xFF00BCD4)),
    ColorOption("Teal", Color(0xFF009688)),
    ColorOption("Green", Color(0xFF4CAF50)),
    ColorOption("Light Green", Color(0xFF8BC34A)),
    ColorOption("Lime", Color(0xFFCDDC39)),
    ColorOption("Yellow", Color(0xFFFFEB3B)),
    ColorOption("Amber", Color(0xFFFFC107)),
    ColorOption("Orange", Color(0xFFFF9800)),
    ColorOption("Deep Orange", Color(0xFFFF5722)),
    ColorOption("Brown", Color(0xFF795548)),
    ColorOption("Blue Gray", Color(0xFF607D8B)),
    ColorOption("Neon Cyan", Color(0xFF00E6FF)),
    ColorOption("Neon Pink", Color(0xFFFF00FF)),
    ColorOption("Neon Green", Color(0xFF00FF55)),
    ColorOption("Gold", Color(0xFFFFD700))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerDialog(
    title: String,
    initialColor: Int,
    onColorSelected: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(PresetColors) { option ->
                    val colorInt = option.color.toArgb()
                    val isSelected = colorInt == initialColor
                    
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(option.color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                            .clickable {
                                onColorSelected(colorInt)
                                onDismissRequest()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✓",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(androidx.compose.ui.res.stringResource(io.github.itsmelissadev.swiftsense.R.string.aod_dialog_close))
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface
    )
}
