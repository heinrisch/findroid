package dev.jdtech.jellyfin.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.jdtech.jellyfin.models.isDownloaded
import dev.jdtech.jellyfin.models.isDownloading
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.utils.Downloader
import timber.log.Timber


@HiltWorker
class VideoDownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    val repository: JellyfinRepository,
    val downloader: Downloader,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Timber.i("Starting VideoDownloader")

        repository.getLatestEpisodesForDownload()
            .filter { it.played }
            .filter { it.isDownloaded() }
            .forEach { e->
                e.sources.forEach {
                    Timber.i("Deleting played item ${e.seriesName} - ${e.name}")
                    downloader.deleteItem(e, it)
                }
            }

        val maxDownloads = 20
        val currentDownloads = repository.getDownloads().filter { !it.isDownloaded() && !it.isDownloading() }.size

        Timber.i(repository.getLatestEpisodesForDownload().joinToString("\n") {
            "${it.seriesName} - ${it.name} - canDownload ${it.canDownload} - played ${it.played} - downloaded ${it.isDownloaded()} - downloading ${it.isDownloading()} "
        })

        repository.getLatestEpisodesForDownload()
            .filter { it.canDownload }
            .filter { !it.played }
            .take(maxOf(0, maxDownloads - currentDownloads))
            .filter { !it.isDownloaded() }
            .filter { !it.isDownloading() }
            .forEach {
                Timber.i("Downloading: ${it.seriesName} - ${it.name}")
                downloader.downloadItem(it, it.sources[0].id, 0)
            }

        return Result.success()
    }
}