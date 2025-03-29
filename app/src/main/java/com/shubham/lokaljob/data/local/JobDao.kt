package com.shubham.lokaljob.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shubham.lokaljob.data.model.Job
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs")
    fun getAllJobs(): Flow<List<Job>>

    @Query("SELECT * FROM jobs WHERE isBookmarked = 1")
    fun getBookmarkedJobs(): Flow<List<Job>>

    @Query("SELECT * FROM jobs WHERE id = :id")
    suspend fun getJobById(id: Int): Job?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJobs(jobs: List<Job>)

    @Update
    suspend fun updateJob(job: Job)

    @Query("UPDATE jobs SET isBookmarked = :isBookmarked WHERE id = :jobId")
    suspend fun updateBookmarkStatus(jobId: Int, isBookmarked: Boolean)
} 