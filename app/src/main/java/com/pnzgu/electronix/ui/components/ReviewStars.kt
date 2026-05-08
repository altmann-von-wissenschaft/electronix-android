package com.pnzgu.electronix.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pnzgu.electronix.R
import com.pnzgu.electronix.data.dto.ReviewDto

private val starGray: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

@Composable
fun ReviewRatingStarsDisplay(
    rating: Int,
    modifier: Modifier = Modifier,
    starSize: Dp = 18.dp,
    maxStars: Int = 5,
) {
    Row(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(maxStars) { index ->
            val filled = index < rating.coerceIn(0, maxStars)
            Icon(
                imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                modifier = Modifier.size(starSize),
                tint = if (filled) starGray else starGray.copy(alpha = 0.35f),
            )
        }
    }
}

@Composable
fun ReviewRatingStarsInteractive(
    rating: Int,
    onRatingSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 36.dp,
    maxStars: Int = 5,
) {
    Row(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (star in 1..maxStars) {
            val filled = rating > 0 && star <= rating
            IconButton(
                onClick = { onRatingSelected(star) },
                modifier = Modifier.size(starSize + 8.dp),
            ) {
                Icon(
                    imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = null,
                    modifier = Modifier.size(starSize),
                    tint = if (filled) starGray else starGray.copy(alpha = 0.35f),
                )
            }
        }
    }
}

@Composable
fun ReviewListEntry(
    review: ReviewDto,
    modifier: Modifier = Modifier,
    showAuthorUserIdForStaff: Boolean = false,
    footer: @Composable () -> Unit = {},
) {
    val nickname = review.authorNickname?.takeIf { it.isNotBlank() } ?: "—"
    Column(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ReviewRatingStarsDisplay(rating = review.rating)
            Text(
                nickname,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (showAuthorUserIdForStaff) {
            Spacer(Modifier.height(10.dp))
            CopyableIdentifierRow(
                label = stringResource(R.string.review_author_uuid_for_staff),
                value = review.userId,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(review.title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            review.content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        footer()
    }
}
