package com.shubham.lokaljob.di

import android.content.Context
import androidx.room.Room
import com.shubham.lokaljob.data.api.JobApi
import com.shubham.lokaljob.data.local.AppDatabase
import com.shubham.lokaljob.data.local.JobDao
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

    // Network
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://testapi.getlokalapp.com/") // Replace with your actual API base URL
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
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
        ).build()
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