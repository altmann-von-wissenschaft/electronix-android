package com.pnzgu.electronix.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pnzgu.electronix.AppContainer
import com.pnzgu.electronix.R
import com.pnzgu.electronix.data.dto.UserDto
import com.pnzgu.electronix.domain.hasRole
import com.pnzgu.electronix.domain.isAdministrator
import com.pnzgu.electronix.domain.isManager
import com.pnzgu.electronix.domain.isModerator
import com.pnzgu.electronix.ui.screens.AdminCategoriesScreen
import com.pnzgu.electronix.ui.screens.AdminCategoryEditScreen
import com.pnzgu.electronix.ui.screens.AdminCharacteristicEditScreen
import com.pnzgu.electronix.ui.screens.AdminCharacteristicsScreen
import com.pnzgu.electronix.ui.screens.AdminOrderStatusScreen
import com.pnzgu.electronix.ui.screens.AdminOrdersScreen
import com.pnzgu.electronix.ui.screens.AboutScreen
import com.pnzgu.electronix.ui.screens.AdminProductEditScreen
import com.pnzgu.electronix.ui.screens.AdminProductsScreen
import com.pnzgu.electronix.ui.screens.AdminUserDetailScreen
import com.pnzgu.electronix.ui.screens.AdminUsersScreen
import com.pnzgu.electronix.ui.screens.CartScreen
import com.pnzgu.electronix.ui.screens.CategoryTreeScreen
import com.pnzgu.electronix.ui.screens.LoginScreen
import com.pnzgu.electronix.ui.screens.ModPendingReviewsScreen
import com.pnzgu.electronix.ui.screens.OrderDetailScreen
import com.pnzgu.electronix.ui.screens.OrdersScreen
import com.pnzgu.electronix.ui.screens.ProductDetailScreen
import com.pnzgu.electronix.ui.screens.ProductListScreen
import com.pnzgu.electronix.ui.screens.ProfileScreen
import com.pnzgu.electronix.ui.screens.RegisterScreen
import com.pnzgu.electronix.ui.screens.ReviewCreateScreen
import com.pnzgu.electronix.ui.screens.SalesReportScreen
import com.pnzgu.electronix.ui.screens.SettingsScreen
import com.pnzgu.electronix.ui.screens.SupportAnswerScreen
import com.pnzgu.electronix.ui.screens.SupportCreateScreen
import com.pnzgu.electronix.ui.screens.SupportMyScreen
import com.pnzgu.electronix.ui.screens.SupportQuestionDetailScreen
import com.pnzgu.electronix.ui.screens.SupportUnansweredScreen
import com.pnzgu.electronix.ui.viewmodel.AdminCategoryEditViewModel
import com.pnzgu.electronix.ui.viewmodel.AdminCategoryEditViewModel.CategoryEditMode
import com.pnzgu.electronix.ui.viewmodel.DrawerBadgeViewModel
import com.pnzgu.electronix.ui.viewmodel.DrawerBadges
import com.pnzgu.electronix.ui.viewmodel.SessionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield

/** Drawer + ModalNavigationDrawer glitch if layout/nav settle overlaps programmatic open. */
private const val DRAWER_OPEN_GUARD_AFTER_NAV_MS = 520L

/** Extra wait after guard so the catalog pane finishes layout before drawer consumes inset/offset. */
private const val DRAWER_OPEN_EXTRA_SETTLE_MS = 48L

/** Hamburger on catalog ROOT after pop — disable briefly so ultra-fast taps cannot race drawer vs composition. */
private const val CATALOG_ROOT_MENU_COOLDOWN_MS = 420L

@Composable
private fun DrawerLink(
    nav: NavController,
    route: String,
    label: String,
    drawerScope: CoroutineScope,
    closeDrawerSuspended: suspend () -> Unit,
    restoreNavigationState: Boolean = true,
    badge: Int? = null,
) {
    NavigationDrawerItem(
        label = {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                if (badge != null && badge > 0) {
                    val text = if (badge > 99) "99+" else badge.toString()
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.widthIn(min = 22.dp),
                    ) {
                        Text(
                            text,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        },
        selected = false,
        onClick = {
            drawerScope.launch {
                closeDrawerSuspended()
                nav.navigate(route) {
                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = restoreNavigationState
                }
            }
        },
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent,
        ),
    )
}

@Composable
fun AppRoot(
    container: AppContainer,
) {
    val nav = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val user by container.sessionRepository.user.collectAsStateWithLifecycle()
    val sessionViewModel: SessionViewModel = viewModel(factory = SessionViewModel.factory(container))
    val badgeViewModel: DrawerBadgeViewModel = viewModel(factory = DrawerBadgeViewModel.factory(container))
    val drawerBadges by badgeViewModel.badges.collectAsStateWithLifecycle()
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ -> }

    LaunchedEffect(Unit) {
        sessionViewModel.hydrate()
    }

    LaunchedEffect(user?.id) {
        badgeViewModel.refresh(user)
        if (user == null) {
            container.resetCartBadge()
        } else {
            if (Build.VERSION.SDK_INT >= 33) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            withContext(Dispatchers.IO) {
                container.refreshCartBadgeFromServer()
                container.pushSync.syncFcmTokenAndPreferences()
            }
        }
    }

    LaunchedEffect(Unit) {
        container.drawerBadgesRefresh.collect {
            badgeViewModel.refresh(container.sessionRepository.user.value)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    var lastDestinationChangeUptimeMs by remember { mutableStateOf(0L) }
    var catalogHasLeftRootOnce by remember { mutableStateOf(false) }
    var catalogRootMenuInteractive by remember { mutableStateOf(true) }
    val catalogRootMenuCooldownJob = remember { mutableStateOf<Job?>(null) }

    DisposableEffect(nav, scope) {
        val listener =
            NavController.OnDestinationChangedListener { _, destination, arguments ->
                lastDestinationChangeUptimeMs = SystemClock.uptimeMillis()

                val route = destination.route.orEmpty()
                if (!route.startsWith("categories")) return@OnDestinationChangedListener

                val parentKey = arguments?.getString("parentKey") ?: return@OnDestinationChangedListener
                if (parentKey != "ROOT") {
                    catalogHasLeftRootOnce = true
                    return@OnDestinationChangedListener
                }

                // Первый заход на корень (холодный старт): не гасим меню. Кулдаун только после того, как хоть раз ушли с ROOT.
                if (!catalogHasLeftRootOnce) return@OnDestinationChangedListener

                catalogRootMenuCooldownJob.value?.cancel()
                catalogRootMenuInteractive = false
                catalogRootMenuCooldownJob.value =
                    scope.launch {
                        delay(CATALOG_ROOT_MENU_COOLDOWN_MS)
                        catalogRootMenuInteractive = true
                        catalogRootMenuCooldownJob.value = null
                    }
            }
        nav.addOnDestinationChangedListener(listener)
        onDispose {
            catalogRootMenuCooldownJob.value?.cancel()
            nav.removeOnDestinationChangedListener(listener)
        }
    }

    DisposableEffect(lifecycleOwner, badgeViewModel, container) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                sessionViewModel.refreshMe()
                badgeViewModel.refresh(container.sessionRepository.user.value)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val closeDrawerForNavTransaction: suspend () -> Unit = {
        drawerState.close()
    }

    val pendingDrawerOpenJob = remember { mutableStateOf<Job?>(null) }

    fun openDrawerSafe() {
        pendingDrawerOpenJob.value?.cancel()
        pendingDrawerOpenJob.value =
            scope.launch {
                val elapsed = SystemClock.uptimeMillis() - lastDestinationChangeUptimeMs
                val remaining = (DRAWER_OPEN_GUARD_AFTER_NAV_MS - elapsed).coerceAtLeast(0L)
                delay(remaining)
                if (!isActive) return@launch
                delay(DRAWER_OPEN_EXTRA_SETTLE_MS)
                if (!isActive) return@launch
                // Сброс внутреннего offset drawer — иначе основная область может остаться со сдвигом/alpha как «пустая».
                runCatching { drawerState.snapTo(DrawerValue.Closed) }
                yield()
                if (!isActive) return@launch
                if (drawerState.currentValue != DrawerValue.Open) {
                    drawerState.open()
                }
            }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxHeight()
                    .navigationBarsPadding(),
            ) {
                Column(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        "Electronix",
                        Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(Modifier.padding(horizontal = 12.dp))
                    Spacer(Modifier.height(8.dp))
                    AppDrawerContent(nav, user, drawerBadges, scope, closeDrawerForNavTransaction)
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(Modifier.padding(horizontal = 12.dp))
                    DrawerLink(
                        nav,
                        "settings",
                        stringResource(R.string.nav_settings),
                        scope,
                        closeDrawerForNavTransaction,
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        },
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
            NavHost(
                navController = nav,
                startDestination = "categories/ROOT",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding(),
            ) {
                composable(
                    "categories/{parentKey}",
                    arguments = listOf(navArgument("parentKey") { type = NavType.StringType; defaultValue = "ROOT" }),
                    // Без анимации смены экрана — иначе при быстром «Назад» + меню основная панель под drawer уходит в некорректный layout.
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None },
                ) { entry ->
                    CategoryTreeScreen(
                        parentKey = entry.arguments!!.getString("parentKey")!!,
                        backStackEntry = entry,
                        container = container,
                        nav = nav,
                        userRoles = user,
                        catalogRootDrawerMenuEnabled = catalogRootMenuInteractive,
                        onOpenDrawer = { openDrawerSafe() },
                    )
                }
                composable("products/{categoryId}") { entry ->
                    ProductListScreen(
                        categoryId = entry.arguments!!.getString("categoryId")!!,
                        backStackEntry = entry,
                        container = container,
                        nav = nav,
                        onOpenDrawer = { openDrawerSafe() },
                    )
                }
                composable("product/{id}") { entry ->
                    ProductDetailScreen(
                        productId = entry.arguments!!.getString("id")!!,
                        backStackEntry = entry,
                        container = container,
                        nav = nav,
                        user = user,
                        onOpenDrawer = { openDrawerSafe() },
                    )
                }
                composable("login") { entry ->
                    LoginScreen(
                        backStackEntry = entry,
                        container = container,
                        nav = nav,
                        onOpenDrawer = { openDrawerSafe() },
                        onLoggedIn = {
                            scope.launch {
                                closeDrawerForNavTransaction()
                                badgeViewModel.refresh(container.sessionRepository.user.value)
                                nav.navigate("categories/ROOT") {
                                    popUpTo("login") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                    )
                }
                composable("register") { entry ->
                    RegisterScreen(backStackEntry = entry, container = container, nav = nav)
                }
                composable("profile") { entry ->
                    ProfileScreen(
                        user = user,
                        backStackEntry = entry,
                        container = container,
                        nav = nav,
                        onOpenDrawer = { openDrawerSafe() },
                        onLogout = {
                            nav.navigate("categories/ROOT") {
                                popUpTo(nav.graph.findStartDestination().id) { inclusive = true }
                            }
                        },
                    )
                }
                composable(
                    route = "cart?from_catalog={from_catalog}",
                    arguments = listOf(
                        navArgument("from_catalog") {
                            type = NavType.BoolType
                            defaultValue = false
                        },
                    ),
                ) { entry ->
                    val openedFromCatalog = entry.arguments!!.getBoolean("from_catalog")
                    CartScreen(
                        backStackEntry = entry,
                        container = container,
                        nav = nav,
                        onOpenDrawer = { openDrawerSafe() },
                        openedFromCatalogBrowsing = openedFromCatalog,
                    )
                }
                composable("orders") { entry ->
                    OrdersScreen(backStackEntry = entry, container = container, nav = nav, onOpenDrawer = { openDrawerSafe() })
                }
                composable("order/{id}") { entry ->
                    OrderDetailScreen(
                        orderId = entry.arguments!!.getString("id")!!,
                        backStackEntry = entry,
                        container = container,
                        nav = nav,
                        onOpenDrawer = { openDrawerSafe() },
                    )
                }
                composable("support_my") { entry ->
                    SupportMyScreen(backStackEntry = entry, container = container, nav = nav, onOpenDrawer = { openDrawerSafe() })
                }
                composable("support_new") { entry ->
                    SupportCreateScreen(backStackEntry = entry, container = container, nav = nav)
                }
                composable("support_q/{id}") { entry ->
                    SupportQuestionDetailScreen(
                        qId = entry.arguments!!.getString("id")!!,
                        backStackEntry = entry,
                        container = container,
                        nav = nav,
                    )
                }
                composable("support_unanswered") { entry ->
                    SupportUnansweredScreen(backStackEntry = entry, container = container, nav = nav, onOpenDrawer = { openDrawerSafe() })
                }
                composable("support_answer/{id}") { entry ->
                    SupportAnswerScreen(
                        questionId = entry.arguments!!.getString("id")!!,
                        backStackEntry = entry,
                        container = container,
                        nav = nav,
                    )
                }
                composable("review_create/{productId}") { entry ->
                    ReviewCreateScreen(
                        productId = entry.arguments!!.getString("productId")!!,
                        backStackEntry = entry,
                        container = container,
                        nav = nav,
                    )
                }
                composable("mod_reviews") { entry ->
                    ModPendingReviewsScreen(backStackEntry = entry, container = container, nav = nav, onOpenDrawer = { openDrawerSafe() })
                }
                composable("admin_orders") { entry ->
                    AdminOrdersScreen(backStackEntry = entry, container = container, nav = nav, onOpenDrawer = { openDrawerSafe() })
                }
                composable("admin_order/{id}") { entry ->
                    AdminOrderStatusScreen(
                        orderId = entry.arguments!!.getString("id")!!,
                        backStackEntry = entry,
                        container = container,
                        nav = nav,
                    )
                }
                composable("admin_users") { entry ->
                    AdminUsersScreen(backStackEntry = entry, container = container, nav = nav, onOpenDrawer = { openDrawerSafe() })
                }
                composable("admin_user/{id}") { entry ->
                    AdminUserDetailScreen(
                        userId = entry.arguments!!.getString("id")!!,
                        backStackEntry = entry,
                        container = container,
                        nav = nav,
                        canManageRoles = user?.isAdministrator() == true,
                    )
                }
                composable(
                    "admin_categories/{parentKey}",
                    arguments = listOf(navArgument("parentKey") { type = NavType.StringType; defaultValue = "ROOT" }),
                ) { entry ->
                    AdminCategoriesScreen(
                        parentKey = entry.arguments!!.getString("parentKey")!!,
                        backStackEntry = entry,
                        container = container,
                        nav = nav,
                        onOpenDrawer = { openDrawerSafe() },
                    )
                }
                composable(
                    "admin_category_edit/create/{parentKey}",
                    arguments = listOf(navArgument("parentKey") { type = NavType.StringType }),
                ) { entry ->
                    val pk = entry.arguments!!.getString("parentKey")!!
                    val parentId = if (pk == AdminCategoryEditViewModel.CREATE_PARENT_NONE) null else pk
                    AdminCategoryEditScreen(
                        backStackEntry = entry,
                        container = container,
                        nav = nav,
                        mode = CategoryEditMode.Create(parentId),
                    )
                }
                composable("admin_category_edit/{categoryId}") { entry ->
                    val categoryId = entry.arguments!!.getString("categoryId")!!
                    AdminCategoryEditScreen(
                        backStackEntry = entry,
                        container = container,
                        nav = nav,
                        mode = CategoryEditMode.Edit(categoryId),
                    )
                }
                composable("admin_products") { entry ->
                    AdminProductsScreen(backStackEntry = entry, container = container, nav = nav, onOpenDrawer = { openDrawerSafe() })
                }
                composable("admin_product/{id}") { entry ->
                    AdminProductEditScreen(
                        productKey = entry.arguments!!.getString("id")!!,
                        backStackEntry = entry,
                        container = container,
                        nav = nav,
                    )
                }
                composable("admin_characteristics") { entry ->
                    AdminCharacteristicsScreen(backStackEntry = entry, container = container, nav = nav, onOpenDrawer = { openDrawerSafe() })
                }
                composable("admin_char/{id}") { entry ->
                    AdminCharacteristicEditScreen(
                        charId = entry.arguments!!.getString("id")!!,
                        backStackEntry = entry,
                        container = container,
                        nav = nav,
                    )
                }
                composable("sales_report") { entry ->
                    SalesReportScreen(backStackEntry = entry, container = container, nav = nav, onOpenDrawer = { openDrawerSafe() })
                }
                composable("settings") {
                    SettingsScreen(
                        container = container,
                        user = user,
                        onOpenDrawer = { openDrawerSafe() },
                        onThemeSelected = { mode ->
                            scope.launch { container.preferences.setThemeMode(mode) }
                        },
                    )
                }
                composable("about") {
                    AboutScreen(onOpenDrawer = { openDrawerSafe() })
                }
            }
        }
    }
}

@Composable
private fun AppDrawerContent(
    nav: NavController,
    user: UserDto?,
    badges: DrawerBadges,
    drawerScope: CoroutineScope,
    closeDrawerSuspended: suspend () -> Unit,
) {
    DrawerLink(nav, "categories/ROOT", stringResource(R.string.nav_catalog), drawerScope, closeDrawerSuspended)
    DrawerLink(nav, "about", stringResource(R.string.nav_about), drawerScope, closeDrawerSuspended)
    if (user == null) {
        DrawerLink(nav, "login", stringResource(R.string.nav_login), drawerScope, closeDrawerSuspended)
        DrawerLink(nav, "register", stringResource(R.string.nav_register), drawerScope, closeDrawerSuspended)
    } else {
        DrawerLink(nav, "profile", stringResource(R.string.nav_profile), drawerScope, closeDrawerSuspended)
        DrawerLink(nav, "cart?from_catalog=false", stringResource(R.string.nav_cart), drawerScope, closeDrawerSuspended)
        DrawerLink(
            nav,
            "orders",
            stringResource(R.string.nav_orders),
            drawerScope,
            closeDrawerSuspended,
            badge = badges.orders.takeIf { it > 0 },
        )
        DrawerLink(
            nav,
            "support_new",
            stringResource(R.string.nav_support_new),
            drawerScope,
            closeDrawerSuspended,
            restoreNavigationState = false,
        )
        DrawerLink(
            nav,
            "support_my",
            stringResource(R.string.nav_support_my),
            drawerScope,
            closeDrawerSuspended,
            restoreNavigationState = false,
            badge = badges.supportMy.takeIf { it > 0 },
        )
    }
    if (user.isManager()) {
        DrawerLink(
            nav,
            "support_unanswered",
            stringResource(R.string.nav_support_queue),
            drawerScope,
            closeDrawerSuspended,
            badge = badges.supportQueue.takeIf { it > 0 },
        )
        DrawerLink(nav, "admin_orders", stringResource(R.string.nav_admin_orders), drawerScope, closeDrawerSuspended)
        DrawerLink(nav, "sales_report", stringResource(R.string.sales_report), drawerScope, closeDrawerSuspended)
    }
    if (user.isModerator()) {
        DrawerLink(
            nav,
            "mod_reviews",
            stringResource(R.string.nav_mod_reviews),
            drawerScope,
            closeDrawerSuspended,
            badge = badges.modReviews.takeIf { it > 0 },
        )
    }
    if (user.hasRole("MODERATOR") || user.isAdministrator()) {
        DrawerLink(nav, "admin_users", stringResource(R.string.nav_admin_users), drawerScope, closeDrawerSuspended)
    }
    if (user.isAdministrator()) {
        DrawerLink(nav, "admin_categories/ROOT", stringResource(R.string.nav_admin_categories), drawerScope, closeDrawerSuspended)
        DrawerLink(nav, "admin_products", stringResource(R.string.nav_admin_products), drawerScope, closeDrawerSuspended)
        DrawerLink(nav, "admin_characteristics", stringResource(R.string.nav_admin_characteristics), drawerScope, closeDrawerSuspended)
    }
}
