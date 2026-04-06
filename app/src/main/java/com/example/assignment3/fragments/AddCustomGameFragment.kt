package com.example.assignment3.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.assignment3.R
import com.example.assignment3.data.AppDatabase
import com.example.assignment3.data.GameEntity
import com.example.assignment3.data.ImageStorageManager
import com.example.assignment3.databinding.FragmentAddCustomGameBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class AddCustomGameFragment : Fragment() {

    private var _binding: FragmentAddCustomGameBinding? = null
    private val binding get() = _binding!!

    private var selectedThumbnailUri: Uri? = null
    private var selectedHeaderUri: Uri? = null

    private val thumbnailPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedThumbnailUri = it
            binding.thumbnailImage.load(it)
        }
    }

    private val headerPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedHeaderUri = it
            binding.headerImage.load(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddCustomGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupEdgeToEdge()

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.selectThumbnailButton.setOnClickListener {
            thumbnailPicker.launch("image/*")
        }

        binding.thumbnailImage.setOnClickListener {
            thumbnailPicker.launch("image/*")
        }

        binding.selectHeaderButton.setOnClickListener {
            headerPicker.launch("image/*")
        }

        binding.headerImage.setOnClickListener {
            headerPicker.launch("image/*")
        }

        binding.saveButton.setOnClickListener {
            saveGame()
        }
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Apply padding to root to physically resize the view
            v.updatePadding(
                bottom = bars.top
            )
            insets
        }
    }

    private fun saveGame() {
        val name = binding.gameNameInput.text.toString().trim()
        if (name.isEmpty()) {
            binding.gameNameInput.error = getString(R.string.error_empty_name)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val gameDao = AppDatabase.getDatabase(requireContext()).gameDao()
            
            // Save images to internal storage
            val savedThumbnailPath = withContext(Dispatchers.IO) {
                selectedThumbnailUri?.let { uri ->
                    ImageStorageManager.saveImageToInternalStorage(requireContext(), uri)
                }
            }
            
            val savedHeaderPath = withContext(Dispatchers.IO) {
                selectedHeaderUri?.let { uri ->
                    ImageStorageManager.saveImageToInternalStorage(requireContext(), uri)
                }
            }

            // Generate a random ID for custom games to avoid conflict with API IDs
            val customId = Random.nextInt(1000000, Int.MAX_VALUE)

            val game = GameEntity(
                id = customId,
                name = name,
                imageUrl = savedThumbnailPath ?: "",
                apiImageUrl = "", // No API fallback for custom games
                imageUrlAdditional = savedHeaderPath,
                releaseDate = "Custom Game",
                genres = "Local",
                status = "NONE",
                isFavorite = false
            )

            gameDao.insertGame(game)
            
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), getString(R.string.game_added_message, name), Toast.LENGTH_SHORT).show()
                findNavController().popBackStack(R.id.homeFragment, false)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
