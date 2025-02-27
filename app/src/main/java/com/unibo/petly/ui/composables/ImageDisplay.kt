package com.unibo.petly.ui.composables

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun ImageDisplay(
    uri: Uri?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    defaultHigh: Dp = 80.dp,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary // Parametro tint
) {
    if (uri != null) {
        AsyncImage(
            model = uri,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        // If there isn't a valid uri, display a default image
        Image(
            Icons.Outlined.Image,
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(tint), // Usa il parametro tint
            modifier = modifier
                .background(MaterialTheme.colorScheme.secondary)
                .padding(defaultHigh/2)
        )
    }
}