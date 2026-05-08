package com.pnzgu.electronix.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun ProductImage(
    model: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
) {
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        contentScale = ContentScale.Crop,
    )
}
