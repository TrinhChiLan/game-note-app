package com.example.assignment3.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.assignment3.R
import com.example.assignment3.adapters.NotesAdapter
import com.example.assignment3.databinding.FragmentDetailBinding
import com.example.assignment3.viewmodels.DetailState
import com.example.assignment3.viewmodels.DetailViewModel
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt

class DetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DetailViewModel by viewModels()
    private val args: DetailFragmentArgs by navArgs()

    private var targetGame: com.example.assignment3.data.GameEntity? = null
    private var isHeaderImagePicker = false

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            targetGame?.let { game ->
                viewModel.updateGameImage(game, it.toString(), isHeaderImagePicker)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val notesAdapter = NotesAdapter(
            onNoteClick = { note ->
                if (note.type == "CHECKLIST") {
                    val action = DetailFragmentDirections.actionDetailFragmentToChecklistEditorFragment(
                        gameId = args.gameId,
                        noteId = note.id
                    )
                    findNavController().navigate(action)
                } else {
                    val action = DetailFragmentDirections.actionDetailFragmentToNoteEditorFragment(
                        gameId = args.gameId,
                        noteId = note.id
                    )
                    findNavController().navigate(action)
                }
            },
            onNoteLongClick = { note ->
                showNoteOptionsDialog(note)
            }
        )

        binding.notesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.notesRecyclerView.adapter = notesAdapter

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.addNoteFab.setOnClickListener {
            showAddNoteTypeDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.detailState.collect { state ->
                when (state) {
                    is DetailState.Loading -> {
                        // Show progress
                    }
                    is DetailState.Success -> {
                        val game = state.game
                        targetGame = game
                        binding.detailGameName.text = game.name

                        // Use additional image if available, else fallback to main image
                        val displayImageUrl =  game.imageUrlAdditional ?: game.imageUrl
                        binding.detailImage.load(displayImageUrl) {
                            crossfade(true)
                            placeholder(android.R.drawable.ic_menu_gallery)
                            error(android.R.drawable.ic_menu_report_image)
                        }
                        
                        // Favorite button logic
                        if (game.isFavorite) {
                            binding.favoriteButton.setImageResource(R.drawable.baseline_favorite_24)
                            binding.favoriteButton.clearColorFilter()
                        } else {
                            binding.favoriteButton.setImageResource(R.drawable.favorite_24_hollow)
                            binding.favoriteButton.setColorFilter(android.graphics.Color.WHITE, android.graphics.PorterDuff.Mode.SRC_ATOP)
                        }

                        binding.favoriteButton.setOnClickListener {
                            viewModel.toggleFavorite(game)
                        }

                        // Options button logic
                        binding.optionsButton.setOnClickListener {
                            showGameOptionsDialog(game)
                        }

                        // Handle Status Pill UI
                        updateStatusPill(game.status)

                        // Pill Click Listener
                        binding.statusContainer.setOnClickListener {
                            showStatusSelectionDialog(game)
                        }

                        notesAdapter.submitList(state.notes)
                    }
                    is DetailState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        viewModel.loadGameDetails(args.gameId)
    }

    private fun showGameOptionsDialog(game: com.example.assignment3.data.GameEntity) {
        val favoriteText = if (game.isFavorite) getString(R.string.unfavorite) else getString(R.string.favorite)
        
        val optionsList = mutableListOf(
            favoriteText, 
            getString(R.string.change_thumbnail),
            getString(R.string.change_header_image)
        )
        
        // Add remove options only if custom images exist
        if (game.imageUrl != game.apiImageUrl) {
            optionsList.add(getString(R.string.remove_thumbnail))
        }
        if (game.imageUrlAdditional != null) {
            optionsList.add(getString(R.string.remove_header_image))
        }
        
        optionsList.add(getString(R.string.remove_game))

        val options = optionsList.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.game_options_description)
            .setItems(options) { _, which ->
                val selectedOption = options[which]
                when (selectedOption) {
                    favoriteText -> viewModel.toggleFavorite(game)
                    getString(R.string.change_thumbnail) -> {
                        isHeaderImagePicker = false
                        imagePickerLauncher.launch("image/*")
                    }
                    getString(R.string.change_header_image) -> {
                        isHeaderImagePicker = true
                        imagePickerLauncher.launch("image/*")
                    }
                    getString(R.string.remove_thumbnail) -> viewModel.removeGameImage(game, false)
                    getString(R.string.remove_header_image) -> viewModel.removeGameImage(game, true)
                    getString(R.string.remove_game) -> showDeleteGameConfirmation(game)
                }
            }
            .show()
    }

    private fun showNoteOptionsDialog(note: com.example.assignment3.data.NoteEntity) {
        val options = arrayOf("Copy Note", "Delete Note")
        AlertDialog.Builder(requireContext())
            .setTitle("Note Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.copyNote(note)
                    1 -> showDeleteNoteConfirmation(note)
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
                findNavController().popBackStack()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteNoteConfirmation(note: com.example.assignment3.data.NoteEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to delete this note?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteNote(note)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showAddNoteTypeDialog() {
        val types = arrayOf(getString(R.string.text_note_chip), getString(R.string.checklist_note_chip))
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.add_note)
            .setItems(types) { _, which ->
                when (which) {
                    0 -> { // Text Note
                        val action = DetailFragmentDirections.actionDetailFragmentToNoteEditorFragment(
                            gameId = args.gameId,
                            noteId = -1
                        )
                        findNavController().navigate(action)
                    }
                    1 -> { // Checklist
                        val action = DetailFragmentDirections.actionDetailFragmentToChecklistEditorFragment(
                            gameId = args.gameId,
                            noteId = -1
                        )
                        findNavController().navigate(action)
                    }
                }
            }
            .show()
    }

    private fun updateStatusPill(status: String) {
        binding.statusContainer.visibility = View.VISIBLE
        
        val drawable = androidx.core.content.ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_edit)?.apply {
            val size = (14 * resources.displayMetrics.density).toInt()
            setBounds(0, 0, size, size)
            val color = when (status) {
                "PLAYING" -> "#22C55E".toColorInt()
                "WANT_TO_PLAY" -> "#8B5CF6".toColorInt()
                "FINISHED" -> "#EAB308".toColorInt()
                else -> android.graphics.Color.GRAY
            }
            androidx.core.graphics.drawable.DrawableCompat.setTint(this, color)
        }

        binding.detailStatusChip.setCompoundDrawables(null, null, drawable, null)
        binding.detailStatusChip.compoundDrawablePadding = 16

        when (status) {
            "PLAYING" -> {
                binding.detailStatusChip.text = getString(R.string.playing)
                binding.detailStatusChip.setBackgroundResource(R.drawable.bg_status_playing)
                binding.detailStatusChip.setTextColor("#22C55E".toColorInt())
            }
            "WANT_TO_PLAY" -> {
                binding.detailStatusChip.text = getString(R.string.want_to_play)
                binding.detailStatusChip.setBackgroundResource(R.drawable.bg_status_want_to_play)
                binding.detailStatusChip.setTextColor("#8B5CF6".toColorInt())
            }
            "FINISHED" -> {
                binding.detailStatusChip.text = getString(R.string.finished)
                binding.detailStatusChip.setBackgroundResource(R.drawable.bg_status_finished)
                binding.detailStatusChip.setTextColor("#EAB308".toColorInt())
            }
            else -> {
                binding.detailStatusChip.text = getString(R.string.add_status)
                binding.detailStatusChip.setBackgroundResource(R.drawable.bg_status_none)
                binding.detailStatusChip.setTextColor(android.graphics.Color.GRAY)
            }
        }
    }

    private fun showStatusSelectionDialog(game: com.example.assignment3.data.GameEntity) {
        val statuses = arrayOf(getString(R.string.playing), "Want to play", "Finished", "None")
        val statusValues = arrayOf("PLAYING", "WANT_TO_PLAY", "FINISHED", "NONE")
        
        AlertDialog.Builder(requireContext())
            .setTitle("Select Status")
            .setItems(statuses) { _, which ->
                viewModel.updateGameStatus(game, statusValues[which])
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
