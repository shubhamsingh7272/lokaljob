package com.shubham.lokaljob.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.shubham.lokaljob.data.model.Job
import com.shubham.lokaljob.databinding.ItemJobBinding

class JobAdapter(
    private val onJobClick: (Job) -> Unit,
    private val onBookmarkClick: (Job) -> Unit
) : ListAdapter<Job, JobAdapter.JobViewHolder>(JobDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val binding = ItemJobBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return JobViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class JobViewHolder(
        private val binding: ItemJobBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onJobClick(getItem(position))
                }
            }

            binding.bookmarkButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onBookmarkClick(getItem(position))
                }
            }
        }

        fun bind(job: Job) {
            binding.apply {
                titleTextView.text = job.title
                companyTextView.text = job.company
                locationTextView.text = job.primaryDetails.Place
                salaryTextView.text = job.primaryDetails.Salary
                phoneTextView.text = job.phone
                bookmarkButton.setImageResource(
                    if (job.isBookmarked) {
                        com.shubham.lokaljob.R.drawable.ic_bookmark
                    } else {
                        com.shubham.lokaljob.R.drawable.ic_bookmark_border
                    }
                )
            }
        }
    }

    private class JobDiffCallback : DiffUtil.ItemCallback<Job>() {
        override fun areItemsTheSame(oldItem: Job, newItem: Job): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Job, newItem: Job): Boolean {
            return oldItem == newItem
        }
    }
} 