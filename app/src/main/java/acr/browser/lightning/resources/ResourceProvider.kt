package acr.browser.lightning.resources

import android.app.Application
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import javax.inject.Inject

/**
 * Provides access to Android resources.
 */
interface ResourceProvider {
    /**
     * A zero-argument string resource.
     */
    fun stringResource(id: Int): String

    /**
     * A string resource formatted with one or many arguments.
     */
    fun stringResource(id: Int, vararg args: Any): String

    /**
     * A string array.
     */
    fun stringArrayResource(id: Int): Array<String>

    /**
     * A drawable resource.
     */
    fun drawableResource(id: Int): Drawable?
}

/**
 * The default implementation of [ResourceProvider] that delegates to [Application].
 */
class DefaultResourceProvider @Inject constructor(
    private val application: Application
) : ResourceProvider {
    override fun stringResource(id: Int): String = application.getString(id)

    override fun stringResource(id: Int, vararg args: Any): String =
        application.getString(id, *args)

    override fun stringArrayResource(id: Int): Array<String> =
        application.resources.getStringArray(id)

    override fun drawableResource(id: Int): Drawable? =
        AppCompatResources.getDrawable(application, id)
}
