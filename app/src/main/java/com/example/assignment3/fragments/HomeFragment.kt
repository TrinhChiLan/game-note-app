package com.example.assignment3.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.assignment3.R
import com.example.assignment3.adapters.HomeAdapter
import com.example.assignment3.databinding.FragmentHomeBinding
import com.example.assignment3.viewmodels.HomeViewModel
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    private var targetGame: com.example.assignment3.data.GameEntity? = null
    private var isHeaderPicker = false

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            targetGame?.let { game ->
                viewModel.updateGameImage(game, it.toString(), isHeaderPicker)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = HomeAdapter(
            onSearchQueryChanged = { query ->
                viewModel.setSearchQuery(query)
            },
            onGameClick = { game ->
                val action = HomeFragmentDirections.actionHomeFragmentToDetailFragment(game.id)
                findNavController().navigate(action)
            },
            onGameLongClick = { game ->
                showGameOptionsDialog(game)
            }
        )

        binding.gamesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.gamesRecyclerView.adapter = adapter

        binding.addGameFab.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_searchFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.homeItems.collect { items ->
                adapter.submitList(items)
            }
        }
    }

    private fun showGameOptionsDialog(game: com.example.assignment3.data.GameEntity) {
        val favoriteText = if (game.isFavorite) getString(R.string.unfavorite) else getString(R.string.favorite)
        val options = arrayOf(
            favoriteText, 
            getString(R.string.change_thumbnail),
            getString(R.string.change_header_image),
            getString(R.string.remove_game)
        )
        AlertDialog.Builder(requireContext())
            .setTitle(game.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.toggleFavorite(game)
                    1 -> {
                        targetGame = game
                        isHeaderPicker = false
                        imagePickerLauncher.launch("image/*")
                    }
                    2 -> {
                        targetGame = game
                        isHeaderPicker = true
                        imagePickerLauncher.launch("image/*")
                    }
                    3 -> showDeleteGameConfirmation(game)
                }
            }
            .show()
    }

    private fun showDeleteGameConfirmation(game: com.example.assignment3.data.GameEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.remove_game)
            .setMessage(getString(R.string.remove_game_confirm_message, game.name))
            .setPositiveButton(R.string.remove) { _, _ ->
                viewModel.deleteGame(game)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
