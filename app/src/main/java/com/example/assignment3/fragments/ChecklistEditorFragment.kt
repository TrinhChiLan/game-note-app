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
import com.example.assignment3.databinding.FragmentChecklistEditorBinding
import com.example.assignment3.databinding.ItemChecklistEditBinding
import com.example.assignment3.viewmodels.NoteEditorState
import com.example.assignment3.viewmodels.NoteEditorViewModel
import kotlinx.coroutines.launch

class ChecklistEditorFragment : Fragment() {

    private var _binding: FragmentChecklistEditorBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NoteEditorViewModel by viewModels()
    private val args: ChecklistEditorFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChecklistEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbarChecklistEditor.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnAddItem.setOnClickListener {
            addChecklistItem("", false)
        }

        binding.saveChecklistFab.setOnClickListener {
            saveChecklist()
        }

        observeViewModel()
        viewModel.loadNote(args.noteId)
    }

    private fun addChecklistItem(text: String, isChecked: Boolean) {
        val itemBinding = ItemChecklistEditBinding.inflate(layoutInflater, binding.checklistItemsContainer, false)
        itemBinding.editItemText.setText(text)
        itemBinding.checkItem.isChecked = isChecked
        itemBinding.btnRemoveItem.setOnClickListener {
            binding.checklistItemsContainer.removeView(itemBinding.root)
        }
        // Add before the "Add Item" button
        binding.checklistItemsContainer.addView(itemBinding.root, binding.checklistItemsContainer.childCount - 1)
    }

    private fun saveChecklist() {
        val title = binding.editChecklistTitle.text.toString().trim()
        val items = mutableListOf<String>()
        
        for (i in 0 until binding.checklistItemsContainer.childCount - 1) {
            val view = binding.checklistItemsContainer.getChildAt(i)
            val itemBinding = ItemChecklistEditBinding.bind(view)
            val text = itemBinding.editItemText.text.toString().trim()
            val isChecked = itemBinding.checkItem.isChecked
            if (text.isNotEmpty()) {
                // Save as "checked|text"
                items.add("$isChecked|$text")
            }
        }

        if (items.isEmpty()) {
            Toast.makeText(requireContext(), "Checklist cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.saveNote(args.gameId, title, items.joinToString("\n"), "CHECKLIST")
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.noteEditorState.collect { state ->
                when (state) {
                    is NoteEditorState.Success -> {
                        state.note?.let { note ->
                            binding.editChecklistTitle.setText(note.title)
                            // Clear existing items except the add button
                            while (binding.checklistItemsContainer.childCount > 1) {
                                binding.checklistItemsContainer.removeViewAt(0)
                            }
                            note.content.split("\n").forEach { line ->
                                if (line.isNotBlank()) {
                                    val parts = line.split("|", limit = 2)
                                    if (parts.size == 2) {
                                        addChecklistItem(parts[1], parts[0].toBoolean())
                                    } else {
                                        addChecklistItem(line, false)
                                    }
                                }
                            }
                        }
                        if (state.note == null && binding.checklistItemsContainer.childCount == 1) {
                            addChecklistItem("", false) // Add one empty item for new checklist
                        }
                    }
                    is NoteEditorState.Saved -> {
                        findNavController().popBackStack()
                    }
                    is NoteEditorState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
