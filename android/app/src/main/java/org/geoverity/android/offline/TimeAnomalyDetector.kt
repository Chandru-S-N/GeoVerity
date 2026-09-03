package org.geoverity.android.offline

import kotlin.math.abs

sealed class TimeReconciliationResult {
    data class Valid(val reconciledTimestamp: Long, val deviationMs: Long) : TimeReconciliationResult()
    data class Anomaly(val deviationMs: Long, val reason: String) : TimeReconciliationResult()
}

object TimeAnomalyDetector {

    const val DEFAULT_MAX_DEVIATION_MS = 120_000L // 2 minutes (120 seconds)

    /**
     * Reconciles offline capture device time against monotonic elapsed realtime.
     */
    fun reconcile(
        lastTrustedServerTimestamp: Long,
        lastTrustedElapsedRealtime: Long,
        captureElapsedRealtime: Long,
        deviceCaptureTime: Long,
        maxDeviationMs: Long = DEFAULT_MAX_DEVIATION_MS
    ): TimeReconciliationResult {

        if (captureElapsedRealtime < lastTrustedElapsedRealtime) {
            return TimeReconciliationResult.Anomaly(
                deviationMs = -1L,
                reason = "Monotonic elapsed time regression detected. Device may have rebooted or manipulated monotonic timer."
            )
        }

        val monotonicDelta = captureElapsedRealtime - lastTrustedElapsedRealtime
        val expectedDeviceTime = lastTrustedServerTimestamp + monotonicDelta
        val deviation = abs(deviceCaptureTime - expectedDeviceTime)

        return if (deviation > maxDeviationMs) {
            TimeReconciliationResult.Anomaly(
                deviationMs = deviation,
                reason = "Time deviation of ${deviation}ms exceeds maximum allowed threshold of ${maxDeviationMs}ms. Clock tampering suspected."
            )
        } else {
            TimeReconciliationResult.Valid(
                reconciledTimestamp = expectedDeviceTime,
                deviationMs = deviation
            )
        }
    }
}
