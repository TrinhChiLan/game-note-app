package com.example.assignment3.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.assignment3.R
import com.example.assignment3.databinding.FragmentNoteEditorBinding
import com.example.assignment3.viewmodels.NoteEditorState
import com.example.assignment3.viewmodels.NoteEditorViewModel
import kotlinx.coroutines.launch

class NoteEditorFragment : Fragment() {

    private var _binding: FragmentNoteEditorBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NoteEditorViewModel by viewModels()
    private val args: NoteEditorFragmentArgs by navArgs()

    private var selectedNoteType: String = "TEXT" // Default to text note

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupNoteTypeChips()
        setupSaveButton()
        observeViewModel()

        // Load existing note or prepare for new note
        viewModel.loadNote(args.noteId)
    }

    private fun setupToolbar() {
        binding.toolbarNoteEditor.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupNoteTypeChips() {
        binding.noteTypeChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val chipId = checkedIds.firstOrNull()
            selectedNoteType = when (chipId) {
                R.id.chip_text_note -> "TEXT"
                R.id.chip_checklist_note -> "CHECKLIST"
                else -> "TEXT"
            }
        }
        // Set initial checked state
        binding.chipTextNote.isChecked = true
    }

    private fun setupSaveButton() {
        binding.saveNoteFab.setOnClickListener {
            val title = binding.editNoteTitle.text.toString().trim()
            val content = binding.editNoteContent.text.toString().trim()

            if (content.isEmpty()) {
                Toast.makeText(requireContext(), "Note content cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.saveNote(args.gameId, title, content, selectedNoteType)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.noteEditorState.collect { state ->
                when (state) {
                    is NoteEditorState.Loading -> {
                        // Show loading indicator if needed
                    }
                    is NoteEditorState.Success -> {
                        state.note?.let { note ->
                            binding.editNoteTitle.setText(note.title)
                            binding.editNoteContent.setText(note.content)
                            selectedNoteType = note.type
                            when (note.type) {
                                "TEXT" -> binding.chipTextNote.isChecked = true
                                "CHECKLIST" -> binding.chipChecklistNote.isChecked = true
                            }
                        }
                    }
                    is NoteEditorState.Saved -> {
                        Toast.makeText(requireContext(), "Note saved!", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                    is NoteEditorState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
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
