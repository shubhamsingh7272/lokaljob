package com.shubham.lokaljob.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
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
                        titleTextView.text = it.title
                        companyTextView.text = it.company
                        locationTextView.text = it.primaryDetails.Place
                        salaryTextView.text = it.primaryDetails.Salary
                        phoneTextView.text = it.phone
                        descriptionTextView.text = it.description
                        requirementsTextView.text = it.content
                        bookmarkFab.setImageResource(
                            if (it.isBookmarked) {
                                com.shubham.lokaljob.R.drawable.ic_bookmark
                            } else {
                                com.shubham.lokaljob.R.drawable.ic_bookmark_border
                            }
                        )
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