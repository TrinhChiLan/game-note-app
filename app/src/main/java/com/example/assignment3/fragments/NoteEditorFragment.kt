package com.example.assignment3.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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

    // Always TEXT in this fragment now
    private val selectedNoteType: String = "TEXT"

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
        setupSaveButton()
        observeViewModel()
        setupBackNavigation()

        // Load existing note or prepare for new note
        viewModel.loadNote(args.noteId)
    }

    private fun setupToolbar() {
        binding.toolbarNoteEditor.setNavigationOnClickListener {
            handleBackNavigation()
        }
    }

    private fun setupSaveButton() {
        binding.saveNoteFab.setOnClickListener {
            performSave()
        }
    }

    private fun setupBackNavigation() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackNavigation()
            }
        })
    }

    private fun handleBackNavigation() {
        val content = binding.editNoteContent.text.toString().trim()
        if (content.isNotEmpty()) {
            performSave()
        } else {
            findNavController().popBackStack()
        }
    }

    private fun performSave() {
        val title = binding.editNoteTitle.text.toString().trim()
        val content = binding.editNoteContent.text.toString().trim()

        if (content.isEmpty()) {
            if (findNavController().currentDestination?.id == R.id.noteEditorFragment) {
                findNavController().popBackStack()
            }
            return
        }

        viewModel.saveNote(args.gameId, title, content, selectedNoteType)
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
                            // Note: We don't change type here as this editor is strictly for TEXT notes now
                        }
                    }
                    is NoteEditorState.Saved -> {
                        Toast.makeText(requireContext(), getString(R.string.note_saved_message), Toast.LENGTH_SHORT).show()
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
