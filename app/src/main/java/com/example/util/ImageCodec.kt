package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import java.util.Locale

object ImageCodec {
    private val bitmapCache = object : android.util.LruCache<Int, ImageBitmap>(48) {}

    const val DEFAULT_MAX_BANNER_BASE64_LEN = 480_000
    const val MAX_RAW_GIF_UPLOAD_BYTES = 345_000 // ~460K base64 + data-uri prefix <= 480K

    fun isGifBase64(base64: String?): Boolean {
        if (base64.isNullOrBlank()) return false
        val trimmed = base64.trimStart()
        if (trimmed.startsWith("data:image/gif;base64,", ignoreCase = true)) return true
        // Fallback header check for raw unprefixed GIF base64 ("R0lGOD" -> "GIF8")
        return trimmed.startsWith("R0lGOD")
    }

    fun isPngBase64(base64: String?): Boolean {
        if (base64.isNullOrBlank()) return false
        val trimmed = base64.trimStart()
        if (trimmed.startsWith("data:image/png;base64,", ignoreCase = true)) return true
        return trimmed.startsWith("iVBORw0KGgo")
    }

    fun hasGifMagicBytes(bytes: ByteArray): Boolean {
        if (bytes.size < 6) return false
        return bytes[0] == 'G'.code.toByte() &&
            bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() &&
            bytes[3] == '8'.code.toByte() &&
            (bytes[4] == '7'.code.toByte() || bytes[4] == '9'.code.toByte()) &&
            bytes[5] == 'a'.code.toByte()
    }

    fun hasPngMagicBytes(bytes: ByteArray): Boolean {
        if (bytes.size < 8) return false
        return bytes[0] == 0x89.toByte() &&
            bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() &&
            bytes[3] == 0x47.toByte()
    }

    fun decodeToBytes(base64: String?): ByteArray? {
        if (base64.isNullOrBlank()) return null
        return try {
            val clean = base64.substringAfter("base64,", base64)
            Base64.decode(clean, Base64.DEFAULT)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Encodes an image URI (animated GIF, transparent PNG, JPG, or WEBP) into a Firestore-safe
     * Base64 string, returning a descriptive [Result] so callers can display actionable feedback
     * (e.g., if an animated GIF exceeds the cloud document size budget).
     */
    fun encodeUriToBase64Result(
        context: Context,
        uri: Uri,
        maxDimension: Int = 520,
        quality: Int = 68,
        allowGif: Boolean = true,
        maxBase64Len: Int = DEFAULT_MAX_BANNER_BASE64_LEN
    ): Result<String> {
        return try {
            val mime = context.contentResolver.getType(uri)?.lowercase(Locale.US)
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return Result.failure(IllegalArgumentException("Unable to read selected image."))
            if (rawBytes.isEmpty()) {
                return Result.failure(IllegalArgumentException("Selected image file is empty."))
            }
            if (rawBytes.size > 10 * 1024 * 1024) {
                return Result.failure(IllegalArgumentException("Image file exceeds 10 MB limit."))
            }

            // 1. Preserve live animated GIF bytes untouched so animation plays natively
            val isGif = mime == "image/gif" || hasGifMagicBytes(rawBytes)
            if (isGif) {
                if (!allowGif) {
                    return Result.failure(IllegalArgumentException("GIF format is not enabled for this field."))
                }
                val encoded = "data:image/gif;base64," + Base64.encodeToString(rawBytes, Base64.NO_WRAP)
                if (encoded.length > maxBase64Len) {
                    val kbLimit = ((maxBase64Len - 32) * 3) / 4 / 1024
                    val actualKb = (rawBytes.size / 1024).coerceAtLeast(1)
                    return Result.failure(
                        IllegalArgumentException(
                            "Animated GIF is ${actualKb} KB (max ~${kbLimit} KB for fast live cloud sync). Please pick a smaller or compressed GIF."
                        )
                    )
                }
                return Result.success(encoded)
            }

            // 2. Decode bounds for memory-safe downsampling
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
            var sampleSize = 1
            if (bounds.outWidth > 0 && bounds.outHeight > 0) {
                while ((bounds.outWidth / sampleSize) > maxDimension * 2 ||
                    (bounds.outHeight / sampleSize) > maxDimension * 2
                ) {
                    sampleSize *= 2
                }
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val rawBitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
                ?: return Result.failure(IllegalArgumentException("Invalid or corrupted image file."))

            try {
                // 3. Preserve transparent PNG when source is PNG (downscaling if needed to stay under cloud budget)
                val isPng = mime == "image/png" || hasPngMagicBytes(rawBytes)
                if (isPng) {
                    val dimSteps = listOf(maxDimension, (maxDimension * 3) / 4, maxDimension / 2, 240)
                        .distinct()
                        .filter { it >= 120 }
                    for (dim in dimSteps) {
                        val pngEncoded = encodeBitmapToPngBase64(rawBitmap, dim)
                        if (pngEncoded.length <= maxBase64Len) {
                            return Result.success(pngEncoded)
                        }
                    }
                }

                // 4. Fallback / JPEG encoding with iterative quality step-down
                var currentQuality = quality
                var encoded = encodeBitmapToBase64(rawBitmap, maxDimension, currentQuality)
                while (encoded.length > maxBase64Len && currentQuality > 35) {
                    currentQuality -= 10
                    encoded = encodeBitmapToBase64(rawBitmap, maxDimension, currentQuality)
                }
                if (encoded.length > maxBase64Len) {
                    return Result.failure(
                        IllegalArgumentException("Image is too large even after compression. Please pick a simpler image.")
                    )
                }
                Result.success(encoded)
            } finally {
                rawBitmap.recycle()
            }
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Could not process image: ${e.message}"))
        }
    }

    fun encodeUriToBase64(
        context: Context,
        uri: Uri,
        maxDimension: Int = 520,
        quality: Int = 68,
        allowGif: Boolean = true,
        maxBase64Len: Int = DEFAULT_MAX_BANNER_BASE64_LEN
    ): String? {
        return encodeUriToBase64Result(
            context = context,
            uri = uri,
            maxDimension = maxDimension,
            quality = quality,
            allowGif = allowGif,
            maxBase64Len = maxBase64Len
        ).getOrNull()
    }

    fun encodeBitmapToPngBase64(bitmap: Bitmap, maxDimension: Int = 520): String {
        val scaled = scaleDown(bitmap, maxDimension)
        return try {
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
            "data:image/png;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } finally {
            if (scaled !== bitmap) scaled.recycle()
        }
    }

    fun encodeBitmapToBase64(bitmap: Bitmap, maxDimension: Int = 520, quality: Int = 68): String {
        val scaled = scaleDown(bitmap, maxDimension)
        return try {
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            val bytes = out.toByteArray()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } finally {
            if (scaled !== bitmap) scaled.recycle()
        }
    }

    fun rotateBase64(base64: String, degrees: Float = 90f): String {
        val bmp = decodeToBitmap(base64) ?: return base64
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        return try {
            if (isPngBase64(base64)) encodeBitmapToPngBase64(rotated)
            else encodeBitmapToBase64(rotated)
        } finally {
            if (rotated !== bmp) rotated.recycle()
            bmp.recycle()
        }
    }

    fun decodeToBitmap(base64: String?): Bitmap? {
        if (base64.isNullOrBlank()) return null
        return try {
            val bytes = decodeToBytes(base64) ?: return null
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    fun decodeToImageBitmap(base64: String?): ImageBitmap? {
        if (base64.isNullOrBlank()) return null
        val key = base64.hashCode()
        bitmapCache.get(key)?.let { return it }
        val bmp = decodeToBitmap(base64) ?: return null
        val img = bmp.asImageBitmap()
        bitmapCache.put(key, img)
        return img
    }

    private fun scaleDown(src: Bitmap, maxDimension: Int): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= maxDimension && h <= maxDimension) return src
        val ratio = maxDimension.toFloat() / maxOf(w, h).toFloat()
        val newW = (w * ratio).toInt().coerceAtLeast(1)
        val newH = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, newW, newH, true)
    }
}
