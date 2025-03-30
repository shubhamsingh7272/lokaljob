package com.shubham.lokaljob.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.shubham.lokaljob.data.api.JobApi
import com.shubham.lokaljob.data.local.AppDatabase
import com.shubham.lokaljob.data.local.JobDao
import com.shubham.lokaljob.data.model.Job
import com.shubham.lokaljob.data.repository.JobRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Custom Gson provider
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(Job::class.java, JsonDeserializer { json, _, _ ->
                val jsonObject = json.asJsonObject
                
                // First create the Job object with the database fields
                val job = Job(
                    id = if (jsonObject.has("id")) jsonObject.get("id").asInt else 0,
                    title = if (jsonObject.has("title")) jsonObject.get("title").asString else "",
                    company = if (jsonObject.has("company_name")) jsonObject.get("company_name").asString else "",
                    primaryDetails = if (jsonObject.has("primary_details")) 
                        Gson().fromJson(jsonObject.get("primary_details"), com.shubham.lokaljob.data.model.PrimaryDetails::class.java) 
                    else com.shubham.lokaljob.data.model.PrimaryDetails(),
                    content = if (jsonObject.has("content")) jsonObject.get("content").asString else "",
                    isBookmarked = if (jsonObject.has("is_bookmarked")) jsonObject.get("is_bookmarked").asBoolean else false,
                    phone = if (jsonObject.has("whatsapp_no")) jsonObject.get("whatsapp_no").asString else "",
                    category = if (jsonObject.has("job_category")) jsonObject.get("job_category").asString else "",
                    role = if (jsonObject.has("job_role")) jsonObject.get("job_role").asString else "",
                    description = if (jsonObject.has("other_details")) jsonObject.get("other_details").asString else "",
                    openingsCount = if (jsonObject.has("openings_count") && !jsonObject.get("openings_count").isJsonNull) 
                        jsonObject.get("openings_count").asInt else null,
                    numApplications = if (jsonObject.has("num_applications") && !jsonObject.get("num_applications").isJsonNull) 
                        jsonObject.get("num_applications").asInt else null
                )
                
                // Then set the transient fields using a different Gson instance that doesn't have custom type adapters
                val nestedGson = GsonBuilder().create()
                
                // Debug log the raw JSON elements for transient fields
                if (jsonObject.has("job_tags")) {
                    Log.d("Deserializer", "job_tags raw: ${jsonObject.get("job_tags")}")
                } else {
                    Log.d("Deserializer", "job_tags field is missing in JSON")
                }
                
                if (jsonObject.has("contact_preference")) {
                    Log.d("Deserializer", "contact_preference raw: ${jsonObject.get("contact_preference")}")
                } else {
                    Log.d("Deserializer", "contact_preference field is missing in JSON")
                }
                
                if (jsonObject.has("creatives")) {
                    Log.d("Deserializer", "creatives raw: ${jsonObject.get("creatives")}")
                } else {
                    Log.d("Deserializer", "creatives field is missing in JSON")
                }
                
                if (jsonObject.has("contentV3")) {
                    Log.d("Deserializer", "contentV3 raw: ${jsonObject.get("contentV3")}")
                } else {
                    Log.d("Deserializer", "contentV3 field is missing in JSON")
                }
                
                // Parse JobTags
                if (jsonObject.has("job_tags") && !jsonObject.get("job_tags").isJsonNull) {
                    try {
                        job.jobTags = nestedGson.fromJson(
                            jsonObject.get("job_tags"), 
                            Array<com.shubham.lokaljob.data.model.JobTag>::class.java
                        ).toList()
                    } catch (e: Exception) {
                        // If there's an error parsing, set to empty list
                        job.jobTags = emptyList()
                    }
                }
                
                // Parse ContactPreference
                if (jsonObject.has("contact_preference") && !jsonObject.get("contact_preference").isJsonNull) {
                    try {
                        job.contactPreference = nestedGson.fromJson(
                            jsonObject.get("contact_preference"), 
                            com.shubham.lokaljob.data.model.ContactPreference::class.java
                        )
                    } catch (e: Exception) {
                        // If there's an error parsing, create a default instance
                        job.contactPreference = com.shubham.lokaljob.data.model.ContactPreference()
                    }
                }
                
                // Parse Creatives
                if (jsonObject.has("creatives") && !jsonObject.get("creatives").isJsonNull) {
                    try {
                        job.creatives = nestedGson.fromJson(
                            jsonObject.get("creatives"), 
                            Array<com.shubham.lokaljob.data.model.Creative>::class.java
                        ).toList()
                    } catch (e: Exception) {
                        // If there's an error parsing, set to empty list
                        job.creatives = emptyList()
                    }
                }
                
                // Parse ContentV3
                if (jsonObject.has("contentV3") && !jsonObject.get("contentV3").isJsonNull) {
                    try {
                        job.contentV3 = nestedGson.fromJson(
                            jsonObject.get("contentV3"), 
                            com.shubham.lokaljob.data.model.ContentV3::class.java
                        )
                    } catch (e: Exception) {
                        // If there's an error parsing, create a default instance
                        job.contentV3 = com.shubham.lokaljob.data.model.ContentV3(items = emptyList())
                    }
                }
                
                job
            })
            .create()
    }

    // Network
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .addInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                
                // Only log for the jobs endpoint
                if (request.url.toString().contains("common/jobs")) {
                    try {
                        val responseBody = response.peekBody(Long.MAX_VALUE)
                        val rawJson = responseBody.string()
                        Log.d("RawResponse", "Raw JSON response: ${rawJson.take(500)}...") // Take first 500 chars to avoid log overflow
                        
                        // Log field presence
                        Log.d("RawResponse", "Contains job_tags: ${rawJson.contains("job_tags")}")
                        Log.d("RawResponse", "Contains contact_preference: ${rawJson.contains("contact_preference")}")
                        Log.d("RawResponse", "Contains creatives: ${rawJson.contains("creatives")}")
                        Log.d("RawResponse", "Contains contentV3: ${rawJson.contains("contentV3")}")
                    } catch (e: Exception) {
                        Log.e("RawResponse", "Error logging response", e)
                    }
                }
                
                response
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://testapi.getlokalapp.com/") // Replace with your actual API base URL
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideJobApi(retrofit: Retrofit): JobApi {
        return retrofit.create(JobApi::class.java)
    }

    // Database
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "lokaljob.db"
        )
        .fallbackToDestructiveMigration() // Handle schema changes by recreating the database
        .build()
    }

    @Provides
    @Singleton
    fun provideJobDao(database: AppDatabase) = database.jobDao()

    // Repository
    @Provides
    @Singleton
    fun provideJobRepository(
        jobApi: JobApi,
        jobDao: JobDao
    ): JobRepository {
        return JobRepository(jobApi, jobDao)
    }
} 