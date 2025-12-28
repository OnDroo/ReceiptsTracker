package link.ondrov.receipts.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object FileUtils {
    fun copyUriToInternalStorage(context: Context, uri: Uri): File? {
        return try {
            val contentResolver = context.contentResolver
            val uniqueId = UUID.randomUUID().toString()
            val fileName = "$uniqueId.jpg"
            val outputFile = File(context.filesDir, fileName)

            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}