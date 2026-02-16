package com.johnchourp.learnbyzantinemusic.analysis

import android.graphics.Bitmap
import android.graphics.Rect
import kotlin.math.max

object BinaryImageOps {
    fun estimateThreshold(bitmap: Bitmap): Int {
        val width = bitmap.width
        val height = bitmap.height
        val total = width * height
        if (total == 0) {
            return 128
        }
        val pixels = IntArray(total)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        var sum = 0L
        for (pixel in pixels) {
            sum += luminance(pixel)
        }
        val mean = (sum / total).toInt()
        return (mean - 20).coerceIn(40, 210)
    }

    fun estimateAdaptiveThreshold(bitmap: Bitmap): Int {
        val width = bitmap.width
        val height = bitmap.height
        val total = width * height
        if (total == 0) {
            return 128
        }

        val pixels = IntArray(total)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val histogram = IntArray(256)
        var sum = 0L
        for (pixel in pixels) {
            val lum = luminance(pixel)
            histogram[lum] += 1
            sum += lum
        }

        val mean = (sum / total).toInt()
        val otsu = estimateOtsuThreshold(histogram, total)
        return ((otsu + mean) / 2).coerceIn(35, 220)
    }

    fun bitmapToBinary(bitmap: Bitmap, threshold: Int): BinaryImage {
        val width = bitmap.width
        val height = bitmap.height
        val total = width * height
        val pixels = IntArray(total)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val binary = BooleanArray(total)
        for (index in pixels.indices) {
            val lum = luminance(pixels[index])
            binary[index] = lum <= threshold
        }
        return BinaryImage(width, height, binary)
    }

    fun denoise(binary: BinaryImage): BinaryImage {
        val out = binary.copyData()
        val width = binary.width
        val height = binary.height
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                if (!binary.get(x, y)) {
                    continue
                }
                var neighbors = 0
                for (ny in y - 1..y + 1) {
                    for (nx in x - 1..x + 1) {
                        if (nx == x && ny == y) {
                            continue
                        }
                        if (binary.get(nx, ny)) {
                            neighbors += 1
                        }
                    }
                }
                if (neighbors <= 1) {
                    out.set(x, y, false)
                }
            }
        }
        return out
    }

    fun applyMorphology(binary: BinaryImage): BinaryImage {
        val closed = dilate(binary)
        val closedEroded = erode(closed)
        val opened = erode(closedEroded)
        return dilate(opened)
    }

    fun detectFirstLineRect(binary: BinaryImage): Rect {
        val rowCounts = IntArray(binary.height)
        for (y in 0 until binary.height) {
            var count = 0
            for (x in 0 until binary.width) {
                if (binary.get(x, y)) {
                    count += 1
                }
            }
            rowCounts[y] = count
        }

        val rowThreshold = max(3, binary.width / 90)
        val runs = mutableListOf<IntRange>()
        var start = -1
        for (index in rowCounts.indices) {
            if (rowCounts[index] >= rowThreshold) {
                if (start < 0) {
                    start = index
                }
            } else if (start >= 0) {
                runs.add(start..(index - 1))
                start = -1
            }
        }
        if (start >= 0) {
            runs.add(start..(rowCounts.lastIndex))
        }

        val firstRun = runs.firstOrNull { (it.last - it.first + 1) >= 6 }
            ?: return Rect(0, 0, binary.width, binary.height)

        val top = (firstRun.first - 8).coerceAtLeast(0)
        val bottom = (firstRun.last + 8).coerceAtMost(binary.height - 1)

        var minX = binary.width
        var maxX = -1
        for (y in top..bottom) {
            for (x in 0 until binary.width) {
                if (binary.get(x, y)) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                }
            }
        }

        if (maxX < minX) {
            return Rect(0, top, binary.width, bottom + 1)
        }
        val left = (minX - 8).coerceAtLeast(0)
        val right = (maxX + 9).coerceAtMost(binary.width)
        return Rect(left, top, right, bottom + 1)
    }

    fun crop(binary: BinaryImage, rect: Rect): BinaryImage {
        val out = BooleanArray(rect.width() * rect.height())
        var outIndex = 0
        for (y in rect.top until rect.bottom) {
            for (x in rect.left until rect.right) {
                out[outIndex] = binary.get(x, y)
                outIndex += 1
            }
        }
        return BinaryImage(rect.width(), rect.height(), out)
    }

    fun detectConnectedComponents(binary: BinaryImage, minArea: Int): List<Rect> {
        if (binary.width <= 0 || binary.height <= 0) {
            return emptyList()
        }
        val visited = BooleanArray(binary.width * binary.height)
        val components = mutableListOf<Rect>()

        fun index(x: Int, y: Int): Int = y * binary.width + x

        for (y in 0 until binary.height) {
            for (x in 0 until binary.width) {
                if (!binary.get(x, y)) {
                    continue
                }
                val rootIndex = index(x, y)
                if (visited[rootIndex]) {
                    continue
                }
                val queue = ArrayDeque<Pair<Int, Int>>()
                queue.addLast(x to y)
                visited[rootIndex] = true

                var minX = x
                var maxX = x
                var minY = y
                var maxY = y
                var area = 0

                while (queue.isNotEmpty()) {
                    val (cx, cy) = queue.removeFirst()
                    area += 1
                    if (cx < minX) minX = cx
                    if (cx > maxX) maxX = cx
                    if (cy < minY) minY = cy
                    if (cy > maxY) maxY = cy

                    for (ny in cy - 1..cy + 1) {
                        if (ny !in 0 until binary.height) continue
                        for (nx in cx - 1..cx + 1) {
                            if (nx !in 0 until binary.width) continue
                            if (!binary.get(nx, ny)) continue
                            val ni = index(nx, ny)
                            if (visited[ni]) continue
                            visited[ni] = true
                            queue.addLast(nx to ny)
                        }
                    }
                }

                val width = maxX - minX + 1
                val height = maxY - minY + 1
                val bboxArea = width * height
                if (area >= minArea && bboxArea >= minArea) {
                    components.add(Rect(minX, minY, maxX + 1, maxY + 1))
                }
            }
        }

        return components.sortedBy { it.left }
    }

    fun normalize(binary: BinaryImage, targetSize: Int): BinaryImage {
        if (binary.width <= 0 || binary.height <= 0) {
            return BinaryImage(targetSize, targetSize, BooleanArray(targetSize * targetSize))
        }

        val scale = minOf(
            (targetSize - 8).toFloat() / binary.width.toFloat(),
            (targetSize - 8).toFloat() / binary.height.toFloat()
        )
        val scaledWidth = max(1, (binary.width * scale).toInt())
        val scaledHeight = max(1, (binary.height * scale).toInt())

        val out = BinaryImage(targetSize, targetSize, BooleanArray(targetSize * targetSize))
        val offsetX = (targetSize - scaledWidth) / 2
        val offsetY = (targetSize - scaledHeight) / 2

        for (y in 0 until scaledHeight) {
            for (x in 0 until scaledWidth) {
                val srcX = (x.toFloat() / scaledWidth.toFloat() * binary.width).toInt().coerceIn(0, binary.width - 1)
                val srcY = (y.toFloat() / scaledHeight.toFloat() * binary.height).toInt().coerceIn(0, binary.height - 1)
                out.set(offsetX + x, offsetY + y, binary.get(srcX, srcY))
            }
        }

        return out
    }

    fun foregroundF1Score(a: BinaryImage, b: BinaryImage): Float {
        if (a.width != b.width || a.height != b.height) {
            return 0f
        }
        var intersection = 0
        var aCount = 0
        var bCount = 0
        for (i in a.data.indices) {
            val av = a.data[i]
            val bv = b.data[i]
            if (av) aCount += 1
            if (bv) bCount += 1
            if (av && bv) intersection += 1
        }
        if (aCount == 0 || bCount == 0 || intersection == 0) {
            return 0f
        }
        val precision = intersection.toFloat() / aCount.toFloat()
        val recall = intersection.toFloat() / bCount.toFloat()
        return (2f * precision * recall) / (precision + recall)
    }

    private fun dilate(binary: BinaryImage): BinaryImage {
        val out = BinaryImage(binary.width, binary.height, BooleanArray(binary.width * binary.height))
        for (y in 0 until binary.height) {
            for (x in 0 until binary.width) {
                var on = false
                for (ny in y - 1..y + 1) {
                    if (ny !in 0 until binary.height) continue
                    for (nx in x - 1..x + 1) {
                        if (nx !in 0 until binary.width) continue
                        if (binary.get(nx, ny)) {
                            on = true
                            break
                        }
                    }
                    if (on) break
                }
                out.set(x, y, on)
            }
        }
        return out
    }

    private fun erode(binary: BinaryImage): BinaryImage {
        val out = BinaryImage(binary.width, binary.height, BooleanArray(binary.width * binary.height))
        for (y in 0 until binary.height) {
            for (x in 0 until binary.width) {
                var on = true
                for (ny in y - 1..y + 1) {
                    if (ny !in 0 until binary.height) {
                        on = false
                        break
                    }
                    for (nx in x - 1..x + 1) {
                        if (nx !in 0 until binary.width || !binary.get(nx, ny)) {
                            on = false
                            break
                        }
                    }
                    if (!on) break
                }
                out.set(x, y, on)
            }
        }
        return out
    }

    private fun estimateOtsuThreshold(histogram: IntArray, total: Int): Int {
        var sumAll = 0L
        for (i in histogram.indices) {
            sumAll += i.toLong() * histogram[i].toLong()
        }

        var sumBackground = 0L
        var weightBackground = 0
        var bestVariance = -1.0
        var bestThreshold = 128

        for (threshold in histogram.indices) {
            weightBackground += histogram[threshold]
            if (weightBackground == 0) continue

            val weightForeground = total - weightBackground
            if (weightForeground == 0) break

            sumBackground += threshold.toLong() * histogram[threshold].toLong()

            val meanBackground = sumBackground.toDouble() / weightBackground.toDouble()
            val meanForeground = (sumAll - sumBackground).toDouble() / weightForeground.toDouble()
            val diff = meanBackground - meanForeground
            val variance = weightBackground.toDouble() * weightForeground.toDouble() * diff * diff

            if (variance > bestVariance) {
                bestVariance = variance
                bestThreshold = threshold
            }
        }

        return bestThreshold
    }

    private fun BinaryImage.copyData(): BinaryImage =
        BinaryImage(width, height, data.copyOf())

    private fun luminance(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return (0.299f * r + 0.587f * g + 0.114f * b).toInt()
    }
}
