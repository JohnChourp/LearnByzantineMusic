package com.johnchourp.learnbyzantinemusic.analysis

data class BinaryImage(
    val width: Int,
    val height: Int,
    val data: BooleanArray
) {
    init {
        require(data.size == width * height) {
            "BinaryImage data size mismatch. Expected ${width * height}, got ${data.size}."
        }
    }

    fun get(x: Int, y: Int): Boolean = data[y * width + x]

    fun set(x: Int, y: Int, value: Boolean) {
        data[y * width + x] = value
    }
}
