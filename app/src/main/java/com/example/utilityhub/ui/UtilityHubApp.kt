package com.example.utilityhub.ui

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.utilityhub.R
import com.example.utilityhub.data.prefs.ThemeManager
import com.example.utilityhub.features.calculators.CurrencyScreen
import com.example.utilityhub.features.calculators.MeasurementScreen
import com.example.utilityhub.features.calculators.PasswordGeneratorScreen
import com.example.utilityhub.features.calculators.QuickCalcScreen
import com.example.utilityhub.features.ai.PriceCompareScreen
import com.example.utilityhub.features.media.CreationsScreen
import com.example.utilityhub.features.media.FileTransferScreen
import com.example.utilityhub.features.media.MusicPlayerScreen
import com.example.utilityhub.features.media.StudioScreen
import com.example.utilityhub.features.media.VideoPlayerScreen
import com.example.utilityhub.features.support.SupportBotScreen
import com.example.utilityhub.features.system.SystemHealthScreen
import com.example.utilityhub.features.qr.QRScreen
import com.example.utilityhub.features.text.TextStudioScreen
import com.example.utilityhub.navigation.Screen
import com.example.utilityhub.navigation.allScreens
import com.example.utilityhub.data.db.SwaraDao
import kotlinx.coroutines.launch

val EliteGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFFD700).copy(alpha = 0.2f), // Gold
        Color(0xFFFF8C00).copy(alpha = 0.1f)  // Dark Orange
    )
)

fun Modifier.glassmorphism(radius: Dp = 15.dp): Modifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    this.blur(radius)
} else {
    this.background(Color.White.copy(alpha = 0.05f))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtilityHubApp(
    themeManager: ThemeManager, 
    historyViewModel: HistoryViewModel, 
    playlistDao: com.example.utilityhub.data.db.PlaylistDao, 
    swaraDao: SwaraDao, 
    currencyCacheDao: com.example.utilityhub.data.db.CurrencyCacheDao,
    isPipMode: Boolean = false,
    triggerOpenBot: Boolean = false,
    onBotOpened: () -> Unit = {},
    startScreen: String? = null,
    onStartScreenHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isWideScreen = screenWidthDp >= 600

    val pulseMode by themeManager.uiPulseMode.collectAsState(initial = "NEUTRAL")

    val isGalleryAccessEnabled by themeManager.isGalleryAccessEnabled.collectAsState(initial = true)
    val menuMode by themeManager.menuMode.collectAsState(initial = "BASIC")
    val accentColor by themeManager.accentColor.collectAsState(initial = "AMBER")
    val seenTutorials by themeManager.seenTutorials.collectAsState(initial = emptySet())
    val isSwaraEnabled by themeManager.isSwaraEnabled.collectAsState(initial = true)
    val hideComingSoon by themeManager.hideComingSoon.collectAsState(initial = false)
    val isVaultLocked by themeManager.isVaultLocked.collectAsState(initial = false)
    
    var showStudioTutorial by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentScreen = allScreens.find { it.route == currentRoute } ?: Screen.Home

    // Drawer Expansion States
    var creativeExpanded by remember { mutableStateOf(true) }
    var calcExpanded by remember { mutableStateOf(true) }
    var securityExpanded by remember { mutableStateOf(true) }

    // Handle Global Wake Word Navigation
    LaunchedEffect(triggerOpenBot) {
        if (triggerOpenBot && isSwaraEnabled) {
            navController.navigate(Screen.SupportBot.route) {
                launchSingleTop = true
            }
            onBotOpened()
        }
    }

    LaunchedEffect(startScreen) {
        if (startScreen != null) {
            navController.navigate(startScreen) {
                popUpTo(Screen.Home.route)
                launchSingleTop = true
            }
            onStartScreenHandled()
        }
    }

    LaunchedEffect(currentRoute) {
        if (currentRoute == Screen.VideoPlayer.route || currentRoute == Screen.MusicPlayer.route) {
            drawerState.close()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentRoute != Screen.VideoPlayer.route && 
                         currentRoute != Screen.MusicPlayer.route,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.Transparent,
                drawerTonalElevation = 0.dp,
                modifier = Modifier
                    .width(300.dp)
                    .background(EliteGradient)
                    .glassmorphism()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. Compact Header
                    DrawerHeader()
                    
                    // 2. Scrollable Sections
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        ExpandableDrawerSection(
                            title = "CREATIVE & MEDIA", 
                            isExpanded = creativeExpanded,
                            onToggle = { creativeExpanded = !creativeExpanded }
                        ) {
                            DrawerItem(Screen.MediaStudio, Icons.Default.Brush, currentRoute, navController, drawerState, scope)
                            DrawerItem(Screen.Creations, Icons.Default.Folder, currentRoute, navController, drawerState, scope)
                            DrawerItem(Screen.FileTransfer, Icons.Default.RocketLaunch, currentRoute, navController, drawerState, scope)
                            if (isSwaraEnabled) {
                                DrawerItem(Screen.SupportBot, Icons.Default.SmartToy, currentRoute, navController, drawerState, scope)
                            }
                            DrawerItem(Screen.TextStudio, Icons.Default.Translate, currentRoute, navController, drawerState, scope)
                        }

                        DrawerDivider()

                        ExpandableDrawerSection(
                            title = "CALCULATIONS", 
                            isExpanded = calcExpanded,
                            onToggle = { calcExpanded = !calcExpanded }
                        ) {
                            DrawerItem(Screen.Currency, Icons.Default.CurrencyExchange, currentRoute, navController, drawerState, scope)
                            DrawerItem(Screen.QuickCalc, Icons.Default.Percent, currentRoute, navController, drawerState, scope)
                            DrawerItem(Screen.SmartPriceHub, Icons.Default.LocalOffer, currentRoute, navController, drawerState, scope)
                            DrawerItem(Screen.Measurement, Icons.Default.Straighten, currentRoute, navController, drawerState, scope)
                        }

                        DrawerDivider()

                        ExpandableDrawerSection(
                            title = "SECURITY & UTILITIES", 
                            isExpanded = securityExpanded,
                            onToggle = { securityExpanded = !securityExpanded }
                        ) {
                            DrawerItem(Screen.Password, Icons.Default.Password, currentRoute, navController, drawerState, scope)
                            DrawerItem(Screen.QR, Icons.Default.QrCodeScanner, currentRoute, navController, drawerState, scope)
                        }
                    }

                    // 3. Compact System Footer
                    DrawerFooter(currentRoute, navController, drawerState, scope)
                }
            }
        }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (isWideScreen && currentRoute != Screen.VideoPlayer.route && currentRoute != Screen.MusicPlayer.route) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menu")
                        }
                    }
                ) {
                    val navScreens = listOf(Screen.Home, Screen.Creations, Screen.Settings)
                    navScreens.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationRailItem(
                            icon = { Text(screen.icon, fontSize = 20.sp) },
                            label = { Text(stringResource(screen.titleRes), maxLines = 1) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationRailItemDefaults.colors(
                                indicatorColor = if (selected) Color(0xFFFFE8D6) else Color.Transparent
                            )
                        )
                    }
                }
            }
            
            Scaffold(
                topBar = {
                    if (currentRoute != Screen.VideoPlayer.route && 
                        currentRoute != Screen.MusicPlayer.route && 
                        currentRoute != Screen.SupportBot.route) {
                        TopAppBar(
                            title = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        stringResource(currentScreen.titleRes),
                                        fontWeight = FontWeight.Bold
                                    ) 
                                    if (pulseMode != "NEUTRAL") {
                                        Spacer(Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(
                                                    if (pulseMode == "EFFICIENT") Color(0xFF00B0FF) else Color(0xFF9C27B0),
                                                    CircleShape
                                                )
                                        )
                                    }
                                }
                            },
                            navigationIcon = {
                                if (currentRoute == Screen.Settings.route || currentRoute == Screen.History.route) {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                    }
                                } else if (!isWideScreen) {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, "Menu")
                                    }
                                } else {
                                    // In wide screen, menu is in the rail header
                                    null
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                                navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            actions = {
                                if (isSwaraEnabled && currentRoute != Screen.SupportBot.route) {
                                    IconButton(onClick = { 
                                        navController.navigate(Screen.SupportBot.route) {
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }) {
                                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Swara Bot", modifier = Modifier.size(20.dp))
                                    }
                                }
                                if (currentRoute != Screen.Home.route) {
                                    if (currentRoute == Screen.MediaStudio.route || currentRoute == Screen.TextToAudio.route) {
                                        IconButton(onClick = { showStudioTutorial = true }) {
                                            Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
                                        }
                                    }
                                    IconButton(onClick = {
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.Home.route) { inclusive = true }
                                        }
                                    }) {
                                        Icon(Icons.Default.Home, contentDescription = "Home")
                                    }
                                }
                            }
                        )
                    }
                },
                bottomBar = {
                    if (!isWideScreen && currentRoute != Screen.VideoPlayer.route && currentRoute != Screen.MusicPlayer.route) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            val bottomNavScreens = listOf(Screen.Home, Screen.Creations, Screen.Settings)
                            bottomNavScreens.forEach { screen ->
                                val selected = currentRoute == screen.route
                                NavigationBarItem(
                                    icon = { Text(screen.icon, fontSize = 20.sp) },
                                    label = { Text(stringResource(screen.titleRes), maxLines = 1) },
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(Screen.Home.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = if (selected) Color(0xFFFFE8D6) else Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(Screen.Home.route) { 
                        HomeScreen(
                            menuMode = menuMode,
                            onToggleMenuMode = { mode ->
                                scope.launch { themeManager.setMenuMode(mode) }
                            },
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            hasSeenTutorial = seenTutorials.contains("home"),
                            onMarkTutorialSeen = { scope.launch { themeManager.markTutorialSeen("home") } },
                            hideComingSoon = hideComingSoon,
                            historyViewModel = historyViewModel
                        )
                    }
                    composable(Screen.TextStudio.route) { TextStudioScreen(historyViewModel, initialTab = 0) }
                    composable(Screen.TextToAudio.route) { 
                        StudioScreen(
                            historyViewModel, 
                            initialTool = "audio",
                            hasSeenTutorial = seenTutorials.contains("studio"),
                            onMarkTutorialSeen = { scope.launch { themeManager.markTutorialSeen("studio") } },
                            hideComingSoon = hideComingSoon,
                            showTutorialExternal = showStudioTutorial,
                            onDismissTutorialExternal = { showStudioTutorial = false }
                        ) 
                    }
                    composable(
                        route = Screen.Currency.route + "?amount={amount}&from={from}&to={to}",
                        arguments = listOf(
                            navArgument("amount") { defaultValue = "" },
                            navArgument("from") { defaultValue = "" },
                            navArgument("to") { defaultValue = "" }
                        )
                    ) { backStackEntry ->
                        val amount = backStackEntry.arguments?.getString("amount") ?: ""
                        val from = backStackEntry.arguments?.getString("from") ?: ""
                        val to = backStackEntry.arguments?.getString("to") ?: ""
                        CurrencyScreen(historyViewModel, currencyCacheDao, amount, from, to)
                    }
                    composable(Screen.QuickCalc.route) { QuickCalcScreen(historyViewModel) }
                    composable(Screen.SmartPriceHub.route) { PriceCompareScreen() }
                    composable(Screen.QR.route) { QRScreen() }
                    composable(Screen.Measurement.route) { MeasurementScreen(historyViewModel) }
                    composable(Screen.Password.route) { PasswordGeneratorScreen() }
                    composable(Screen.MediaStudio.route) { 
                        StudioScreen(
                            historyViewModel,
                            hasSeenTutorial = seenTutorials.contains("studio"),
                            onMarkTutorialSeen = { scope.launch { themeManager.markTutorialSeen("studio") } },
                            hideComingSoon = hideComingSoon,
                            showTutorialExternal = showStudioTutorial,
                            onDismissTutorialExternal = { showStudioTutorial = false }
                        ) 
                    }
                    composable(Screen.Creations.route) {
                        CreationsScreen(
                            isAccessEnabled = isGalleryAccessEnabled,
                            isVaultLocked = isVaultLocked,
                            onToggleAccess = { enabled ->
                                scope.launch { themeManager.setGalleryAccessEnabled(enabled) }
                            }
                        )
                    }
                    composable(Screen.VideoPlayer.route) { 
                        VideoPlayerScreen(
                            isPipMode = isPipMode,
                            onSavePosition = { uri, pos -> scope.launch { themeManager.savePlaybackPosition(uri, pos) } },
                            onGetPosition = { uri -> themeManager.getPlaybackPosition(uri) },
                            onClearPosition = { uri -> scope.launch { themeManager.clearPlaybackPosition(uri) } },
                            playerSeekTime = themeManager.playerSeekTime,
                            subtitleFontSizeFlow = themeManager.subtitleFontSize,
                            subtitleColorFlow = themeManager.subtitleColor,
                            subtitleOpacityFlow = themeManager.subtitleOpacity,
                            subtitleEdgeTypeFlow = themeManager.subtitleEdgeType,
                            onSetSubtitleFontSize = { scope.launch { themeManager.setSubtitleFontSize(it) } },
                            onSetSubtitleColor = { scope.launch { themeManager.setSubtitleColor(it) } },
                            onSetSubtitleOpacity = { scope.launch { themeManager.setSubtitleOpacity(it) } },
                            onSetSubtitleEdgeType = { scope.launch { themeManager.setSubtitleEdgeType(it) } },
                            nightFilterEnabledFlow = themeManager.nightFilterEnabled,
                            onSetNightFilterEnabled = { scope.launch { themeManager.setNightFilterEnabled(it) } },
                            vividModeEnabledFlow = themeManager.vividModeEnabled,
                            onSetVividModeEnabled = { scope.launch { themeManager.setVividModeEnabled(it) } },
                            hasSeenTutorial = seenTutorials.contains("player"),
                            onMarkTutorialSeen = { scope.launch { themeManager.markTutorialSeen("player") } },
                            onNavigateBack = { navController.popBackStack() }
                        ) 
                    }
                    composable(Screen.MusicPlayer.route) { 
                        MusicPlayerScreen(
                            playlistDao = playlistDao,
                            hasSeenTutorial = seenTutorials.contains("music"),
                            onMarkTutorialSeen = { scope.launch { themeManager.markTutorialSeen("music") } },
                            onNavigateBack = { navController.popBackStack() }
                        ) 
                    }
                    composable(Screen.SystemHealth.route) { SystemHealthScreen() }
                    composable(Screen.FileTransfer.route) {
                        FileTransferScreen(
                            themeManager = themeManager,
                            hasSeenTutorial = seenTutorials.contains("file_transfer"),
                            onMarkTutorialSeen = { scope.launch { themeManager.markTutorialSeen("file_transfer") } }
                        )
                    }
                    composable(Screen.SupportBot.route) {
                        SupportBotScreen(
                            themeManager = themeManager, 
                            swaraDao = swaraDao, 
                            historyViewModel = historyViewModel,
                            onNavigate = { route -> navController.navigate(route) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.History.route) { HistoryScreen(historyViewModel) }
                    composable(Screen.Settings.route) { 
                        val settingsViewModel = remember { SettingsViewModel(themeManager) }
                        SettingsScreen(
                            settingsViewModel, 
                            onNavigateToSupport = { navController.navigate(Screen.SupportBot.route) },
                            onBack = { navController.popBackStack() }
                        ) 
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), Color.Transparent)
                )
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color.Transparent,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(R.drawable.ic_app_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                "Pro Toolbox",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "ELITE EDITION",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun ExpandableDrawerSection(
    title: String, 
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
        
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                CustomGrid(columns = 2) {
                    content()
                }
            }
        }
    }
}

@Composable
fun CustomGrid(columns: Int, content: @Composable () -> Unit) {
    Layout(content = content) { measurables, constraints ->
        val itemWidth = constraints.maxWidth / columns
        val itemConstraints = constraints.copy(minWidth = itemWidth, maxWidth = itemWidth)
        val placeables = measurables.map { it.measure(itemConstraints) }
        
        val rows = (placeables.size + columns - 1) / columns
        val rowHeight = if (placeables.isNotEmpty()) placeables[0].height else 0
        val gridHeight = rowHeight * rows
        
        layout(constraints.maxWidth, gridHeight) {
            var x = 0
            var y = 0
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(x, y)
                if ((index + 1) % columns == 0) {
                    x = 0
                    y += rowHeight
                } else {
                    x += itemWidth
                }
            }
        }
    }
}

@Composable
fun DrawerFooter(
    currentRoute: String?,
    navController: androidx.navigation.NavController,
    drawerState: DrawerState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FooterLink(Icons.Default.History, "History", currentRoute == Screen.History.route) {
                    navController.navigate(Screen.History.route) { launchSingleTop = true }
                    scope.launch { drawerState.close() }
                }
                FooterLink(Icons.Default.Settings, "Settings", currentRoute == Screen.Settings.route) {
                    navController.navigate(Screen.Settings.route) { launchSingleTop = true }
                    scope.launch { drawerState.close() }
                }
                FooterLink(Icons.Default.SystemUpdate, "Update", false) {
                    // Check update logic
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "v2.1.0 • Private P2P • No Ads",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun FooterLink(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onClick() }.padding(8.dp)
    ) {
        Icon(
            icon, 
            null, 
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Text(
            label, 
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp
        )
    }
}

@Composable
fun DrawerItem(
    screen: Screen,
    icon: ImageVector,
    currentRoute: String?,
    navController: androidx.navigation.NavController,
    drawerState: DrawerState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val selected = currentRoute == screen.route
    Surface(
        onClick = {
            navController.navigate(screen.route) {
                launchSingleTop = true
                restoreState = true
            }
            scope.launch { drawerState.close() }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
        border = if (selected) BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon, 
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(screen.titleRes),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun DrawerDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    )
}
