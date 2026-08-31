package de.moritzstaat.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.moritzstaat.launcher.ui.actions.AppActionSheet
import de.moritzstaat.launcher.ui.actions.AppActionsViewModel
import de.moritzstaat.launcher.ui.actions.RenameDialog
import de.moritzstaat.launcher.ui.alphabet.AlphabetBar
import de.moritzstaat.launcher.ui.applist.AppListPanel
import de.moritzstaat.launcher.ui.applist.AppListScrim
import de.moritzstaat.launcher.ui.applist.rememberAppListSheetState
import de.moritzstaat.launcher.ui.home.HomeScreen
import de.moritzstaat.launcher.ui.home.HomeViewModel
import de.moritzstaat.launcher.ui.search.SearchBar
import de.moritzstaat.launcher.ui.search.SearchResultsList
import de.moritzstaat.launcher.ui.search.SearchViewModel
import de.moritzstaat.launcher.ui.shell.OverlayTarget
import de.moritzstaat.launcher.ui.shell.ShellViewModel
import de.moritzstaat.launcher.ui.widget.WidgetPicker
import de.moritzstaat.launcher.ui.widget.WidgetViewModel
import de.moritzstaat.launcher.data.widget.WidgetSlot
import kotlinx.coroutines.launch

/**
 * Root of the launcher UI: home screen, the app list sheet on top of it, the alphabet bar on
 * the right edge and, above all of them, the modal overlays.
 *
 * The back gesture is always consumed here. A home screen has nowhere to go back to, and an
 * unhandled back press would drop the user onto a black screen.
 */
@Composable
fun LauncherRoot(shellViewModel: ShellViewModel) {
    val homeViewModel: HomeViewModel = viewModel()
    val searchViewModel: SearchViewModel = viewModel()
    val actionsViewModel: AppActionsViewModel = viewModel()

    val overlay by shellViewModel.overlay.collectAsStateWithLifecycle()
    val favorites by homeViewModel.favorites.collectAsStateWithLifecycle()
    val apps by homeViewModel.apps.collectAsStateWithLifecycle()
    val sections by homeViewModel.sections.collectAsStateWithLifecycle()
    val query by searchViewModel.query.collectAsStateWithLifecycle()
    val results by searchViewModel.results.collectAsStateWithLifecycle()
    val notifications by homeViewModel.notifications.collectAsStateWithLifecycle()
    val actionsState by actionsViewModel.state.collectAsStateWithLifecycle()
    val renaming by actionsViewModel.renaming.collectAsStateWithLifecycle()

    val widgetViewModel: WidgetViewModel = viewModel()
    var widgetPickerSlot by remember { mutableStateOf<WidgetSlot?>(null) }

    val scope = rememberCoroutineScope()
    val sheetState = rememberAppListSheetState()
    val listState = rememberLazyListState()

    // First list index of every section. Index 0 is the sheet's top inset item.
    val sectionStarts = remember(apps) {
        buildMap {
            apps.forEachIndexed { index, entry -> putIfAbsent(entry.section, index + 1) }
        }
    }

    // The shell owns the truth about what is open; the sheet only reports where it landed.
    LaunchedEffect(overlay) {
        if (overlay != OverlayTarget.AppList) {
            sheetState.close()
            searchViewModel.clear()
        }
    }

    val onSheetSettled: (Boolean) -> Unit = { open ->
        if (open) shellViewModel.open(OverlayTarget.AppList) else shellViewModel.closeOverlays()
    }

    BackHandler(enabled = true) {
        when {
            widgetPickerSlot != null -> widgetPickerSlot = null
            actionsState != null -> actionsViewModel.dismiss()
            query.isNotEmpty() -> searchViewModel.clear()
            else -> shellViewModel.closeOverlays()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeScreen(
            favorites = favorites,
            iconLoader = homeViewModel.iconLoader,
            sheetState = sheetState,
            onLaunch = homeViewModel::launch,
            onLongPressFavorite = actionsViewModel::open,
            onReorderFavorites = homeViewModel::reorderFavorites,
            onOpenSettings = { shellViewModel.open(OverlayTarget.Settings) },
            onSheetSettled = onSheetSettled,
            notifications = notifications,
            onNotificationClick = homeViewModel::openNotification,
            onNotificationDismiss = homeViewModel::dismissNotification,
        )
        AppListScrim(sheetState = sheetState)
        AppListPanel(
            apps = apps,
            iconLoader = homeViewModel.iconLoader,
            sheetState = sheetState,
            listState = listState,
            onLaunch = homeViewModel::launch,
            onLongPress = actionsViewModel::open,
            onSettled = onSheetSettled,
            notifications = notifications,
            onNotificationClick = homeViewModel::openNotification,
            onNotificationDismiss = homeViewModel::dismissNotification,
            searchActive = query.isNotBlank(),
            resultsContent = {
                SearchResultsList(
                    results = results,
                    iconLoader = searchViewModel.iconLoader,
                    onLaunchApp = homeViewModel::launch,
                    onLongPressApp = actionsViewModel::open,
                    onShortcut = { searchViewModel.startShortcut(it, null) },
                    onContact = { searchViewModel.openContact(it.hit) },
                    onWebSearch = searchViewModel::openWebSearch,
                )
            },
            overlayContent = {
                SearchBar(
                    query = query,
                    onQueryChange = searchViewModel::setQuery,
                    onSubmit = { searchViewModel.openWebSearch(query) },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            },
        )
        AlphabetBar(
            sections = sections,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .safeDrawingPadding(),
            onTouchStart = {
                // Touching the bar opens the list immediately, before the first letter change.
                shellViewModel.open(OverlayTarget.AppList)
                sheetState.open(onSheetSettled)
            },
            onSectionActive = { section ->
                val target = sectionStarts[section] ?: return@AlphabetBar
                scope.launch { listState.scrollToItem(target) }
            },
        )
        actionsState?.let { state ->
            AppActionSheet(
                state = state,
                shortcutRepository = homeViewModel.shortcutRepository,
                onDismiss = actionsViewModel::dismiss,
                onShortcut = actionsViewModel::startShortcut,
                onToggleFavorite = actionsViewModel::toggleFavorite,
                onRename = actionsViewModel::startRename,
                onChangeIcon = actionsViewModel::dismiss,
                onToggleNotificationRedaction = actionsViewModel::toggleNotificationRedaction,
                onHide = actionsViewModel::hide,
                onAppInfo = actionsViewModel::openAppInfo,
                onUninstall = actionsViewModel::requestUninstall,
            )
        }
        renaming?.let { entry ->
            RenameDialog(
                entry = entry,
                onDismiss = actionsViewModel::cancelRename,
                onConfirm = actionsViewModel::confirmRename,
            )
        }
        if (overlay == OverlayTarget.Settings) {
            SetupOverlay(
                onDismiss = { shellViewModel.closeOverlays() },
                onAddWidget = { slot -> widgetPickerSlot = slot },
            )
        }
        widgetPickerSlot?.let { slot ->
            WidgetPicker(
                slot = slot,
                ownerKey = null,
                host = widgetViewModel.host,
                onPlaced = { id -> widgetViewModel.place(id, slot) },
                onDiscard = widgetViewModel::discard,
                onDismiss = { widgetPickerSlot = null },
            )
        }
    }
}
