package com.shubham.lokaljob.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.shubham.lokaljob.databinding.FragmentJobListBinding
import com.shubham.lokaljob.ui.adapter.JobAdapter
import com.shubham.lokaljob.ui.viewmodel.JobViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class JobListFragment : Fragment() {

    private var _binding: FragmentJobListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JobViewModel by viewModels()
    private lateinit var jobAdapter: JobAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJobListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
        setupSwipeRefresh()
    }

    private fun setupRecyclerView() {
        jobAdapter = JobAdapter(
            onJobClick = { job ->
                findNavController().navigate(
                    JobListFragmentDirections.actionJobsToJobDetail(job.id)
                )
            },
            onBookmarkClick = { job ->
                viewModel.toggleBookmark(job.id)
            }
        )

        binding.jobsRecyclerView.adapter = jobAdapter
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.jobs.collectLatest { jobs ->
                Log.d("JobListFragment", "Received ${jobs.size} jobs")
                jobAdapter.submitList(jobs)
                updateEmptyState(jobs.isEmpty())
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                Log.d("JobListFragment", "Loading state: $isLoading")
                binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collectLatest { error ->
                error?.let {
                    Log.e("JobListFragment", "Error: $it")
                    binding.errorTextView.text = it
                    binding.errorTextView.visibility = View.VISIBLE
                } ?: run {
                    binding.errorTextView.visibility = View.GONE
                }
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadJobs()
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        Log.d("JobListFragment", "Empty state: $isEmpty")
        binding.emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.jobsRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 