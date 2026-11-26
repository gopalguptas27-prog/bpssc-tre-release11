package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Base64
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Science
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.model.PaperType
import com.example.data.model.RankCategory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipFile

/**
 * Click-to-chat WhatsApp helper using official https://wa.me/<number> links.
 * Never auto-sends messages and never exposes another User's phone number.
 */
object WhatsAppHelper {

    /**
     * Normalizes user-entered phone numbers to E.164 format (default India +91).
     * Returns null if the number is invalid.
     */
    fun normalizePhoneNumber(raw: String?, defaultCountryCode: String = "91"): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isBlank()) return null

        val hasLeadingPlus = trimmed.startsWith("+")
        val digitsOnly = trimmed.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) return null

        return when {
            hasLeadingPlus -> {
                if (digitsOnly.length in 10..15) "+$digitsOnly" else null
            }
            digitsOnly.length == 10 && digitsOnly.first() in '5'..'9' -> {
                "+$defaultCountryCode$digitsOnly"
            }
            digitsOnly.length == 11 && digitsOnly.startsWith("0") && digitsOnly[1] in '5'..'9' -> {
                "+$defaultCountryCode${digitsOnly.substring(1)}"
            }
            digitsOnly.length == 12 && digitsOnly.startsWith(defaultCountryCode) -> {
                "+$digitsOnly"
            }
            digitsOnly.length in 11..15 -> {
                "+$digitsOnly"
            }
            else -> null
        }
    }

    /**
     * Empty input is allowed (WhatsApp number is optional). Non-empty must normalize validly.
     */
    fun isValidOptionalInput(raw: String?): Boolean {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isBlank()) return true
        return normalizePhoneNumber(trimmed) != null
    }

    /**
     * Builds the official click-to-chat URL: https://wa.me/<digits>?text=<encoded>
     */
    fun buildWhatsAppUrl(phoneNumber: String, prefillMessage: String? = null): String? {
        val normalized = normalizePhoneNumber(phoneNumber) ?: return null
        val digits = normalized.removePrefix("+")
        val base = "https://wa.me/$digits"
        val cleanMsg = prefillMessage?.trim().orEmpty()
        return if (cleanMsg.isBlank()) {
            base
        } else {
            val encoded = URLEncoder.encode(cleanMsg, "UTF-8").replace("+", "%20")
            "$base?text=$encoded"
        }
    }

    /**
     * Launches WhatsApp (or browser handoff to WhatsApp Web/App) via ACTION_VIEW.
     * Strictly user-initiated on button click.
     */
    fun openWhatsAppChat(
        context: Context,
        phoneNumber: String?,
        prefillMessage: String? = null
    ): Boolean {
        val url = phoneNumber?.let { buildWhatsAppUrl(it, prefillMessage) } ?: return false
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * Curated Built-in Set Icon Library + Safe Custom Icon Encoder (Admin-only management).
 */
enum class SetIconOption(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val accentHex: Long
) {
    CALCULATOR("CALCULATOR", "Math / Calculator", Icons.Rounded.Calculate, 0xFF0284C7),
    SCIENCE_FLASK("SCIENCE_FLASK", "Science / Lab", Icons.Rounded.Science, 0xFF059669),
    GLOBE("GLOBE", "Geography / Globe", Icons.Rounded.Public, 0xFF0D9488),
    HISTORY_BOOK("HISTORY_BOOK", "History / Book", Icons.Rounded.HistoryEdu, 0xFFD97706),
    LANGUAGE_BOOK("LANGUAGE_BOOK", "Language / Book", Icons.AutoMirrored.Rounded.MenuBook, 0xFF7C3AED),
    GRADUATION_CAP("GRADUATION_CAP", "Exam / Cap", Icons.Rounded.School, 0xFF2563EB),
    NEWSPAPER("NEWSPAPER", "Current Affairs", Icons.Rounded.Newspaper, 0xFFEA580C),
    GENERAL_KNOWLEDGE("GENERAL_KNOWLEDGE", "General Knowledge", Icons.Rounded.Lightbulb, 0xFFEAB308),
    EXAM_PAPER("EXAM_PAPER", "Question Paper", Icons.AutoMirrored.Rounded.Assignment, 0xFF4F46E5),
    TROPHY("TROPHY", "Trophy / Rank", Icons.Rounded.EmojiEvents, 0xFFCA8A04);

    fun accentColor(): Color = Color(accentHex)

    companion object {
        fun defaultForPaperType(paperType: PaperType): SetIconOption = when (paperType) {
            PaperType.LANGUAGE -> LANGUAGE_BOOK
            PaperType.COMMON_GS -> GRADUATION_CAP
            PaperType.COMBINED -> EXAM_PAPER
            PaperType.CLASS_6_8_MATH -> CALCULATOR
            PaperType.CLASS_6_8_SCIENCE -> SCIENCE_FLASK
            PaperType.CLASS_9_10_MATH_80 -> CALCULATOR
            PaperType.CLASS_6_8_MATH_SCIENCE -> SCIENCE_FLASK
            PaperType.CLASS_9_10_MATH -> CALCULATOR
            PaperType.PRACTICE -> GENERAL_KNOWLEDGE
        }

        fun defaultForRankCategory(category: RankCategory): SetIconOption = when (category) {
            RankCategory.LANGUAGE -> LANGUAGE_BOOK
            RankCategory.COMMON -> GRADUATION_CAP
            RankCategory.LANGUAGE_COMMON -> EXAM_PAPER
            RankCategory.MATHEMATICS -> CALCULATOR
            RankCategory.SCIENCE -> SCIENCE_FLASK
            RankCategory.FULL_MATHS_SCIENCE -> SCIENCE_FLASK
            RankCategory.FULL_MATHS -> CALCULATOR
        }

        fun resolve(iconKey: String?, paperType: PaperType): SetIconOption {
            if (!iconKey.isNullOrBlank()) {
                entries.firstOrNull { it.key.equals(iconKey.trim(), ignoreCase = true) }?.let { return it }
            }
            return defaultForPaperType(paperType)
        }

        fun resolveForCategory(iconKey: String?, category: RankCategory): SetIconOption {
            if (!iconKey.isNullOrBlank()) {
                entries.firstOrNull { it.key.equals(iconKey.trim(), ignoreCase = true) }?.let { return it }
            }
            return defaultForRankCategory(category)
        }
    }
}

object SetIconEncoder {
    private val ALLOWED_MIMES = setOf("image/png", "image/jpeg", "image/jpg", "image/webp", "image/gif")
    const val MAX_BASE64_LEN = 240_000

    fun encodeSquareIconFromUri(context: Context, uri: Uri): Result<String> {
        return try {
            val mime = context.contentResolver.getType(uri)?.lowercase(Locale.US)
            if (mime != null && mime !in ALLOWED_MIMES) {
                return Result.failure(IllegalArgumentException("Unsupported image format ($mime). Use GIF, PNG, JPG, or WEBP."))
            }
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return Result.failure(IllegalArgumentException("Unable to read selected image."))
            if (rawBytes.isEmpty()) {
                return Result.failure(IllegalArgumentException("Selected image file is empty."))
            }
            if (rawBytes.size > 8 * 1024 * 1024) {
                return Result.failure(IllegalArgumentException("Image file exceeds 8 MB limit."))
            }

            // Preserve live animated GIF icons
            val isGif = mime == "image/gif" || ImageCodec.hasGifMagicBytes(rawBytes)
            if (isGif) {
                val b64 = "data:image/gif;base64," + Base64.encodeToString(rawBytes, Base64.NO_WRAP)
                if (b64.length > MAX_BASE64_LEN) {
                    val kbLimit = ((MAX_BASE64_LEN - 32) * 3) / 4 / 1024
                    return Result.failure(
                        IllegalArgumentException("GIF icon exceeds ~${kbLimit} KB limit. Please pick a smaller animated GIF icon.")
                    )
                }
                return Result.success(b64)
            }

            val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                ?: return Result.failure(IllegalArgumentException("Invalid or corrupted image file."))

            val side = minOf(decoded.width, decoded.height).coerceAtLeast(1)
            val startX = ((decoded.width - side) / 2).coerceAtLeast(0)
            val startY = ((decoded.height - side) / 2).coerceAtLeast(0)
            val cropped = Bitmap.createBitmap(decoded, startX, startY, side, side)
            val scaled = Bitmap.createScaledBitmap(cropped, 160, 160, true)

            // Preserve alpha transparency for PNG icons
            val isPng = mime == "image/png" || ImageCodec.hasPngMagicBytes(rawBytes)
            if (isPng) {
                val pngOut = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.PNG, 100, pngOut)
                val pngB64 = "data:image/png;base64," + Base64.encodeToString(pngOut.toByteArray(), Base64.NO_WRAP)
                if (pngB64.length <= MAX_BASE64_LEN) {
                    return Result.success(pngB64)
                }
                // Try 120x120 PNG before falling back to JPEG
                val smaller = Bitmap.createScaledBitmap(cropped, 120, 120, true)
                val smallOut = ByteArrayOutputStream()
                smaller.compress(Bitmap.CompressFormat.PNG, 100, smallOut)
                val smallPngB64 = "data:image/png;base64," + Base64.encodeToString(smallOut.toByteArray(), Base64.NO_WRAP)
                if (smallPngB64.length <= MAX_BASE64_LEN) {
                    return Result.success(smallPngB64)
                }
            }

            val out = ByteArrayOutputStream()
            var quality = 85
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            var b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            while (b64.length > MAX_BASE64_LEN && quality > 35) {
                out.reset()
                quality -= 15
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            }
            if (b64.length > MAX_BASE64_LEN) {
                return Result.failure(IllegalArgumentException("Compressed icon still too large. Please pick a simpler image."))
            }
            Result.success(b64)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Could not process set icon: ${e.message}"))
        }
    }
}

/**
 * Compatible Android APK file picker helper.
 *
 * Uses ACTION_GET_CONTENT with a wildcard MIME type and CATEGORY_OPENABLE (and NO restrictive
 * EXTRA_MIME_TYPES filter) so .apk files stored in Internal Storage, Downloads, Documents,
 * SD Card, and third-party file managers are always visible and selectable without broad
 * storage permissions.
 */
object ApkPickerHelper {

    fun createApkPickerIntent(): Intent = Intent(Intent.ACTION_GET_CONTENT).apply {
        type = "*/*"
        addCategory(Intent.CATEGORY_OPENABLE)
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    fun createApkPickerChooser(title: String = "Select Signed .APK File"): Intent =
        Intent.createChooser(createApkPickerIntent(), title)

    fun extractUriFromResult(data: Intent?): Uri? {
        if (data == null) return null
        data.data?.let { return it }
        val clip = data.clipData
        if (clip != null && clip.itemCount > 0) {
            return clip.getItemAt(0)?.uri
        }
        return null
    }
}

/**
 * Inspects a locally selected APK via ACTION_GET_CONTENT to verify .apk extension,
 * AndroidManifest.xml archive structure, Android APK package signature (v1/v2/v3),
 * packageName, versionName, versionCode, file size, and full SHA-256 checksum on-device
 * (Play-policy compliant; does not require REQUEST_INSTALL_PACKAGES).
 */
data class ApkInspectionResult(
    val fileName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val fileSizeBytes: Long,
    val sha256Checksum: String,
    val isSigned: Boolean = false,
    val signingSchemeInfo: String = "",
    val signerCertSha256: String? = null,
    val matchesCurrentPackage: Boolean,
    val isNewerThanInstalled: Boolean,
    val validationError: String? = null
)

object ApkInspector {

    private val APK_SIG_BLOCK_MAGIC = "APK Sig Block 42".toByteArray(Charsets.US_ASCII)

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "Unknown size"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(Locale.US, "%.2f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    fun resolveDisplayName(context: Context, uri: Uri): String {
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val queried = c.getString(idx)?.trim().orEmpty()
                        if (queried.isNotBlank()) return queried
                    }
                }
            }
        } catch (_: Exception) {
        }

        val rawSegment = uri.lastPathSegment?.trim().orEmpty()
        if (rawSegment.isNotBlank()) {
            val decoded = runCatching { URLDecoder.decode(rawSegment, "UTF-8") }.getOrDefault(rawSegment)
            val afterColon = decoded.substringAfterLast(':')
            val filePart = afterColon.substringAfterLast('/').trim()
            if (filePart.isNotBlank()) return filePart
        }
        return ""
    }

    fun inspectPickedApk(
        context: Context,
        uri: Uri,
        currentPackageName: String,
        currentVersionCode: Long
    ): ApkInspectionResult {
        val displayName = resolveDisplayName(context, uri)
        val shownName = displayName.ifBlank { "selected file" }

        if (!displayName.lowercase(Locale.US).endsWith(".apk")) {
            return ApkInspectionResult(
                fileName = shownName,
                packageName = "",
                versionName = "",
                versionCode = 0L,
                fileSizeBytes = 0L,
                sha256Checksum = "",
                isSigned = false,
                matchesCurrentPackage = false,
                isNewerThanInstalled = false,
                validationError = "Invalid file '$shownName'. Please select a valid Android .apk file."
            )
        }

        val tempFile = File(context.cacheDir, "inspect_${System.currentTimeMillis()}.apk")
        return try {
            val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        total += read
                    }
                    total
                }
            } ?: return ApkInspectionResult(
                fileName = displayName,
                packageName = "",
                versionName = "",
                versionCode = 0L,
                fileSizeBytes = 0L,
                sha256Checksum = "",
                isSigned = false,
                matchesCurrentPackage = false,
                isNewerThanInstalled = false,
                validationError = "Unable to read '$displayName'. Grant file access or try selecting from Downloads/Internal Storage."
            )

            if (copied <= 0L) {
                return ApkInspectionResult(
                    fileName = displayName,
                    packageName = "",
                    versionName = "",
                    versionCode = 0L,
                    fileSizeBytes = 0L,
                    sha256Checksum = "",
                    isSigned = false,
                    matchesCurrentPackage = false,
                    isNewerThanInstalled = false,
                    validationError = "Selected file '$displayName' is empty (0 bytes)."
                )
            }

            inspectApkFile(
                context = context,
                apkFile = tempFile,
                displayName = displayName,
                currentPackageName = currentPackageName,
                currentVersionCode = currentVersionCode
            )
        } catch (e: Exception) {
            ApkInspectionResult(
                fileName = displayName,
                packageName = "",
                versionName = "",
                versionCode = 0L,
                fileSizeBytes = 0L,
                sha256Checksum = "",
                isSigned = false,
                matchesCurrentPackage = false,
                isNewerThanInstalled = false,
                validationError = "APK inspection failed for '$displayName': ${e.message ?: "Unknown I/O error"}"
            )
        } finally {
            runCatching { tempFile.delete() }
        }
    }

    /**
     * Validates an on-disk APK file:
     * 1. .apk extension check
     * 2. ZIP header (PK\x03\x04) + SHA-256 checksum + exact file size
     * 3. Valid ZIP archive containing AndroidManifest.xml
     * 4. Android APK package signature (PackageManager signingInfo/signatures + v1/v2/v3 fallback)
     * 5. Package name & versionCode verification against current app
     */
    fun inspectApkFile(
        context: Context,
        apkFile: File,
        displayName: String = apkFile.name,
        currentPackageName: String,
        currentVersionCode: Long
    ): ApkInspectionResult {
        val shownName = displayName.ifBlank { apkFile.name.ifBlank { "selected file" } }
        if (!shownName.lowercase(Locale.US).endsWith(".apk")) {
            return ApkInspectionResult(
                fileName = shownName,
                packageName = "",
                versionName = "",
                versionCode = 0L,
                fileSizeBytes = apkFile.length().coerceAtLeast(0L),
                sha256Checksum = "",
                isSigned = false,
                matchesCurrentPackage = false,
                isNewerThanInstalled = false,
                validationError = "Invalid file '$shownName'. Only .apk Android package files are supported."
            )
        }

        if (!apkFile.exists() || apkFile.length() <= 0L) {
            return ApkInspectionResult(
                fileName = shownName,
                packageName = "",
                versionName = "",
                versionCode = 0L,
                fileSizeBytes = 0L,
                sha256Checksum = "",
                isSigned = false,
                matchesCurrentPackage = false,
                isNewerThanInstalled = false,
                validationError = "Selected file '$shownName' could not be found or is empty."
            )
        }

        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            var totalBytes = 0L
            var zipHeaderValid = false
            FileInputStream(apkFile).use { input ->
                val buffer = ByteArray(16 * 1024)
                var firstChunk = true
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    if (firstChunk) {
                        zipHeaderValid = read >= 4 &&
                            buffer[0] == 0x50.toByte() &&
                            buffer[1] == 0x4B.toByte() &&
                            buffer[2] == 0x03.toByte() &&
                            buffer[3] == 0x04.toByte()
                        firstChunk = false
                    }
                    digest.update(buffer, 0, read)
                    totalBytes += read
                }
            }
            val sha256 = digest.digest().joinToString("") { "%02x".format(it) }

            if (!zipHeaderValid || totalBytes < 64L) {
                return ApkInspectionResult(
                    fileName = shownName,
                    packageName = "",
                    versionName = "",
                    versionCode = 0L,
                    fileSizeBytes = totalBytes,
                    sha256Checksum = sha256,
                    isSigned = false,
                    matchesCurrentPackage = false,
                    isNewerThanInstalled = false,
                    validationError = "Invalid file '$shownName': not a valid Android APK archive (corrupted or renamed non-APK file)."
                )
            }

            // Verify ZIP structure contains AndroidManifest.xml and check v1 JAR signature entries
            var hasAndroidManifest = false
            var hasV1Signature = false
            val zipOpenError = try {
                ZipFile(apkFile).use { zip ->
                    hasAndroidManifest = zip.getEntry("AndroidManifest.xml") != null
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val name = entries.nextElement().name.uppercase(Locale.US)
                        if (name.startsWith("META-INF/") &&
                            (name.endsWith(".RSA") || name.endsWith(".DSA") || name.endsWith(".EC"))
                        ) {
                            hasV1Signature = true
                            break
                        }
                    }
                }
                null
            } catch (zEx: Exception) {
                zEx.message ?: "Corrupted ZIP/APK structure"
            }

            if (zipOpenError != null || !hasAndroidManifest) {
                return ApkInspectionResult(
                    fileName = shownName,
                    packageName = "",
                    versionName = "",
                    versionCode = 0L,
                    fileSizeBytes = totalBytes,
                    sha256Checksum = sha256,
                    isSigned = false,
                    matchesCurrentPackage = false,
                    isNewerThanInstalled = false,
                    validationError = if (zipOpenError != null) {
                        "Corrupted APK archive '$shownName': $zipOpenError"
                    } else {
                        "Invalid APK '$shownName': AndroidManifest.xml is missing. Renamed ZIP/document files are not valid APKs."
                    }
                )
            }

            val hasV2V3Block = hasApkSigningBlockMagic(apkFile)

            val pm = context.packageManager
            val pkgInfo = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageArchiveInfo(
                        apkFile.absolutePath,
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                    ) ?: pm.getPackageArchiveInfo(
                        apkFile.absolutePath,
                        PackageManager.PackageInfoFlags.of(0L)
                    ) ?: run {
                        @Suppress("DEPRECATION")
                        pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
                            ?: pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    @Suppress("DEPRECATION")
                    pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
                        ?: pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNATURES)
                        ?: pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
                }
            } catch (_: Exception) {
                null
            }

            if (pkgInfo == null) {
                return ApkInspectionResult(
                    fileName = shownName,
                    packageName = "",
                    versionName = "",
                    versionCode = 0L,
                    fileSizeBytes = totalBytes,
                    sha256Checksum = sha256,
                    isSigned = hasV1Signature || hasV2V3Block,
                    matchesCurrentPackage = false,
                    isNewerThanInstalled = false,
                    validationError = "Could not parse Android package metadata from '$shownName'. Ensure this is a valid compiled Android APK."
                )
            }

            // Extract signing certificate(s) from PackageManager if present
            val pmCertBytes: ByteArray? = extractFirstSignerCertBytes(pkgInfo)
            val signerCertSha256 = pmCertBytes?.let { bytes ->
                MessageDigest.getInstance("SHA-256")
                    .digest(bytes)
                    .joinToString(":") { "%02X".format(it) }
            }

            val isSigned = pmCertBytes != null || hasV1Signature || hasV2V3Block
            val signingSchemeInfo = when {
                pmCertBytes != null && hasV2V3Block && hasV1Signature -> "Verified Signed APK (v1 + v2/v3)"
                pmCertBytes != null && hasV2V3Block -> "Verified Signed APK (v2/v3)"
                pmCertBytes != null -> "Verified Signed APK (Certificate Verified)"
                hasV2V3Block && hasV1Signature -> "Signed APK (v1 + v2/v3 Block)"
                hasV2V3Block -> "Signed APK (v2/v3 Signing Block)"
                hasV1Signature -> "Signed APK (v1 JAR Signature)"
                else -> "Unsigned APK"
            }

            val apkPkg = pkgInfo.packageName.orEmpty()
            val apkVerName = pkgInfo.versionName.orEmpty()
            val apkVerCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.versionCode.toLong()
            }
            val matchesPkg = apkPkg == currentPackageName
            val isNewer = apkVerCode > currentVersionCode

            val err = when {
                !isSigned -> "Unsigned APK: '$shownName' has no valid Android package signature. Please select a signed release .apk."
                !matchesPkg -> "Package mismatch: APK package is '$apkPkg', expected '$currentPackageName'."
                !isNewer -> "Version code ($apkVerCode) must be greater than installed version code ($currentVersionCode)."
                else -> null
            }

            ApkInspectionResult(
                fileName = shownName,
                packageName = apkPkg,
                versionName = apkVerName,
                versionCode = apkVerCode,
                fileSizeBytes = totalBytes,
                sha256Checksum = sha256,
                isSigned = isSigned,
                signingSchemeInfo = signingSchemeInfo,
                signerCertSha256 = signerCertSha256,
                matchesCurrentPackage = matchesPkg,
                isNewerThanInstalled = isNewer,
                validationError = err
            )
        } catch (e: Exception) {
            ApkInspectionResult(
                fileName = shownName,
                packageName = "",
                versionName = "",
                versionCode = 0L,
                fileSizeBytes = apkFile.length().coerceAtLeast(0L),
                sha256Checksum = "",
                isSigned = false,
                matchesCurrentPackage = false,
                isNewerThanInstalled = false,
                validationError = "APK inspection failed for '$shownName': ${e.message ?: "Unexpected error"}"
            )
        }
    }

    private fun extractFirstSignerCertBytes(pkgInfo: android.content.pm.PackageInfo): ByteArray? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pkgInfo.signingInfo
                val sigs = when {
                    info == null -> null
                    info.hasMultipleSigners() -> info.apkContentsSigners
                    else -> info.signingCertificateHistory
                }
                sigs?.firstOrNull()?.toByteArray()
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.signatures?.firstOrNull()?.toByteArray()
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Checks for the official Android APK Signing Scheme v2/v3/v4 magic ("APK Sig Block 42")
     * immediately preceding the ZIP Central Directory.
     */
    internal fun hasApkSigningBlockMagic(file: File): Boolean {
        return try {
            val len = file.length()
            if (len < 64L) return false
            RandomAccessFile(file, "r").use { raf ->
                // Locate End of Central Directory (EOCD) signature 0x06054b50 in last 64KB
                val searchWindow = minOf(len, 65_557L).toInt()
                val tailStart = len - searchWindow
                val tail = ByteArray(searchWindow)
                raf.seek(tailStart)
                raf.readFully(tail)

                var eocdOffset = -1
                for (i in searchWindow - 22 downTo 0) {
                    if (tail[i] == 0x50.toByte() &&
                        tail[i + 1] == 0x4B.toByte() &&
                        tail[i + 2] == 0x05.toByte() &&
                        tail[i + 3] == 0x06.toByte()
                    ) {
                        eocdOffset = i
                        break
                    }
                }
                if (eocdOffset < 0) return false

                // Central Directory offset is a 4-byte little-endian uint at EOCD + 16
                val cdOffset = ((tail[eocdOffset + 16].toInt() and 0xFF).toLong()) or
                    ((tail[eocdOffset + 17].toInt() and 0xFF).toLong() shl 8) or
                    ((tail[eocdOffset + 18].toInt() and 0xFF).toLong() shl 16) or
                    ((tail[eocdOffset + 19].toInt() and 0xFF).toLong() shl 24)

                if (cdOffset < 32L || cdOffset > len) return false

                val magicBuf = ByteArray(16)
                raf.seek(cdOffset - 16L)
                raf.readFully(magicBuf)
                magicBuf.contentEquals(APK_SIG_BLOCK_MAGIC)
            }
        } catch (_: Exception) {
            false
        }
    }
}
