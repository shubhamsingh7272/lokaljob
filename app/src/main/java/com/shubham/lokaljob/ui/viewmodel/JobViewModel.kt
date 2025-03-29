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

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadJobs()
        loadBookmarkedJobs()
    }

    fun loadJobs() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.getAllJobs().collect { jobList ->
                    Log.d("JobViewModel", "Received ${jobList.size} jobs")
                    _jobs.value = jobList
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("JobViewModel", "Error loading jobs", e)
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    private fun loadBookmarkedJobs() {
        viewModelScope.launch {
            repository.getBookmarkedJobs()
                .catch { e ->
                    Log.e("JobViewModel", "Error loading bookmarked jobs", e)
                    _error.value = e.message
                }
                .collect { jobs ->
                    Log.d("JobViewModel", "Received ${jobs.size} bookmarked jobs")
                    _bookmarkedJobs.value = jobs
                }
        }
    }

    fun loadJobDetails(jobId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val job = _jobs.value.find { it.id == jobId } ?: _bookmarkedJobs.value.find { it.id == jobId }
                _selectedJob.value = job
            } catch (e: Exception) {
                Log.e("JobViewModel", "Error loading job details", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleBookmark(jobId: Int) {
        viewModelScope.launch {
            try {
                repository.toggleBookmark(jobId)
            } catch (e: Exception) {
                Log.e("JobViewModel", "Error toggling bookmark", e)
                _error.value = e.message
            }
        }
    }
} 