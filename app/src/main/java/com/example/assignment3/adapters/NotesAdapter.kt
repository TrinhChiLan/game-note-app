package com.example.assignment3.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment3.data.NoteEntity
import com.example.assignment3.databinding.ItemNoteChecklistBinding
import com.example.assignment3.databinding.ItemNoteTextBinding
import androidx.core.graphics.toColorInt
import com.google.android.material.R as MaterialR

class NotesAdapter(
    private val onNoteClick: (NoteEntity) -> Unit,
    private val onNoteLongClick: (NoteEntity) -> Unit
) : ListAdapter<NoteEntity, RecyclerView.ViewHolder>(NoteDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).type == "TEXT") VIEW_TYPE_TEXT else VIEW_TYPE_CHECKLIST
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_TEXT) {
            val binding = ItemNoteTextBinding.inflate(inflater, parent, false)
            TextViewHolder(binding)
        } else {
            val binding = ItemNoteChecklistBinding.inflate(inflater, parent, false)
            ChecklistViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val note = getItem(position)
        if (holder is TextViewHolder) {
            holder.bind(note, onNoteClick, onNoteLongClick)
        } else if (holder is ChecklistViewHolder) {
            holder.bind(note, onNoteClick, onNoteLongClick)
        }
    }

    class TextViewHolder(private val binding: ItemNoteTextBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(note: NoteEntity, onClick: (NoteEntity) -> Unit, onLongClick: (NoteEntity) -> Unit) {
            if (note.title.isNullOrEmpty()) {
                binding.noteTitle.visibility = View.GONE
            } else {
                binding.noteTitle.visibility = View.VISIBLE
                binding.noteTitle.text = note.title
            }
            binding.noteContent.text = note.content
            
            // Resolve onSurface color from theme
            val typedValue = TypedValue()
            binding.root.context.theme.resolveAttribute(MaterialR.attr.colorOnSurface, typedValue, true)
            binding.noteContent.setTextColor(typedValue.data)
            
            binding.root.setOnClickListener { onClick(note) }
            binding.root.setOnLongClickListener {
                onLongClick(note)
                true
            }
        }
    }

    class ChecklistViewHolder(private val binding: ItemNoteChecklistBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(note: NoteEntity, onClick: (NoteEntity) -> Unit, onLongClick: (NoteEntity) -> Unit) {
            if (note.title.isNullOrEmpty()) {
                binding.checklistTitle.visibility = View.GONE
            } else {
                binding.checklistTitle.visibility = View.VISIBLE
                binding.checklistTitle.text = note.title
            }

            binding.checklistItemsContainer.removeAllViews()

            // Resolve onSurface color from theme
            val typedValue = TypedValue()
            binding.root.context.theme.resolveAttribute(MaterialR.attr.colorOnSurface, typedValue, true)
            val onSurfaceColor = typedValue.data

            // Format: "checked|text"
            val lines = note.content.split("\n").filter { it.isNotBlank() }
            lines.forEach { line ->
                val parts = line.split("|", limit = 2)
                val isChecked = parts.getOrNull(0) == "true"
                val text = parts.getOrNull(1) ?: line

                val checkBox = CheckBox(binding.root.context).apply {
                    this.text = text
                    this.isChecked = isChecked
                    this.isEnabled = true
                    this.isClickable = false
                                                                                                                                                    this.setTextColor(onSurfaceColor)
                    
                    // Set color for the checkbox tint to make it more visible
                    val colorStateList = ColorStateList(
                        arrayOf(
                            intArrayOf(android.R.attr.state_checked),
                            intArrayOf(-android.R.attr.state_checked)
                        ),
                        intArrayOf(
                            "#2ECC71".toColorInt(), // Green when checked
                            Color.GRAY // Gray when unchecked
                        )
                    )
                    this.buttonTintList = colorStateList
                }
                binding.checklistItemsContainer.addView(checkBox)
            }

            binding.root.setOnClickListener { onClick(note) }
            binding.root.setOnLongClickListener {
                onLongClick(note)
                true
            }
        }
    }

    private class NoteDiffCallback : DiffUtil.ItemCallback<NoteEntity>() {
        override fun areItemsTheSame(oldItem: NoteEntity, newItem: NoteEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NoteEntity, newItem: NoteEntity): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private const val VIEW_TYPE_TEXT = 0
        private const val VIEW_TYPE_CHECKLIST = 1
    }
}
