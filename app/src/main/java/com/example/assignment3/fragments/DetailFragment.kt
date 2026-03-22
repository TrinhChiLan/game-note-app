package com.example.assignment3.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

class DetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DetailViewModel by viewModels()
    private val args: DetailFragmentArgs by navArgs()

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

        binding.toolbar.setNavigationOnClickListener {
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
                        binding.detailGameName.text = game.name
                        binding.detailImage.load(game.imageUrl)
                        
                        // Favorite button logic
                        val favoriteIcon = if (game.isFavorite) {
                            android.R.drawable.btn_star_big_on
                        } else {
                            android.R.drawable.btn_star_big_off
                        }
                        binding.favoriteButton.setImageResource(favoriteIcon)
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
        val favoriteText = if (game.isFavorite) "Unfavorite" else "Favorite"
        val options = arrayOf(favoriteText, "Remove Game")
        AlertDialog.Builder(requireContext())
            .setTitle("Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.toggleFavorite(game)
                    1 -> showDeleteGameConfirmation(game)
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
            .setTitle("Remove Game")
            .setMessage("Are you sure you want to remove this game from your notes?")
            .setPositiveButton("Remove") { _, _ ->
                viewModel.deleteGame(game)
                findNavController().popBackStack()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteNoteConfirmation(note: com.example.assignment3.data.NoteEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to delete this note?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteNote(note)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddNoteTypeDialog() {
        val types = arrayOf("Text Note", "Checklist")
        AlertDialog.Builder(requireContext())
            .setTitle("Add Note")
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
                "PLAYING" -> android.graphics.Color.parseColor("#22C55E")
                "WANT_TO_PLAY" -> android.graphics.Color.parseColor("#8B5CF6")
                "FINISHED" -> android.graphics.Color.parseColor("#EAB308")
                else -> android.graphics.Color.GRAY
            }
            androidx.core.graphics.drawable.DrawableCompat.setTint(this, color)
        }

        binding.detailStatusChip.setCompoundDrawables(null, null, drawable, null)
        binding.detailStatusChip.compoundDrawablePadding = 16

        when (status) {
            "PLAYING" -> {
                binding.detailStatusChip.text = "Playing"
                binding.detailStatusChip.setBackgroundResource(R.drawable.bg_status_playing)
                binding.detailStatusChip.setTextColor(android.graphics.Color.parseColor("#22C55E"))
            }
            "WANT_TO_PLAY" -> {
                binding.detailStatusChip.text = "Want to play"
                binding.detailStatusChip.setBackgroundResource(R.drawable.bg_status_want_to_play)
                binding.detailStatusChip.setTextColor(android.graphics.Color.parseColor("#8B5CF6"))
            }
            "FINISHED" -> {
                binding.detailStatusChip.text = "Finished"
                binding.detailStatusChip.setBackgroundResource(R.drawable.bg_status_finished)
                binding.detailStatusChip.setTextColor(android.graphics.Color.parseColor("#EAB308"))
            }
            else -> {
                binding.detailStatusChip.text = "Add a status"
                binding.detailStatusChip.setBackgroundResource(R.drawable.bg_status_none)
                binding.detailStatusChip.setTextColor(android.graphics.Color.GRAY)
            }
        }
    }

    private fun showStatusSelectionDialog(game: com.example.assignment3.data.GameEntity) {
        val statuses = arrayOf("Playing", "Want to play", "Finished", "None")
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
