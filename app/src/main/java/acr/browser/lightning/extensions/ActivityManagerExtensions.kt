package acr.browser.lightning.extensions

import android.app.ActivityManager

/**
 * The device's total RAM in Gigabytes.
 */
fun ActivityManager.totalMemory(): Long = ActivityManager.MemoryInfo().let { memoryInfo ->
    getMemoryInfo(memoryInfo)
    (memoryInfo.totalMem / 1000000000)
}
