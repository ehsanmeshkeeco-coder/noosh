package com.example.data.remote.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.NooshApplication
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/**
 * Section 65: WorkManager Sync Worker
 * Operates with NetworkType.CONNECTED as a persistent safety synchronization mechanism.
 * Uses exponential backoff (Section 72).
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "SyncWorker started execution. Run attempt: $runAttemptCount")
        val app = applicationContext as? NooshApplication ?: return Result.failure()

        if (isStopped) {
            Log.d(TAG, "SyncWorker is stopped before execution.")
            return Result.retry()
        }

        return try {
            val syncManager = app.syncManager
            val allSuccess = syncManager.processOutboxSync()

            if (isStopped) {
                Log.d(TAG, "SyncWorker was stopped during execution.")
                return Result.retry()
            }

            if (allSuccess) {
                Log.d(TAG, "SyncWorker completed all outbox items successfully.")
                Result.success()
            } else {
                Log.w(TAG, "SyncWorker completed with some pending/retrying items.")
                if (runAttemptCount < MAX_ATTEMPTS) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        } catch (e: CancellationException) {
            Log.d(TAG, "SyncWorker coroutine cancelled: ${e.message}")
            Result.retry()
        } catch (e: Exception) {
            if (e is CancellationException) {
                Log.d(TAG, "SyncWorker coroutine cancelled: ${e.message}")
                Result.retry()
            } else {
                Log.e(TAG, "SyncWorker failed with exception: ${e.message}", e)
                if (runAttemptCount < MAX_ATTEMPTS) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val UNIQUE_PERIODIC_NAME = "noosh_periodic_sync"
        private const val UNIQUE_IMMEDIATE_NAME = "noosh_immediate_sync"
        private const val MAX_ATTEMPTS = 5

        fun enqueueImmediateSync(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val request = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        30,
                        TimeUnit.SECONDS
                    )
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    UNIQUE_IMMEDIATE_NAME,
                    ExistingWorkPolicy.KEEP,
                    request
                )
                Log.d(TAG, "Enqueued immediate SyncWorker.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enqueue immediate SyncWorker: ${e.message}")
            }
        }

        fun schedulePeriodicSync(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        30,
                        TimeUnit.SECONDS
                    )
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    UNIQUE_PERIODIC_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
                Log.d(TAG, "Scheduled periodic SyncWorker.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule periodic SyncWorker: ${e.message}")
            }
        }
    }
}
