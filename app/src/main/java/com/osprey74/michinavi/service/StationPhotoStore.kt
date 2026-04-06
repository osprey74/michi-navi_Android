package com.osprey74.michinavi.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import kotlin.math.max

/**
 * 道の駅フォトアルバムの保存・読み込み・削除を管理するサービス。
 *
 * 写真は `{filesDir}/Albums/{stationId}/photo_{1-3}.jpg` に保存される。
 * ファイルパスが固定のため DB 不要 — ファイル存在チェックのみで管理する。
 */
class StationPhotoStore(private val context: Context) {

    companion object {
        const val MAX_PHOTOS = 3
        private const val MAX_DIMENSION = 1024
        private const val JPEG_QUALITY = 72
    }

    // -- Paths --

    private fun albumDirectory(stationId: String): File =
        File(context.filesDir, "Albums/$stationId")

    private fun photoFile(slot: Int, stationId: String): File =
        File(albumDirectory(stationId), "photo_${slot + 1}.jpg")

    // -- Public API --

    /** 指定 stationId の写真を3スロット分返す（空スロットは null） */
    fun loadPhotos(stationId: String): List<Bitmap?> =
        (0 until MAX_PHOTOS).map { slot ->
            val file = photoFile(slot, stationId)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        }

    /** content:// URI から画像を読み込み、リサイズして指定スロットに保存する */
    fun savePhoto(uri: Uri, slot: Int, stationId: String) {
        val dir = albumDirectory(stationId)
        dir.mkdirs()

        val original = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return

        val resized = resizeBitmap(original)
        photoFile(slot, stationId).outputStream().use { out ->
            resized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        if (resized !== original) resized.recycle()
        original.recycle()
    }

    /** 指定スロットの写真ファイルを削除する */
    fun deletePhoto(slot: Int, stationId: String) {
        photoFile(slot, stationId).delete()
    }

    // -- Resize helper --

    private fun resizeBitmap(bitmap: Bitmap): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= MAX_DIMENSION) return bitmap
        val scale = MAX_DIMENSION.toFloat() / longest
        val newW = (bitmap.width * scale).toInt()
        val newH = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }
}
