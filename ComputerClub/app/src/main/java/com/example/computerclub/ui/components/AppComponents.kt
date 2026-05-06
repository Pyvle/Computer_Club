package com.example.computerclub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.computerclub.ui.theme.AppBackground
import com.example.computerclub.ui.theme.AppBorder
import com.example.computerclub.ui.theme.AppSurface
import com.example.computerclub.ui.theme.AppSurfaceAlt
import com.example.computerclub.ui.theme.BrandIndigo
import com.example.computerclub.ui.theme.BrandIndigoSoft
import com.example.computerclub.ui.theme.ShapeLarge
import com.example.computerclub.ui.theme.ShapeMedium
import com.example.computerclub.ui.theme.ShapeSmall
import com.example.computerclub.ui.theme.StatusError
import com.example.computerclub.ui.theme.StatusErrorSoft
import com.example.computerclub.ui.theme.StatusInfo
import com.example.computerclub.ui.theme.StatusInfoSoft
import com.example.computerclub.ui.theme.StatusSuccess
import com.example.computerclub.ui.theme.StatusSuccessSoft
import com.example.computerclub.ui.theme.StatusWarning
import com.example.computerclub.ui.theme.StatusWarningSoft
import com.example.computerclub.ui.theme.TextMuted
import com.example.computerclub.ui.theme.TextSecondary

// --- AppCard ---

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val baseModifier = modifier
        .background(color = AppSurface, shape = ShapeLarge)
        .border(width = 1.dp, color = AppBorder, shape = ShapeLarge)

    val finalModifier = if (onClick != null) {
        baseModifier.clickable(onClick = onClick)
    } else baseModifier

    Box(modifier = finalModifier) {
        content()
    }
}

// --- AppPrimaryButton ---

@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled && !loading,
        shape = ShapeMedium,
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandIndigo,
            contentColor = Color.White,
            disabledContainerColor = BrandIndigo.copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 0.7f),
        )
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

// --- AppSecondaryButton ---

@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        shape = ShapeMedium,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = BrandIndigo,
            disabledContentColor = BrandIndigo.copy(alpha = 0.4f),
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = if (enabled) BrandIndigo else BrandIndigo.copy(alpha = 0.4f)
        )
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

// --- AppStatusChip ---

@Composable
fun AppStatusChip(label: String, tone: ChipTone) {
    val (background, foreground) = when (tone) {
        ChipTone.SUCCESS -> StatusSuccessSoft to StatusSuccess
        ChipTone.WARNING -> StatusWarningSoft to StatusWarning
        ChipTone.ERROR -> StatusErrorSoft to StatusError
        ChipTone.INFO -> StatusInfoSoft to StatusInfo
        ChipTone.NEUTRAL -> AppSurfaceAlt to TextSecondary
    }

    Box(
        modifier = Modifier
            .background(color = background, shape = ShapeSmall)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = foreground
        )
    }
}

// --- AppSectionHeader ---

@Composable
fun AppSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (action != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelLarge,
                    color = BrandIndigo
                )
            }
        }
    }
}

// --- AppTextField ---

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder.isNotEmpty()) ({ Text(placeholder, color = TextMuted) }) else null,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        isError = isError,
        supportingText = if (supportingText != null) ({ Text(supportingText) }) else null,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        singleLine = singleLine,
        shape = ShapeMedium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BrandIndigo,
            unfocusedBorderColor = AppBorder,
            focusedLabelColor = BrandIndigo,
            unfocusedLabelColor = TextSecondary,
            cursorColor = BrandIndigo,
        )
    )
}

// --- AppEmptyState ---

@Composable
fun AppEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String = "",
    action: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = TextMuted
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
        if (action != null && onActionClick != null) {
            AppSecondaryButton(
                text = action,
                onClick = onActionClick,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// --- AppScreenContainer ---

@Composable
fun AppScreenContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = AppBackground
    ) {
        content()
    }
}
