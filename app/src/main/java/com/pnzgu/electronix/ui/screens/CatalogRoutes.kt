package com.pnzgu.electronix.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.compose.foundation.layout.imePadding
import com.pnzgu.electronix.AppContainer
import com.pnzgu.electronix.R
import com.pnzgu.electronix.data.dto.UserDto
import com.pnzgu.electronix.domain.isAdministrator
import com.pnzgu.electronix.ui.components.ElectronixCenteredLoader
import com.pnzgu.electronix.ui.components.ElectronixInlineError
import com.pnzgu.electronix.ui.components.ProductCatalogRowCard
import com.pnzgu.electronix.ui.components.ProductDetailDescriptionCard
import com.pnzgu.electronix.ui.components.ProductDetailHeroGallery
import com.pnzgu.electronix.ui.components.ProductDetailPriceStrip
import com.pnzgu.electronix.ui.components.ProductDetailSpecsCard
import com.pnzgu.electronix.ui.components.ProductImage
import com.pnzgu.electronix.ui.components.ReviewListEntry
import com.pnzgu.electronix.ui.viewmodel.CategoryNavEvent
import com.pnzgu.electronix.ui.viewmodel.CategoryTreeViewModel
import com.pnzgu.electronix.ui.viewmodel.ProductDetailViewModel
import com.pnzgu.electronix.ui.viewmodel.ProductListViewModel
import com.pnzgu.electronix.util.contentImageUrl
import kotlinx.coroutines.launch

/** Push cart on top of the catalog chain so Back returns to the same category level. Avoid launchSingleTop — it can reorder catalog entries together with [DrawerLink] cart routes. */
private fun NavController.navigateToCatalogCart() {
    navigate("cart?from_catalog=true") {
        restoreState = false
    }
}

/** Avoids racing the nav graph when the user taps back repeatedly. */
private fun NavController.navigateUpOrIgnore() {
    if (!popBackStack()) {
        // Already at the back stack root for this NavController
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogCartIconButton(container: AppContainer, nav: NavController) {
    val count by container.cartBadgeCount.collectAsStateWithLifecycle(0)
    val cartLabel = stringResource(R.string.nav_cart)
    BadgedBox(
        badge = {
            if (count > 0) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ) {
                    Text(
                        text = if (count > 99) "99+" else count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        },
    ) {
        IconButton(onClick = { nav.navigateToCatalogCart() }) {
            Icon(Icons.Default.ShoppingCart, contentDescription = cartLabel)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CatalogTopBarTitle(text: String) {
    Text(
        text = text,
        maxLines = 1,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier
            .fillMaxWidth()
            .basicMarquee(
                initialDelayMillis = 800,
                repeatDelayMillis = 1_400,
            ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTreeScreen(
    parentKey: String,
    backStackEntry: NavBackStackEntry,
    container: AppContainer,
    nav: NavController,
    userRoles: UserDto?,
    catalogRootDrawerMenuEnabled: Boolean = true,
    onOpenDrawer: () -> Unit,
) {
    val vm: CategoryTreeViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        key = "cat_$parentKey",
        factory = CategoryTreeViewModel.factory(container, parentKey),
    )
    val state by vm.ui.collectAsStateWithLifecycle()
    val parentId = if (parentKey == "ROOT") null else parentKey

    LaunchedEffect(backStackEntry.id) {
        backStackEntry.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.navEvents.collect { event ->
                val top = nav.currentBackStackEntry ?: return@collect
                if (top.id != backStackEntry.id) return@collect
                val route = top.destination.route.orEmpty()
                if (!route.startsWith("categories/")) return@collect
                when (event) {
                    is CategoryNavEvent.OpenProducts -> nav.navigate("products/${event.categoryId}")
                    is CategoryNavEvent.OpenFolder -> nav.navigate("categories/${event.categoryId}")
                }
            }
        }
    }

    val title = state.titleFromApi
        ?: stringResource(R.string.nav_catalog)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { CatalogTopBarTitle(title) },
                navigationIcon = {
                    if (parentId != null) {
                        IconButton(onClick = { nav.navigateUpOrIgnore() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    } else {
                        IconButton(
                            enabled = catalogRootDrawerMenuEnabled,
                            onClick = onOpenDrawer,
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = null)
                        }
                    }
                },
                actions = {
                    CatalogCartIconButton(container, nav)
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isPullRefreshing,
            onRefresh = { vm.pullRefresh() },
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when {
                state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ElectronixInlineError(
                        message = state.error ?: stringResource(R.string.error),
                        onRetry = { vm.retryAfterError() },
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
                state.categories == null -> ElectronixCenteredLoader()
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    lazyItems(state.categories!!, key = { it.id }) { cat ->
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .clickable { vm.onCategoryClick(cat.id) },
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                        ) {
                            ListItem(
                                headlineContent = {
                                    Text(cat.name, style = MaterialTheme.typography.titleMedium)
                                },
                                supportingContent = if (userRoles.isAdministrator()) {
                                    { Text("id: ${cat.id}", style = MaterialTheme.typography.bodySmall) }
                                } else null,
                                trailingContent = {
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    categoryId: String,
    backStackEntry: NavBackStackEntry,
    container: AppContainer,
    nav: NavController,
    onOpenDrawer: () -> Unit,
) {
    val vm: ProductListViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        key = "plist_$categoryId",
        factory = ProductListViewModel.factory(container, categoryId),
    )
    val state by vm.ui.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    CatalogTopBarTitle(state.categoryName.ifBlank { stringResource(R.string.products) })
                },
                navigationIcon = {
                    IconButton(onClick = { nav.navigateUpOrIgnore() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    CatalogCartIconButton(container, nav)
                    IconButton(onClick = { showFilters = true }) {
                        Icon(Icons.Default.FilterList, stringResource(R.string.filters))
                    }
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isPullRefreshing,
            onRefresh = { vm.pullRefresh() },
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            Column(Modifier.fillMaxSize()) {
                when {
                    state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ElectronixInlineError(
                            message = state.error!!,
                            onRetry = { vm.retryList() },
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                    state.products == null -> ElectronixCenteredLoader()
                    state.products!!.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    else -> LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        lazyItems(state.products!!, key = { it.id }) { p ->
                            ProductCatalogRowCard(
                                product = p,
                                contentBaseUrl = container.contentBaseUrl,
                                priceLabel = stringResource(R.string.price),
                                stockLabel = stringResource(R.string.stock),
                                onClick = { nav.navigate("product/${p.id}") },
                            )
                        }
                        item {
                            if (state.hasMore && !state.loading) {
                                Button(
                                    onClick = { vm.loadNextPage() },
                                    Modifier.padding(8.dp),
                                ) { Text("Ещё") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilters && state.presets != null) {
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            sheetState = sheetState,
        ) {
            Column(
                Modifier
                    .padding(16.dp)
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(stringResource(R.string.filters), style = MaterialTheme.typography.titleLarge)
                state.presets!!.forEach { ch ->
                    val r = state.filterRanges[ch.characteristicId] ?: com.pnzgu.electronix.ui.viewmodel.FilterRange()
                    Text("${ch.characteristicName} (${ch.unit})", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = r.min,
                            onValueChange = { vm.updateFilterMin(ch.characteristicId, it) },
                            label = { Text(stringResource(R.string.min_short)) },
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = r.max,
                            onValueChange = { vm.updateFilterMax(ch.characteristicId, it) },
                            label = { Text(stringResource(R.string.max_short)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        vm.clearFilters()
                        vm.bumpRefresh()
                    }) { Text(stringResource(R.string.clear_filters)) }
                    Button(onClick = {
                        showFilters = false
                        vm.bumpRefresh()
                    }) { Text(stringResource(R.string.apply_filters)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    backStackEntry: NavBackStackEntry,
    container: AppContainer,
    nav: NavController,
    user: UserDto?,
    onOpenDrawer: () -> Unit,
) {
    val vm: ProductDetailViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        key = "pdetail_$productId",
        factory = ProductDetailViewModel.factory(container, productId),
    )
    val state by vm.ui.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val addedToCartMsg = stringResource(R.string.added_to_cart)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val nm = state.product?.name.orEmpty().ifBlank { null }
                    if (nm == null) {
                        Text(stringResource(R.string.loading))
                    } else {
                        CatalogTopBarTitle(nm)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { nav.navigateUpOrIgnore() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    CatalogCartIconButton(container, nav)
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isPullRefreshing,
            onRefresh = { vm.pullRefresh() },
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when {
                state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ElectronixInlineError(
                        message = state.error!!,
                        onRetry = { vm.retryAfterError() },
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
                state.product == null -> ElectronixCenteredLoader()
                else -> {
                    val p = state.product!!
                    val galleryUrls = remember(p.id, p.mainImagePath, p.imagePaths) {
                        buildList {
                            contentImageUrl(container.contentBaseUrl, p.mainImagePath)?.let { add(it) }
                            p.imagePaths.forEach { path ->
                                contentImageUrl(container.contentBaseUrl, path)?.let { u ->
                                    if (!contains(u)) add(u)
                                }
                            }
                        }
                    }
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item {
                            if (galleryUrls.isNotEmpty()) {
                                ProductDetailHeroGallery(p, container.contentBaseUrl)
                            } else {
                                val img = contentImageUrl(container.contentBaseUrl, p.mainImagePath)
                                if (img != null) {
                                    ProductImage(
                                        img,
                                        p.name,
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(24.dp)),
                                        height = 260.dp,
                                    )
                                }
                            }
                        }
                        item {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Text(
                                    p.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                ProductDetailPriceStrip(
                                    priceLabel = stringResource(R.string.price),
                                    price = p.price,
                                    stockLabel = stringResource(R.string.stock),
                                    stock = p.stock,
                                )
                                ProductDetailDescriptionCard(p.description ?: "")
                                ProductDetailSpecsCard(p.characteristics)
                            }
                        }
                        if (user != null) {
                            item {
                                Card(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                ) {
                                    Column(
                                        Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        OutlinedTextField(
                                            value = state.quantityText,
                                            onValueChange = { vm.setQuantity(it) },
                                            label = { Text(stringResource(R.string.quantity)) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                        )
                                        Button(
                                            onClick = {
                                                vm.addToCart {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            addedToCartMsg,
                                                            duration = SnackbarDuration.Short,
                                                        )
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                        ) { Text(stringResource(R.string.add_to_cart)) }
                                        state.cartMessage?.let { err ->
                                            Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                        }
                                        TextButton(onClick = { nav.navigate("review_create/${p.id}") }) {
                                            Text(stringResource(R.string.create_review))
                                        }
                                    }
                                }
                            }
                        } else {
                            item {
                                Card(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                ) {
                                    Column(
                                        Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Text(
                                            stringResource(R.string.product_guest_cart_hint),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        TextButton(
                                            onClick = { nav.navigate("login") },
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(stringResource(R.string.login_action))
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Text(
                                stringResource(R.string.product_reviews_heading),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                        lazyItems(state.reviews?.data ?: emptyList(), key = { it.id }) { r ->
                            Card(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    ReviewListEntry(review = r)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
