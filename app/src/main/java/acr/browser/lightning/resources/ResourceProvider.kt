package acr.browser.lightning.resources

import android.app.Application
import javax.inject.Inject

interface ResourceProvider {
    fun stringResource(id: Int): String

    fun stringResource(id: Int, vararg args: Any): String

    fun stringArrayResource(id: Int): Array<String>
}

class DefaultResourceProvider @Inject constructor(
    private val application: Application
) : ResourceProvider {
    override fun stringResource(id: Int): String = application.getString(id)

    override fun stringResource(id: Int, vararg args: Any): String =
        application.getString(id, *args)

    override fun stringArrayResource(id: Int): Array<String> =
        application.resources.getStringArray(id)
}
