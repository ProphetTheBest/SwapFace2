package com.example.faceswapapp.ui

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.faceswapapp.viewmodel.InpaintJobPersistable
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import androidx.compose.ui.geometry.Offset
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import androidx.datastore.preferences.core.edit

private val Context.dataStore by preferencesDataStore("inpaint_job_queue")

private val jobJson = Json {
    serializersModule = SerializersModule {
        contextual(Offset::class, OffsetSerializer)
    }
    ignoreUnknownKeys = true
}

object JobQueueDataStore {
    private val JOBS_KEY = stringPreferencesKey("jobs_json")

    suspend fun saveJobList(context: Context, jobs: List<InpaintJobPersistable>) {
        val json = jobJson.encodeToString(jobs)
        context.dataStore.edit { prefs ->
            prefs[JOBS_KEY] = json
        }
    }

    suspend fun loadJobList(context: Context): List<InpaintJobPersistable> {
        val prefs = context.dataStore.data.first()
        val json = prefs[JOBS_KEY] ?: return emptyList()
        return try {
            jobJson.decodeFromString<List<InpaintJobPersistable>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }
}