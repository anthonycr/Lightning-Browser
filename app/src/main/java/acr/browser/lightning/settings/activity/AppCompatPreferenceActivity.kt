package acr.browser.lightning.settings.activity

import acr.browser.lightning.R
import android.content.res.Configuration
import android.os.Bundle
import android.preference.PreferenceActivity
import android.support.annotation.LayoutRes
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatDelegate
import android.support.v7.widget.Toolbar
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup

/**
 * A [android.preference.PreferenceActivity] which implements and proxies the necessary calls
 * to be used with AppCompat.
 *
 *
 * This technique can be used with an [android.app.Activity] class, not just
 * [android.preference.PreferenceActivity].
 */
abstract class AppCompatPreferenceActivity : PreferenceActivity() {

    private lateinit var delegate: AppCompatDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        delegate = AppCompatDelegate.create(this, null)
        overridePendingTransition(R.anim.slide_in_from_right, R.anim.fade_out_scale)
        delegate.installViewFactory()
        delegate.onCreate(savedInstanceState)
        super.onCreate(savedInstanceState)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        delegate.onPostCreate(savedInstanceState)
    }

    fun setSupportActionbar(toolbar: Toolbar?) = delegate.setSupportActionBar(toolbar)

    fun getSupportActionBar(): ActionBar? = delegate.supportActionBar

    override fun getMenuInflater(): MenuInflater = delegate.menuInflater

    override fun setContentView(@LayoutRes layoutResID: Int) =
            delegate.setContentView(layoutResID)

    override fun setContentView(view: View) =
            delegate.setContentView(view)

    override fun setContentView(view: View, params: ViewGroup.LayoutParams) =
            delegate.setContentView(view, params)

    override fun addContentView(view: View, params: ViewGroup.LayoutParams) =
            delegate.addContentView(view, params)

    override fun onPostResume() {
        super.onPostResume()
        delegate.onPostResume()
    }

    override fun onTitleChanged(title: CharSequence, color: Int) {
        super.onTitleChanged(title, color)
        delegate.setTitle(title)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        delegate.onConfigurationChanged(newConfig)
    }

    override fun onPause() {
        super.onPause()
        if (isFinishing) {
            overridePendingTransition(R.anim.fade_in_scale, R.anim.slide_out_to_right)
        }
    }

    override fun onStop() {
        super.onStop()
        delegate.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        delegate.onDestroy()
    }

    override fun invalidateOptionsMenu() =
            delegate.invalidateOptionsMenu()
}
