package com.pnzgu.electronix.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pnzgu.electronix.AppContainer
import com.pnzgu.electronix.R
import com.pnzgu.electronix.data.dto.CategoryDto
import kotlinx.coroutines.launch

@Composable
fun HierarchicalCategoryPicker(
    container: AppContainer,
    enabled: Boolean,
    onLeafCategoryId: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var options by remember { mutableStateOf<List<CategoryDto>>(emptyList()) }
    val initialHint = stringResource(R.string.category_picker_initial)
    val subHint = stringResource(R.string.category_picker_choose_sub)
    var displayText by remember { mutableStateOf(initialHint) }
    val menuOpen = expanded && enabled && options.isNotEmpty()
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(Unit) {
        runCatching { container.catalogRepository.categoriesForCatalogRoot() }
            .onSuccess { options = it }
    }

    fun applyRootList() {
        scope.launch {
            displayText = initialHint
            runCatching { container.catalogRepository.categoriesForCatalogRoot() }
                .onSuccess {
                    options = it
                    onLeafCategoryId(null)
                }
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            OutlinedTextField(
                value = displayText,
                onValueChange = {},
                readOnly = true,
                enabled = enabled && options.isNotEmpty(),
                label = { Text(stringResource(R.string.category)) },
                trailingIcon = {
                    IconButton(
                        onClick = { if (enabled && options.isNotEmpty()) expanded = !expanded },
                        enabled = enabled && options.isNotEmpty(),
                    ) {
                        Icon(
                            if (menuOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled && options.isNotEmpty(),
                    ) { expanded = true },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            )
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { expanded = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                options.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat.name, style = MaterialTheme.typography.bodyLarge) },
                        onClick = {
                            expanded = false
                            scope.launch {
                                runCatching { container.catalogRepository.categories(cat.id) }
                                    .onSuccess { children ->
                                        if (children.isEmpty()) {
                                            displayText = cat.name
                                            options = listOf(cat)
                                            onLeafCategoryId(cat.id)
                                        } else {
                                            displayText = subHint
                                            options = children
                                            onLeafCategoryId(null)
                                        }
                                    }
                            }
                        },
                    )
                }
            }
        }
        IconButton(
            onClick = { applyRootList() },
            enabled = enabled,
        ) {
            Icon(
                Icons.Filled.RestartAlt,
                contentDescription = stringResource(R.string.category_picker_reset),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
