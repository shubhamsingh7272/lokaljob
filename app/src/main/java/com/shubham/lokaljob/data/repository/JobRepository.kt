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

    fun getAllJobs(): Flow<List<Job>> = flow {
        try {
            val response = jobApi.getJobs(page = 1)
            Log.d("JobRepository", "API Response: ${response.results.size} jobs received")
            
            // Log the raw JSON for the first job
            if (response.results.isNotEmpty()) {
                val firstJob = response.results[0]
                val rawJson = Gson().toJson(firstJob)
                Log.d("JobRepository", "First job raw JSON: $rawJson")
                
                // Check specific fields
                Log.d("JobRepository", "jobTags raw: ${rawJson.contains("job_tags")}")
                Log.d("JobRepository", "contactPreference raw: ${rawJson.contains("contact_preference")}")
                Log.d("JobRepository", "creatives raw: ${rawJson.contains("creatives")}")
                Log.d("JobRepository", "contentV3 raw: ${rawJson.contains("contentV3")}")
            }
            
            // Store all jobs in the cache for later use in getJobById
            response.results.forEach { job ->
                jobsCache[job.id] = job
            }
            
            // Log each job's data for debugging
            response.results.forEachIndexed { index, job ->
                Log.d("JobRepository", "Job $index: id=${job.id}, title=${job.title}, company=${job.company}")
                
                // Log transient fields
                Log.d("JobRepository", "Job $index transient fields:")
                Log.d("JobRepository", "- jobTags: ${job.jobTags?.size ?: 0} tags")
                if (job.jobTags?.isNotEmpty() == true) {
                    job.jobTags?.take(2)?.forEach { tag ->
                        // Use safe access with fallback empty strings
                        val tagTitle = tag.title.takeIf { it.isNotBlank() } ?: tag.value
                        Log.d("JobRepository", "  - Tag: ${tagTitle}, color: ${tag.color.ifBlank { tag.bgColor ?: "none" }}")
                    }
                }
                
                Log.d("JobRepository", "- contactPreference: ${job.contactPreference != null}")
                job.contactPreference?.let { cp ->
                    Log.d("JobRepository", "  - WhatsApp: ${cp.whatsapp}")
                }
                
                Log.d("JobRepository", "- creatives: ${job.creatives?.size ?: 0} items")
                if (job.creatives?.isNotEmpty() == true) {
                    job.creatives?.firstOrNull()?.let { creative ->
                        // Use file as fallback if url is empty
                        val imageUrl = creative.url.takeIf { it.isNotBlank() } ?: creative.file
                        Log.d("JobRepository", "  - First creative URL: $imageUrl")
                    }
                }
                
                Log.d("JobRepository", "- contentV3: ${job.contentV3 != null}")
                if (job.contentV3 != null && job.contentV3?.items?.isNotEmpty() == true) {
                    Log.d("JobRepository", "  - ContentV3 items: ${job.contentV3?.items?.size ?: 0}")
                }
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
            
            // Combine database jobs with detailed information from API
            // This ensures we preserve transient fields while using Room for persistence
            val dbJobs = jobDao.getAllJobs()
            
            dbJobs.map { jobs ->
                jobs.map { dbJob ->
                    // Find the matching job from API response to get transient fields
                    val apiJob = response.results.find { it.id == dbJob.id }
                    
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