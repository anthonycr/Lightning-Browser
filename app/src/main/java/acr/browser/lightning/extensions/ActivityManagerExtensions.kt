package acr.browser.lightning.extensions

import android.app.ActivityManager

/**
 * The device's total RAM in Gigabytes.
 */
fun ActivityManager.totalMemory(): Long = ActivityManager.MemoryInfo().let { memoryInfo ->
    getMemoryInfo(memoryInfo)
    (memoryInfo.totalMem / 1000000000)
}

/**
 * Defaults to one tab per GB of total RAM, with a floor of at least 4 active tabs.
 */
fun ActivityManager.activeTabLimit(): Int = totalMemory().coerceAtLeast(4).toInt()
