package dev.jdtech.jellyfin.work

import android.app.DownloadManager
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidSourceType
import dev.jdtech.jellyfin.models.isDownloaded
import dev.jdtech.jellyfin.models.isDownloading
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.utils.Downloader
import kotlinx.coroutines.delay
import timber.log.Timber
import java.io.File


@HiltWorker
class VideoDownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    val databaseDao: ServerDatabaseDao,
    val repository: JellyfinRepository,
    val downloader: Downloader,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Timber.i("Starting VideoDownloader")

        repository.getLatestEpisodesForDownload()
            .filter { it.played }
            .filter { it.isDownloaded() }
            .forEach { e ->
                e.sources.forEach {
                    Timber.i("Deleting played item ${e.seriesName} - ${e.name}")
                    downloader.deleteItem(e, it)
                    delay(10000)
                }
            }

        repository.getDownloads()
            .filterIsInstance<FindroidEpisode>()
            .filter { it.isDownloaded() }
            .filter { it.played }
            .forEach { e ->
                e.sources.forEach {
                    Timber.i("Deleting played items from downloads ${e.seriesName} - ${e.name}")
                    downloader.deleteItem(e, it)
                    delay(10000)
                }
            }

        val storageLocation = context.getExternalFilesDirs(null)[0]
        val downloadFolder = FindroidItem.downloadFolder(storageLocation)
        val downloadPaths = File(downloadFolder.path!!).listFiles()!!.map { it.path }
        val visitedPaths = mutableSetOf<String>()
        repository.getDownloads().filterIsInstance<FindroidEpisode>().forEach { item ->
            val sources = item.sources.filter { it.type == FindroidSourceType.LOCAL }
            sources.forEach { source ->
                if (source.path in downloadPaths) {
                    visitedPaths.add(source.path)
                } else {
                    Timber.i("Source does not exist in download folder, deleting: ${source.name}")
                    databaseDao.deleteSource(source.id)
                }
            }
            if (sources.isEmpty()) {
                Timber.i("Download has no sources: ${item.name}")
                databaseDao.deleteEpisode(item.id)
            }
        }

        downloadPaths.filter { it !in visitedPaths }.forEach {
            Timber.i("Download not connected to source, deleting: $it")
            File(it).delete()
        }

        val maxDownloads = 20
        val downloadCount = repository.getDownloads()
            .filterIsInstance<FindroidEpisode>()
            .distinctBy { it.id }
            .filter { it.isDownloaded() || it.isDownloading() }
            .filter { !it.played }
            .size
        Timber.i("Current number of downloads: $downloadCount")

        repository.getLatestEpisodesForDownload()
            .filter { it.canDownload }
            .filter { !it.played }
            .filter { !it.isDownloading() }
            .filter { !it.isDownloaded() }
            .take(maxOf(0, maxDownloads - downloadCount))
            .filter {
                Timber.i("To sync: ${it.seriesName} - ${it.name} - canDownload ${it.canDownload} - played ${it.played} - downloaded ${it.isDownloaded()} - downloading ${it.isDownloading()}")
                true
            }
            .forEach { episode ->
                Timber.i("Downloading: ${episode.seriesName} - ${episode.name}")
                val (downloadId, error) = downloader.downloadItem(episode, episode.sources[0].id, 0)
                error?.let { Timber.e("Got error while trying to download: $it") }
                var maxRuns = 50
                do {
                    delay(6000)
                    val (status, progress) = downloader.getProgress(downloadId)
                    Timber.i("Download progress: $progress | $status | retries: $maxRuns")
                    if (status !in listOf(DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING)) {
                        Timber.i("Status not running or pending, stopping delay | $status")
                        maxRuns = 0
                    }
                } while (--maxRuns > 0)
            }

        return Result.success()
    }

    companion object {
        const val TAG = "video-downloader-@383fb86"
    }
}