package dev.jdtech.jellyfin.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.Constants
import dev.jdtech.jellyfin.core.R
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.FavoriteListItem
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.models.View
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.utils.Downloader
import dev.jdtech.jellyfin.work.VideoDownloadWorker
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel
@Inject
constructor(
    private val application: Application,
    private val appPreferences: AppPreferences,
    private val repository: JellyfinRepository,
    private val downloader: Downloader
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val eventsChannel = Channel<DownloadsEvent>()
    val eventsChannelFlow = eventsChannel.receiveAsFlow()

    sealed class UiState {
        data class Normal(val sections: List<FavoriteListItem>) : UiState()
        data object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    init {
        testServerConnection()
    }

    private fun testServerConnection() {
        viewModelScope.launch {
            try {
                if (appPreferences.offlineMode) return@launch
                repository.getPublicSystemInfo()
                // Give the UI a chance to load
                delay(100)
            } catch (e: Exception) {
                eventsChannel.send(DownloadsEvent.ConnectionError(e))
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)

            val sections = mutableListOf<FavoriteListItem>()
            val items = repository.getDownloads()

            sections.add(FavoriteListItem.DownloadStats(items))

            FavoriteListItem.FavoriteSection(
                Constants.FAVORITE_TYPE_EPISODES,
                UiText.StringResource(R.string.continue_watching),
                items.filter { it.playbackPositionTicks > 0 }
            ).let {
                if (it.items.isNotEmpty()) {
                    sections.add(it)
                }
            }

            items.filterIsInstance<FindroidEpisode>()
                .sortedByDescending { it.sortingDate }
                .forEach { episode ->
                    FavoriteListItem.FavoriteItem(
                        View(
                            episode.id,
                            "ignore",
                            listOf(episode),
                            CollectionType.TvShows
                        )
                    ).let {
                        sections.add(it)

                    }
                }

            _uiState.emit(UiState.Normal(sections))
        }
    }

    fun clearOldestDownloads() {
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            val items = repository.getDownloads()
            items.filterIsInstance<FindroidEpisode>()
                .sortedBy { it.sortingDate }
                .take(5)
                .forEach { e ->
                    e.sources.forEach {
                        Timber.i("Deleting item ${e.seriesName} - ${e.name}")
                        downloader.deleteItem(e, it)
                    }
                }
            loadData()
        }
    }

    fun sync() {
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            val workRequest = OneTimeWorkRequestBuilder<VideoDownloadWorker>()
                .build()
            val workManager = WorkManager.getInstance(application)
                workManager.enqueueUniqueWork(VideoDownloadWorker.TAG, ExistingWorkPolicy.REPLACE, workRequest)
            loadData()
        }
    }
}

sealed interface DownloadsEvent {
    data class ConnectionError(val error: Exception) : DownloadsEvent
}
