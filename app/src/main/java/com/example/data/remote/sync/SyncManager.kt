package com.example.data.remote.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.local.room.dao.WaterIntakeDao
import com.example.data.remote.supabase.SupabaseClient
import com.example.data.remote.supabase.SupabaseWaterIntake
import com.example.domain.repository.HealthRepository
import com.example.domain.repository.SyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncManager(
    private val context: Context,
    private val waterIntakeDao: WaterIntakeDao,
    private val supabaseClient: SupabaseClient
) : SyncRepository, HealthRepository {

    override fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    override suspend fun syncPendingIntakes(): Boolean = withContext(Dispatchers.IO) {
        if (!isOnline() || !supabaseClient.isConfigured) {
            Log.d(TAG, "Offline or Supabase not configured. Skipping network sync.")
            return@withContext false
        }

        try {
            val unsynced = waterIntakeDao.getUnsyncedIntakes()
            var allSuccessful = true
            for (item in unsynced) {
                val dto = SupabaseWaterIntake(
                    id = item.id,
                    userId = item.userId,
                    amountMl = item.amountMl,
                    consumedAt = item.consumedAt,
                    source = item.source,
                    createdAt = item.createdAt
                )
                val success = supabaseClient.insertWaterIntake(dto)
                if (success) {
                    waterIntakeDao.markAsSynced(item.id)
                } else {
                    allSuccessful = false
                }
            }
            allSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}")
            false
        }
    }

    override suspend fun pullRemoteUpdates(): Boolean = withContext(Dispatchers.IO) {
        // Realtime/Pull implementation
        true
    }

    override suspend fun syncHealthData(
        currentDailyIntakeMl: Int,
        dailyGoalMl: Int,
        completedReminders: Int,
        missedReminders: Int,
        streakDays: Int
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isOnline() || !supabaseClient.isConfigured) return@withContext false
        val event = com.example.data.remote.supabase.SupabaseHealthSyncEvent(
            userId = "default_user",
            dailyIntakeMl = currentDailyIntakeMl,
            dailyGoalMl = dailyGoalMl,
            streakDays = streakDays
        )
        supabaseClient.syncHealthEvent(event)
    }

    companion object {
        private const val TAG = "SyncManager"
    }
}
