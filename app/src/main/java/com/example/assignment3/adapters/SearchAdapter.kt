package com.example.assignment3.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.assignment3.R
import com.example.assignment3.network.RawgGame
import com.example.assignment3.databinding.ItemGameSearchBinding

class SearchAdapter(private val onGameClick: (RawgGame) -> Unit) :
    ListAdapter<Pair<RawgGame, Boolean>, SearchAdapter.SearchViewHolder>(SearchDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val binding = ItemGameSearchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SearchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val (game, isInLibrary) = getItem(position)
        holder.bind(game, isInLibrary, onGameClick)
    }

    class SearchViewHolder(private val binding: ItemGameSearchBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(game: RawgGame, isInLibrary: Boolean, onGameClick: (RawgGame) -> Unit) {
            binding.gameName.text = game.name
            
            val genre = game.genres?.firstOrNull()?.name ?: "Unknown"
            val year = game.released?.take(4) ?: "xxxx"
            binding.gameInfo.text = "$genre - $year"
            
            binding.gameImage.load(game.backgroundImage) {
                crossfade(true)
                placeholder(R.drawable.placeholder)
                error(android.R.drawable.ic_menu_report_image)
            }
            
            binding.inLibraryPill.visibility = if (isInLibrary) View.VISIBLE else View.GONE
            
            binding.root.setOnClickListener { 
                if (!isInLibrary) {
                    onGameClick(game)
                }
            }
        }
    }

    private class SearchDiffCallback : DiffUtil.ItemCallback<Pair<RawgGame, Boolean>>() {
        override fun areItemsTheSame(oldItem: Pair<RawgGame, Boolean>, newItem: Pair<RawgGame, Boolean>): Boolean {
            return oldItem.first.id == newItem.first.id
        }

        override fun areContentsTheSame(oldItem: Pair<RawgGame, Boolean>, newItem: Pair<RawgGame, Boolean>): Boolean {
            return oldItem == newItem
        }
    }
}
