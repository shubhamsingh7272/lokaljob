# LokalJob Android Application

## Overview
LokalJob is a native Android application that connects job seekers with local employment opportunities. The app displays job listings fetched from an API, allows users to view detailed job information, bookmark favorite positions, and contact employers directly through WhatsApp.

## Features
- **Job Listings**: Browse through available job opportunities with essential information like title, company, salary, and location
- **Detailed Job View**: Access comprehensive job details including job description, requirements, openings count, and application status
- **Job Tags**: Visual categorization of jobs with color-coded tags
- **Bookmark System**: Save favorite jobs for later viewing
- **Direct Contact**: Connect with employers through WhatsApp integration
- **Offline Support**: View previously loaded jobs when offline through local database storage

## Technical Architecture
The app follows the MVVM (Model-View-ViewModel) architecture pattern and implements modern Android development practices:

### Key Components
- **UI Layer**: Fragments with ViewBinding for UI display and user interaction
- **ViewModel Layer**: ViewModels to handle UI-related data and business logic
- **Repository Layer**: Centralized data management combining local and remote data sources
- **Database**: Room persistence library for local storage
- **Network**: Retrofit and OkHttp for API communication
- **Dependency Injection**: Hilt for dependency management
- **Image Loading**: Glide for efficient image loading and caching

### Data Flow
1. Data is fetched from the remote API using Retrofit
2. Parsed using Gson with custom type adapters for complex objects
3. Stored in Room database for offline access
4. Combined with transient fields in the repository layer
5. Exposed to the UI through ViewModels using Kotlin Flow
6. Rendered in the UI using ViewBinding

## Data Model
The main data entity is the `Job` class which contains:
- Basic information (ID, title, company, etc.)
- Primary details (location, salary, job type)
- Transient fields that are not stored in the database:
  - Job tags for categorization
  - Contact preferences for employer communication
  - Creative assets like images
  - Additional structured content (contentV3)

## Design Patterns
- **Repository Pattern**: Single source of truth for data operations
- **Observer Pattern**: Using Kotlin Flows for reactive UI updates
- **Adapter Pattern**: Custom Gson adapters for complex JSON parsing
- **Dependency Injection**: Hilt for providing dependencies
- **ViewBinding**: Type-safe interaction with UI elements

## Dependencies
- **AndroidX Core & AppCompat**: Core Android functionality and backward compatibility
- **Material Design Components**: Modern UI elements following Material Design guidelines
- **Navigation Component**: Simplified fragment navigation
- **Retrofit**: Type-safe HTTP client for API communication
- **Gson**: JSON parsing and serialization
- **Room**: SQLite database abstraction layer
- **Hilt**: Dependency injection
- **Glide**: Image loading and caching
- **Coroutines**: Asynchronous operations with Kotlin Coroutines
- **SwipeRefreshLayout**: Pull-to-refresh functionality

## Setup and Installation
1. Clone the repository
2. Open the project in Android Studio (Arctic Fox or newer recommended)
3. Sync Gradle and resolve dependencies
4. Build and run on an Android device or emulator (API 26+ required)

## Architecture Diagram
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│                 │    │                 │    │                 │
│  UI (Fragments) │◄───┤   ViewModels    │◄───┤   Repository    │
│                 │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └────────┬────────┘
                                                       │
                                                       ▼
                                              ┌─────────────────┐
                                              │                 │
                                              │ Data Sources    │
                                              │                 │
                                              └────────┬────────┘
                                                       │
                                                       ▼
                           ┌─────────────────┐    ┌─────────────────┐
                           │                 │    │                 │
                           │  Room Database  │    │  Network API    │
                           │                 │    │                 │
                           └─────────────────┘    └─────────────────┘
```

## Key Implementation Challenges
- **Transient Field Handling**: Managing fields that are received from API but not stored in database
- **Complex JSON Parsing**: Custom Gson type adapter for handling nested structures
- **UI State Management**: Proper handling of loading, error, and success states
- **Data Persistence**: Combining database and in-memory caching for optimal performance
- **Offline Support**: Ensuring app functionality without network connectivity

## Future Enhancements
- User authentication system
- Job application tracking
- Notifications for new matching jobs
- Advanced filtering and search capabilities
- Map view for job locations
- In-app messaging with employers 