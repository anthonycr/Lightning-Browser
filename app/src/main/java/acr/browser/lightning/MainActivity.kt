package acr.browser.lightning

import acr.browser.lightning.browser.activity.BrowserActivity
import android.content.Intent
import android.os.Build
import android.view.KeyEvent
import android.view.Menu
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import com.anthonycr.bonsai.Completable

class MainActivity : BrowserActivity() {

    @Suppress("DEPRECATION")
    public override fun updateCookiePreference(): Completable {
        return Completable.create { subscriber ->
            val cookieManager = CookieManager.getInstance()
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                CookieSyncManager.createInstance(this@MainActivity)
            }
            cookieManager.setAcceptCookie(preferences.cookiesEnabled)
            subscriber.onComplete()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onNewIntent(intent: Intent) {
        if (BrowserActivity.isPanicTrigger(intent)) {
            panicClean()
        } else {
            handleNewIntent(intent)
            super.onNewIntent(intent)
        }
    }

    override fun onPause() {
        super.onPause()
        saveOpenTabs()
    }

    override fun updateHistory(title: String?, url: String) {
        addItemToHistory(title, url)
    }

    public override fun isIncognito(): Boolean = false

    override fun closeActivity() {
        closeDrawers {
            performExitCleanUp()
            moveTaskToBack(true)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.isCtrlPressed) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_P ->
                    // Open a new private window
                    if (event.isShiftPressed) {
                        startActivity(Intent(this, IncognitoActivity::class.java))
                        overridePendingTransition(R.anim.slide_up_in, R.anim.fade_out_scale)
                        return true
                    }
            }
        }
        return super.dispatchKeyEvent(event)
    }


}
