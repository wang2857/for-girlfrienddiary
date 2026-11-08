package com.synn.diary.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.synn.diary.data.AppContainer
import com.synn.diary.ui.calendar.CalendarScreen
import com.synn.diary.ui.calendar.CalendarViewModel
import com.synn.diary.ui.editor.EditorScreen
import com.synn.diary.ui.lock.UnlockEntryDialog
import com.synn.diary.ui.editor.EditorViewModel
import com.synn.diary.ui.settings.SettingsScreen
import com.synn.diary.ui.trash.TrashScreen
import com.synn.diary.ui.trash.TrashViewModel
import com.synn.diary.ui.search.SearchScreen
import com.synn.diary.ui.search.SearchViewModel
import com.synn.diary.ui.timeline.TimelineScreen
import com.synn.diary.ui.timeline.TimelineViewModel
import com.synn.diary.util.VmFactory
import kotlinx.coroutines.launch

private const val ROUTE_TIMELINE = "timeline"
private const val ROUTE_CALENDAR = "calendar"
private const val ROUTE_SEARCH = "search"
private const val ROUTE_EDITOR = "editor/{entryId}"
private const val KEY_LOCATION_ASKED = "location_permission_asked"

@Composable
fun DiaryNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val switchTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("diary_prefs", Context.MODE_PRIVATE) }

    // 所有"打开某篇日记"的入口都走这里：时间线、日历、搜索都一样。
    // 上锁的条目必须先验指纹或密码，不能从别的入口绕过去。
    var pendingUnlock by remember { mutableStateOf<Long?>(null) }
    val openEntry: (Long) -> Unit = { id ->
        scope.launch {
            val entry = container.entryRepository.find(id)
            if (entry?.locked == true && container.lockManager.canLockEntries()) {
                pendingUnlock = id
            } else {
                // 锁着但没有可用凭证（老数据 / 刚关掉应用锁）：直接放开，避免日记永远打不开
                if (entry?.locked == true) {
                    container.entryRepository.setLocked(id, false)
                }
                navController.navigate("editor/$id")
            }
        }
    }
    val createEntry: () -> Unit = {
        scope.launch {
            val id = container.entryRepository.create()
            navController.navigate("editor/$id")
        }
    }

    // 定位只在第一次"写一笔"时问一次。拒绝就不再打扰，之后可以在系统设置里开。
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { _ ->
        // 给不给权限都要能写日记
        createEntry()
    }

    CompositionLocalProvider(
        LocalImageFiles provides { name -> container.imageStore.file(name) },
    ) {
        NavHost(navController = navController, startDestination = ROUTE_TIMELINE) {
        composable(ROUTE_TIMELINE) {
            val viewModel: TimelineViewModel = viewModel(
                factory = VmFactory { TimelineViewModel(container.entryRepository) },
            )
            TimelineScreen(
                viewModel = viewModel,
                lockManager = container.lockManager,
                onOpenEntry = openEntry,
                onCreateEntry = {
                    // 先入库再跳转：后续每次按键都能按 id 直接更新，不必区分"新建/编辑"两套逻辑。
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ) == PackageManager.PERMISSION_GRANTED
                    val askedBefore = prefs.getBoolean(KEY_LOCATION_ASKED, false)
                    if (!granted && !askedBefore) {
                        prefs.edit().putBoolean(KEY_LOCATION_ASKED, true).apply()
                        locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    } else {
                        createEntry()
                    }
                },
                onOpenSearch = { navController.navigate(ROUTE_SEARCH) },
                onSelectTab = switchTab,
            )
        }

        composable(ROUTE_CALENDAR) {
            val viewModel: CalendarViewModel = viewModel(
                factory = VmFactory { CalendarViewModel(container.entryRepository) },
            )
            CalendarScreen(
                viewModel = viewModel,
                onOpenEntry = openEntry,
                onSelectTab = switchTab,
            )
        }

        composable("trash") {
            val viewModel: TrashViewModel = viewModel(
                factory = VmFactory { TrashViewModel(container.entryRepository, container.trashSettings) },
            )
            TrashScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }

        composable("settings") {
            SettingsScreen(
                lockManager = container.lockManager,
                onOpenTrash = { navController.navigate("trash") },
                onDisableLock = {
                    container.lockManager.disableLock()
                    // 没有密码就再也验不了单篇锁，所以一并解开，避免日记打不开
                    scope.launch { container.entryRepository.unlockAllEntries() }
                },
                onSelectTab = switchTab,
            )
        }

        composable(ROUTE_SEARCH) {
            val viewModel: SearchViewModel = viewModel(
                factory = VmFactory { SearchViewModel(container.entryRepository) },
            )
            SearchScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenEntry = openEntry,
            )
        }

        composable(
            route = ROUTE_EDITOR,
            arguments = listOf(navArgument("entryId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getLong("entryId") ?: -1L
            val viewModel: EditorViewModel = viewModel(
                key = "editor-$entryId",
                factory = VmFactory {
                    EditorViewModel(
                        repository = container.entryRepository,
                        weatherService = container.weatherService,
                        imageStore = container.imageStore,
                        appScope = container.appScope,
                        entryId = entryId,
                    )
                },
            )
            EditorScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

    // 上锁日记的验证框，放在 NavHost 外面，任何入口触发都走同一个框
    pendingUnlock?.let { target ->
        UnlockEntryDialog(
            verifyPin = container.lockManager::verifyPin,
            biometricEnabled = container.lockManager.biometricEnabled,
            onUnlocked = {
                pendingUnlock = null
                navController.navigate("editor/$target")
            },
            onDismiss = { pendingUnlock = null },
        )
    }
}
