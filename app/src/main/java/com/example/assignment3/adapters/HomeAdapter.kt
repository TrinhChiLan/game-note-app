package com.example.assignment3.adapters

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.assignment3.R
import com.example.assignment3.data.GameEntity
import com.example.assignment3.databinding.ItemGameHomeBinding
import com.example.assignment3.databinding.ItemGameHomeTallBinding
import com.example.assignment3.databinding.ItemHeaderBinding
import com.example.assignment3.databinding.ItemSearchBarBinding
import androidx.core.graphics.toColorInt
import com.example.assignment3.MainActivity

sealed class HomeItem {
    object SearchBar : HomeItem()
    data class Header(val title: String) : HomeItem()
    data class Game(val game: GameEntity) : HomeItem()
}

class HomeAdapter(
    private val onSearchQueryChanged: (String) -> Unit,
    private val onGameClick: (GameEntity) -> Unit,
    private val onGameLongClick: (GameEntity) -> Unit
) : ListAdapter<HomeItem, RecyclerView.ViewHolder>(HomeDiffCallback()) {

    private var useTallLayout: Boolean = false

    fun setUseTallLayout(useTall: Boolean) {
        if (this.useTallLayout != useTall) {
            this.useTallLayout = useTall
            notifyDataSetChanged()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HomeItem.SearchBar -> VIEW_TYPE_SEARCH
            is HomeItem.Header -> VIEW_TYPE_HEADER
            is HomeItem.Game -> if (useTallLayout) VIEW_TYPE_GAME_TALL else VIEW_TYPE_GAME
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SEARCH -> SearchViewHolder(ItemSearchBarBinding.inflate(inflater, parent, false))
            VIEW_TYPE_HEADER -> HeaderViewHolder(ItemHeaderBinding.inflate(inflater, parent, false))
            VIEW_TYPE_GAME_TALL -> GameTallViewHolder(ItemGameHomeTallBinding.inflate(inflater, parent, false))
            else -> GameViewHolder(ItemGameHomeBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is SearchViewHolder -> holder.bind(onSearchQueryChanged)
            is HeaderViewHolder -> holder.bind((item as HomeItem.Header).title)
            is GameViewHolder -> holder.bind((item as HomeItem.Game).game, onGameClick, onGameLongClick)
            is GameTallViewHolder -> holder.bind((item as HomeItem.Game).game, onGameClick, onGameLongClick)
        }
    }

    class SearchViewHolder(private val binding: ItemSearchBarBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(onSearchQueryChanged: (String) -> Unit) {
            binding.menuButton.setOnClickListener {
                (binding.root.context as? MainActivity)?.openDrawer()
            }

            binding.searchInputInternal.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    onSearchQueryChanged(s?.toString() ?: "")
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    class HeaderViewHolder(private val binding: ItemHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String) = with(binding) { headerTitle.text = title }
    }

    class GameViewHolder(private val binding: ItemGameHomeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(game: GameEntity, onGameClick: (GameEntity) -> Unit, onGameLongClick: (GameEntity) -> Unit) {
            binding.gameName.text = game.name
            bindStatus(binding.statusChip, game.status)
            binding.gameImage.load(game.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.placeholder)
                error(android.R.drawable.ic_menu_report_image)
            }
            binding.root.setOnClickListener { onGameClick(game) }
            binding.root.setOnLongClickListener { onGameLongClick(game); true }
        }
    }

    class GameTallViewHolder(private val binding: ItemGameHomeTallBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(game: GameEntity, onGameClick: (GameEntity) -> Unit, onGameLongClick: (GameEntity) -> Unit) {
            binding.gameName.text = game.name
            bindStatus(binding.statusChip, game.status)
            binding.gameImage.load(game.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.placeholder)
                error(android.R.drawable.ic_menu_report_image)
            }
            binding.root.setOnClickListener { onGameClick(game) }
            binding.root.setOnLongClickListener { onGameLongClick(game); true }
        }
    }

    private class HomeDiffCallback : DiffUtil.ItemCallback<HomeItem>() {
        override fun areItemsTheSame(oldItem: HomeItem, newItem: HomeItem) = when {
            oldItem is HomeItem.SearchBar && newItem is HomeItem.SearchBar -> true
            oldItem is HomeItem.Game && newItem is HomeItem.Game -> oldItem.game.id == newItem.game.id
            oldItem is HomeItem.Header && newItem is HomeItem.Header -> oldItem.title == newItem.title
            else -> false
        }
        override fun areContentsTheSame(oldItem: HomeItem, newItem: HomeItem) = oldItem == newItem
    }

    companion object {
        private const val VIEW_TYPE_SEARCH = 0
        private const val VIEW_TYPE_HEADER = 1
        private const val VIEW_TYPE_GAME = 2
        private const val VIEW_TYPE_GAME_TALL = 3

        private fun bindStatus(statusChip: android.widget.TextView, status: String) {
            when (status) {
                "PLAYING" -> {
                    statusChip.visibility = View.VISIBLE
                    statusChip.text = "Playing"
                    statusChip.setBackgroundResource(R.drawable.bg_status_playing)
                    statusChip.setTextColor("#22C55E".toColorInt())
                    statusChip.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_status_dot_green, 0, 0, 0)
                }
                "WANT_TO_PLAY" -> {
                    statusChip.visibility = View.VISIBLE
                    statusChip.text = "Want to play"
                    statusChip.setBackgroundResource(R.drawable.bg_status_want_to_play)
                    statusChip.setTextColor("#8B5CF6".toColorInt())
                    statusChip.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_status_dot_purple, 0, 0, 0)
                }
                "FINISHED" -> {
                    statusChip.visibility = View.VISIBLE
                    statusChip.text = "Finished"
                    statusChip.setBackgroundResource(R.drawable.bg_status_finished)
                    statusChip.setTextColor("#EAB308".toColorInt())
                    statusChip.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_status_dot_yellow, 0, 0, 0)
                }
                else -> statusChip.visibility = View.GONE
            }
            statusChip.compoundDrawablePadding = 8
        }
    }
}
