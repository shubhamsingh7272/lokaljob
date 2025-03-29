package com.shubham.lokaljob.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import com.shubham.lokaljob.data.local.Converters

@Entity(tableName = "jobs")
@TypeConverters(Converters::class)
data class Job(
    @PrimaryKey
    val id: Int,
    val title: String = "",
    @SerializedName("company_name")
    val company: String = "",
    @SerializedName("primary_details")
    val primaryDetails: PrimaryDetails = PrimaryDetails(),
    val content: String = "",
    @SerializedName("is_bookmarked")
    val isBookmarked: Boolean = false,
    @SerializedName("whatsapp_no")
    val phone: String = "",
    @SerializedName("job_category")
    val category: String = "",
    @SerializedName("job_role")
    val role: String = "",
    @SerializedName("other_details")
    val description: String = ""
)

data class PrimaryDetails(
    val Place: String = "",
    val Salary: String = "",
    @SerializedName("Job_Type")
    val jobType: String = "",
    val Experience: String = "",
    @SerializedName("Fees_Charged")
    val feesCharged: String = "",
    val Qualification: String = ""
) 