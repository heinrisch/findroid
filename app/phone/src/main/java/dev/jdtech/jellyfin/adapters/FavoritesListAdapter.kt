package dev.jdtech.jellyfin.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.Constants
import dev.jdtech.jellyfin.adapters.ViewListAdapter.ViewSingleViewHolder
import dev.jdtech.jellyfin.bindCardItemImage
import dev.jdtech.jellyfin.bindItemImage
import dev.jdtech.jellyfin.databinding.FavoriteSectionBinding
import dev.jdtech.jellyfin.databinding.ViewDownloadStatsBinding
import dev.jdtech.jellyfin.databinding.ViewSingleItemBinding
import dev.jdtech.jellyfin.models.FavoriteListItem
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.isDownloaded
import dev.jdtech.jellyfin.models.isDownloading
import dev.jdtech.jellyfin.utils.DateUtils

private const val ITEM_VIEW_TYPE_SECTION = 0
private const val ITEM_VIEW_TYPE_ITEM = 1
private const val ITEM_VIEW_TYPE_DOWNLOAD_STATS = 2

class FavoritesListAdapter(
    private val onItemClickListener: (item: FindroidItem) -> Unit,
) : ListAdapter<FavoriteListItem, RecyclerView.ViewHolder>(DiffCallback) {

    class SectionViewHolder(private var binding: FavoriteSectionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            section: FavoriteListItem,
            onItemClickListener: (item: FindroidItem) -> Unit,
        ) {
            if (section is FavoriteListItem.FavoriteSection) {
                if (section.id == Constants.FAVORITE_TYPE_MOVIES || section.id == Constants.FAVORITE_TYPE_SHOWS) {
                    binding.itemsRecyclerView.adapter =
                        ViewItemListAdapter(onItemClickListener, fixedWidth = true)
                    (binding.itemsRecyclerView.adapter as ViewItemListAdapter).submitList(section.items)
                } else if (section.id == Constants.FAVORITE_TYPE_EPISODES) {
                    binding.itemsRecyclerView.adapter =
                        HomeEpisodeListAdapter(onItemClickListener)
                    (binding.itemsRecyclerView.adapter as HomeEpisodeListAdapter).submitList(section.items)
                }
                binding.sectionName.text = section.name.asString(binding.root.resources)
            } else {
                throw UnsupportedOperationException("Not supported item")
            }
        }
    }

    class ViewSingleViewHolder(private var binding: ViewSingleItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            dataItem: FavoriteListItem.FavoriteItem,
            onItemClickListener: (item: FindroidItem) -> Unit,
        ) {
            val view = dataItem.view
            val item = view.items!!.first()
            binding.viewName.text = item.name
            binding.viewDate.text = when (item) {
                is FindroidEpisode -> DateUtils.formatDateTimeToUserPreference(item.sortingDate)
                else -> "unknown"
            }
            bindCardItemImage(binding.itemImage, item)
            bindItemImage(binding.profileIcon, item)
            binding.downloadedIcon.isVisible = item.isDownloaded()
            binding.itemImage.setOnClickListener {
                onItemClickListener(item)
            }

        }
    }

    class ViewDownloadStatsViewHolder(private var binding: ViewDownloadStatsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(
            dataItem: FavoriteListItem.DownloadStats,
            onItemClickListener: (item: FindroidItem) -> Unit,
        ) {
            val items = dataItem.items
            binding.downloadsTop.text = "Downloads: ${items.filterIsInstance<FindroidEpisode>().distinctBy { it.id }.size}"
            binding.downloadsBottom.text = "In Progress: ${items.filterIsInstance<FindroidEpisode>().distinctBy { it.id }.filter { it.isDownloading() }.size}"
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<FavoriteListItem>() {
        override fun areItemsTheSame(
            oldItem: FavoriteListItem,
            newItem: FavoriteListItem
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: FavoriteListItem,
            newItem: FavoriteListItem,
        ): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            ITEM_VIEW_TYPE_SECTION -> SectionViewHolder(
                FavoriteSectionBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                ),
            )
            ITEM_VIEW_TYPE_ITEM -> ViewSingleViewHolder(
                ViewSingleItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                ),
            )
            ITEM_VIEW_TYPE_DOWNLOAD_STATS -> ViewDownloadStatsViewHolder(
                ViewDownloadStatsBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                ),
            )
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is FavoriteListItem.FavoriteItem -> ITEM_VIEW_TYPE_ITEM
            is FavoriteListItem.FavoriteSection -> ITEM_VIEW_TYPE_SECTION
            is FavoriteListItem.DownloadStats -> ITEM_VIEW_TYPE_DOWNLOAD_STATS
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val collection = getItem(position)

        when (holder.itemViewType) {
            ITEM_VIEW_TYPE_SECTION -> {
                (holder as SectionViewHolder).bind(collection, onItemClickListener)
            }
            ITEM_VIEW_TYPE_ITEM -> {
                val view = getItem(position) as FavoriteListItem.FavoriteItem
                (holder as ViewSingleViewHolder).bind(view, onItemClickListener)
            }
            ITEM_VIEW_TYPE_DOWNLOAD_STATS -> {
                val view = getItem(position) as FavoriteListItem.DownloadStats
                (holder as ViewDownloadStatsViewHolder).bind(view, onItemClickListener)
            }
        }
    }
}
