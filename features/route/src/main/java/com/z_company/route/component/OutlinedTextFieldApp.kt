package com.z_company.route.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.Shapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlinedTextFieldApp(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = Shapes.medium,
    borderColor: Color = Color.Transparent,
    colorBackgroundEmptyField: Color = MaterialTheme.colorScheme.surface,
    colorBackgroundNotEmptyField: Color = MaterialTheme.colorScheme.secondary
) {
    val animatedBackgroundColorsNumber by animateColorAsState(
        targetValue = if (value.isEmpty()) colorBackgroundEmptyField
        else colorBackgroundNotEmptyField,
        animationSpec = tween(
            durationMillis = 200,
            easing = FastOutSlowInEasing
        )
    )

    val color = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.primary,
        unfocusedTextColor = MaterialTheme.colorScheme.primary,
        disabledTextColor = MaterialTheme.colorScheme.primary,
        unfocusedContainerColor = animatedBackgroundColorsNumber,
        focusedContainerColor = animatedBackgroundColorsNumber,
        focusedBorderColor = borderColor,
        unfocusedBorderColor = borderColor
    )

    BasicTextField(
        modifier = modifier,
        value = value,
        singleLine = singleLine,
        readOnly = readOnly,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        onValueChange = onValueChange,
        textStyle = textStyle.copy(color = MaterialTheme.colorScheme.primary),
        maxLines = maxLines,
        minLines = minLines,
    ) { innerTextField ->
        OutlinedTextFieldDefaults.DecorationBox(
            value = value,
            innerTextField = innerTextField,
            enabled = enabled,
            singleLine = singleLine,
            interactionSource = interactionSource,
            visualTransformation = VisualTransformation.None,
            label = label,
            colors = color,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            supportingText = supportingText,
            prefix = prefix,
            suffix = suffix,
            placeholder = placeholder,
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 10.dp,
                end = 16.dp,
                bottom = 10.dp
            ),
            container = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 2.dp, shape = shape) // тень только здесь!
                        .background(animatedBackgroundColorsNumber, shape)   // если нужен фон
                ) {
                    OutlinedTextFieldDefaults.ContainerBox(
                        enabled = enabled,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = color,
                        shape = shape,
                        focusedBorderThickness = 0.4.dp,
                        unfocusedBorderThickness = 0.4.dp,
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlinedTextFieldApp(
    modifier: Modifier = Modifier,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colorBackgroundEmptyField: Color = MaterialTheme.colorScheme.surface,
    colorBackgroundNotEmptyField: Color = MaterialTheme.colorScheme.secondary,
) {
    val animatedBackgroundColorsNumber by animateColorAsState(
        targetValue = if (value.text.isEmpty()) colorBackgroundEmptyField
        else colorBackgroundNotEmptyField,
        animationSpec = tween(
            durationMillis = 200,
            easing = FastOutSlowInEasing
        )
    )

    val color = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.primary,
        unfocusedTextColor = MaterialTheme.colorScheme.primary,
        unfocusedContainerColor = animatedBackgroundColorsNumber,
        focusedContainerColor = animatedBackgroundColorsNumber,
        focusedBorderColor = Color.Transparent,
        unfocusedBorderColor = Color.Transparent
    )

    BasicTextField(
        modifier = modifier.shadow(elevation = 2.dp, shape = Shapes.medium),
        value = value,
        singleLine = singleLine,
        readOnly = readOnly,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        onValueChange = onValueChange,
        textStyle = textStyle.copy(color = MaterialTheme.colorScheme.primary),
        maxLines = maxLines,
        minLines = minLines,
    ) { innerTextField ->
        OutlinedTextFieldDefaults.DecorationBox(
            value = value.text,
            innerTextField = innerTextField,
            enabled = enabled,
            singleLine = singleLine,
            interactionSource = interactionSource,
            visualTransformation = VisualTransformation.None,
            label = label,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            supportingText = supportingText,
            prefix = prefix,
            suffix = suffix,
            placeholder = placeholder,
            contentPadding = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 10.dp),
            container = {
                OutlinedTextFieldDefaults.ContainerBox(
                    enabled = enabled,
                    isError = isError,
                    interactionSource = interactionSource,
                    colors = color,
                    shape = shape,
                    focusedBorderThickness = 0.dp,
                    unfocusedBorderThickness = 0.dp
                )
            }
        )
    }
}