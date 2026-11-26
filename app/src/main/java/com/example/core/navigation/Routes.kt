package com.example.core.navigation

import kotlinx.serialization.Serializable

sealed interface MyCareRoute {
    @Serializable data object LanguageSelection : MyCareRoute

    // 5 Primary Bottom Navigation Destinations
    @Serializable data object Home : MyCareRoute
    @Serializable data object Wallet : MyCareRoute
    @Serializable data object HealthHub : MyCareRoute
    @Serializable data object Memory : MyCareRoute
    @Serializable data object Settings : MyCareRoute

    // Feature Modules
    @Serializable data object MedicineList : MyCareRoute
    @Serializable data class MedicineDetail(val medicineId: Long) : MyCareRoute

    @Serializable data object ExerciseList : MyCareRoute
    @Serializable data class ExerciseDetail(val exerciseId: Long) : MyCareRoute

    @Serializable data object Tasks : MyCareRoute
    @Serializable data object Grocery : MyCareRoute
    @Serializable data class GroceryListDetail(val listId: Long) : MyCareRoute

    @Serializable data class AlbumDetail(val albumId: Long) : MyCareRoute

    @Serializable data object MedicalRecords : MyCareRoute
    @Serializable data class MedicalFolderDetail(val folderId: Long) : MyCareRoute

    @Serializable data object DoctorSearch : MyCareRoute
    @Serializable data object Nutrition : MyCareRoute
    @Serializable data object Travel : MyCareRoute
    @Serializable data class TravelDetail(val travelId: Long) : MyCareRoute
    @Serializable data object Emergency : MyCareRoute

    // Full-screen Media / Document Viewer
    @Serializable data class MediaViewer(
        val title: String,
        val filePath: String,
        val mediaType: String,
        val mimeType: String
    ) : MyCareRoute
}
