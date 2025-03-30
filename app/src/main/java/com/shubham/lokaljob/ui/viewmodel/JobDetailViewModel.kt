package com.shubham.lokaljob.ui.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shubham.lokaljob.data.model.Job
import com.shubham.lokaljob.data.repository.JobRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JobDetailViewModel @Inject constructor(
    private val repository: JobRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val jobId: Int = checkNotNull(savedStateHandle["jobId"])

    private val _job = MutableStateFlow<Job?>(null)
    val job: StateFlow<Job?> = _job.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        Log.d("JobDetailViewModel", "Initializing with jobId: $jobId")
        loadJobDetails()
    }

    private fun loadJobDetails() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d("JobDetailViewModel", "Loading job details for jobId: $jobId")
                
                val loadedJob = repository.getJobById(jobId)
                
                if (loadedJob != null) {
                    Log.d("JobDetailViewModel", "Successfully loaded job: id=${loadedJob.id}, title=${loadedJob.title}")
                    Log.d("JobDetailViewModel", "Job has jobTags: ${loadedJob.jobTags?.size ?: 0}")
                    Log.d("JobDetailViewModel", "Job has contactPreference: ${loadedJob.contactPreference != null}")
                    Log.d("JobDetailViewModel", "Job has creatives: ${loadedJob.creatives?.size ?: 0}")
                    Log.d("JobDetailViewModel", "Job has contentV3: ${loadedJob.contentV3?.items?.size ?: 0}")
                    _job.value = loadedJob
                } else {
                    Log.e("JobDetailViewModel", "Job not found with id: $jobId")
                    _error.value = "Job not found with id: $jobId"
                }
                
                _isLoading.value = false
            } catch (e: Exception) {
                Log.e("JobDetailViewModel", "Error loading job details", e)
                _error.value = e.message ?: "Unknown error occurred"
                _isLoading.value = false
            }
        }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            try {
                Log.d("JobDetailViewModel", "Toggling bookmark for jobId: $jobId")
                repository.toggleBookmark(jobId)
                // Reload job details to get updated bookmark status
                loadJobDetails()
            } catch (e: Exception) {
                Log.e("JobDetailViewModel", "Error toggling bookmark", e)
                _error.value = e.message ?: "Error toggling bookmark"
            }
        }
    }
} 