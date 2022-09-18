/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.settings.fragment

import acr.browser.lightning.AppTheme
import acr.browser.lightning.R
import acr.browser.lightning.browser.di.injector
import acr.browser.lightning.extensions.resizeAndShow
import acr.browser.lightning.extensions.withSingleChoiceItems
import acr.browser.lightning.preference.UserPreferences
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import javax.inject.Inject

class DisplaySettingsFragment : AbstractSettingsFragment() {

    @Inject internal lateinit var userPreferences: UserPreferences

    override fun providePreferencesXmlResource() = R.xml.preference_display

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        injector.inject(this)

        // preferences storage
        clickableDynamicPreference(
            preference = SETTINGS_THEME,
            summary = userPreferences.useTheme.toDisplayString(),
            onClick = ::showThemePicker
        )

        clickablePreference(
            preference = SETTINGS_TEXTSIZE,
            onClick = ::showTextSizePicker
        )

        checkBoxPreference(
            preference = SETTINGS_HIDESTATUSBAR,
            isChecked = userPreferences.hideStatusBarEnabled,
            onCheckChange = { userPreferences.hideStatusBarEnabled = it }
        )

        checkBoxPreference(
            preference = SETTINGS_FULLSCREEN,
            isChecked = userPreferences.fullScreenEnabled,
            onCheckChange = { userPreferences.fullScreenEnabled = it }
        )

        checkBoxPreference(
            preference = SETTINGS_VIEWPORT,
            isChecked = userPreferences.useWideViewPortEnabled,
            onCheckChange = { userPreferences.useWideViewPortEnabled = it }
        )

        checkBoxPreference(
            preference = SETTINGS_OVERVIEWMODE,
            isChecked = userPreferences.overviewModeEnabled,
            onCheckChange = { userPreferences.overviewModeEnabled = it }
        )

        checkBoxPreference(
            preference = SETTINGS_REFLOW,
            isChecked = userPreferences.textReflowEnabled,
            onCheckChange = { userPreferences.textReflowEnabled = it }
        )

        checkBoxPreference(
            preference = SETTINGS_BLACK_STATUS,
            isChecked = userPreferences.useBlackStatusBar,
            onCheckChange = { userPreferences.useBlackStatusBar = it }
        )

        checkBoxPreference(
            preference = SETTINGS_DRAWERTABS,
            isChecked = userPreferences.showTabsInDrawer,
            onCheckChange = { userPreferences.showTabsInDrawer = it }
        )

        checkBoxPreference(
            preference = SETTINGS_SWAPTABS,
            isChecked = userPreferences.bookmarksAndTabsSwapped,
            onCheckChange = { userPreferences.bookmarksAndTabsSwapped = it }
        )
    }

    private fun showTextSizePicker() {
        val maxValue = 5
        AlertDialog.Builder(activity).apply {
            val layoutInflater = activity.layoutInflater
            val customView =
                (layoutInflater.inflate(R.layout.dialog_seek_bar, null) as LinearLayout).apply {
                    val text = TextView(activity).apply {
                        setText(R.string.untitled)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.WRAP_CONTENT
                        )
                        gravity = Gravity.CENTER_HORIZONTAL
                    }
                    addView(text)
                    findViewById<SeekBar>(R.id.text_size_seekbar).apply {
                        setOnSeekBarChangeListener(TextSeekBarListener(text))
                        max = maxValue
                        progress = maxValue - userPreferences.textSize
                    }
                }
            setView(customView)
            setTitle(R.string.title_text_size)
            setPositiveButton(android.R.string.ok) { _, _ ->
                val seekBar = customView.findViewById<SeekBar>(R.id.text_size_seekbar)
                userPreferences.textSize = maxValue - seekBar.progress
            }
        }.resizeAndShow()
    }

    private fun showThemePicker(summaryUpdater: SummaryUpdater) {
        val currentTheme = userPreferences.useTheme
        AlertDialog.Builder(activity).apply {
            setTitle(resources.getString(R.string.theme))
            val values = AppTheme.values().map { Pair(it, it.toDisplayString()) }
            withSingleChoiceItems(values, userPreferences.useTheme) {
                userPreferences.useTheme = it
                summaryUpdater.updateSummary(it.toDisplayString())
            }
            setPositiveButton(resources.getString(R.string.action_ok)) { _, _ ->
                if (currentTheme != userPreferences.useTheme) {
                    activity.onBackPressed()
                }
            }
            setOnCancelListener {
                if (currentTheme != userPreferences.useTheme) {
                    activity.onBackPressed()
                }
            }
        }.resizeAndShow()
    }

    private fun AppTheme.toDisplayString(): String = getString(
        when (this) {
            AppTheme.LIGHT -> R.string.light_theme
            AppTheme.DARK -> R.string.dark_theme
            AppTheme.BLACK -> R.string.black_theme
        }
    )

    private class TextSeekBarListener(
        private val sampleText: TextView
    ) : SeekBar.OnSeekBarChangeListener {

        override fun onProgressChanged(view: SeekBar, size: Int, user: Boolean) {
            this.sampleText.textSize = getTextSize(size)
        }

        override fun onStartTrackingTouch(arg0: SeekBar) {}

        override fun onStopTrackingTouch(arg0: SeekBar) {}

    }

    companion object {

        private const val SETTINGS_HIDESTATUSBAR = "fullScreenOption"
        private const val SETTINGS_FULLSCREEN = "fullscreen"
        private const val SETTINGS_VIEWPORT = "wideViewPort"
        private const val SETTINGS_OVERVIEWMODE = "overViewMode"
        private const val SETTINGS_REFLOW = "text_reflow"
        private const val SETTINGS_THEME = "app_theme"
        private const val SETTINGS_TEXTSIZE = "text_size"
        private const val SETTINGS_DRAWERTABS = "cb_drawertabs"
        private const val SETTINGS_SWAPTABS = "cb_swapdrawers"
        private const val SETTINGS_BLACK_STATUS = "black_status_bar"

        private const val XX_LARGE = 30.0f
        private const val X_LARGE = 26.0f
        private const val LARGE = 22.0f
        private const val MEDIUM = 18.0f
        private const val SMALL = 14.0f
        private const val X_SMALL = 10.0f

        private fun getTextSize(size: Int): Float = when (size) {
            0 -> X_SMALL
            1 -> SMALL
            2 -> MEDIUM
            3 -> LARGE
            4 -> X_LARGE
            5 -> XX_LARGE
            else -> MEDIUM
        }
    }
}
