package com.johnchourp.learnbyzantinemusic.recordings.index

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class RecordingsReindexWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val rootUriRaw = inputData.getString(KEY_ROOT_URI) ?: return Result.failure()
        val rootUri = runCatching { Uri.parse(rootUriRaw) }.getOrNull() ?: return Result.failure()
        val repository = RecordingsRepository.getInstance(applicationContext)
        val result = repository.reindex(rootUri)
        return if (result.completed) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        const val KEY_ROOT_URI = "root_uri"
    }
}
