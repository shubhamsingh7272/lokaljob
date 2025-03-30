package com.shubham.lokaljob.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.shubham.lokaljob.R
import com.shubham.lokaljob.data.model.Job
import com.shubham.lokaljob.databinding.ItemJobBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class JobAdapter(
    private val onJobClicked: (Job) -> Unit,
    private val onBookmarkClicked: (Job) -> Unit
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
        val job = getItem(position)
        holder.bind(job)
    }

    inner class JobViewHolder(private val binding: ItemJobBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onJobClicked(getItem(position))
                }
            }

            binding.bookmarkIcon.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onBookmarkClicked(getItem(position))
                }
            }
        }

        fun bind(job: Job) {
            binding.apply {
                jobTitle.text = job.title
                jobLocation.text = job.primaryDetails.Place
                jobCompany.text = job.company
                jobSalary.text = job.primaryDetails.Salary.ifEmpty { "Salary not specified" }
                
                // No direct postedDate field, but we can add a placeholder
                jobPostingDate.visibility = View.GONE
                
                // Set bookmark icon based on job's bookmarked status
                bookmarkIcon.setImageResource(
                    if (job.isBookmarked) R.drawable.ic_bookmark_filled
                    else R.drawable.ic_bookmark_outline
                )
                
                // Load job image if available
                if (!job.creatives.isNullOrEmpty() && job.creatives!!.isNotEmpty() && job.creatives!![0].url.isNotEmpty()) {
                    Glide.with(jobImage.context)
                        .load(job.creatives!![0].url)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .centerCrop()
                        .into(jobImage)
                    jobImage.visibility = View.VISIBLE
                } else {
                    jobImage.visibility = View.GONE
                }
                
                // Set job type indicator
                jobType.text = job.primaryDetails.jobType.ifEmpty { "Full-time" }
                
                // Display tags if available
                if (!job.jobTags.isNullOrEmpty()) {
                    // Show first tag in the UI
                    val firstTag = job.jobTags!![0]
                    jobTag.text = firstTag.title
                    jobTag.visibility = View.VISIBLE
                    
                    // If there are more tags, indicate with a +N
                    if (job.jobTags!!.size > 1) {
                        val remaining = job.jobTags!!.size - 1
                        additionalTags.text = "+$remaining"
                        additionalTags.visibility = View.VISIBLE
                    } else {
                        additionalTags.visibility = View.GONE
                    }
                } else {
                    jobTag.visibility = View.GONE
                    additionalTags.visibility = View.GONE
                }
            }
        }
    }

    class JobDiffCallback : DiffUtil.ItemCallback<Job>() {
        override fun areItemsTheSame(oldItem: Job, newItem: Job): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Job, newItem: Job): Boolean {
            return oldItem == newItem
        }
    }
} 