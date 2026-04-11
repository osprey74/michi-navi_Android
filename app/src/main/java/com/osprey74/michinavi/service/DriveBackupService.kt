package com.osprey74.michinavi.service

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.osprey74.michinavi.model.BackupData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Google Drive REST API を使ったバックアップ / 復元サービス。
 *
 * play-services-auth の GoogleSignIn でサインイン済みの Account を受け取り、
 * GoogleAuthUtil でアクセストークンを取得 → HttpURLConnection で Drive v3 を直接呼び出す。
 * 重い google-api-client ライブラリ不要。
 *
 * バックアップ構成:
 *   Google Drive / MichiNavi / michi-navi-backup.zip
 *     ├── backup.json           (お気に入り・踏破済みID)
 *     ├── Albums/…/photo_*.jpg  (道の駅フォト)
 *     └── CountrySignAlbums/…/photo_*.jpg (CSフォト)
 */
class DriveBackupService(private val context: Context) {

    companion object {
        private const val FOLDER_NAME = "MichiNavi"
        private const val BACKUP_FILE = "michi-navi-backup.zip"
        private const val FILES_URL = "https://www.googleapis.com/drive/v3/files"
        private const val UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
        private const val SCOPE = "oauth2:https://www.googleapis.com/auth/drive.file"
        private const val FOLDER_MIME = "application/vnd.google-apps.folder"
    }

    private val settingsRepo = ServiceLocator.settingsRepository
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    // ==================== Public API ====================

    /** Google Drive にバックアップ */
    suspend fun backup(account: Account): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val token = getAccessToken(account)
            val zip = createBackupZip()
            try {
                val folderId = findOrCreateFolder(token)
                findFileId(token, folderId)?.let { deleteFile(token, it) }
                uploadFile(token, zip, folderId)
            } finally {
                zip.delete()
            }
        }
    }

    /** Google Drive からデータを復元 */
    suspend fun restore(account: Account): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val token = getAccessToken(account)
            val folderId = findFolderId(token)
                ?: error("バックアップが見つかりません")
            val fileId = findFileId(token, folderId)
                ?: error("バックアップファイルが見つかりません")

            val zip = File(context.cacheDir, "restore_tmp.zip")
            try {
                downloadFile(token, fileId, zip)
                restoreFromZip(zip)
            } finally {
                zip.delete()
            }
        }
    }

    /** 最終バックアップ日時（ISO 8601）を返す。バックアップが無ければ null */
    suspend fun getLastBackupTime(account: Account): String? =
        withContext(Dispatchers.IO) {
            try {
                val token = getAccessToken(account)
                val fid = findFolderId(token) ?: return@withContext null
                queryModifiedTime(token, fid)
            } catch (_: Exception) {
                null
            }
        }

    // ==================== Token ====================

    private fun getAccessToken(account: Account): String =
        GoogleAuthUtil.getToken(context, account, SCOPE)

    // ==================== ZIP build / restore ====================

    private suspend fun createBackupZip(): File {
        val tmp = File(context.cacheDir, "backup_tmp.zip")
        val data = settingsRepo.getBackupData()
        val jsonBytes = json.encodeToString(BackupData.serializer(), data).toByteArray()

        ZipOutputStream(BufferedOutputStream(tmp.outputStream())).use { zos ->
            zos.putNextEntry(ZipEntry("backup.json"))
            zos.write(jsonBytes)
            zos.closeEntry()

            packDir(zos, File(context.filesDir, "Albums"), "Albums")
            packDir(zos, File(context.filesDir, "CountrySignAlbums"), "CountrySignAlbums")
        }
        return tmp
    }

    private fun packDir(zos: ZipOutputStream, dir: File, prefix: String) {
        if (!dir.exists()) return
        dir.walkTopDown().filter { it.isFile }.forEach { f ->
            zos.putNextEntry(ZipEntry("$prefix/${f.relativeTo(dir).path}"))
            f.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()
        }
    }

    private suspend fun restoreFromZip(zipFile: File) {
        ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zis ->
            generateSequence { zis.nextEntry }.forEach { entry ->
                if (!entry.isDirectory) when {
                    entry.name == "backup.json" -> {
                        val d = json.decodeFromString(
                            BackupData.serializer(),
                            zis.readBytes().toString(Charsets.UTF_8),
                        )
                        settingsRepo.restoreBackupData(d)
                    }
                    entry.name.startsWith("Albums/") ||
                    entry.name.startsWith("CountrySignAlbums/") -> {
                        val out = File(context.filesDir, entry.name)
                        out.parentFile?.mkdirs()
                        out.outputStream().use { zis.copyTo(it) }
                    }
                }
                zis.closeEntry()
            }
        }
    }

    // ==================== Drive helpers ====================

    private fun findOrCreateFolder(token: String): String =
        findFolderId(token) ?: createFolder(token)

    private fun findFolderId(token: String): String? {
        val q = enc("name='$FOLDER_NAME' and mimeType='$FOLDER_MIME' and trashed=false")
        val body = httpGet("$FILES_URL?q=$q&fields=files(id)", token)
        return parseFiles(body).firstOrNull()?.id
    }

    private fun createFolder(token: String): String {
        val meta = """{"name":"$FOLDER_NAME","mimeType":"$FOLDER_MIME"}"""
        val body = httpPostJson(FILES_URL, token, meta)
        return parseFile(body).id
    }

    private fun findFileId(token: String, folderId: String): String? {
        val q = enc("name='$BACKUP_FILE' and '$folderId' in parents and trashed=false")
        val body = httpGet("$FILES_URL?q=$q&fields=files(id)", token)
        return parseFiles(body).firstOrNull()?.id
    }

    private fun queryModifiedTime(token: String, folderId: String): String? {
        val q = enc("name='$BACKUP_FILE' and '$folderId' in parents and trashed=false")
        val body = httpGet("$FILES_URL?q=$q&fields=files(id,modifiedTime)", token)
        return parseFiles(body).firstOrNull()?.modifiedTime
    }

    private fun deleteFile(token: String, fileId: String) {
        val c = open("$FILES_URL/$fileId", token).apply { requestMethod = "DELETE" }
        c.responseCode
        c.disconnect()
    }

    private fun uploadFile(token: String, file: File, folderId: String) {
        val boundary = "michi_${System.currentTimeMillis()}"
        val meta = """{"name":"$BACKUP_FILE","parents":["$folderId"]}"""

        val c = open("$UPLOAD_URL?uploadType=multipart", token).apply {
            requestMethod = "POST"; doOutput = true
            setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
        }
        c.outputStream.use { os ->
            val header = "--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$meta\r\n--$boundary\r\nContent-Type: application/zip\r\n\r\n"
            os.write(header.toByteArray())
            file.inputStream().use { it.copyTo(os) }
            os.write("\r\n--$boundary--".toByteArray())
        }
        check(c, "Upload"); c.disconnect()
    }

    private fun downloadFile(token: String, fileId: String, dest: File) {
        val c = open("$FILES_URL/$fileId?alt=media", token)
        check(c, "Download")
        c.inputStream.use { inp -> dest.outputStream().use { inp.copyTo(it) } }
        c.disconnect()
    }

    // ==================== HTTP ====================

    private fun open(url: String, token: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            setRequestProperty("Authorization", "Bearer $token")
        }

    private fun httpGet(url: String, token: String): String {
        val c = open(url, token)
        check(c, "GET"); return c.inputStream.bufferedReader().readText().also { c.disconnect() }
    }

    private fun httpPostJson(url: String, token: String, body: String): String {
        val c = open(url, token).apply {
            requestMethod = "POST"; doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }
        c.outputStream.use { it.write(body.toByteArray()) }
        check(c, "POST"); return c.inputStream.bufferedReader().readText().also { c.disconnect() }
    }

    private fun check(c: HttpURLConnection, tag: String) {
        if (c.responseCode !in 200..299) {
            val err = c.errorStream?.bufferedReader()?.readText() ?: ""
            c.disconnect()
            throw IOException("$tag failed (${c.responseCode}): $err")
        }
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    // ==================== JSON models (Drive API response) ====================

    @kotlinx.serialization.Serializable
    private data class FileEntry(val id: String = "", val modifiedTime: String? = null)

    @kotlinx.serialization.Serializable
    private data class FileList(val files: List<FileEntry> = emptyList())

    private fun parseFiles(body: String): List<FileEntry> =
        json.decodeFromString(FileList.serializer(), body).files

    private fun parseFile(body: String): FileEntry =
        json.decodeFromString(FileEntry.serializer(), body)
}
