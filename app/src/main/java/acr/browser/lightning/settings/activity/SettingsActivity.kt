/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.settings.activity

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.R
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.anthonycr.grant.PermissionsManager

class SettingsActivity : ThemableSettingsActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
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

    override fun onBuildHeaders(target: MutableList<Header>) {
        loadHeadersFromResource(R.xml.preferences_headers, target)
        fragments.clear()

        target.removeAll { it.titleRes == R.string.debug_title && BrowserApp.isRelease }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // Workaround for bug in the AppCompat support library
            target.forEach { it.iconRes = R.drawable.empty }
        }

        fragments.addAll(target.map(Header::fragment))
    }

    override fun isValidFragment(fragmentName: String): Boolean = fragments.contains(fragmentName)

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        private val fragments = mutableListOf<String>()
    }
}
