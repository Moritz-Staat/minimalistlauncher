package de.moritzstaat.launcher.ui.search

import android.app.Application
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.moritzstaat.launcher.LauncherApplication
import de.moritzstaat.launcher.data.search.ContactHit
import de.moritzstaat.launcher.data.search.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

/** Search field state and results. */
class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val services = (application as LauncherApplication).services
    private val appContext = application.applicationContext

    val iconLoader = services.iconLoader

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val results: StateFlow<List<SearchResult>> =
        combine(_query.debounce(DEBOUNCE_MS), services.appIndex.visibleApps) { query, apps ->
            query to apps
        }
            .mapLatest { (query, apps) -> services.searchEngine.search(query, apps) }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    fun setQuery(value: String) {
        _query.value = value
    }

    fun clear() {
        _query.value = ""
    }

    fun openContact(hit: ContactHit) {
        start(Intent(Intent.ACTION_VIEW, hit.contentUri()))
    }

    /**
     * Hands the query to whatever handles web searches. Falls back to a plain search URL when
     * no app answers ACTION_WEB_SEARCH, so the last row is never a dead end.
     */
    fun openWebSearch(query: String) {
        val webSearch = Intent(Intent.ACTION_WEB_SEARCH)
            .putExtra(SearchManager.QUERY, query)
        if (!start(webSearch)) {
            val url = "https://duckduckgo.com/?q=" + Uri.encode(query)
            start(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    fun startShortcut(result: SearchResult.Shortcut, bounds: Rect?) {
        services.shortcutRepository.start(result.shortcut, bounds)
    }

    private fun start(intent: Intent): Boolean = try {
        appContext.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    private companion object {
        const val DEBOUNCE_MS = 60L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
