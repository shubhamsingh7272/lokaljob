package com.shubham.lokaljob.ui.viewmodel

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
class BookmarksViewModel @Inject constructor(
    private val repository: JobRepository
) : ViewModel() {

    private val _bookmarkedJobs = MutableStateFlow<List<Job>>(emptyList())
    val bookmarkedJobs: StateFlow<List<Job>> = _bookmarkedJobs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadBookmarkedJobs()
    }

    fun loadBookmarkedJobs() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.getBookmarkedJobs().collect { jobs ->
                    _bookmarkedJobs.value = jobs
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun toggleBookmark(jobId: Int) {
        viewModelScope.launch {
            try {
                repository.toggleBookmark(jobId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
} 