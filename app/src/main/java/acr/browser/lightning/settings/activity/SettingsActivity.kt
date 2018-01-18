/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.settings.activity

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.R
import android.os.Build
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.anthonycr.grant.PermissionsManager
import java.util.*

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
        setSupportActionbar(toolbarContainer.findViewById<Toolbar>(R.id.toolbar))
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onBuildHeaders(target: MutableList<Header>) {
        loadHeadersFromResource(R.xml.preferences_headers, target)
        fragments.clear()
        val headerIterator = target.iterator()
        while (headerIterator.hasNext()) {
            val header = headerIterator.next()
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                // Workaround for bug in the AppCompat support library
                header.iconRes = R.drawable.empty
            }

            if (header.titleRes == R.string.debug_title) {
                if (BrowserApp.isRelease) {
                    headerIterator.remove()
                } else {
                    fragments.add(header.fragment)
                }
            } else {
                fragments.add(header.fragment)
            }
        }
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
        private val fragments = ArrayList<String>(7)
    }
}
