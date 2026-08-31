package de.moritzstaat.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.moritzstaat.launcher.data.app.AppSorting
import de.moritzstaat.launcher.data.widget.WidgetSlot
import de.moritzstaat.launcher.ui.actions.AppActionSheet
import de.moritzstaat.launcher.ui.actions.AppActionsViewModel
import de.moritzstaat.launcher.ui.actions.FolderPickerDialog
import de.moritzstaat.launcher.ui.actions.RenameDialog
import de.moritzstaat.launcher.ui.alphabet.AlphabetBar
import de.moritzstaat.launcher.ui.applist.AppListPanel
import de.moritzstaat.launcher.ui.applist.AppListScrim
import de.moritzstaat.launcher.ui.applist.rememberAppListSheetState
import de.moritzstaat.launcher.ui.home.HomeScreen
import de.moritzstaat.launcher.ui.home.HomeViewModel
import de.moritzstaat.launcher.ui.icons.IconChooserDialog
import de.moritzstaat.launcher.ui.popup.AppPopupContent
import de.moritzstaat.launcher.ui.popup.FolderPopupContent
import de.moritzstaat.launcher.ui.popup.LauncherPopup
import de.moritzstaat.launcher.ui.popup.PopupTarget
import de.moritzstaat.launcher.ui.popup.PopupViewModel
import de.moritzstaat.launcher.ui.search.SearchBar
import de.moritzstaat.launcher.ui.search.SearchResultsList
import de.moritzstaat.launcher.ui.search.SearchViewModel
import de.moritzstaat.launcher.ui.shell.OverlayTarget
import de.moritzstaat.launcher.ui.shell.ShellViewModel
import de.moritzstaat.launcher.ui.widget.WidgetPicker
import de.moritzstaat.launcher.ui.widget.WidgetViewModel
import kotlinx.coroutines.launch

/**
 * Root of the launcher UI: home screen, the app list sheet on top of it, the alphabet bar on
 * the right edge and, above all of them, pop-ups and modal overlays.
 *
 * The back gesture is always consumed here. A home screen has nowhere to go back to, and an
 * unhandled back press would drop the user onto a black screen.
 */
@Composable
fun LauncherRoot(shellViewModel: ShellViewModel) {
    val homeViewModel: HomeViewModel = viewModel()
    val searchViewModel: SearchViewModel = viewModel()
    val actionsViewModel: AppActionsViewModel = viewModel()
    val popupViewModel: PopupViewModel = viewModel()
    val widgetViewModel: WidgetViewModel = viewModel()

    val overlay by shellViewModel.overlay.collectAsStateWithLifecycle()
    val favorites by homeViewModel.favorites.collectAsStateWithLifecycle()
    val items by homeViewModel.items.collectAsStateWithLifecycle()
    val sections by homeViewModel.sections.collectAsStateWithLifecycle()
    val folders by homeViewModel.folders.collectAsStateWithLifecycle()
    val query by searchViewModel.query.collectAsStateWithLifecycle()
    val results by searchViewModel.results.collectAsStateWithLifecycle()
    val notifications by homeViewModel.notifications.collectAsStateWithLifecycle()
    val actionsState by actionsViewModel.state.collectAsStateWithLifecycle()
    val renaming by actionsViewModel.renaming.collectAsStateWithLifecycle()
    val folderPicking by actionsViewModel.folderPicking.collectAsStateWithLifecycle()
    val choosingIcon by actionsViewModel.choosingIcon.collectAsStateWithLifecycle()
    val popup by popupViewModel.target.collectAsStateWithLifecycle()
    val popupShortcuts by popupViewModel.shortcuts.collectAsStateWithLifecycle()

    var widgetPickerSlot by remember { mutableStateOf<WidgetSlot?>(null) }

    val scope = rememberCoroutineScope()
    val sheetState = rememberAppListSheetState()
    val listState = rememberLazyListState()

    // First list index of every section. Index 0 is the top inset item of the sheet.
    val sectionStarts = remember(items) {
        buildMap {
            items.forEachIndexed { index, item ->
                putIfAbsent(AppSorting.sectionFor(item.label), index + 1)
            }
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
            popup != null -> popupViewModel.dismiss()
            query.isNotEmpty() -> searchViewModel.clear()
            else -> shellViewModel.closeOverlays()
        }
    }

    // The whole launcher blurs behind an open pop-up; read in the draw phase only.
    val popupBlur = animateFloatAsState(
        targetValue = if (popup != null) 1f else 0f,
        label = "popupBlur",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val progress = popupBlur.value
                    if (progress > 0.01f) {
                        val radius = POPUP_BLUR_PX * progress
                        renderEffect = BlurEffect(radius, radius, TileMode.Decal)
                    }
                },
        ) {
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
                items = items,
                iconLoader = homeViewModel.iconLoader,
                sheetState = sheetState,
                listState = listState,
                onLaunch = homeViewModel::launch,
                onOpenFolder = popupViewModel::openFolder,
                onLongPress = actionsViewModel::open,
                onOpenPopup = popupViewModel::openApp,
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
                    // Touching the bar opens the list before the first letter change.
                    shellViewModel.open(OverlayTarget.AppList)
                    sheetState.open(onSheetSettled)
                },
                onSectionActive = { section ->
                    val target = sectionStarts[section] ?: return@AlphabetBar
                    scope.launch { listState.scrollToItem(target) }
                },
            )
        }

        popup?.let { target ->
            LauncherPopup(anchorTopPx = target.anchorTopPx, onDismiss = popupViewModel::dismiss) {
                when (target) {
                    is PopupTarget.App -> AppPopupContent(
                        entry = target.entry,
                        iconLoader = popupViewModel.iconLoader,
                        shortcuts = popupShortcuts,
                        shortcutRepository = popupViewModel.shortcutRepository,
                        notification = notifications[target.entry.key.packageName],
                        onLaunch = {
                            homeViewModel.launch(target.entry.key)
                            popupViewModel.dismiss()
                        },
                        onShortcut = popupViewModel::startShortcut,
                        onNotificationClick = {
                            notifications[target.entry.key.packageName]
                                ?.let(homeViewModel::openNotification)
                            popupViewModel.dismiss()
                        },
                        onNotificationDismiss = {
                            notifications[target.entry.key.packageName]
                                ?.let(homeViewModel::dismissNotification)
                        },
                        onOpenActions = {
                            popupViewModel.dismiss()
                            actionsViewModel.open(target.entry, null)
                        },
                    )

                    is PopupTarget.Folder -> FolderPopupContent(
                        folder = target.folder,
                        iconLoader = popupViewModel.iconLoader,
                        notifications = notifications,
                        onLaunch = { entry ->
                            homeViewModel.launch(entry.key)
                            popupViewModel.dismiss()
                        },
                        onLongPress = { entry ->
                            popupViewModel.dismiss()
                            actionsViewModel.open(entry, null)
                        },
                    )
                }
            }
        }

        actionsState?.let { state ->
            AppActionSheet(
                state = state,
                shortcutRepository = homeViewModel.shortcutRepository,
                onDismiss = actionsViewModel::dismiss,
                onShortcut = actionsViewModel::startShortcut,
                onToggleFavorite = actionsViewModel::toggleFavorite,
                onRename = actionsViewModel::startRename,
                onChangeIcon = actionsViewModel::startIconChooser,
                onMoveToFolder = actionsViewModel::startFolderPicking,
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
        choosingIcon?.let { entry ->
            IconChooserDialog(entry = entry, onDismiss = actionsViewModel::cancelIconChooser)
        }
        folderPicking?.let { entry ->
            FolderPickerDialog(
                entry = entry,
                folders = folders,
                onDismiss = actionsViewModel::cancelFolderPicking,
                onPickExisting = actionsViewModel::moveToFolder,
                onCreate = actionsViewModel::moveToNewFolder,
                onRemoveFromFolders = actionsViewModel::removeFromFolders,
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

private const val POPUP_BLUR_PX = 22f
