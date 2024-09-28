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
        repository.getLatestEpisodes()
            .map { repository.getEpisode(it.id) }
            .filter { it.canDownload }
            .map {
                if (it.played && it.isDownloaded()) {
                    downloader.deleteItem(it, it.sources[0])
                }
                it
            }
            .filter { !it.played }
            .take(20)
            .filter { !it.isDownloaded() }
            .filter { !it.isDownloading() }
            .forEach {
                Timber.i("Downloading: ${it.seriesName} - ${it.name}")
                downloader.downloadItem(it, it.sources[0].id, 0)
            }

        return Result.success()
    }
}