package com.example.credit_score.utils

import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Класс для сравнения лиц с использованием технологии ML Kit от Google
 */
class FaceComparator {
    private val TAG = "FaceComparator"

    // Порог схожести для успешной проверки (85%)
    private val SIMILARITY_THRESHOLD = 0.85f

    // Создаем детектор с высокой точностью для сравнения лиц
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()
    )

    data class ComparisonResult(
        val isMatch: Boolean,
        val similarityScore: Float,
        val errorMessage: String? = null
    )

    /**
     * Сравнивает лицо с паспорта и селфи пользователя
     */
    suspend fun compareFaces(passportFaceBitmap: Bitmap, selfiesBitmap: Bitmap): ComparisonResult {
        return try {
            // Получаем лица с изображений
            val passportFace = getFaceFromImage(passportFaceBitmap)
            val selfieFace = getFaceFromImage(selfiesBitmap)

            if (passportFace == null) {
                return ComparisonResult(false, 0f, "Не удалось обнаружить лицо на паспорте")
            }

            if (selfieFace == null) {
                return ComparisonResult(false, 0f, "Не удалось обнаружить лицо на селфи")
            }

            // Рассчитываем сходство между лицами
            val similarity = calculateFaceSimilarity(passportFace, selfieFace)
            val isMatch = similarity >= SIMILARITY_THRESHOLD

            Log.d(TAG, "Сравнение лиц: уровень схожести = $similarity, порог = $SIMILARITY_THRESHOLD, совпадение = $isMatch")

            ComparisonResult(isMatch, similarity)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при сравнении лиц: ${e.message}")
            ComparisonResult(false, 0f, "Ошибка при сравнении лиц: ${e.message}")
        }
    }

    /**
     * Получает объект Face из изображения
     */
    private suspend fun getFaceFromImage(bitmap: Bitmap): Face? = suspendCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)

        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    Log.w(TAG, "Лица не обнаружены на изображении")
                    continuation.resume(null)
                    return@addOnSuccessListener
                }

                // Если найдено несколько лиц, выбираем самое большое
                val face = faces.maxByOrNull {
                    val bounds = it.boundingBox
                    bounds.width() * bounds.height()
                }

                continuation.resume(face)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Ошибка обработки изображения: ${e.message}")
                continuation.resumeWithException(e)
            }
    }

    /**
     * Вычисляет схожесть между двумя лицами
     * Использует ключевые точки лица для сравнения
     */
    private fun calculateFaceSimilarity(face1: Face, face2: Face): Float {
        // Получаем ключевые точки лица
        val landmarks1 = getLandmarkPoints(face1)
        val landmarks2 = getLandmarkPoints(face2)

        if (landmarks1.isEmpty() || landmarks2.isEmpty()) {
            Log.w(TAG, "Недостаточно ключевых точек для сравнения")
            return 0.5f // Консервативная оценка
        }

        // Нормализуем ключевые точки
        val normalizedPoints1 = normalizePoints(landmarks1, face1.boundingBox)
        val normalizedPoints2 = normalizePoints(landmarks2, face2.boundingBox)

        // Сравниваем расстояния между ключевыми точками
        val distances1 = calculateDistances(normalizedPoints1)
        val distances2 = calculateDistances(normalizedPoints2)

        // Рассчитываем схожесть на основе расстояний
        var similaritySum = 0f
        var count = 0
        for (i in distances1.indices) {
            if (i < distances2.size) {
                val diff = abs(distances1[i] - distances2[i])
                val similarity = 1f - diff.coerceAtMost(1f)
                similaritySum += similarity
                count++
            }
        }

        if (count == 0) {
            Log.w(TAG, "Не удалось сравнить расстояния между точками")
            return 0.5f
        }

        val landmarkSimilarity = similaritySum / count
        Log.d(TAG, "Схожесть по ключевым точкам: $landmarkSimilarity")

        // Дополнительно учитываем наклон головы
        val headRotationSimilarity = compareHeadRotation(face1, face2)
        Log.d(TAG, "Схожесть по наклону головы: $headRotationSimilarity")

        // Комбинируем с весами
        val totalSimilarity = (landmarkSimilarity * 0.8f + headRotationSimilarity * 0.2f)
        return totalSimilarity.coerceIn(0f, 1f)
    }

    /**
     * Получает ключевые точки лица
     */
    private fun getLandmarkPoints(face: Face): List<PointF> {
        val landmarks = mutableListOf<PointF>()
        listOf(
            FaceLandmark.LEFT_EYE,
            FaceLandmark.RIGHT_EYE,
            FaceLandmark.NOSE_BASE,
            FaceLandmark.LEFT_CHEEK,
            FaceLandmark.RIGHT_CHEEK,
            FaceLandmark.MOUTH_LEFT,
            FaceLandmark.MOUTH_RIGHT
        ).forEach { landmarkType ->
            face.getLandmark(landmarkType)?.position?.let { point ->
                landmarks.add(point)
            }
        }
        return landmarks
    }

    /**
     * Нормализует координаты точек относительно границ лица
     */
    private fun normalizePoints(points: List<PointF>, boundingBox: android.graphics.Rect): List<PointF> {
        val width = boundingBox.width().toFloat()
        val height = boundingBox.height().toFloat()
        val left = boundingBox.left.toFloat()
        val top = boundingBox.top.toFloat()

        return points.map { point ->
            PointF(
                (point.x - left) / width,
                (point.y - top) / height
            )
        }
    }

    /**
     * Вычисляет расстояния между всеми парами точек
     */
    private fun calculateDistances(points: List<PointF>): List<Float> {
        val distances = mutableListOf<Float>()
        for (i in points.indices) {
            for (j in i + 1 until points.size) {
                val p1 = points[i]
                val p2 = points[j]
                val distance = sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
                distances.add(distance)
            }
        }
        return distances
    }

    /**
     * Сравнивает наклон головы
     */
    private fun compareHeadRotation(face1: Face, face2: Face): Float {
        val rotX1 = face1.headEulerAngleX
        val rotY1 = face1.headEulerAngleY
        val rotZ1 = face1.headEulerAngleZ

        val rotX2 = face2.headEulerAngleX
        val rotY2 = face2.headEulerAngleY
        val rotZ2 = face2.headEulerAngleZ

        // Вычисляем разницу в углах наклона (нормализуем от 0 до 1)
        val xDiff = 1f - (abs(rotX1 - rotX2) / 45f).coerceAtMost(1f)
        val yDiff = 1f - (abs(rotY1 - rotY2) / 45f).coerceAtMost(1f)
        val zDiff = 1f - (abs(rotZ1 - rotZ2) / 45f).coerceAtMost(1f)

        return (xDiff + yDiff + zDiff) / 3f
    }

    /**
     * Освобождает ресурсы детектора
     */
    fun close() {
        faceDetector.close()
    }
}