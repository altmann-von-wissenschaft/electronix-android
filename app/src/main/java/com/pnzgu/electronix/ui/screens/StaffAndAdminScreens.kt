package com.pnzgu.electronix.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pnzgu.electronix.data.local.NotificationSnapshot
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.pnzgu.electronix.AppContainer
import com.pnzgu.electronix.R
import com.pnzgu.electronix.data.dto.CategoryCharacteristicDto
import com.pnzgu.electronix.data.dto.CharacteristicDto
import com.pnzgu.electronix.data.dto.UserDto
import com.pnzgu.electronix.domain.isManager
import com.pnzgu.electronix.domain.isModerator
import com.pnzgu.electronix.ui.components.CopyableIdentifierRow
import com.pnzgu.electronix.ui.components.ElectronixCenteredLoader
import com.pnzgu.electronix.ui.components.ElectronixInlineError
import com.pnzgu.electronix.ui.components.ElectronixLoaderRing
import com.pnzgu.electronix.ui.components.ProductCatalogRowCard
import com.pnzgu.electronix.ui.components.SalesRevenueChart
import com.pnzgu.electronix.ui.components.HierarchicalCategoryPicker
import com.pnzgu.electronix.ui.components.ReviewListEntry
import com.pnzgu.electronix.ui.theme.ThemeMode
import com.pnzgu.electronix.ui.viewmodel.AdminCategoryBrowseViewModel
import com.pnzgu.electronix.ui.viewmodel.AdminCharacteristicEditViewModel
import com.pnzgu.electronix.ui.viewmodel.AdminCharacteristicsListViewModel
import com.pnzgu.electronix.ui.ManagementForegroundSync
import com.pnzgu.electronix.ui.orderStatusLabel
import com.pnzgu.electronix.ui.viewmodel.AdminCategoryEditViewModel
import com.pnzgu.electronix.ui.viewmodel.CategoryCharAssignmentRow
import com.pnzgu.electronix.ui.viewmodel.AdminOrderStatusViewModel
import com.pnzgu.electronix.ui.viewmodel.AdminOrdersListViewModel
import com.pnzgu.electronix.ui.viewmodel.AdminProductEditViewModel
import com.pnzgu.electronix.ui.viewmodel.AdminProductsListViewModel
import com.pnzgu.electronix.ui.viewmodel.AdminUserDetailViewModel
import com.pnzgu.electronix.ui.viewmodel.AdminUsersListViewModel
import com.pnzgu.electronix.ui.viewmodel.ModPendingReviewsViewModel
import com.pnzgu.electronix.ui.viewmodel.SalesReportViewModel
import com.pnzgu.electronix.ui.viewmodel.SupportUnansweredViewModel
import coil3.compose.AsyncImage
import com.pnzgu.electronix.util.contentImageUrl
import com.pnzgu.electronix.util.formatRubles
import com.pnzgu.electronix.util.parseApiDateTimeToMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportUnansweredScreen(backStackEntry: NavBackStackEntry, container: AppContainer, nav: NavController, onOpenDrawer: () -> Unit) {
    val vm: SupportUnansweredViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        factory = SupportUnansweredViewModel.factory(container),
    )
    val data by vm.data.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        container.preferences.markSupportQueueSectionVisited()
        container.requestDrawerBadgesRefresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_support_queue)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, null)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(data?.data.orEmpty(), key = { it.id }) { q ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { nav.navigate("support_answer/${q.id}") },
                ) {
                    Column {
                        ListItem(headlineContent = { Text(q.subject) }, supportingContent = { Text(q.content) })
                        CopyableIdentifierRow(
                            label = stringResource(R.string.user_uuid_label),
                            value = q.userId,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModPendingReviewsScreen(backStackEntry: NavBackStackEntry, container: AppContainer, nav: NavController, onOpenDrawer: () -> Unit) {
    val vm: ModPendingReviewsViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        factory = ModPendingReviewsViewModel.factory(container),
    )
    val data by vm.data.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    ManagementForegroundSync(lifecycleOwner = lifecycleOwner) { vm.reload() }

    LaunchedEffect(Unit) {
        container.preferences.markModReviewsSectionVisited()
        container.requestDrawerBadgesRefresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_mod_reviews)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, null)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(data?.data.orEmpty(), key = { it.id }) { r ->
                Card(Modifier.fillMaxWidth().padding(8.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        ReviewListEntry(review = r, showAuthorUserIdForStaff = true)
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(onClick = { vm.approve(r.id) }) { Text(stringResource(R.string.approve)) }
                            Button(onClick = { vm.delete(r.id) }) { Text(stringResource(R.string.delete)) }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrdersScreen(backStackEntry: NavBackStackEntry, container: AppContainer, nav: NavController, onOpenDrawer: () -> Unit) {
    val vm: AdminOrdersListViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        factory = AdminOrdersListViewModel.factory(container),
    )
    val list by vm.list.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    ManagementForegroundSync(lifecycleOwner = lifecycleOwner) { vm.reload() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_admin_orders)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, null)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(list.orEmpty(), key = { it.id }) { o ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { nav.navigate("admin_order/${o.id}") },
                ) {
                    ListItem(
                        headlineContent = {
                            val datePart = o.createdAt.substringBefore('T').ifEmpty { o.createdAt.take(10) }
                            Text(stringResource(R.string.order_summary_line, datePart))
                        },
                        supportingContent = {
                            Text("${orderStatusLabel(o.status)} · ${formatRubles(o.totalAmount)}")
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrderStatusScreen(orderId: String, backStackEntry: NavBackStackEntry, container: AppContainer, nav: NavController) {
    val vm: AdminOrderStatusViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        key = "admord_$orderId",
        factory = AdminOrderStatusViewModel.factory(container, orderId),
    )
    val state by vm.ui.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    ManagementForegroundSync(lifecycleOwner = lifecycleOwner) { vm.reload() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.update_status)) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
            )
        },
    ) { padding ->
        val order = state.order
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (order == null) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    ElectronixLoaderRing(modifier = Modifier.size(48.dp))
                }
            } else {
                CopyableIdentifierRow(
                    label = stringResource(R.string.order_uuid_label),
                    value = order.id,
                )
                CopyableIdentifierRow(
                    label = stringResource(R.string.order_customer_uuid),
                    value = order.userId,
                )
                val staffId = order.lastStatusChangedByUserId
                if (staffId != null) {
                    CopyableIdentifierRow(
                        label = stringResource(R.string.order_staff_uuid),
                        value = staffId,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(R.string.order_staff_uuid),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "—",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    "${stringResource(R.string.order_status)}: ${orderStatusLabel(order.status)}",
                    style = MaterialTheme.typography.titleMedium,
                )
                val allowed = AdminOrderStatusViewModel.allowedNextStatuses(order.status)
                if (allowed.isEmpty()) {
                    Text(
                        stringResource(R.string.admin_order_no_transitions),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    var menuOpen by remember { mutableStateOf(false) }
                    Text(
                        stringResource(R.string.admin_order_new_status),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = if (state.statusPick.isBlank()) "" else orderStatusLabel(state.statusPick),
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text(stringResource(R.string.category_picker_initial)) },
                            trailingIcon = {
                                IconButton(onClick = { menuOpen = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        )
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            allowed.forEach { code ->
                                DropdownMenuItem(
                                    text = { Text(orderStatusLabel(code)) },
                                    onClick = {
                                        vm.setStatusPick(code)
                                        menuOpen = false
                                    },
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    state.notes,
                    { vm.setNotes(it) },
                    label = { Text(stringResource(R.string.admin_order_note)) },
                    placeholder = { Text(stringResource(R.string.admin_order_note_cancel_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                )
                state.saveError?.let { err ->
                    val msg = when (err) {
                        "__pick__" -> stringResource(R.string.admin_order_error_pick)
                        "__cancel_note__" -> stringResource(R.string.admin_order_error_cancel_note)
                        else -> err
                    }
                    Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (allowed.isNotEmpty()) {
                    Button(
                        onClick = { vm.save() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.admin_order_apply_status)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(backStackEntry: NavBackStackEntry, container: AppContainer, nav: NavController, onOpenDrawer: () -> Unit) {
    val vm: AdminUsersListViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        factory = AdminUsersListViewModel.factory(container),
    )
    val ui by vm.ui.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_admin_users)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = ui.query,
                onValueChange = vm::setQuery,
                label = { Text(stringResource(R.string.admin_users_search_hint)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !ui.busy,
                singleLine = true,
            )
            ui.error?.let { err ->
                val msg = when (err) {
                    "__empty__" -> stringResource(R.string.admin_users_empty_query)
                    "__invalid__" -> stringResource(R.string.admin_users_invalid_uuid)
                    "__not_found__" -> stringResource(R.string.admin_users_not_found)
                    else -> err
                }
                Text(
                    msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(
                onClick = { vm.find { id -> nav.navigate("admin_user/$id") } },
                enabled = !ui.busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (ui.busy) stringResource(R.string.loading) else stringResource(R.string.admin_users_find))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserDetailScreen(
    userId: String,
    backStackEntry: NavBackStackEntry,
    container: AppContainer,
    nav: NavController,
    canManageRoles: Boolean,
) {
    val vm: AdminUserDetailViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        key = "adu_$userId",
        factory = AdminUserDetailViewModel.factory(container, userId),
    )
    val state by vm.ui.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_admin_users)) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
            )
        },
    ) { padding ->
        val u = state.user
        if (u == null) {
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                ElectronixCenteredLoader()
            }
        } else {
            Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CopyableIdentifierRow(
                    label = stringResource(R.string.user_uuid_label),
                    value = u.id,
                )
                Text(u.email)
                Text("${stringResource(R.string.roles)}: ${u.roles.joinToString()}")
                u.roleAssignments.forEach { ra ->
                    val whenLabel = ra.assignedAt.take(19).replace('T', ' ')
                    Text(
                        text = if (ra.assignedByUserId.isNullOrBlank()) {
                            stringResource(R.string.role_assignment_system, ra.roleCode, whenLabel)
                        } else {
                            stringResource(
                                R.string.role_assignment_admin,
                                ra.roleCode,
                                ra.assignedByUserId,
                                whenLabel,
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Button(onClick = { vm.toggleBlock() }) {
                    Text(if (u.isBlocked) stringResource(R.string.unblock) else stringResource(R.string.block))
                }
                if (canManageRoles) {
                    OutlinedTextField(state.roleCode, { vm.setRoleCode(it) }, label = { Text(stringResource(R.string.assign_role)) })
                    Button(onClick = { vm.assignRole() }) { Text(stringResource(R.string.assign_role)) }
                    u.roles.forEach { rc ->
                        Button(onClick = { vm.removeRole(rc) }) { Text("${stringResource(R.string.remove_role)} $rc") }
                    }
                }
            }
        }
    }
}

private fun notifyAdminCategoriesReload(nav: NavController) {
    nav.previousBackStackEntry?.savedStateHandle?.set(
        "admin_categories_reload",
        System.currentTimeMillis(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCategoriesScreen(
    parentKey: String,
    backStackEntry: NavBackStackEntry,
    container: AppContainer,
    nav: NavController,
    onOpenDrawer: () -> Unit,
) {
    val vm: AdminCategoryBrowseViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        key = "admin_cat_$parentKey",
        factory = AdminCategoryBrowseViewModel.factory(container, parentKey),
    )
    val state by vm.ui.collectAsStateWithLifecycle()
    val reloadSignal by backStackEntry.savedStateHandle
        .getStateFlow("admin_categories_reload", 0L)
        .collectAsStateWithLifecycle()
    LaunchedEffect(reloadSignal) {
        if (reloadSignal > 0L) vm.reload()
    }

    val title = state.title ?: stringResource(R.string.nav_admin_categories)
    val listReady = state.categories != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    if (parentKey == "ROOT") {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = null)
                        }
                    } else {
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (listReady && state.error == null) {
                FloatingActionButton(
                    onClick = {
                        val segment = state.createParentId ?: AdminCategoryEditViewModel.CREATE_PARENT_NONE
                        nav.navigate("admin_category_edit/create/$segment")
                    },
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Default.Add, stringResource(R.string.add_category))
                }
            }
        },
    ) { padding ->
        when {
            state.categories == null && state.error == null ->
                Box(
                    Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    ElectronixCenteredLoader()
                }
            state.categories == null && state.error != null -> Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                ElectronixInlineError(
                    message = state.error!!,
                    onRetry = { vm.reload() },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            else -> LazyColumn(
                Modifier.padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.categories.isNullOrEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(state.categories.orEmpty(), key = { it.id }) { c ->
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            Row(
                                Modifier
                                    .weight(1f)
                                    .clickable { nav.navigate("admin_categories/${c.id}") }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            ) {
                                ListItem(
                                    headlineContent = {
                                        Text(c.name, style = MaterialTheme.typography.titleMedium)
                                    },
                                    leadingContent = {
                                        Icon(
                                            Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    },
                                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                )
                            }
                            IconButton(
                                onClick = { nav.navigate("admin_category_edit/${c.id}") },
                            ) {
                                Icon(Icons.Default.Edit, stringResource(R.string.edit_category))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryCharacteristicAssignmentRow(
    row: CategoryCharAssignmentRow,
    rowIndex: Int,
    catalog: List<CharacteristicDto>,
    takenElsewhere: Set<String>,
    onPick: (Int, String) -> Unit,
    onRequired: (Int, Boolean) -> Unit,
    onRemove: (Int) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val choices = catalog.filter { ch ->
        ch.id == row.selectedCharacteristicId || ch.id !in takenElsewhere
    }
    val selected = catalog.find { it.id == row.selectedCharacteristicId }
    val display = selected?.let { "${it.name} (${it.unit})" }
        ?: stringResource(R.string.category_char_select)
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.weight(1f)) {
            OutlinedTextField(
                value = display,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { menuOpen = true }, enabled = catalog.isNotEmpty()) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                modifier = Modifier.fillMaxWidth(),
            ) {
                choices.forEach { ch ->
                    DropdownMenuItem(
                        text = { Text("${ch.name} (${ch.unit})") },
                        onClick = {
                            onPick(rowIndex, ch.id)
                            menuOpen = false
                        },
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.category_char_required),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Switch(row.isRequired, { onRequired(rowIndex, it) })
        }
        IconButton(onClick = { onRemove(rowIndex) }) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCategoryEditScreen(
    backStackEntry: NavBackStackEntry,
    container: AppContainer,
    nav: NavController,
    mode: AdminCategoryEditViewModel.CategoryEditMode,
) {
    val vmKey = when (mode) {
        is AdminCategoryEditViewModel.CategoryEditMode.Create ->
            "adcat_create_${mode.parentId ?: "root"}"
        is AdminCategoryEditViewModel.CategoryEditMode.Edit ->
            "adcat_edit_${mode.categoryId}"
    }
    val vm: AdminCategoryEditViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        key = vmKey,
        factory = AdminCategoryEditViewModel.factory(container, mode),
    )
    val state by vm.ui.collectAsStateWithLifecycle()
    val topTitle =
        if (vm.isNew) stringResource(R.string.add_category) else stringResource(R.string.edit_category)

    fun afterSave() {
        notifyAdminCategoriesReload(nav)
        nav.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(topTitle, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                state.name,
                { vm.setName(it) },
                label = { Text(stringResource(R.string.name)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
            OutlinedTextField(
                state.displayOrder,
                { vm.setDisplayOrder(it) },
                label = { Text(stringResource(R.string.display_order)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
            Text(
                stringResource(R.string.category_chars_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            if (state.characteristicCatalog.isEmpty()) {
                Text(
                    stringResource(R.string.nav_admin_characteristics),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.assignedRows.forEachIndexed { index, row ->
                val taken = state.assignedRows.mapIndexedNotNull { i, r ->
                    if (i != index) r.selectedCharacteristicId else null
                }.toSet()
                CategoryCharacteristicAssignmentRow(
                    row = row,
                    rowIndex = index,
                    catalog = state.characteristicCatalog,
                    takenElsewhere = taken,
                    onPick = { i, id -> vm.setRowCharacteristic(i, id) },
                    onRequired = { i, req -> vm.setRowRequired(i, req) },
                    onRemove = { i -> vm.removeCharacteristicRow(i) },
                )
            }
            TextButton(
                onClick = { vm.addCharacteristicRow() },
                enabled = state.characteristicCatalog.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.category_add_char))
                }
            }
            if (vm.isNew) {
                Button(
                    onClick = { vm.create { afterSave() } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.create)) }
            } else {
                Button(
                    onClick = { vm.save { afterSave() } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.save)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProductsScreen(backStackEntry: NavBackStackEntry, container: AppContainer, nav: NavController, onOpenDrawer: () -> Unit) {
    val vm: AdminProductsListViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        factory = AdminProductsListViewModel.factory(container),
    )
    val state by vm.ui.collectAsStateWithLifecycle()
    val pullRefreshing = state.loading && state.items.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_admin_products)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, null)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { nav.navigate("admin_product/new") },
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Add, stringResource(R.string.add_product))
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = pullRefreshing,
            onRefresh = { vm.refresh() },
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            Column(Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = vm::onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    placeholder = { Text(stringResource(R.string.admin_products_search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
                state.error?.takeIf { state.items.isNotEmpty() }?.let { msg ->
                    ElectronixInlineError(
                        message = msg,
                        onRetry = { vm.retryAfterError() },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                when {
                    state.loading && state.items.isEmpty() -> ElectronixCenteredLoader()
                    state.error != null && state.items.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            ElectronixInlineError(
                                message = state.error!!,
                                onRetry = { vm.retryAfterError() },
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                        }
                    }
                    state.items.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(R.string.empty),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    else -> LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp, top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.items, key = { it.id }) { p ->
                            ProductCatalogRowCard(
                                product = p,
                                contentBaseUrl = container.contentBaseUrl,
                                priceLabel = stringResource(R.string.price),
                                stockLabel = stringResource(R.string.stock),
                                onClick = { nav.navigate("admin_product/${p.id}") },
                            )
                        }
                        item {
                            if (state.hasMore && !state.loadingMore) {
                                Button(
                                    onClick = { vm.loadMore() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    Text(stringResource(R.string.admin_products_load_more))
                                }
                            }
                            if (state.loadingMore) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    ElectronixLoaderRing(modifier = Modifier.size(36.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProductEditScreen(productKey: String, backStackEntry: NavBackStackEntry, container: AppContainer, nav: NavController) {
    val vm: AdminProductEditViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        key = "adprod_$productKey",
        factory = AdminProductEditViewModel.factory(container, productKey),
    )
    val state by vm.ui.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val pickImages = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents(),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            val blobs = withContext(Dispatchers.IO) {
                uris.mapIndexedNotNull { idx, uri ->
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream -> stream.readBytes() }
                            ?.let { bytes -> bytes to "photo_${idx}.jpg" }
                    } catch (_: Exception) {
                        null
                    }
                }
            }
            if (blobs.isNotEmpty()) vm.uploadPhotos(blobs)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_admin_products)) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                state.name,
                { vm.setName(it) },
                label = { Text(stringResource(R.string.name)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
            OutlinedTextField(
                state.description,
                { vm.setDescription(it) },
                label = { Text(stringResource(R.string.description)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
            OutlinedTextField(
                state.price,
                { vm.setPrice(it) },
                label = { Text(stringResource(R.string.price)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
            OutlinedTextField(
                state.stock,
                { vm.setStock(it) },
                label = { Text(stringResource(R.string.stock)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
            if (vm.isNew) {
                HierarchicalCategoryPicker(
                    container = container,
                    enabled = true,
                    onLeafCategoryId = { id -> vm.setCategoryId(id ?: "") },
                )
            } else {
                Text(
                    "${stringResource(R.string.category)}: ${state.categoryDisplayName.ifBlank { "—" }}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.categoryCharacteristics.isNotEmpty()) {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            stringResource(R.string.characteristics_values),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        state.categoryCharacteristics.forEach { ch: CategoryCharacteristicDto ->
                            val value = state.characteristicValues[ch.characteristicId].orEmpty()
                            OutlinedTextField(
                                value = value,
                                onValueChange = { newValue: String ->
                                    vm.setCharacteristicValue(ch.characteristicId, newValue)
                                },
                                label = {
                                    Text(
                                        if (ch.isRequired) "${ch.characteristicName} (${ch.unit}) *"
                                        else "${ch.characteristicName} (${ch.unit})",
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                        }
                    }
                }
            }
            state.formError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            if (!vm.isNew && state.loaded != null) {
                val loaded = state.loaded!!
                val previewUrls = remember(loaded.id, loaded.mainImagePath, loaded.imagePaths, container.contentBaseUrl) {
                    buildList {
                        contentImageUrl(container.contentBaseUrl, loaded.mainImagePath)?.let { add(it) }
                        loaded.imagePaths.forEach { path ->
                            contentImageUrl(container.contentBaseUrl, path)?.let { u -> if (!contains(u)) add(u) }
                        }
                    }
                }
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            stringResource(R.string.admin_product_photos),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(R.string.admin_product_photos_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            itemsIndexed(previewUrls) { _, url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(RoundedCornerShape(14.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                            item {
                                FilledTonalButton(
                                    onClick = { pickImages.launch("image/*") },
                                    enabled = !state.uploadBusy,
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    Icon(
                                        Icons.Outlined.AddPhotoAlternate,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 8.dp),
                                    )
                                    Text(stringResource(R.string.admin_product_add_photos))
                                }
                            }
                        }
                        state.uploadError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (vm.isNew) {
                Button(
                    onClick = {
                        vm.create { id ->
                            nav.navigate("admin_product/$id") {
                                popUpTo("admin_products") { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.categoryId.isNotBlank(),
                ) { Text(stringResource(R.string.create)) }
            } else {
                Button(
                    onClick = { vm.save { nav.popBackStack() } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.save)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCharacteristicsScreen(backStackEntry: NavBackStackEntry, container: AppContainer, nav: NavController, onOpenDrawer: () -> Unit) {
    val vm: AdminCharacteristicsListViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        factory = AdminCharacteristicsListViewModel.factory(container),
    )
    val page by vm.page.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_admin_characteristics)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, null)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { nav.navigate("admin_char/new") },
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Add, stringResource(R.string.add_characteristic))
            }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(page?.data.orEmpty(), key = { it.id }) { c ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { nav.navigate("admin_char/${c.id}") },
                ) {
                    ListItem(headlineContent = { Text(c.name) }, supportingContent = { Text(c.unit) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCharacteristicEditScreen(charId: String, backStackEntry: NavBackStackEntry, container: AppContainer, nav: NavController) {
    val vm: AdminCharacteristicEditViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        key = "adchar_$charId",
        factory = AdminCharacteristicEditViewModel.factory(container, charId),
    )
    val state by vm.ui.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_admin_characteristics)) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(state.name, { vm.setName(it) }, label = { Text(stringResource(R.string.name)) })
            OutlinedTextField(state.unit, { vm.setUnit(it) }, label = { Text(stringResource(R.string.unit)) })
            if (vm.isNew) {
                Button(onClick = { vm.create { nav.popBackStack() } }) { Text(stringResource(R.string.create)) }
            } else {
                Button(onClick = { vm.save { nav.popBackStack() } }) { Text(stringResource(R.string.save)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesReportScreen(backStackEntry: NavBackStackEntry, container: AppContainer, nav: NavController, onOpenDrawer: () -> Unit) {
    val vm: SalesReportViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        factory = SalesReportViewModel.factory(container),
    )
    val state by vm.ui.collectAsStateWithLifecycle()
    var showStart by remember { mutableStateOf(false) }
    var showEnd by remember { mutableStateOf(false) }
    var methodExpanded by remember { mutableStateOf(false) }

    if (showStart) {
        key(state.selectionStartMillis) {
            val dp = rememberDatePickerState(initialSelectedDateMillis = state.selectionStartMillis)
            DatePickerDialog(
                onDismissRequest = { showStart = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            dp.selectedDateMillis?.let { vm.setSelectionStartMillis(it) }
                            showStart = false
                        },
                    ) { Text(stringResource(R.string.dialog_ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { showStart = false }) { Text(stringResource(R.string.dialog_cancel)) }
                },
            ) {
                DatePicker(state = dp)
            }
        }
    }
    if (showEnd) {
        key(state.selectionEndMillis) {
            val dp = rememberDatePickerState(initialSelectedDateMillis = state.selectionEndMillis)
            DatePickerDialog(
                onDismissRequest = { showEnd = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            dp.selectedDateMillis?.let { vm.setSelectionEndMillis(it) }
                            showEnd = false
                        },
                    ) { Text(stringResource(R.string.dialog_ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { showEnd = false }) { Text(stringResource(R.string.dialog_cancel)) }
                },
            ) {
                DatePicker(state = dp)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sales_report)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.sales_report_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = { showStart = true },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                            ) {
                                Icon(Icons.Default.Event, contentDescription = null, Modifier.padding(end = 8.dp))
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        stringResource(R.string.sales_report_period_start),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        formatRuDateCompactFromMillis(state.selectionStartMillis),
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = { showEnd = true },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                            ) {
                                Icon(Icons.Default.Event, contentDescription = null, Modifier.padding(end = 8.dp))
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        stringResource(R.string.sales_report_period_end),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        formatRuDateCompactFromMillis(state.selectionEndMillis),
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }

                    state.periodError?.let { code ->
                        val msg = when (code) {
                            "__period_invalid__" -> stringResource(R.string.sales_report_period_invalid)
                            else -> code
                        }
                        Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    state.loadError?.let { err ->
                        ElectronixInlineError(
                            message = err,
                            onRetry = { vm.reload() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Button(
                        onClick = { vm.reload() },
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        if (state.loading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                ElectronixLoaderRing(modifier = Modifier.size(22.dp))
                                Text(stringResource(R.string.loading))
                            }
                        } else {
                            Text(stringResource(R.string.sales_report_apply))
                        }
                    }
                }
            }

            if (state.loading && state.report == null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ElectronixLoaderRing(modifier = Modifier.size(52.dp))
                }
            }

            state.report?.let { r ->
                Text(
                    stringResource(
                        R.string.sales_report_period_server,
                        formatRuDateFromIso(r.period.startDate),
                        formatRuDateFromIso(r.period.endDate),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(
                                    stringResource(R.string.sales_report_kpi_orders),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    "${r.totalOrders}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    stringResource(R.string.sales_report_kpi_revenue),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    formatRubles(r.totalRevenue),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        HorizontalDivider()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                stringResource(R.string.sales_report_kpi_aov),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                formatRubles(r.averageOrderValue),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }

                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.sales_report_chart_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            if (r.granularity.equals("month", ignoreCase = true)) {
                                stringResource(R.string.sales_report_series_hint_month)
                            } else {
                                stringResource(R.string.sales_report_series_hint_day)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SalesRevenueChart(
                            series = r.series,
                            granularity = r.granularity,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }

                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        TextButton(onClick = { methodExpanded = !methodExpanded }) {
                            Text(stringResource(R.string.sales_report_method_title))
                        }
                        if (methodExpanded) {
                            Text(
                                stringResource(R.string.sales_report_method_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatRuDateFromMillis(millis: Long): String {
    val d = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
    return DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru")).format(d)
}

/** Compact numeric date for tight layouts (e.g. period pickers). */
private fun formatRuDateCompactFromMillis(millis: Long): String {
    val d = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
    return DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale("ru")).format(d)
}

private fun formatRuDateFromIso(iso: String): String {
    val ms = parseApiDateTimeToMillis(iso)
    return if (ms == 0L) iso else formatRuDateFromMillis(ms)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    user: UserDto?,
    onOpenDrawer: () -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
) {
    val themeMode by container.preferences.themeModeFlow.collectAsStateWithLifecycle(initialValue = ThemeMode.System)
    val notif by container.preferences.notificationSnapshotFlow.collectAsStateWithLifecycle(
        initialValue = NotificationSnapshot(),
    )
    val scope = rememberCoroutineScope()
    var themeMenuOpen by remember { mutableStateOf(false) }
    val themeOptions = listOf(
        ThemeMode.System to stringResource(R.string.theme_system),
        ThemeMode.Light to stringResource(R.string.theme_light),
        ThemeMode.Dark to stringResource(R.string.theme_dark),
    )
    val themeLabel = themeOptions.firstOrNull { it.first == themeMode }?.second
        ?: stringResource(R.string.theme_system)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_settings)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(stringResource(R.string.theme_title), style = MaterialTheme.typography.titleMedium)
            Box(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = themeLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.theme_title)) },
                    trailingIcon = {
                        IconButton(onClick = { themeMenuOpen = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                )
                DropdownMenu(
                    expanded = themeMenuOpen,
                    onDismissRequest = { themeMenuOpen = false },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    themeOptions.forEach { (mode, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                themeMenuOpen = false
                                onThemeSelected(mode)
                            },
                        )
                    }
                }
            }

            if (user != null) {
                Text(stringResource(R.string.notifications_section), style = MaterialTheme.typography.titleMedium)
                    NotificationSwitchRow(
                        title = stringResource(R.string.notif_order_status),
                        subtitle = stringResource(R.string.notif_order_status_desc),
                        checked = notif.notifyOrderStatus,
                        onCheckedChange = { v ->
                            scope.launch {
                                container.preferences.setNotifyOrderStatus(v)
                                container.pushSync.syncPushPreferencesToServer()
                            }
                        },
                    )
                NotificationSwitchRow(
                    title = stringResource(R.string.notif_support_reply),
                    subtitle = stringResource(R.string.notif_support_reply_desc),
                    checked = notif.notifySupportReply,
                    onCheckedChange = { v ->
                        scope.launch {
                            container.preferences.setNotifySupportReply(v)
                            container.pushSync.syncPushPreferencesToServer()
                        }
                    },
                )
                if (user.isModerator()) {
                    NotificationSwitchRow(
                        title = stringResource(R.string.notif_review_moderation),
                        subtitle = stringResource(R.string.notif_review_moderation_desc),
                        checked = notif.notifyReviewModeration,
                        onCheckedChange = { v ->
                            scope.launch {
                                container.preferences.setNotifyReviewModeration(v)
                                container.pushSync.syncPushPreferencesToServer()
                            }
                        },
                    )
                }
                if (user.isManager()) {
                    NotificationSwitchRow(
                        title = stringResource(R.string.notif_support_queue),
                        subtitle = stringResource(R.string.notif_support_queue_desc),
                        checked = notif.notifySupportQueue,
                        onCheckedChange = { v ->
                            scope.launch {
                                container.preferences.setNotifySupportQueue(v)
                                container.pushSync.syncPushPreferencesToServer()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
