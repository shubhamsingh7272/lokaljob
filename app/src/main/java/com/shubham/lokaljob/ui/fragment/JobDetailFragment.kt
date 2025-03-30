package com.shubham.lokaljob.ui.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.shubham.lokaljob.R
import com.shubham.lokaljob.data.model.ContactPreference
import com.shubham.lokaljob.data.model.ContentV3
import com.shubham.lokaljob.data.model.Creative
import com.shubham.lokaljob.data.model.JobTag
import com.shubham.lokaljob.databinding.FragmentJobDetailBinding
import com.shubham.lokaljob.ui.viewmodel.JobDetailViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class JobDetailFragment : Fragment() {

    private var _binding: FragmentJobDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JobDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJobDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupObservers()
        setupBookmarkButton()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.job.collectLatest { job ->
                job?.let {
                    binding.apply {
                        // Log job details for debugging
                        Log.d("JobDetailFragment", "Loaded job: id=${it.id}, title=${it.title}")
                        Log.d("JobDetailFragment", "Has jobTags: ${it.jobTags?.size ?: 0}")
                        Log.d("JobDetailFragment", "Has contactPreference: ${it.contactPreference != null}")
                        Log.d("JobDetailFragment", "Has creatives: ${it.creatives?.size ?: 0}")
                        Log.d("JobDetailFragment", "Has contentV3: ${it.contentV3?.items?.size ?: 0}")
                        
                        titleTextView.text = it.title
                        companyTextView.text = it.company
                        locationTextView.text = it.primaryDetails.Place
                        salaryTextView.text = it.primaryDetails.Salary
                        phoneTextView.text = it.phone
                        descriptionTextView.text = it.description
                        requirementsTextView.text = it.content
                        bookmarkFab.setImageResource(
                            if (it.isBookmarked) {
                                R.drawable.ic_bookmark
                            } else {
                                R.drawable.ic_bookmark_border
                            }
                        )
                        
                        // Load job image if available
                        loadJobImage(it.creatives)
                        
                        // Add job tags if available
                        setupJobTags(it.jobTags)
                        
                        // Show openings count if available
                        if (it.openingsCount != null && it.openingsCount > 0) {
                            openingsTextView.visibility = View.VISIBLE
                            openingsTextView.text = "${it.openingsCount} Openings"
                        } else {
                            openingsTextView.visibility = View.GONE
                        }
                        
                        // Show applications count if available
                        if (it.numApplications != null && it.numApplications > 0) {
                            applicationsTextView.visibility = View.VISIBLE
                            applicationsTextView.text = "${it.numApplications} Applications"
                        } else {
                            applicationsTextView.visibility = View.GONE
                        }
                        
                        // Setup WhatsApp button if available
                        setupWhatsAppButton(it.contactPreference)
                        
                        // Add additional details from contentV3
                        setupAdditionalDetails(it.contentV3)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collectLatest { error ->
                error?.let {
                    binding.errorTextView.text = it
                    binding.errorTextView.visibility = View.VISIBLE
                } ?: run {
                    binding.errorTextView.visibility = View.GONE
                }
            }
        }
    }
    
    private fun loadJobImage(creatives: List<Creative>?) {
        Log.d("JobDetailFragment", "loadJobImage called with ${creatives?.size ?: 0} creatives")
        
        val creativesList = creatives ?: return
        if (creativesList.isEmpty()) return
        
        // Try to get the image URL from either url or file property
        val creative = creativesList[0]
        val imageUrl = when {
            creative.url.isNotBlank() -> creative.url
            creative.file.isNotBlank() -> creative.file
            else -> creative.thumbUrl ?: ""
        }
        
        Log.d("JobDetailFragment", "Using image URL: $imageUrl")
        
        if (imageUrl.isNotBlank()) {
            binding.imageCardView.visibility = View.VISIBLE
            Glide.with(requireContext())
                .load(imageUrl)
                .centerCrop()
                .into(binding.jobImageView)
        } else {
            binding.imageCardView.visibility = View.GONE
            Log.d("JobDetailFragment", "No valid image URL found")
        }
    }
    
    private fun setupJobTags(jobTags: List<JobTag>?) {
        Log.d("JobDetailFragment", "setupJobTags called with ${jobTags?.size ?: 0} tags")
        
        val tagsList = jobTags ?: return
        if (tagsList.isEmpty()) return
        
        binding.tagsChipGroup.visibility = View.VISIBLE
        binding.tagsChipGroup.removeAllViews()
        
        tagsList.forEach { tag ->
            val chip = Chip(requireContext())
            
            // Use title if available, otherwise use value
            val tagText = when {
                tag.title.isNotBlank() -> tag.title
                tag.value.isNotBlank() -> tag.value
                else -> "Tag"
            }
            
            chip.text = tagText
            Log.d("JobDetailFragment", "Adding tag: $tagText")
            
            try {
                // Set chip colors based on tag colors
                chip.chipBackgroundColor = ContextCompat.getColorStateList(
                    requireContext(),
                    android.R.color.transparent
                )
                
                // Try different color properties
                val bgColor = when {
                    tag.color.isNotBlank() -> tag.color
                    tag.bgColor?.isNotBlank() == true -> tag.bgColor
                    else -> null
                }
                
                if (bgColor != null) {
                    Log.d("JobDetailFragment", "Using background color: $bgColor")
                    chip.setChipBackgroundColor(
                        android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor(bgColor)
                        )
                    )
                }
                
                val textColor = tag.textColor
                if (textColor != null && textColor.isNotBlank()) {
                    Log.d("JobDetailFragment", "Using text color: $textColor")
                    chip.setTextColor(android.graphics.Color.parseColor(textColor))
                }
            } catch (e: Exception) {
                Log.e("JobDetailFragment", "Error setting chip colors", e)
                // Use default colors if parsing fails
            }
            
            binding.tagsChipGroup.addView(chip)
        }
    }
    
    private fun setupWhatsAppButton(contactPreference: ContactPreference?) {
        Log.d("JobDetailFragment", "setupWhatsAppButton called with contactPreference: $contactPreference")
        
        val whatsappLink = contactPreference?.whatsappLink
        if (whatsappLink == null || whatsappLink.isBlank()) {
            binding.whatsappButton.visibility = View.GONE
            return
        }
        
        binding.whatsappButton.visibility = View.VISIBLE
        binding.whatsappButton.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(whatsappLink)
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("JobDetailFragment", "Error opening WhatsApp link", e)
                Toast.makeText(requireContext(), "Error opening WhatsApp", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupAdditionalDetails(contentV3: ContentV3?) {
        Log.d("JobDetailFragment", "setupAdditionalDetails called with contentV3: ${contentV3?.items?.size ?: 0} items")
        
        val items = contentV3?.items
        if (items == null || items.isEmpty()) {
            binding.additionalDetailsCard.visibility = View.GONE
            return
        }
        
        binding.additionalDetailsCard.visibility = View.VISIBLE
        binding.additionalDetailsContainer.removeAllViews()
        
        items.forEach { contentItem ->
            val detailView = layoutInflater.inflate(
                R.layout.item_detail_row,
                binding.additionalDetailsContainer,
                false
            )
            
            detailView.findViewById<TextView>(R.id.labelTextView).text = contentItem.fieldName
            detailView.findViewById<TextView>(R.id.valueTextView).text = contentItem.fieldValue
            
            Log.d("JobDetailFragment", "Added detail: ${contentItem.fieldName} = ${contentItem.fieldValue}")
            binding.additionalDetailsContainer.addView(detailView)
        }
    }

    private fun setupBookmarkButton() {
        binding.bookmarkFab.setOnClickListener {
            viewModel.toggleBookmark()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 