package com.osprey74.michinavi.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.signin.GoogleSignIn
import java.util.concurrent.TimeUnit

/**
 * データ変更後に自動でGoogle Driveへバックアップするワーカー。
 *
 * [BackupScheduler.schedule] で呼び出す。
 * 変更ごとに既存スケジュールを REPLACE するのでデバウンスとして機能する。
 */
class BackupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val account = GoogleSignIn.getLastSignedInAccount(applicationContext)?.account
            ?: return Result.success() // 未サインイン → スキップ

        val service = DriveBackupService(applicationContext)
        return if (service.backup(account).isSuccess) Result.success()
        else Result.retry()
    }
}

/** データ変更時に呼び出して、一定時間後に自動バックアップをスケジュールする。 */
object BackupScheduler {
    private const val WORK_NAME = "auto_backup"
    private const val DELAY_MINUTES = 3L

    fun schedule(context: Context) {
        val request = OneTimeWorkRequestBuilder<BackupWorker>()
            .setInitialDelay(DELAY_MINUTES, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }
}
