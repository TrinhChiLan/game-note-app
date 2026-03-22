package com.example.assignment3.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.assignment3.network.RawgGame
import com.example.assignment3.databinding.ItemGameSearchBinding

class SearchAdapter(private val onGameClick: (RawgGame) -> Unit) :
    ListAdapter<RawgGame, SearchAdapter.SearchViewHolder>(SearchDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val binding = ItemGameSearchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SearchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        holder.bind(getItem(position), onGameClick)
    }

    class SearchViewHolder(private val binding: ItemGameSearchBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(game: RawgGame, onGameClick: (RawgGame) -> Unit) {
            binding.gameName.text = game.name
            
            // Format: Genre - Year (e.g., Action - 2023)
            val genre = game.genres?.firstOrNull()?.name ?: "Unknown"
            val year = game.released?.take(4) ?: "xxxx"
            binding.gameInfo.text = "$genre - $year"
            
            binding.gameImage.load(game.backgroundImage) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_report_image)
            }
            
            binding.root.setOnClickListener { onGameClick(game) }
        }
    }

    private class SearchDiffCallback : DiffUtil.ItemCallback<RawgGame>() {
        override fun areItemsTheSame(oldItem: RawgGame, newItem: RawgGame): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RawgGame, newItem: RawgGame): Boolean {
            return oldItem == newItem
        }
    }
}
