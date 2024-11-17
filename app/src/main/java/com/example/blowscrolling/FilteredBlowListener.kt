package com.example.blowscrolling

import android.graphics.PointF
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

class FilteredBlowListener: BlowEventListener() {
    private var initializedFilter = false
    private val filter: HighPassFilter = HighPassFilter(1.0 / 300.0)//HighPassFilter(0.0000236)

    override val anomalyThreshold: Double = .20 * barScalar

    private val resetInterval = 100000
    private var sampleCount = 0

    fun resetFilter() {
        initializedFilter = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            if(event.sensor.type == Sensor.TYPE_PRESSURE) {
                val doubleVal = event.values[0].toDouble() * barScalar
                if(!initializedFilter || sampleCount == resetInterval) {
                    filter.initialize(doubleVal, event.timestamp, 0.05 * barScalar)
                    initializedFilter = true
                    sampleCount = 1
                    return
                }

                val floatTS = event.timestamp.toFloat()
                val ts = event.timestamp
                val newValue = abs(filter.apply(doubleVal, ts))


                if(ts - blowTimestamp > blowWindowNS && !blowData.isEmpty()) {
                    if(blowData.size > 2) {
                        triggerOnReleaseEvent()
                    }
                    blowData.clear()
                }

                if(newValue >= anomalyThreshold) {
                    blowData.addLast(PointF(floatTS, newValue.toFloat()))
                    if(blowData.size == 3) {
                        triggerOnBlowEvent()
                    }
                    blowTimestamp = event.timestamp
                }
                else {
                    filter.update(doubleVal, ts)
                    sampleCount++
                }
            }
        }
    }

    /*
    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            if(event.sensor.type == Sensor.TYPE_PRESSURE) {
                if(!initializedFilter) {
                    filter.initializeFilter(event.values[0] * barScalar, event.timestamp)
                    initializedFilter = true
                    return
                }

                val floatTS = event.timestamp.toFloat()
                val ts = event.timestamp
                val newValue = abs(filter.apply(event.values[0] * barScalar, ts))
                Log.d("BLOWLISTENER", "Filtered: ${newValue}")


                if(ts - blowTimestamp > blowWindowNS && !blowData.isEmpty()) {
                    if(blowData.size > 2) {
                        Log.d("BLOWLISTENER", "TRIGERING ONRELEASE")
                        triggerOnReleaseEvent()
                    }
                    blowData.clear()
                }

                val std = getSTD()
                if(std != 0f && count > 200) {
                    val zScore = abs((newValue - mean) / std)
                    //Log.d("BLOWLISTENER", "Z SCORE: ${zScore}")
                    if(zScore >= zScoreThreshold) {
                        blowData.addLast(PointF(floatTS, zScore))
                        if(blowData.size == 3) {
                            triggerOnBlowEvent()
                        }
                        blowTimestamp = event.timestamp
                    }
                    else
                        updateSTD(newValue)
                }
                else
                    updateSTD(newValue)
                /*
                val slope = (newValue - lastBarometerValue) / (floatTS - lastBarometerTimeStamp)

                if(abs(slope) > anomalyThreshold) {
                    blowData.addLast(PointF(floatTS, slope))
                    if(blowData.size == 3) {
                        Log.d("BLOWLISTENER", "TRIGERING ONBLOW")
                        triggerOnBlowEvent()
                    }
                    blowTimestamp = event.timestamp
                }
                */

                lastBarometerValue = newValue
                lastBarometerTimeStamp = floatTS
            }
        }
    }
    */
}