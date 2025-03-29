package com.shubham.lokaljob.data.api

import android.util.Log
import com.shubham.lokaljob.data.model.JobResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface JobApi {
    @GET("common/jobs")
    suspend fun getJobs(@Query("page") page: Int): JobResponse {
        Log.d("JobApi", "Fetching jobs for page: $page")
        return try {
            val response = getJobsInternal(page)
            Log.d("JobApi", "Successfully fetched ${response.results.size} jobs")
            response
        } catch (e: Exception) {
            Log.e("JobApi", "Error fetching jobs", e)
            throw e
        }
    }

    @GET("common/jobs")
    suspend fun getJobsInternal(@Query("page") page: Int): JobResponse
} 