package de.moritzstaat.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.moritzstaat.launcher.ui.alphabet.AlphabetBar
import de.moritzstaat.launcher.ui.applist.AppListPanel
import de.moritzstaat.launcher.ui.applist.AppListScrim
import de.moritzstaat.launcher.ui.applist.rememberAppListSheetState
import de.moritzstaat.launcher.ui.home.HomeScreen
import de.moritzstaat.launcher.ui.home.HomeViewModel
import de.moritzstaat.launcher.ui.shell.OverlayTarget
import de.moritzstaat.launcher.ui.shell.ShellViewModel
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
    val overlay by shellViewModel.overlay.collectAsStateWithLifecycle()
    val favorites by homeViewModel.favorites.collectAsStateWithLifecycle()
    val apps by homeViewModel.apps.collectAsStateWithLifecycle()
    val sections by homeViewModel.sections.collectAsStateWithLifecycle()

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
        if (overlay != OverlayTarget.AppList) sheetState.close()
    }

    val onSheetSettled: (Boolean) -> Unit = { open ->
        if (open) shellViewModel.open(OverlayTarget.AppList) else shellViewModel.closeOverlays()
    }

    BackHandler(enabled = true) {
        shellViewModel.closeOverlays()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeScreen(
            favorites = favorites,
            iconLoader = homeViewModel.iconLoader,
            sheetState = sheetState,
            onLaunch = homeViewModel::launch,
            onOpenSettings = { shellViewModel.open(OverlayTarget.Settings) },
            onSheetSettled = onSheetSettled,
        )
        AppListScrim(sheetState = sheetState)
        AppListPanel(
            apps = apps,
            iconLoader = homeViewModel.iconLoader,
            sheetState = sheetState,
            listState = listState,
            onLaunch = homeViewModel::launch,
            onSettled = onSheetSettled,
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
        if (overlay == OverlayTarget.Settings) {
            SetupOverlay(onDismiss = { shellViewModel.closeOverlays() })
        }
    }
}
