package com.pnzgu.electronix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.pnzgu.electronix.AppContainer
import com.pnzgu.electronix.R
import com.pnzgu.electronix.data.dto.UserDto
import com.pnzgu.electronix.domain.isStoreStaff
import com.pnzgu.electronix.ui.ManagementForegroundSync
import com.pnzgu.electronix.ui.components.CopyableIdentifierRow
import com.pnzgu.electronix.ui.components.ElectronixCenteredLoader
import com.pnzgu.electronix.ui.components.ElectronixInlineError
import com.pnzgu.electronix.ui.components.ReviewListEntry
import com.pnzgu.electronix.ui.orderStatusLabel
import com.pnzgu.electronix.ui.components.ReviewRatingStarsInteractive
import com.pnzgu.electronix.util.formatRubles
import com.pnzgu.electronix.util.maskEmailForDisplay
import com.pnzgu.electronix.ui.viewmodel.CartViewModel
import com.pnzgu.electronix.ui.viewmodel.LoginViewModel
import com.pnzgu.electronix.ui.viewmodel.OrderDetailViewModel
import com.pnzgu.electronix.ui.viewmodel.OrdersViewModel
import com.pnzgu.electronix.ui.viewmodel.ProfileViewModel
import com.pnzgu.electronix.ui.viewmodel.RegisterViewModel
import com.pnzgu.electronix.ui.viewmodel.ReviewCreateViewModel
import com.pnzgu.electronix.ui.viewmodel.SupportAnswerViewModel
import com.pnzgu.electronix.ui.viewmodel.SupportCreateViewModel
import com.pnzgu.electronix.ui.viewmodel.SupportMyViewModel
import com.pnzgu.electronix.ui.viewmodel.SupportQuestionDetailViewModel
import com.pnzgu.electronix.data.dto.OrderDto
import kotlinx.coroutines.launch

private fun NavController.popOrGoToCatalogRoot() {
    if (!popBackStack()) {
        navigate("categories/ROOT") {
            popUpTo(graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}

@Composable
private fun orderRowTitle(order: OrderDto): String {
    val datePart = order.createdAt.substringBefore('T').ifEmpty { order.createdAt.take(10) }
    return stringResource(R.string.order_summary_line, datePart)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    backStackEntry: NavBackStackEntry,
    container: AppContainer,
    nav: NavController,
    onOpenDrawer: () -> Unit,
    onLoggedIn: () -> Unit,
) {
    val vm: LoginViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        factory = LoginViewModel.factory(container),
    )
    val email by vm.email.collectAsStateWithLifecycle()
    val password by vm.password.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    var showPassword by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_login)) },
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
                .padding(horizontal = 20.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    OutlinedTextField(
                        email,
                        { vm.setEmail(it) },
                        label = { Text(stringResource(R.string.email)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                    OutlinedTextField(
                        password,
                        { vm.setPassword(it) },
                        label = { Text(stringResource(R.string.password)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPassword) {
                                        stringResource(R.string.hide_password)
                                    } else {
                                        stringResource(R.string.show_password)
                                    },
                                )
                            }
                        },
                    )
                    error?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Button(
                        onClick = { vm.login(onLoggedIn) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    ) { Text(stringResource(R.string.login_action)) }
                    TextButton(
                        onClick = { nav.navigate("register") },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.nav_register)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(backStackEntry: NavBackStackEntry, container: AppContainer, nav: NavController) {
    val vm: RegisterViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        factory = RegisterViewModel.factory(container),
    )
    val email by vm.email.collectAsStateWithLifecycle()
    val password by vm.password.collectAsStateWithLifecycle()
    val nickname by vm.nickname.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var showPassword by remember { mutableStateOf(false) }

    BackHandler { nav.popOrGoToCatalogRoot() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_register)) },
                navigationIcon = {
                    IconButton(onClick = { nav.popOrGoToCatalogRoot() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 20.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.register_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    OutlinedTextField(
                        email,
                        { vm.setEmail(it) },
                        label = { Text(stringResource(R.string.email)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                    OutlinedTextField(
                        password,
                        { vm.setPassword(it) },
                        label = { Text(stringResource(R.string.password)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPassword) {
                                        stringResource(R.string.hide_password)
                                    } else {
                                        stringResource(R.string.show_password)
                                    },
                                )
                            }
                        },
                    )
                    OutlinedTextField(
                        nickname,
                        { vm.setNickname(it) },
                        label = { Text(stringResource(R.string.nickname)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                    error?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Button(
                        onClick = {
                            vm.register {
                                nav.navigate("login") {
                                    popUpTo("register") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    ) { Text(if (busy) stringResource(R.string.loading) else stringResource(R.string.register_action)) }
                    TextButton(
                        onClick = { nav.popOrGoToCatalogRoot() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.nav_catalog)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: UserDto?,
    backStackEntry: NavBackStackEntry,
    container: AppContainer,
    nav: NavController,
    onOpenDrawer: () -> Unit,
    onLogout: () -> Unit,
) {
    val vm: ProfileViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        factory = ProfileViewModel.factory(container),
    )
    val changePasswordBusy by vm.changePasswordBusy.collectAsStateWithLifecycle()
    val changePasswordError by vm.changePasswordError.collectAsStateWithLifecycle()
    val changePasswordSuccess by vm.changePasswordSuccess.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    LaunchedEffect(changePasswordSuccess) {
        val msg = changePasswordSuccess ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
        showPasswordDialog = false
        currentPassword = ""
        newPassword = ""
        confirmNewPassword = ""
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_profile)) },
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
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (user == null) {
                Text("—")
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Text(
                            user.nickname?.takeIf { it.isNotBlank() } ?: stringResource(R.string.nickname),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            maskEmailForDisplay(user.email),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        CopyableIdentifierRow(
                            label = stringResource(R.string.profile_user_uuid),
                            value = user.id,
                        )
                        HorizontalDivider()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            val active = !user.isBlocked
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (active) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.errorContainer,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (active) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
                                    contentDescription = null,
                                    tint = if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                            Column {
                                Text(
                                    stringResource(R.string.profile_account_status),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    if (active) stringResource(R.string.profile_status_active)
                                    else stringResource(R.string.profile_status_blocked),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        if (user.isStoreStaff()) {
                            val roleManager = stringResource(R.string.role_manager)
                            val roleModerator = stringResource(R.string.role_moderator)
                            val roleAdministrator = stringResource(R.string.role_administrator)
                            val roleGuest = stringResource(R.string.role_guest)
                            HorizontalDivider()
                            Text(
                                stringResource(R.string.profile_staff_roles),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            val staffRoles = user.roles.filter { r ->
                                r.equals("MANAGER", true) ||
                                    r.equals("MODERATOR", true) ||
                                    r.equals("ADMINISTRATOR", true)
                            }.distinct()
                            Text(
                                staffRoles.joinToString { code ->
                                    when (code.uppercase()) {
                                        "MANAGER" -> roleManager
                                        "MODERATOR" -> roleModerator
                                        "ADMINISTRATOR" -> roleAdministrator
                                        "GUEST" -> roleGuest
                                        else -> code
                                    }
                                },
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
                Button(
                    onClick = { vm.logout(onLogout) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) { Text(stringResource(R.string.logout)) }
                OutlinedButton(
                    onClick = { showPasswordDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) { Text(stringResource(R.string.change_password)) }
            }
        }
    }
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { if (!changePasswordBusy) showPasswordDialog = false },
            title = { Text(stringResource(R.string.change_password)) },
            text = {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it.take(72) },
                        label = { Text(stringResource(R.string.current_password)) },
                        singleLine = true,
                        visualTransformation = if (showCurrent) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showCurrent = !showCurrent }) {
                                Icon(
                                    if (showCurrent) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                )
                            }
                        },
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it.take(72) },
                        label = { Text(stringResource(R.string.new_password)) },
                        singleLine = true,
                        visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showNew = !showNew }) {
                                Icon(
                                    if (showNew) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                )
                            }
                        },
                    )
                    OutlinedTextField(
                        value = confirmNewPassword,
                        onValueChange = { confirmNewPassword = it.take(72) },
                        label = { Text(stringResource(R.string.repeat_new_password)) },
                        singleLine = true,
                        visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showConfirm = !showConfirm }) {
                                Icon(
                                    if (showConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                )
                            }
                        },
                    )
                    changePasswordError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !changePasswordBusy,
                    onClick = {
                        vm.changePassword(currentPassword, newPassword, confirmNewPassword) {}
                    },
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(
                    enabled = !changePasswordBusy,
                    onClick = { showPasswordDialog = false },
                ) { Text(stringResource(R.string.dialog_cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    backStackEntry: NavBackStackEntry,
    container: AppContainer,
    nav: NavController,
    onOpenDrawer: () -> Unit,
    openedFromCatalogBrowsing: Boolean = false,
) {
    val vm: CartViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        factory = CartViewModel.factory(container),
    )
    val state by vm.ui.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_cart)) },
                navigationIcon = {
                    if (openedFromCatalogBrowsing) {
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    } else {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, null)
                        }
                    }
                },
                actions = {
                    if (openedFromCatalogBrowsing) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = null)
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.error != null -> Box(
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
            state.cart == null -> Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                ElectronixCenteredLoader()
            }
            else -> {
                val c = state.cart!!
                if (c.items.isEmpty()) {
                    Column(
                        Modifier
                            .padding(padding)
                            .padding(horizontal = 24.dp, vertical = 32.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            stringResource(R.string.cart_empty_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.cart_empty_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        Modifier.padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(c.items, key = { it.id }) { item ->
                            val lineTitle = item.productName.takeIf { it.isNotBlank() }
                                ?: "${stringResource(R.string.product)} ${item.productId.take(8)}…"
                            val lineTotal = item.productPrice * item.quantity
                            Card(
                                Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                ),
                            ) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            lineTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            "${formatRubles(item.productPrice)} × ${item.quantity}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            "${stringResource(R.string.total)}: ${formatRubles(lineTotal)}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            FilledTonalIconButton(
                                                onClick = { vm.decrementQuantity(item) },
                                                modifier = Modifier.size(40.dp),
                                            ) { Icon(Icons.Default.Remove, contentDescription = null) }
                                            Text(
                                                "${item.quantity}",
                                                style = MaterialTheme.typography.titleMedium,
                                                modifier = Modifier.padding(horizontal = 8.dp),
                                            )
                                            FilledTonalIconButton(
                                                onClick = { vm.incrementQuantity(item) },
                                                modifier = Modifier.size(40.dp),
                                            ) { Icon(Icons.Default.Add, contentDescription = null) }
                                        }
                                        OutlinedButton(
                                            onClick = { vm.removeItem(item.id) },
                                            shape = RoundedCornerShape(12.dp),
                                        ) { Text(stringResource(R.string.remove_from_cart)) }
                                    }
                                }
                            }
                        }
                        item {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                tonalElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ) {
                                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(
                                        stringResource(R.string.cart_summary_title),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            stringResource(R.string.total),
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                        Text(
                                            formatRubles(c.totalPrice),
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    Button(
                                        onClick = { vm.checkout { id -> nav.navigate("order/$id") } },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp),
                                        shape = RoundedCornerShape(14.dp),
                                    ) { Text(stringResource(R.string.checkout)) }
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
fun OrdersScreen(backStackEntry: NavBackStackEntry, container: AppContainer, nav: NavController, onOpenDrawer: () -> Unit) {
    val vm: OrdersViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        factory = OrdersViewModel.factory(container),
    )
    val state by vm.ui.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    ManagementForegroundSync(lifecycleOwner = lifecycleOwner) { vm.reload() }

    LaunchedEffect(Unit) {
        container.preferences.markOrdersSectionVisited()
        container.requestDrawerBadgesRefresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_orders)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, null)
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.error != null -> Box(
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
            state.orders == null -> Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                ElectronixCenteredLoader()
            }
            else -> LazyColumn(Modifier.padding(padding)) {
                items(state.orders!!, key = { it.id }) { o ->
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable { nav.navigate("order/${o.id}") },
                    ) {
                        ListItem(
                            headlineContent = { Text(orderRowTitle(o)) },
                            supportingContent = {
                                Text("${orderStatusLabel(o.status)} · ${formatRubles(o.totalAmount)}")
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: String,
    backStackEntry: NavBackStackEntry,
    container: AppContainer,
    nav: NavController,
    onOpenDrawer: () -> Unit,
) {
    val vm: OrderDetailViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        key = "order_$orderId",
        factory = OrderDetailViewModel.factory(container, orderId),
    )
    val state by vm.ui.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    ManagementForegroundSync(lifecycleOwner = lifecycleOwner) { vm.reload() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_orders)) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, null)
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.error != null -> Box(
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
            state.order == null -> Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                ElectronixCenteredLoader()
            }
            else -> {
                val o = state.order!!
                LazyColumn(
                    Modifier.padding(padding).padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        CopyableIdentifierRow(
                            label = stringResource(R.string.order_uuid_label),
                            value = o.id,
                        )
                    }
                    item {
                        CopyableIdentifierRow(
                            label = stringResource(R.string.order_customer_uuid),
                            value = o.userId,
                        )
                    }
                    item {
                        val staff = o.lastStatusChangedByUserId
                        if (staff != null) {
                            CopyableIdentifierRow(
                                label = stringResource(R.string.order_staff_uuid),
                                value = staff,
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
                    }
                    item {
                        Text(
                            "${stringResource(R.string.order_status)}: ${orderStatusLabel(o.status)}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    item {
                        Text(
                            "${stringResource(R.string.total)}: ${formatRubles(o.totalAmount)}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    items(o.items, key = { it.id }) { i ->
                        val itemTitle = i.productName?.takeIf { it.isNotBlank() }
                            ?: "${stringResource(R.string.product)} ${i.productId.take(8)}…"
                        Text("$itemTitle · ${i.quantity} × ${formatRubles(i.priceAtPurchase)}")
                    }
                    if (o.status.equals("Pending", true)) {
                        item {
                            Button(
                                onClick = { vm.cancelIfPending() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                            ) { Text(stringResource(R.string.cancel_order)) }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportMyScreen(backStackEntry: NavBackStackEntry, container: AppContainer, nav: NavController, onOpenDrawer: () -> Unit) {
    val vm: SupportMyViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        factory = SupportMyViewModel.factory(container),
    )
    val state by vm.ui.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        container.preferences.markSupportMySectionVisited()
        container.requestDrawerBadgesRefresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_support)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, null)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.data?.data.orEmpty(), key = { it.id }) { q ->
                val hasAnswer = q.answer != null
                Card(
                    Modifier
                        .fillMaxWidth()
                        .clickable { nav.navigate("support_q/${q.id}") },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    ListItem(
                        headlineContent = { Text(q.subject, maxLines = 1) },
                        supportingContent = {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(q.content, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    if (hasAnswer) "Есть ответ" else "Ожидает ответа",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasAnswer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportQuestionDetailScreen(qId: String, backStackEntry: NavBackStackEntry, container: AppContainer, nav: NavController) {
    val vm: SupportQuestionDetailViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        key = "sq_$qId",
        factory = SupportQuestionDetailViewModel.factory(container, qId),
    )
    val state by vm.ui.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_support)) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
            )
        },
    ) { padding ->
        val question = state.question
        if (question == null) {
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                ElectronixCenteredLoader()
            }
        } else {
            LazyColumn(
                Modifier
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            ) {
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(question.subject, style = MaterialTheme.typography.titleMedium)
                            Text(question.content, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                question.answer?.let { a ->
                    item {
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    stringResource(R.string.answer),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(a.content, style = MaterialTheme.typography.bodyLarge)
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
fun SupportCreateScreen(backStackEntry: NavBackStackEntry, container: AppContainer, nav: NavController) {
    val vm: SupportCreateViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        key = "support_create",
        factory = SupportCreateViewModel.factory(container),
    )
    val subject by vm.subject.collectAsStateWithLifecycle()
    val content by vm.content.collectAsStateWithLifecycle()
    val submitError by vm.error.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val sentMsg = stringResource(R.string.support_request_sent)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_support_new)) },
                navigationIcon = {
                    IconButton(onClick = { nav.popOrGoToCatalogRoot() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.support_new_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(subject, { vm.setSubject(it) }, label = { Text(stringResource(R.string.subject)) }, modifier = Modifier.fillMaxWidth())
            Text(
                "${subject.length}/120",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                content,
                { vm.setContent(it) },
                label = { Text(stringResource(R.string.content)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
                maxLines = 8,
            )
            Text(
                "${content.length}/2000",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            submitError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = {
                    vm.submit {
                        container.notifySupportListsChanged()
                        container.requestDrawerBadgesRefresh()
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = sentMsg,
                                duration = SnackbarDuration.Short,
                            )
                            nav.popOrGoToCatalogRoot()
                        }
                    }
                },
                enabled = subject.trim().length >= 4 && content.trim().length >= 10,
            ) { Text(stringResource(R.string.send)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportAnswerScreen(questionId: String, backStackEntry: NavBackStackEntry, container: AppContainer, nav: NavController) {
    val vm: SupportAnswerViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        key = "sans_$questionId",
        factory = SupportAnswerViewModel.factory(container, questionId),
    )
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val answerSentMsg = stringResource(R.string.support_answer_sent)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.answer)) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        when {
            ui.loadError != null -> Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                ElectronixInlineError(
                    message = ui.loadError!!,
                    onRetry = { vm.reloadQuestion() },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            ui.question == null -> Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                ElectronixCenteredLoader()
            }
            else -> {
                val q = ui.question!!
                LazyColumn(
                    Modifier
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    item {
                        CopyableIdentifierRow(
                            label = stringResource(R.string.support_ticket_author_uuid),
                            value = q.userId,
                        )
                    }
                    item {
                        Text(
                            stringResource(R.string.support_answer_intro),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    item {
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    stringResource(R.string.support_ticket_question),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(q.subject, style = MaterialTheme.typography.titleMedium)
                                Text(q.content, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    stringResource(R.string.support_your_reply),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                OutlinedTextField(
                                    value = ui.replyText,
                                    onValueChange = { vm.setReplyText(it) },
                                    label = { Text(stringResource(R.string.content)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 4,
                                    shape = RoundedCornerShape(14.dp),
                                )
                                Text(
                                    "${ui.replyText.length}/2000",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                ui.submitError?.let {
                                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                }
                                Button(
                                    onClick = {
                                        vm.submit {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    answerSentMsg,
                                                    duration = SnackbarDuration.Short,
                                                )
                                                nav.popBackStack()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = ui.replyText.trim().length >= 8,
                                ) { Text(stringResource(R.string.send)) }
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
fun ReviewCreateScreen(productId: String, backStackEntry: NavBackStackEntry, container: AppContainer, nav: NavController) {
    val vm: ReviewCreateViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        key = "rc_$productId",
        factory = ReviewCreateViewModel.factory(container, productId),
    )
    val rating by vm.rating.collectAsStateWithLifecycle()
    val title by vm.title.collectAsStateWithLifecycle()
    val content by vm.content.collectAsStateWithLifecycle()
    val submitError by vm.submitError.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val reviewSentMsg = stringResource(R.string.review_sent)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_review)) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.review_pick_rating),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ReviewRatingStarsInteractive(
                rating = rating,
                onRatingSelected = { vm.setRating(it) },
            )
            OutlinedTextField(title, { vm.setTitle(it) }, label = { Text(stringResource(R.string.title_field)) }, modifier = Modifier.fillMaxWidth())
            Text(
                "${title.length}/120",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                content,
                { vm.setContent(it) },
                label = { Text(stringResource(R.string.content)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
                maxLines = 8,
            )
            Text(
                "${content.length}/2000",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            submitError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = {
                    vm.submit {
                        container.requestDrawerBadgesRefresh()
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                reviewSentMsg,
                                duration = SnackbarDuration.Short,
                            )
                            nav.popBackStack()
                        }
                    }
                },
                enabled = rating in 1..5 && title.trim().length >= 4 && content.trim().length >= 15,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.send)) }
        }
    }
}
