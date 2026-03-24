package com.example.assignment3.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.assignment3.R
import com.example.assignment3.adapters.SearchAdapter
import com.example.assignment3.databinding.FragmentSearchBinding
import com.example.assignment3.viewmodels.SearchState
import com.example.assignment3.viewmodels.SearchViewModel
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = SearchAdapter { clickedGame ->
            viewModel.saveGame(clickedGame)
            Toast.makeText(requireContext(), getString(R.string.game_added_message, clickedGame.name), Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }

        binding.searchResultsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.searchResultsRecycler.adapter = adapter

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.searchInput.doAfterTextChanged { text ->
            viewModel.onQueryChanged(text?.toString() ?: "")
        }

        binding.searchInput.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.searchGames(v.text.toString())
                true
            } else {
                false
            }
        }

        binding.addCustomGameButton.setOnClickListener {
            findNavController().navigate(R.id.action_searchFragment_to_addCustomGameFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchState.collect { state ->
                binding.loadingLayout.isVisible = state is SearchState.Loading
                
                when (state) {
                    is SearchState.Loading -> {
                        // UI handled by binding.loadingLayout.isVisible
                    }
                    is SearchState.Success -> {
                        adapter.submitList(state.games)
                    }
                    is SearchState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                    is SearchState.Idle -> {
                        adapter.submitList(emptyList())
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
