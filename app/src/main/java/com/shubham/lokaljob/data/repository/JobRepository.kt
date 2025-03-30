package com.shubham.lokaljob.data.repository

import android.util.Log
import com.google.gson.Gson
import com.shubham.lokaljob.data.api.JobApi
import com.shubham.lokaljob.data.local.JobDao
import com.shubham.lokaljob.data.model.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobRepository @Inject constructor(
    private val jobApi: JobApi,
    private val jobDao: JobDao
) {
    // In-memory cache of API jobs to preserve transient fields
    private val jobsCache = mutableMapOf<Int, Job>()
    
    // Current page for pagination
    private var currentPage = 1
    
    // Total number of pages to fetch (can be adjusted based on API response)
    private val pagesToFetch = 3

    fun getAllJobs(): Flow<List<Job>> = flow {
        try {
            val allJobs = mutableListOf<Job>()
            
            // Fetch multiple pages
            for (page in 1..pagesToFetch) {
                Log.d("JobRepository", "Fetching page $page of $pagesToFetch")
                val response = jobApi.getJobs(page = page)
                
                if (response.results.isEmpty()) {
                    Log.d("JobRepository", "No more results on page $page, stopping pagination")
                    break
                }
                
                allJobs.addAll(response.results)
                Log.d("JobRepository", "Page $page: ${response.results.size} jobs received. Total so far: ${allJobs.size}")
                
                // If this is the first page, log some debug info about the first job
                if (page == 1 && response.results.isNotEmpty()) {
                    val firstJob = response.results[0]
                    val rawJson = Gson().toJson(firstJob)
                    Log.d("JobRepository", "First job raw JSON: ${rawJson.take(500)}...")
                    
                    // Check specific fields
                    Log.d("JobRepository", "jobTags raw: ${rawJson.contains("job_tags")}")
                    Log.d("JobRepository", "contactPreference raw: ${rawJson.contains("contact_preference")}")
                    Log.d("JobRepository", "creatives raw: ${rawJson.contains("creatives")}")
                    Log.d("JobRepository", "contentV3 raw: ${rawJson.contains("contentV3")}")
                }
            }
            
            Log.d("JobRepository", "Total jobs fetched from all pages: ${allJobs.size}")
            
            // Store all jobs in the cache for later use in getJobById
            allJobs.forEach { job ->
                jobsCache[job.id] = job
            }
            
            // Filter out invalid jobs
            val validJobs = allJobs.filter { job ->
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
            
            // Combine database jobs with detailed information from API
            // This ensures we preserve transient fields while using Room for persistence
            val dbJobs = jobDao.getAllJobs()
            
            dbJobs.map { jobs ->
                jobs.map { dbJob ->
                    // Find the matching job from API response to get transient fields
                    val apiJob = jobsCache[dbJob.id]
                    
                    // Copy transient fields if available
                    if (apiJob != null) {
                        dbJob.jobTags = apiJob.jobTags
                        dbJob.contactPreference = apiJob.contactPreference
                        dbJob.creatives = apiJob.creatives
                        dbJob.contentV3 = apiJob.contentV3
                        Log.d("JobRepository", "Combined job id=${dbJob.id} with transient fields from API")
                    } else {
                        Log.d("JobRepository", "No matching API job found for db job id=${dbJob.id}")
                    }
                    
                    dbJob
                }
            }.collect { emit(it) }
        } catch (e: Exception) {
            Log.e("JobRepository", "Error fetching jobs", e)
            emitAll(jobDao.getAllJobs())
        }
    }
    
    // Load more jobs (can be called from UI when user scrolls to bottom)
    fun loadMoreJobs(): Flow<List<Job>> = flow {
        try {
            currentPage++
            Log.d("JobRepository", "Loading more jobs, page: $currentPage")
            
            val response = jobApi.getJobs(page = currentPage)
            Log.d("JobRepository", "API Response for page $currentPage: ${response.results.size} jobs received")
            
            if (response.results.isEmpty()) {
                Log.d("JobRepository", "No more results on page $currentPage")
                emit(emptyList())
                return@flow
            }
            
            // Store all jobs in the cache for later use in getJobById
            response.results.forEach { job ->
                jobsCache[job.id] = job
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
            
            Log.d("JobRepository", "Valid jobs to insert from page $currentPage: ${validJobs.size}")
            jobDao.insertJobs(validJobs)
            
            // Return the new jobs to be displayed
            emit(validJobs)
        } catch (e: Exception) {
            Log.e("JobRepository", "Error fetching more jobs", e)
            emit(emptyList())
        }
    }

    fun getBookmarkedJobs(): Flow<List<Job>> = flow {
        emitAll(jobDao.getBookmarkedJobs())
    }

    suspend fun getJobById(id: Int): Job? {
        Log.d("JobRepository", "getJobById called for id: $id")
        
        // Get job from database
        val dbJob = jobDao.getJobById(id)
        
        if (dbJob == null) {
            Log.e("JobRepository", "Job with id $id not found in database")
            return null
        }
        
        // Get job from cache
        val cachedJob = jobsCache[id]
        
        if (cachedJob != null) {
            Log.d("JobRepository", "Found job in cache. Combining with DB job.")
            // Copy transient fields from cached job
            dbJob.jobTags = cachedJob.jobTags
            dbJob.contactPreference = cachedJob.contactPreference
            dbJob.creatives = cachedJob.creatives
            dbJob.contentV3 = cachedJob.contentV3
            
            // Log the combined job's fields
            Log.d("JobRepository", "Combined job: ${dbJob.id}, ${dbJob.title}")
            Log.d("JobRepository", "- jobTags: ${dbJob.jobTags?.size ?: 0}")
            Log.d("JobRepository", "- contactPreference: ${dbJob.contactPreference != null}")
            Log.d("JobRepository", "- creatives: ${dbJob.creatives?.size ?: 0}")
            Log.d("JobRepository", "- contentV3: ${dbJob.contentV3?.items?.size ?: 0}")
        } else {
            Log.d("JobRepository", "Job not found in cache, returning DB job without transient fields")
        }
        
        return dbJob
    }

    suspend fun toggleBookmark(jobId: Int) {
        jobDao.getJobById(jobId)?.let { job ->
            jobDao.updateJob(job.copy(isBookmarked = !job.isBookmarked))
        }
    }
} 