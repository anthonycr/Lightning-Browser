package acr.browser.lightning.view

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.R
import acr.browser.lightning.controller.UIController
import acr.browser.lightning.dialog.BrowserDialog
import acr.browser.lightning.dialog.DialogItem
import acr.browser.lightning.extensions.resizeAndShow
import acr.browser.lightning.favicon.FaviconModel
import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Message
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.webkit.*
import com.anthonycr.bonsai.Schedulers
import com.anthonycr.grant.PermissionsManager
import com.anthonycr.grant.PermissionsResultAction
import javax.inject.Inject

class LightningChromeClient(
        private val activity: Activity,
        private val lightningView: LightningView
) : WebChromeClient() {

    private val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    private val uiController: UIController
    @Inject internal lateinit var faviconModel: FaviconModel

    init {
        BrowserApp.appComponent.inject(this)
        uiController = activity as UIController
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        if (lightningView.isShown) {
            uiController.updateProgress(newProgress)
        }
    }

    override fun onReceivedIcon(view: WebView, icon: Bitmap) {
        lightningView.titleInfo.setFavicon(icon)
        uiController.tabChanged(lightningView)
        cacheFavicon(view.url, icon)
    }

    /**
     * Naive caching of the favicon according to the domain name of the URL
     *
     * @param icon the icon to cache
     */
    private fun cacheFavicon(url: String?, icon: Bitmap?) {
        if (icon == null || url == null) {
            return
        }

        faviconModel.cacheFaviconForUrl(icon, url)
                .subscribeOn(Schedulers.io())
                .subscribe()
    }


    override fun onReceivedTitle(view: WebView?, title: String?) {
        if (title != null && !title.isEmpty()) {
            lightningView.titleInfo.setTitle(title)
        } else {
            lightningView.titleInfo.setTitle(activity.getString(R.string.untitled))
        }
        uiController.tabChanged(lightningView)
        if (view != null && view.url != null) {
            uiController.updateHistory(title, view.url)
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onPermissionRequest(request: PermissionRequest?) {
        activity.runOnUiThread {
            request?.let {
                val resourcesString = it.resources.joinToString(separator = "\n")
                BrowserDialog.showPositiveNegativeDialog(
                        activity = activity,
                        title = R.string.title_permission_request,
                        message = R.string.message_permission_request,
                        arguments = arrayOf(it.origin.host, resourcesString),
                        positiveButton = DialogItem(title = R.string.action_allow) {
                            if (permissions.isEmpty()) {
                                it.grant(it.resources)
                            } else {
                                PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(
                                        activity,
                                        it.requiredPermissions().toTypedArray(),
                                        object : PermissionsResultAction() {
                                            override fun onGranted() {
                                                it.grant(it.resources)
                                            }

                                            override fun onDenied(permission: String?) {
                                                it.deny()
                                            }

                                        }
                                )
                            }
                        },
                        negativeButton = DialogItem(R.string.action_dont_allow, onClick = it::deny)
                )
            }
        }
    }

    override fun onGeolocationPermissionsShowPrompt(origin: String,
                                                    callback: GeolocationPermissions.Callback) =
            PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(activity, permissions, object : PermissionsResultAction() {
                override fun onGranted() {
                    val remember = true
                    AlertDialog.Builder(activity).apply {
                        setTitle(activity.getString(R.string.location))
                        val org = if (origin.length > 50) {
                            "${origin.subSequence(0, 50)}..."
                        } else {
                            origin
                        }
                        setMessage(org + activity.getString(R.string.message_location))
                        setCancelable(true)
                        setPositiveButton(activity.getString(R.string.action_allow)) { _, _ ->
                            callback.invoke(origin, true, remember)
                        }
                        setNegativeButton(activity.getString(R.string.action_dont_allow)) { _, _ ->
                            callback.invoke(origin, false, remember)
                        }
                    }.resizeAndShow()
                }

                override fun onDenied(permission: String) =//TODO show message and/or turn off setting
                        Unit
            })

    override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean,
                                resultMsg: Message): Boolean {
        uiController.onCreateWindow(resultMsg)
        return true
    }

    override fun onCloseWindow(window: WebView) = uiController.onCloseWindow(lightningView)

    @Suppress("unused", "UNUSED_PARAMETER")
    fun openFileChooser(uploadMsg: ValueCallback<Uri>) = uiController.openFileChooser(uploadMsg)

    @Suppress("unused", "UNUSED_PARAMETER")
    fun openFileChooser(uploadMsg: ValueCallback<Uri>, acceptType: String) =
            uiController.openFileChooser(uploadMsg)

    @Suppress("unused", "UNUSED_PARAMETER")
    fun openFileChooser(uploadMsg: ValueCallback<Uri>, acceptType: String, capture: String) =
            uiController.openFileChooser(uploadMsg)

    override fun onShowFileChooser(webView: WebView, filePathCallback: ValueCallback<Array<Uri>>,
                                   fileChooserParams: WebChromeClient.FileChooserParams): Boolean {
        uiController.showFileChooser(filePathCallback)
        return true
    }

    /**
     * Obtain an image that is displayed as a placeholder on a video until the video has initialized
     * and can begin loading.
     *
     * @return a Bitmap that can be used as a place holder for videos.
     */
    override fun getDefaultVideoPoster(): Bitmap? {
        val resources = activity.resources
        return BitmapFactory.decodeResource(resources, android.R.drawable.spinner_background)
    }

    /**
     * Inflate a view to send to a LightningView when it needs to display a video and has to
     * show a loading dialog. Inflates a progress view and returns it.
     *
     * @return A view that should be used to display the state
     * of a video's loading progress.
     */
    override fun getVideoLoadingProgressView(): View =
            LayoutInflater.from(activity).inflate(R.layout.video_loading_progress, null)

    override fun onHideCustomView() = uiController.onHideCustomView()

    override fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback) =
            uiController.onShowCustomView(view, callback)

    override fun onShowCustomView(view: View, requestedOrientation: Int,
                                  callback: WebChromeClient.CustomViewCallback) =
            uiController.onShowCustomView(view, callback, requestedOrientation)

}
