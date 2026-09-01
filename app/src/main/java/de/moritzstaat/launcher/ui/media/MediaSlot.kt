package de.moritzstaat.launcher.ui.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.moritzstaat.launcher.ui.applist.AppRow

/**
 * What sits in the home screen's widget slot: the media controls while something is playing,
 * and right after an audio output connects the music apps the user picked.
 *
 * Both appear and disappear on their own; the slot collapses to nothing when neither applies,
 * so the home screen layout does not jump.
 */
@Composable
fun MediaSlot(modifier: Modifier = Modifier) {
    val viewModel: MediaViewModel = viewModel()
    val media by viewModel.media.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestedMediaApps.collectAsStateWithLifecycle()
    val outputName by viewModel.outputName.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = media != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            media?.let { state ->
                MediaWidget(
                    state = state,
                    onPlayPause = viewModel::playOrPause,
                    onSkipNext = viewModel::skipNext,
                    onSkipPrevious = viewModel::skipPrevious,
                    onOpenApp = viewModel::openSessionApp,
                    outputName = outputName,
                    customActionIcon = viewModel::customActionIcon,
                    onCustomAction = viewModel::sendCustomAction,
                )
            }
        }
        AnimatedVisibility(
            visible = suggestions.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                suggestions.forEach { entry ->
                    AppRow(
                        entry = entry,
                        iconLoader = viewModel.iconLoader,
                        onClick = { viewModel.launch(entry.key) },
                    )
                }
            }
        }
    }
}
