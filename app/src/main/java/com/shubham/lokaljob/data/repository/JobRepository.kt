package com.shubham.lokaljob.data.repository

import android.util.Log
import com.shubham.lokaljob.data.api.JobApi
import com.shubham.lokaljob.data.local.JobDao
import com.shubham.lokaljob.data.model.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobRepository @Inject constructor(
    private val jobApi: JobApi,
    private val jobDao: JobDao
) {
    fun getAllJobs(): Flow<List<Job>> = flow {
        try {
            val response = jobApi.getJobs(page = 1)
            Log.d("JobRepository", "API Response: ${response.results.size} jobs received")
            
            // Log each job's data for debugging
            response.results.forEachIndexed { index, job ->
                Log.d("JobRepository", "Job $index: id=${job.id}, title=${job.title}, company=${job.company}")
            }
            
            // Filter out invalid jobs
            val validJobs = response.results.filter { job ->
                job.id != 0 && // Filter out jobs with id=0
                job.title != null && // Filter out jobs with null title
                job.company != null && // Filter out jobs with null company
                job.primaryDetails != null && // Filter out jobs with null primaryDetails
                job.title.isNotBlank() && // Filter out jobs with blank title
                job.company.isNotBlank() // Filter out jobs with blank company
            }
            
            Log.d("JobRepository", "Valid jobs to insert: ${validJobs.size}")
            jobDao.insertJobs(validJobs)
            Log.d("JobRepository", "Jobs inserted into database")
            emitAll(jobDao.getAllJobs())
        } catch (e: Exception) {
            Log.e("JobRepository", "Error fetching jobs", e)
            emitAll(jobDao.getAllJobs())
        }
    }

    fun getBookmarkedJobs(): Flow<List<Job>> = flow {
        emitAll(jobDao.getBookmarkedJobs())
    }

    suspend fun getJobById(id: Int): Job? {
        return jobDao.getJobById(id)
    }

    suspend fun toggleBookmark(jobId: Int) {
        jobDao.getJobById(jobId)?.let { job ->
            jobDao.updateJob(job.copy(isBookmarked = !job.isBookmarked))
        }
    }
} 