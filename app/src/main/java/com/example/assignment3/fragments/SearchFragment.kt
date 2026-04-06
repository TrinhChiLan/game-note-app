package com.example.assignment3.fragments

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()

    private var isWifiConnected: Boolean = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateConnectionStatus()
        }

        override fun onLost(network: Network) {
            updateConnectionStatus()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            updateConnectionStatus()
        }
    }

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

        registerNetworkListener()
        updateConnectionStatus()

        val adapter = SearchAdapter { clickedGame ->
            viewModel.saveGame(clickedGame)
            //Toast.makeText(requireContext(), getString(R.string.game_added_message, clickedGame.name), Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }

        binding.searchResultsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.searchResultsRecycler.adapter = adapter

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.searchInput.doAfterTextChanged { text ->
            if (isWifiConnected) {
                viewModel.onQueryChanged(text?.toString() ?: "")
            }
        }

        binding.searchInput.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH && isWifiConnected) {
                viewModel.searchGames(v.text.toString())
                true
            } else {
                false
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchState.collect { state ->
                binding.loadingLayout.isVisible = state is SearchState.Loading && isWifiConnected
                
                if (isWifiConnected) {
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
    }

    private fun registerNetworkListener() {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    private fun unregisterNetworkListener() {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    private fun updateConnectionStatus() {
        val connected = checkWifiConnection(requireContext())
        if (connected != isWifiConnected) {
            isWifiConnected = connected
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                updateOfflineUI(!isWifiConnected)
                if (isWifiConnected) {
                    // Trigger a search refresh if there's text in the input
                    val query = binding.searchInput.text.toString()
                    if (query.isNotBlank()) {
                        viewModel.searchGames(query)
                    }
                }
            }
        }
    }

    private fun updateOfflineUI(isOffline: Boolean) {
        if (_binding == null) return
        binding.offlineLayout.isVisible = isOffline
        binding.searchResultsRecycler.isVisible = !isOffline
        
        binding.searchInput.isEnabled = !isOffline
        binding.searchCard.alpha = if (isOffline) 0.5f else 1.0f
        binding.searchIcon.alpha = if (isOffline) 0.5f else 1.0f
    }

    private fun checkWifiConnection(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unregisterNetworkListener()
        _binding = null
    }
}
