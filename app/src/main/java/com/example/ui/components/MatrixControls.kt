package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MatrixBlack
import com.example.ui.theme.MatrixBlue
import com.example.ui.theme.MatrixDarkSurface
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.MatrixGreenDark
import com.example.ui.theme.MatrixGreenSubtle
import com.example.ui.theme.MatrixRed
import com.example.ui.theme.MatrixSurfaceBorder
import com.example.ui.theme.MatrixSurfaceVariant
import com.example.ui.theme.MatrixTextPrimary
import com.example.ui.theme.MatrixTextSecondary

/**
 * Precision cyber toggle switch with animated slide and high-contrast status feedback.
 */
@Composable
fun MatrixCyberSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String = "matrix_cyber_switch"
) {
    val trackBgColor by animateColorAsState(
        targetValue = if (checked) Color(0xFF10331B) else Color(0xFF141816),
        animationSpec = tween(220),
        label = "switchTrackBg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (checked) MatrixGreen else MatrixSurfaceBorder,
        animationSpec = tween(220),
        label = "switchBorder"
    )

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = tween(220),
        label = "switchThumbOffset"
    )

    val thumbColor by animateColorAsState(
        targetValue = if (checked) MatrixGreen else Color(0xFF6E8074),
        animationSpec = tween(220),
        label = "switchThumbColor"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled
            ) { onCheckedChange(!checked) }
            .testTag(testTag)
    ) {
        // Compact Status Indicator Text
        Text(
            text = if (checked) "ON" else "OFF",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = if (checked) MatrixGreen else MatrixTextSecondary,
            letterSpacing = 0.5.sp
        )

        // Switch Track
        Box(
            modifier = Modifier
                .width(46.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(trackBgColor)
                .border(1.2.dp, borderColor, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.CenterStart
        ) {
            // Sliding Thumb
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(thumbColor)
                    .border(1.dp, if (checked) Color(0xFF003816) else MatrixDarkSurface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(MatrixBlack)
                    )
                }
            }
        }
    }
}

/**
 * Highly attractive, professional full-width option card with icon, title, horizontal non-wrapping badge,
 * full description, and animated cyber switch.
 */
@Composable
fun MatrixOptionCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    badgeText: String? = null,
    badgeColor: Color = MatrixGreen,
    testTag: String = "matrix_option_card"
) {
    val borderColor by animateColorAsState(
        targetValue = if (checked) MatrixGreen.copy(alpha = 0.55f) else MatrixSurfaceBorder,
        animationSpec = tween(200),
        label = "optionCardBorder"
    )

    val cardBgColor by animateColorAsState(
        targetValue = if (checked) Color(0xFF0B1E13) else MatrixDarkSurface,
        animationSpec = tween(200),
        label = "optionCardBg"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Icon + Title/Badge + Cyber Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (icon != null) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (checked) MatrixGreen.copy(alpha = 0.15f) else MatrixBlack)
                                .border(
                                    1.dp,
                                    if (checked) MatrixGreen.copy(alpha = 0.5f) else MatrixSurfaceBorder,
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (checked) MatrixGreen else MatrixTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    // Title and Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = title,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MatrixTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (!badgeText.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(badgeColor.copy(alpha = 0.16f))
                                    .border(0.8.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = badgeColor,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                MatrixCyberSwitch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    testTag = "${testTag}_switch"
                )
            }

            // Description Row with comfortable spacing and readable line height
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 11.sp,
                color = MatrixTextSecondary,
                lineHeight = 16.sp,
                modifier = Modifier.padding(start = if (icon != null) 44.dp else 0.dp)
            )
        }
    }
}

/**
 * Option item for selecting project category, auth mode, or server preset.
 */
data class MatrixOptionItem<T>(
    val value: T,
    val label: String,
    val icon: ImageVector? = null,
    val subtitle: String? = null
)

/**
 * Attractive, professional option selector chips with icons and checkmark feedback.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> MatrixChipSelector(
    options: List<MatrixOptionItem<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = option.value == selected

            val bgColor = if (isSelected) Color(0xFF0F2B17) else MatrixDarkSurface
            val borderCol = if (isSelected) MatrixGreen else MatrixSurfaceBorder
            val textCol = if (isSelected) MatrixGreen else MatrixTextPrimary

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .border(if (isSelected) 1.2.dp else 1.dp, borderCol, RoundedCornerShape(8.dp))
                    .clickable { onSelect(option.value) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (option.icon != null) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            tint = if (isSelected) MatrixGreen else MatrixTextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Text(
                        text = option.label,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp,
                        color = textCol
                    )

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MatrixGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Rich selectable option tile for larger choices (presets, step options, runtimes).
 */
@Composable
fun <T> MatrixOptionTile(
    item: MatrixOptionItem<T>,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MatrixGreen
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else MatrixSurfaceBorder,
        animationSpec = tween(200),
        label = "tileBorder"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.08f) else MatrixDarkSurface,
        animationSpec = tween(200),
        label = "tileBg"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.2.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onSelect)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (item.icon != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) accentColor.copy(alpha = 0.16f) else MatrixBlack)
                            .border(
                                1.dp,
                                if (isSelected) accentColor.copy(alpha = 0.4f) else MatrixSurfaceBorder,
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = if (isSelected) accentColor else MatrixTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column {
                    Text(
                        text = item.label,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isSelected) accentColor else MatrixTextPrimary
                    )
                    if (item.subtitle != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 11.sp,
                            color = MatrixTextSecondary
                        )
                    }
                }
            }

            // Selection indicator
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) accentColor else Color.Transparent)
                    .border(
                        1.5.dp,
                        if (isSelected) accentColor else MatrixSurfaceBorder,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MatrixBlack,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

/**
 * Segmented options control for choosing a mutually exclusive mode.
 */
@Composable
fun <T> MatrixSegmentedOptions(
    options: List<MatrixOptionItem<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MatrixDarkSurface)
            .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val isSelected = option.value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) MatrixGreen else Color.Transparent)
                    .clickable { onSelect(option.value) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (option.icon != null) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            tint = if (isSelected) MatrixBlack else MatrixTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = option.label,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp,
                        color = if (isSelected) MatrixBlack else MatrixTextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Standard Dialog Footer with guaranteed contrast, icons, and sharp cyber styling.
 */
@Composable
fun MatrixDialogButtons(
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String = "CANCEL",
    confirmEnabled: Boolean = true,
    confirmIcon: ImageVector = Icons.Default.Check
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dismiss Button
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MatrixDarkSurface,
                contentColor = MatrixTextSecondary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MatrixTextSecondary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = dismissText,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = MatrixTextSecondary
            )
        }

        // Confirm Button
        Button(
            onClick = onConfirm,
            enabled = confirmEnabled,
            modifier = Modifier
                .weight(1.3f)
                .height(44.dp)
                .border(
                    width = 1.dp,
                    color = if (confirmEnabled) MatrixGreen else MatrixSurfaceBorder,
                    shape = RoundedCornerShape(8.dp)
                ),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (confirmEnabled) MatrixGreen else MatrixSurfaceVariant,
                contentColor = if (confirmEnabled) MatrixBlack else MatrixTextSecondary,
                disabledContainerColor = MatrixSurfaceVariant,
                disabledContentColor = MatrixTextSecondary.copy(alpha = 0.5f)
            )
        ) {
            Icon(
                imageVector = confirmIcon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (confirmEnabled) MatrixBlack else MatrixTextSecondary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = confirmText,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp,
                color = if (confirmEnabled) MatrixBlack else MatrixTextSecondary
            )
        }
    }
}
