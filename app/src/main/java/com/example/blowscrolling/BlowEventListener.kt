package com.example.blowscrolling

import android.graphics.PointF
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.util.Log
import java.lang.Math.pow
import kotlin.math.abs

//data class BlowEvent(val timestamp: Long, val blowData: ArrayDeque<PointF>)

open class BlowEventListener() : SensorEventListener {
    open var blowWindowNS = 450000000L

    protected val barScalar: Double = pow(10.0, 9.0)
    protected open val anomalyThreshold: Double = 1.5000000E-9 * barScalar

    protected open var blowTimestamp = 0L
    protected open var lastBarometerValue: Double = 0.0
    protected open var lastBarometerTimeStamp = 0f

    protected open val blowData = ArrayDeque<PointF>()
    protected open val blowCallbacks = mutableListOf<() -> Unit>()
    protected open val releaseCallbacks = mutableListOf<() -> Unit>()

    fun getBlowData(): List<PointF> {
        return blowData.toList()
    }

    protected fun triggerOnBlowEvent() {
        for(f in blowCallbacks) {
            f()
        }
    }

    fun registerOnBlowCallback(callback: () -> Unit) {
        blowCallbacks.add(callback)
    }

    fun unregisterOnBlowCallback(callback: () -> Unit) {
        var index = -1
        for(i in 0..<blowCallbacks.size) {
            if (blowCallbacks[i] === callback) {
                index = i
                break
            }
        }
        blowCallbacks.removeAt(index)
    }

    protected fun triggerOnReleaseEvent() {
        for(f in releaseCallbacks) {
            f()
        }
    }

    fun registerOnReleaseCallback(callback: () -> Unit) {
        releaseCallbacks.add(callback)
    }

    fun unregisterOnReleaseCallback(callback: () -> Unit) {
        var index = -1
        for(i in 0..<releaseCallbacks.size) {
            if (releaseCallbacks[i] === callback) {
                index = i
                break
            }
        }
        releaseCallbacks.removeAt(index)
    }



    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            if(event.sensor.type == Sensor.TYPE_PRESSURE) {

                val floatTS = event.timestamp.toFloat()
                val ts = event.timestamp
                val newValue = event.values[0].toDouble() * barScalar

                if(ts - blowTimestamp > blowWindowNS && !blowData.isEmpty()) {
                    if(blowData.size > 2) {
                        Log.d("BLOWLISTENER", "TRIGERING ONRELEASE")
                        triggerOnReleaseEvent()
                    }
                    blowData.clear()
                }

                val slope = (newValue - lastBarometerValue) / (floatTS - lastBarometerTimeStamp)

                if(abs(slope) > anomalyThreshold) {
                    blowData.addLast(PointF(floatTS, slope.toFloat()))
                    if(blowData.size == 3) {
                        Log.d("BLOWLISTENER", "TRIGERING ONBLOW")
                        triggerOnBlowEvent()
                    }
                    blowTimestamp = event.timestamp
                }

                lastBarometerValue = newValue
                lastBarometerTimeStamp = floatTS
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}