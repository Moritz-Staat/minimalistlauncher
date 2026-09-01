package de.moritzstaat.launcher.ui.usage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.moritzstaat.launcher.LauncherApplication
import de.moritzstaat.launcher.data.app.AppEntry
import de.moritzstaat.launcher.data.usage.UsageBreakerConfig
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** The usage breaker settings, plus the counts they are about. */
class UsageViewModel(application: Application) : AndroidViewModel(application) {

    private val services = (application as LauncherApplication).services
    private val repository = services.usageRepository

    val config: StateFlow<UsageBreakerConfig> = repository.config

    val apps: StateFlow<List<AppEntry>> = services.appIndex.visibleApps

    val usageAccessGranted: StateFlow<Boolean> = repository.usageAccessGranted

    /** Today's number for one app, so the settings can show what the threshold means. */
    fun opensToday(packageName: String): Int = repository.opensToday(packageName)

    fun refresh() = repository.refresh()

    fun setEnabled(enabled: Boolean) = update { it.copy(enabled = enabled) }

    fun setThreshold(threshold: Int) = update { it.copy(threshold = threshold) }

    fun setPauseSeconds(seconds: Int) = update { it.copy(pauseSeconds = seconds) }

    fun toggleApp(packageName: String) = update { config ->
        val packages = config.packages
        config.copy(
            packages = if (packageName in packages) {
                packages - packageName
            } else {
                packages + packageName
            },
        )
    }

    private fun update(transform: (UsageBreakerConfig) -> UsageBreakerConfig) {
        viewModelScope.launch { services.settings.setUsageBreaker(transform(config.value)) }
    }
}
