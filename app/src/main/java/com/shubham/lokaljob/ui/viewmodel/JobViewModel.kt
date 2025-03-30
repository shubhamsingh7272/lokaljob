package com.shubham.lokaljob.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shubham.lokaljob.data.model.Job
import com.shubham.lokaljob.data.repository.JobRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("KotlinConstantConditions")
@HiltViewModel
class JobViewModel @Inject constructor(
    private val repository: JobRepository
) : ViewModel() {

    private val _jobs = MutableStateFlow<List<Job>>(emptyList())
    val jobs: StateFlow<List<Job>> = _jobs.asStateFlow()

    private val _bookmarkedJobs = MutableStateFlow<List<Job>>(emptyList())
    val bookmarkedJobs: StateFlow<List<Job>> = _bookmarkedJobs

    private val _selectedJob = MutableStateFlow<Job?>(null)
    val selectedJob: StateFlow<Job?> = _selectedJob

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()
    
    private val _canLoadMore = MutableStateFlow(true)
    val canLoadMore: StateFlow<Boolean> = _canLoadMore.asStateFlow()

    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error.asStateFlow()

    init {
        loadJobs()
        loadBookmarkedJobs()
    }

    fun loadJobs() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = ""
                _canLoadMore.value = true // Reset pagination state
                
                repository.getAllJobs().collect { jobList ->
                    Log.d("JobViewModel", "Received ${jobList.size} jobs")
                    _jobs.value = jobList
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("JobViewModel", "Error loading jobs", e)
                _error.value = e.message ?: "Unknown error occurred"
                _isLoading.value = false
            }
        }
    }
    
    fun loadMoreJobs() {
        if (_isLoadingMore.value || !_canLoadMore.value) {
            return // Already loading or no more data
        }
        
        viewModelScope.launch {
            try {
                _isLoadingMore.value = true
                Log.d("JobViewModel", "Loading more jobs")
                
                repository.loadMoreJobs().collect { newJobs ->
                    Log.d("JobViewModel", "Received ${newJobs.size} more jobs")
                    
                    if (newJobs.isEmpty()) {
                        // No more jobs to load
                        _canLoadMore.value = false
                    } else {
                        // Append new jobs to existing list
                        val currentJobs = _jobs.value
                        _jobs.value = currentJobs + newJobs
                    }
                    
                    _isLoadingMore.value = false
                }
            } catch (e: Exception) {
                Log.e("JobViewModel", "Error loading more jobs", e)
                _error.value = e.message ?: "Unknown error occurred"
                _isLoadingMore.value = false
            }
        }
    }

    private fun loadBookmarkedJobs() {
        viewModelScope.launch {
            repository.getBookmarkedJobs()
                .catch { e ->
                    Log.e("JobViewModel", "Error loading bookmarked jobs", e)
                    _error.value = e.message ?: "Unknown error occurred"
                }
                .collect { jobs ->
                    Log.d("JobViewModel", "Received ${jobs.size} bookmarked jobs")
                    _bookmarkedJobs.value = jobs
                }
        }
    }

    fun selectJob(job: Job) {
        _selectedJob.value = job
    }

    fun toggleBookmark(jobId: Int) {
        viewModelScope.launch {
            try {
                repository.toggleBookmark(jobId)
                // Refresh job lists to reflect updated bookmark status
                loadJobs()
                loadBookmarkedJobs()
            } catch (e: Exception) {
                Log.e("JobViewModel", "Error toggling bookmark", e)
                _error.value = e.message ?: "Unknown error occurred"
            }
        }
    }
} 