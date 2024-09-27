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
            .take(10)
            .filter { !it.isDownloaded() }
            .filter { !it.isDownloading() }
            .filter { it.canDownload }
            .forEach {
                Timber.i("Downloading: ${it.seriesName} - ${it.name}")
                downloader.downloadItem(it, it.sources[0].id, 0)
            }

        return Result.success()
    }
}