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
import com.example.assignment3.databinding.ItemHeaderBinding
import com.example.assignment3.databinding.ItemSearchBarBinding
import androidx.core.graphics.toColorInt

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

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HomeItem.SearchBar -> VIEW_TYPE_SEARCH
            is HomeItem.Header -> VIEW_TYPE_HEADER
            is HomeItem.Game -> VIEW_TYPE_GAME
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SEARCH -> {
                val binding = ItemSearchBarBinding.inflate(inflater, parent, false)
                SearchViewHolder(binding)
            }
            VIEW_TYPE_HEADER -> {
                val binding = ItemHeaderBinding.inflate(inflater, parent, false)
                HeaderViewHolder(binding)
            }
            else -> {
                val binding = ItemGameHomeBinding.inflate(inflater, parent, false)
                GameViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is SearchViewHolder -> holder.bind(onSearchQueryChanged)
            is HeaderViewHolder -> holder.bind((item as HomeItem.Header).title)
            is GameViewHolder -> holder.bind((item as HomeItem.Game).game, onGameClick, onGameLongClick)
        }
    }

    class SearchViewHolder(private val binding: ItemSearchBarBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(onSearchQueryChanged: (String) -> Unit) {
            binding.searchInputInternal.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    onSearchQueryChanged(s?.toString() ?: "")
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    class HeaderViewHolder(private val binding: ItemHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String) {
            binding.headerTitle.text = title
        }
    }

    class GameViewHolder(private val binding: ItemGameHomeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(game: GameEntity, onGameClick: (GameEntity) -> Unit, onGameLongClick: (GameEntity) -> Unit) {
            binding.gameName.text = game.name
            
            when (game.status) {
                "PLAYING" -> {
                    binding.statusChip.visibility = View.VISIBLE
                    binding.statusChip.text = "Playing"
                    binding.statusChip.setBackgroundResource(R.drawable.bg_status_playing)
                    binding.statusChip.setTextColor("#22C55E".toColorInt())
                    binding.statusChip.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_status_dot_green, 0, 0, 0)
                    binding.statusChip.compoundDrawablePadding = 8
                }
                "WANT_TO_PLAY" -> {
                    binding.statusChip.visibility = View.VISIBLE
                    binding.statusChip.text = "Want to play"
                    binding.statusChip.setBackgroundResource(R.drawable.bg_status_want_to_play)
                    binding.statusChip.setTextColor("#8B5CF6".toColorInt())
                    binding.statusChip.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_status_dot_purple, 0, 0, 0)
                    binding.statusChip.compoundDrawablePadding = 8
                }
                "FINISHED" -> {
                    binding.statusChip.visibility = View.VISIBLE
                    binding.statusChip.text = "Finished"
                    binding.statusChip.setBackgroundResource(R.drawable.bg_status_finished)
                    binding.statusChip.setTextColor("#EAB308".toColorInt())
                    binding.statusChip.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_status_dot_yellow, 0, 0, 0)
                    binding.statusChip.compoundDrawablePadding = 8
                }
                else -> {
                    binding.statusChip.visibility = View.GONE
                }
            }

            // Consistently use the primary image (main background)
            binding.gameImage.load(game.imageUrl) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_report_image)
            }

            binding.root.setOnClickListener { onGameClick(game) }
            binding.root.setOnLongClickListener {
                onGameLongClick(game)
                true
            }
        }
    }

    private class HomeDiffCallback : DiffUtil.ItemCallback<HomeItem>() {
        override fun areItemsTheSame(oldItem: HomeItem, newItem: HomeItem): Boolean {
            return when {
                oldItem is HomeItem.SearchBar && newItem is HomeItem.SearchBar -> true
                oldItem is HomeItem.Game && newItem is HomeItem.Game -> oldItem.game.id == newItem.game.id
                oldItem is HomeItem.Header && newItem is HomeItem.Header -> oldItem.title == newItem.title
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: HomeItem, newItem: HomeItem): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private const val VIEW_TYPE_SEARCH = 0
        private const val VIEW_TYPE_HEADER = 1
        private const val VIEW_TYPE_GAME = 2
    }
}
