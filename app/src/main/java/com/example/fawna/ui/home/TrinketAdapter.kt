package com.example.fawna.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fawna.databinding.ItemTrinketBinding

class TrinketAdapter(private val onItemClick: (Trinket) -> Unit) : ListAdapter<Trinket, TrinketAdapter.TrinketViewHolder>(TrinketDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrinketViewHolder {
        val binding = ItemTrinketBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrinketViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrinketViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TrinketViewHolder(private val binding: ItemTrinketBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(trinket: Trinket) {
            binding.trinketName.text = trinket.name
            binding.trinketImage.setImageResource(trinket.imageResId)
            binding.root.setOnClickListener { onItemClick(trinket) }
        }
    }

    class TrinketDiffCallback : DiffUtil.ItemCallback<Trinket>() {
        override fun areItemsTheSame(oldItem: Trinket, newItem: Trinket): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Trinket, newItem: Trinket): Boolean {
            return oldItem == newItem
        }
    }
}