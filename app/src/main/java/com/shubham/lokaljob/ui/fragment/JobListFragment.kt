package com.shubham.lokaljob.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
        setupScrollListener()
        setupObservers()
        setupSwipeRefresh()
        viewModel.loadJobs()
    }

    private fun setupRecyclerView() {
        jobAdapter = JobAdapter(
            onJobClicked = { job ->
                viewModel.selectJob(job)
                findNavController().navigate(
                    JobListFragmentDirections.actionJobsToJobDetail(job.id)
                )
            },
            onBookmarkClicked = { job ->
                viewModel.toggleBookmark(job.id)
            }
        )

        binding.jobsRecyclerView.adapter = jobAdapter
    }
    
    private fun setupScrollListener() {
        binding.jobsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                
                if (!viewModel.isLoadingMore.value && !viewModel.isLoading.value) {
                    // Trigger load more when user is within 5 items of the end
                    if ((visibleItemCount + firstVisibleItemPosition + 5) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                        && totalItemCount >= 10
                        && viewModel.canLoadMore.value
                    ) {
                        Log.d("JobListFragment", "Loading more jobs, current count: $totalItemCount")
                        viewModel.loadMoreJobs()
                    }
                }
            }
        })
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.jobs.collectLatest { jobs ->
                        Log.d("JobListFragment", "Received ${jobs.size} jobs")
                        jobAdapter.submitList(jobs)
                        updateEmptyState(jobs.isEmpty())
                    }
                }

                launch {
                    viewModel.isLoading.collectLatest { isLoading ->
                        binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
                        if (isLoading) {
                            binding.emptyStateLayout.visibility = View.GONE
                            binding.errorTextView.visibility = View.GONE
                        }
                    }
                }

                launch {
                    viewModel.isLoadingMore.collectLatest { isLoadingMore ->
                        binding.loadingMoreIndicator.visibility = if (isLoadingMore) View.VISIBLE else View.GONE
                        Log.d("JobListFragment", "Loading more: $isLoadingMore")
                    }
                }

                launch {
                    viewModel.error.collectLatest { errorMessage ->
                        if (errorMessage.isNotBlank()) {
                            binding.errorTextView.text = errorMessage
                            binding.errorTextView.visibility = View.VISIBLE
                            binding.progressIndicator.visibility = View.GONE
                        } else {
                            binding.errorTextView.visibility = View.GONE
                        }
                    }
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
        if (isEmpty && !viewModel.isLoading.value) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.jobsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.jobsRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 