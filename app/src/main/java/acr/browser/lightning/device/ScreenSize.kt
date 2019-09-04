package acr.browser.lightning.device

import android.content.Context
import android.content.res.Configuration
import dagger.Reusable
import javax.inject.Inject

/**
 * A model used to determine the screen size info.
 *
 * Created by anthonycr on 2/19/18.
 */
@Reusable
class ScreenSize @Inject constructor(private val context: Context) {

    fun isTablet(): Boolean =
        context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == Configuration.SCREENLAYOUT_SIZE_XLARGE

}
