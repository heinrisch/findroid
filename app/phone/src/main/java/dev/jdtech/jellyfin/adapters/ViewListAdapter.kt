package dev.jdtech.jellyfin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.bindCardItemImage
import dev.jdtech.jellyfin.bindItemBackdropImage
import dev.jdtech.jellyfin.bindItemImage
import dev.jdtech.jellyfin.bindPersonImage
import dev.jdtech.jellyfin.bindUserImage
import dev.jdtech.jellyfin.databinding.CardOfflineBinding
import dev.jdtech.jellyfin.databinding.NextUpSectionBinding
import dev.jdtech.jellyfin.databinding.ViewItemBinding
import dev.jdtech.jellyfin.databinding.ViewSingleItemBinding
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.HomeItem
import dev.jdtech.jellyfin.models.View
import dev.jdtech.jellyfin.models.isDownloaded
import dev.jdtech.jellyfin.models.toFindroidUserDataDto
import dev.jdtech.jellyfin.utils.DateUtils
import dev.jdtech.jellyfin.core.R as CoreR

private const val ITEM_VIEW_TYPE_NEXT_UP = 0
private const val ITEM_VIEW_TYPE_VIEW = 1
private const val ITEM_VIEW_TYPE_OFFLINE_CARD = 2
private const val ITEM_SINGLE_TYPE_VIEW = 3

class   ViewListAdapter(
    private val onClickListener: (view: View) -> Unit,
    private val onItemClickListener: (item: FindroidItem) -> Unit,
    private val onOnlineClickListener: () -> Unit,
) : ListAdapter<HomeItem, RecyclerView.ViewHolder>(DiffCallback) {

    class ViewViewHolder(private var binding: ViewItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            dataItem: HomeItem.ViewItem,
            onClickListener: (view: View) -> Unit,
            onItemClickListener: (item: FindroidItem) -> Unit,
        ) {
            val view = dataItem.view
            binding.viewName.text = binding.viewName.context.resources.getString(CoreR.string.latest_library, view.name)
            binding.itemsRecyclerView.adapter =
                ViewItemListAdapter(onItemClickListener, fixedWidth = true)
            (binding.itemsRecyclerView.adapter as ViewItemListAdapter).submitList(view.items)
            binding.viewAll.setOnClickListener {
                onClickListener(view)
            }
        }
    }

    class ViewSingleViewHolder(private var binding: ViewSingleItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            dataItem: HomeItem.ViewItem,
            onItemClickListener: (item: FindroidItem) -> Unit,
        ) {
            val view = dataItem.view
            val item = view.items!!.first()
            binding.viewName.text = item.name
            binding.viewDate.text = when(item) {
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

    class NextUpViewHolder(private var binding: NextUpSectionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            section: HomeItem.Section,
            onClickListener: (item: FindroidItem) -> Unit,
        ) {
            binding.sectionName.text = section.homeSection.name.asString(binding.sectionName.context.resources)
            binding.itemsRecyclerView.adapter = HomeEpisodeListAdapter(onClickListener)
            (binding.itemsRecyclerView.adapter as HomeEpisodeListAdapter).submitList(section.homeSection.items)
        }
    }

    class OfflineCardViewHolder(private var binding: CardOfflineBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(onClickListener: () -> Unit) {
            binding.onlineButton.setOnClickListener {
                onClickListener()
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<HomeItem>() {
        override fun areItemsTheSame(oldItem: HomeItem, newItem: HomeItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HomeItem, newItem: HomeItem): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_NEXT_UP -> NextUpViewHolder(
                NextUpSectionBinding.inflate(
                    LayoutInflater.from(
                        parent.context,
                    ),
                    parent,
                    false,
                ),
            )
            ITEM_VIEW_TYPE_VIEW -> ViewViewHolder(
                ViewItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                ),
            )
            ITEM_VIEW_TYPE_OFFLINE_CARD -> {
                OfflineCardViewHolder(
                    CardOfflineBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                )
            }
            ITEM_SINGLE_TYPE_VIEW -> ViewSingleViewHolder(
                ViewSingleItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                ),
            )
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            ITEM_VIEW_TYPE_NEXT_UP -> {
                val view = getItem(position) as HomeItem.Section
                (holder as NextUpViewHolder).bind(view, onItemClickListener)
            }
            ITEM_VIEW_TYPE_VIEW -> {
                val view = getItem(position) as HomeItem.ViewItem
                (holder as ViewViewHolder).bind(view, onClickListener, onItemClickListener)
            }
            ITEM_VIEW_TYPE_OFFLINE_CARD -> {
                (holder as OfflineCardViewHolder).bind(onOnlineClickListener)
            }
            ITEM_SINGLE_TYPE_VIEW -> {
                val view = getItem(position) as HomeItem.ViewItem
                (holder as ViewSingleViewHolder).bind(view, onItemClickListener)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is HomeItem.OfflineCard -> ITEM_VIEW_TYPE_OFFLINE_CARD
            is HomeItem.Section -> ITEM_VIEW_TYPE_NEXT_UP
            is HomeItem.ViewItem -> {
                if (item.view.items?.size == 1) {
                    ITEM_SINGLE_TYPE_VIEW
                } else {
                    ITEM_VIEW_TYPE_VIEW
                }
            }
        }
    }
}
