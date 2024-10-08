package dev.jdtech.jellyfin.models

import java.util.UUID


sealed class FavoriteListItem {

    data class FavoriteSection(
        override val id: UUID,
        val name: UiText,
        var items: List<FindroidItem>,
    ) : FavoriteListItem()

    data class FavoriteItem(
        val view: View
    ): FavoriteListItem() {
        override val id: UUID
            get() = view.id
    }

    class DownloadStats(val items: List<FindroidItem>) : FavoriteListItem() {
        override val id: UUID = UUID.randomUUID()
    }

    abstract val id: UUID
}