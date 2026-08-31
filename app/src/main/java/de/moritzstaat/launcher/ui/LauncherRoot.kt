package de.moritzstaat.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.moritzstaat.launcher.ui.applist.AppListPanel
import de.moritzstaat.launcher.ui.applist.AppListScrim
import de.moritzstaat.launcher.ui.applist.rememberAppListSheetState
import de.moritzstaat.launcher.ui.home.HomeScreen
import de.moritzstaat.launcher.ui.home.HomeViewModel
import de.moritzstaat.launcher.ui.shell.OverlayTarget
import de.moritzstaat.launcher.ui.shell.ShellViewModel

/**
 * Root of the launcher UI: home screen, the app list sheet on top of it and, above both, the
 * modal overlays.
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

    val sheetState = rememberAppListSheetState()
    val listState = rememberLazyListState()

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
        if (overlay == OverlayTarget.Settings) {
            SetupOverlay(onDismiss = { shellViewModel.closeOverlays() })
        }
    }
}
