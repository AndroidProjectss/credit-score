package com.example.credit_score.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Класс для извлечения лица с изображения паспорта
 */
class FaceExtractor {
    private val TAG = "FaceExtractor"

    // Создаем детектор лиц с высокой точностью
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.05f) // Уменьшаем минимальный размер лица
            .build()
    )

    /**
     * Извлекает лицо с изображения паспорта
     * Всегда возвращает Bitmap (либо обрезанное лицо, либо исходное изображение)
     * Сохраняет результат в /sdcard/Download/passport_face_debug.jpg для отладки
     */
    suspend fun extractFaceFromPassport(passportBitmap: Bitmap): Bitmap = suspendCoroutine { continuation ->
        // Логируем размер входного изображения
        Log.d(TAG, "Размер входного изображения: ${passportBitmap.width}x${passportBitmap.height}")

        // Поворачиваем изображение, если оно вертикальное
        val rotatedBitmap = if (passportBitmap.height > passportBitmap.width) {
            val matrix = Matrix()
            matrix.postRotate(90f)
            Bitmap.createBitmap(passportBitmap, 0, 0, passportBitmap.width, passportBitmap.height, matrix, true)
        } else {
            passportBitmap
        }

        // Создаем InputImage из Bitmap
        val image = InputImage.fromBitmap(rotatedBitmap, 0)

        // Запускаем процесс обнаружения лица
        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                try {
                    val resultBitmap: Bitmap
                    if (faces.isEmpty()) {
                        Log.w(TAG, "Лицо не обнаружено на паспорте, возвращаем исходное изображение")
                        // Если лицо не найдено, возвращаем исходное изображение
                        resultBitmap = rotatedBitmap
                    } else {
                        // Если лиц несколько, выбираем самое большое
                        val face = faces.maxByOrNull {
                            val bounds = it.boundingBox
                            bounds.width() * bounds.height()
                        }

                        if (face != null) {
                            // Обрезаем лицо
                            resultBitmap = cropFace(rotatedBitmap, face)
                            Log.d(TAG, "Извлечено лицо: ${resultBitmap.width}x${resultBitmap.height}")
                        } else {
                            Log.w(TAG, "Не удалось выбрать лицо из обнаруженных, возвращаем исходное изображение")
                            resultBitmap = rotatedBitmap
                        }
                    }

                    // Сохраняем результат для отладки
                    val debugFile = File("/sdcard/Download/passport_face_debug.jpg")
                    debugFile.outputStream().use { out ->
                        resultBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    }
                    Log.d(TAG, "Сохранено отладочное изображение: ${debugFile.absolutePath}")

                    // Возвращаем результат
                    continuation.resume(resultBitmap)
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при обработке изображения: ${e.message}")
                    continuation.resumeWithException(e)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Ошибка при обнаружении лица: ${e.message}")
                // В случае ошибки возвращаем исходное изображение
                try {
                    val debugFile = File("/sdcard/Download/passport_face_debug.jpg")
                    debugFile.outputStream().use { out ->
                        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    }
                    Log.d(TAG, "Сохранено отладочное изображение (при ошибке): ${debugFile.absolutePath}")
                    continuation.resume(rotatedBitmap)
                } catch (saveError: Exception) {
                    Log.e(TAG, "Ошибка при сохранении отладочного изображения: ${saveError.message}")
                    continuation.resumeWithException(e)
                }
            }
    }

    /**
     * Обрезает лицо из изображения паспорта на основе обнаруженного Face
     */
    private fun cropFace(originalBitmap: Bitmap, face: Face): Bitmap {
        // Получаем ограничивающий прямоугольник лица
        val bounds = face.boundingBox

        // Добавляем дополнительные отступы для захвата всего лица
        val extraMarginPercent = 0.5f //  50% для надежности

        val extraMarginX = (bounds.width() * extraMarginPercent).toInt()
        val extraMarginY = (bounds.height() * extraMarginPercent).toInt()

        val left = (bounds.left - extraMarginX).coerceAtLeast(0)
        val top = (bounds.top - extraMarginY).coerceAtLeast(0)
        val right = (bounds.right + extraMarginX).coerceAtMost(originalBitmap.width)
        val bottom = (bounds.bottom + extraMarginY).coerceAtMost(originalBitmap.height)

        val width = right - left
        val height = bottom - top

        if (width <= 0 || height <= 0) {
            Log.e(TAG, "Недопустимые размеры лица: $width x $height")
            throw IllegalStateException("Недопустимые размеры лица: $width x $height")
        }

        Log.d(TAG, "Обрезка лица: left=$left, top=$top, width=$width, height=$height")
        return Bitmap.createBitmap(originalBitmap, left, top, width, height)
    }

    /**
     * Очищает ресурсы детектора лиц
     */
    fun close() {
        faceDetector.close()
    }
}