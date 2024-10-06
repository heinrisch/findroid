package dev.jdtech.jellyfin.models

import android.net.Uri
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.repository.JellyfinRepository
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.io.File
import java.util.UUID

interface FindroidItem {
    val id: UUID
    val name: String
    val originalTitle: String?
    val overview: String
    val played: Boolean
    val favorite: Boolean
    val canPlay: Boolean
    val canDownload: Boolean
    val sources: List<FindroidSource>
    val runtimeTicks: Long
    val playbackPositionTicks: Long
    val unplayedItemCount: Int?
    val images: FindroidImages
    val chapters: List<FindroidChapter>?

    companion object {
        const val DOWNLOAD_FOLDER = "downloads"
        fun downloadFolder(storageLocation: File): Uri =  Uri.fromFile(File(storageLocation, DOWNLOAD_FOLDER))
    }
    fun pathToDownloadTo(storageLocation: File, source: FindroidSource): Uri =  Uri.fromFile(File(storageLocation, "$DOWNLOAD_FOLDER/${id}.${source.id}.download"))
    fun downloadedPath(storageLocation: File, source: FindroidSource): Uri =  Uri.fromFile(File(storageLocation, "$DOWNLOAD_FOLDER/${id}.${source.id}"))
}

suspend fun BaseItemDto.toFindroidItem(
    jellyfinRepository: JellyfinRepository,
    serverDatabase: ServerDatabaseDao? = null,
): FindroidItem? {
    return when (type) {
        BaseItemKind.MOVIE -> toFindroidMovie(jellyfinRepository, serverDatabase)
        BaseItemKind.EPISODE -> toFindroidEpisode(jellyfinRepository)
        BaseItemKind.SEASON -> toFindroidSeason(jellyfinRepository)
        BaseItemKind.SERIES -> toFindroidShow(jellyfinRepository)
        BaseItemKind.BOX_SET -> toFindroidBoxSet(jellyfinRepository)
        BaseItemKind.FOLDER -> toFindroidFolder(jellyfinRepository)
        else -> null
    }
}

fun FindroidItem.isDownloading(): Boolean {
    return sources.filter { it.type == FindroidSourceType.LOCAL }.any { it.path.endsWith(".download") }
}

fun FindroidItem.isDownloaded(): Boolean {
    return sources.filter { it.type == FindroidSourceType.LOCAL }.any { !it.path.endsWith(".download") }
}
