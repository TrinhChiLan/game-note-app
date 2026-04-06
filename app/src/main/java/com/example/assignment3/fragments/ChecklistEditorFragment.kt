package com.example.assignment3.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
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

        setupToolbar()
        setupEdgeToEdge()

        binding.btnAddItem.setOnClickListener {
            addChecklistItem("", false)
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackNavigation()
            }
        })

        observeViewModel()
        viewModel.loadNote(args.noteId)
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            
            // Apply padding to root to physically resize the view
            v.updatePadding(
                bottom = bars.bottom.coerceAtLeast(ime.bottom)
            )
            insets
        }
    }

    private fun setupToolbar() {
        binding.toolbarChecklistEditor.setNavigationOnClickListener {
            handleBackNavigation()
        }
    }

    private fun handleBackNavigation() {
        val items = getChecklistItems()
        if (items.isNotEmpty()) {
            saveChecklist()
        } else {
            findNavController().popBackStack()
        }
    }

    private fun addChecklistItem(text: String, isChecked: Boolean) {
        val itemBinding = ItemChecklistEditBinding.inflate(layoutInflater, binding.checklistItemsContainer, false)
        itemBinding.editItemText.setText(text)
        itemBinding.checkItem.isChecked = isChecked
        itemBinding.btnRemoveItem.setOnClickListener {
            binding.checklistItemsContainer.removeView(itemBinding.root)
        }
        binding.checklistItemsContainer.addView(itemBinding.root, binding.checklistItemsContainer.childCount - 1)
    }

    private fun getChecklistItems(): List<String> {
        val items = mutableListOf<String>()
        for (i in 0 until binding.checklistItemsContainer.childCount - 1) {
            val view = binding.checklistItemsContainer.getChildAt(i)
            val itemBinding = ItemChecklistEditBinding.bind(view)
            val text = itemBinding.editItemText.text.toString().trim()
            val isChecked = itemBinding.checkItem.isChecked
            if (text.isNotEmpty()) {
                items.add("$isChecked|$text")
            }
        }
        return items
    }

    private fun saveChecklist() {
        val title = binding.editChecklistTitle.text.toString().trim()
        val items = getChecklistItems()

        if (items.isEmpty()) {
            if (findNavController().currentDestination?.id == com.example.assignment3.R.id.checklistEditorFragment) {
                findNavController().popBackStack()
            }
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
                            addChecklistItem("", false)
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
