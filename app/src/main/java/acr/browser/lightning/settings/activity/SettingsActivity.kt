/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.settings.activity

import acr.browser.lightning.R
import acr.browser.lightning.browser.di.injector
import acr.browser.lightning.device.BuildInfo
import acr.browser.lightning.device.BuildType
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import javax.inject.Inject

class SettingsActivity : ThemableSettingsActivity() {

    @Inject lateinit var buildInfo: BuildInfo

    @Deprecated("Deprecated in Java")
    override fun onCreate(savedInstanceState: Bundle?) {
        injector.inject(this)
        super.onCreate(savedInstanceState)
        // this is a workaround for the Toolbar in PreferenceActivity
        val root = findViewById<ViewGroup>(android.R.id.content)
        val content = root.getChildAt(0) as LinearLayout
        val toolbarContainer = View.inflate(this, R.layout.toolbar_settings, null) as LinearLayout

        root.removeAllViews()
        toolbarContainer.addView(content)
        root.addView(toolbarContainer)

        // now we can set the Toolbar using AppCompatPreferenceActivity
        setSupportActionbar(toolbarContainer.findViewById(R.id.toolbar))
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)
    }

    @Deprecated("Deprecated in Java")
    override fun onBuildHeaders(target: MutableList<Header>) {
        loadHeadersFromResource(R.xml.preferences_headers, target)
        fragments.clear()

        if (buildInfo.buildType == BuildType.RELEASE) {
            target.removeAll { it.titleRes == R.string.debug_title }
        }

        fragments.addAll(target.map(Header::fragment))
    }

    @Deprecated("Deprecated in Java")
    override fun isValidFragment(fragmentName: String): Boolean = fragments.contains(fragmentName)

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return true
    }

    companion object {
        private val fragments = mutableListOf<String>()
    }
}
