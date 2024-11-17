package com.example.blowscrolling

import java.lang.IllegalArgumentException

open class SensorFilter {
    open fun apply(data: Double, timestamp: Long): Double { return 0.0 }
    open fun initialize(data: Double, timestamp: Long, prevOutput: Double = 0.0) { }
}

class HighPassFilter(private val cutoffFrequency: Double) : SensorFilter() {
    private var prevInput: Double = 0.0
    private var prevOutput: Double = 0.0
    private var prevTimestamp: Long = 0

    override fun initialize(data: Double, timestamp: Long, prevOutput: Double) {
        prevTimestamp = timestamp
        prevInput = data
        this.prevOutput = prevOutput
    }

    private fun calculateAlpha(deltaTime: Double): Double {
        return deltaTime / (deltaTime + (1.0 / (2.0 * Math.PI * cutoffFrequency)))
    }

    override fun apply(data: Double, timestamp: Long): Double {
        val dt = timestamp - prevTimestamp

        if (dt < 0)
            throw IllegalArgumentException("Timestamp is to old")

        // Calculate alpha based on time differential
        val alpha = calculateAlpha(dt.toDouble())

        // Apply first-order infinite impulse response filter formula
        return alpha * (prevOutput + data - prevInput)
    }

    fun update(data: Double, timestamp: Long): Double {
        val dt = timestamp - prevTimestamp

        if(dt < 0)
            throw IllegalArgumentException("Timestamp is to old")

        val alpha = calculateAlpha(dt.toDouble())

        // Apply first-order Infinite Impulse Response Filter
        val output = alpha * (prevOutput + data - prevInput)

        prevInput = data
        prevOutput = output
        prevTimestamp = timestamp

        return output
    }
}