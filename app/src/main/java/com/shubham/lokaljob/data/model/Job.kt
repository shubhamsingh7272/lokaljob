package com.shubham.lokaljob.data.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import com.shubham.lokaljob.data.local.Converters

// The Room entity with only fields that need to be stored in the database
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
    val description: String = "",
    @SerializedName("openings_count")
    val openingsCount: Int? = null,
    @SerializedName("num_applications")
    val numApplications: Int? = null
) {
    // These fields will be ignored by Room but included in the JSON deserialization
    @Ignore @Transient @SerializedName("job_tags")
    var jobTags: List<JobTag>? = null
    
    @Ignore @Transient @SerializedName("contact_preference")
    var contactPreference: ContactPreference? = null
    
    @Ignore @Transient @SerializedName("creatives")
    var creatives: List<Creative>? = null
    
    @Ignore @Transient @SerializedName("contentV3")
    var contentV3: ContentV3? = null
    
    // Default no-args constructor required by Room
    constructor() : this(
        id = 0,
        title = "",
        company = "",
        primaryDetails = PrimaryDetails(),
        content = "",
        isBookmarked = false,
        phone = "",
        category = "",
        role = "",
        description = "",
        openingsCount = null,
        numApplications = null
    )
}

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

data class JobTag(
    @SerializedName("value")
    val value: String = "",
    @SerializedName("title", alternate = ["name", "tag_name"])
    val title: String = "",
    @SerializedName("color", alternate = ["tag_color"])
    val color: String = "",
    @SerializedName("bg_color")
    val bgColor: String? = null,
    @SerializedName("text_color")
    val textColor: String? = null
)

data class ContactPreference(
    @SerializedName("preference")
    val preference: Int = 0,
    @SerializedName("whatsapp", alternate = ["is_whatsapp", "has_whatsapp"])
    val whatsapp: Boolean = false,
    @SerializedName("whatsapp_link")
    val whatsappLink: String? = null,
    @SerializedName("preferred_call_start_time")
    val preferredCallStartTime: String? = null,
    @SerializedName("preferred_call_end_time")
    val preferredCallEndTime: String? = null
)

data class Creative(
    @SerializedName("file")
    val file: String = "",
    @SerializedName("url", alternate = ["image_url", "img_url"])
    val url: String = "",
    @SerializedName("thumb_url")
    val thumbUrl: String? = null,
    @SerializedName("creative_type")
    val creativeType: Int = 0
)

data class ContentV3(
    @SerializedName("V3")
    val items: List<ContentV3Item>? = null
)

data class ContentV3Item(
    @SerializedName("field_key")
    val fieldKey: String = "",
    @SerializedName("field_name")
    val fieldName: String = "",
    @SerializedName("field_value")
    val fieldValue: String = ""
) 