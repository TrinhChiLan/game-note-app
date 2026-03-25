package com.example.assignment3.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.assignment3.R
import com.example.assignment3.network.SteamGame
import com.example.assignment3.databinding.ItemGameSearchBinding

class SearchAdapter(private val onGameClick: (SteamGame) -> Unit) :
    ListAdapter<Pair<SteamGame, Boolean>, SearchAdapter.SearchViewHolder>(SearchDiffCallback()) {

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
        
        fun bind(game: SteamGame, isInLibrary: Boolean, onGameClick: (SteamGame) -> Unit) {
            binding.gameName.text = game.name
            
            binding.gameInfo.text = "Steam Game"
            
            binding.gameImage.load(game.headerImage) {
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

    private class SearchDiffCallback : DiffUtil.ItemCallback<Pair<SteamGame, Boolean>>() {
        override fun areItemsTheSame(oldItem: Pair<SteamGame, Boolean>, newItem: Pair<SteamGame, Boolean>): Boolean {
            return oldItem.first.id == newItem.first.id
        }

        override fun areContentsTheSame(oldItem: Pair<SteamGame, Boolean>, newItem: Pair<SteamGame, Boolean>): Boolean {
            return oldItem == newItem
        }
    }
}
