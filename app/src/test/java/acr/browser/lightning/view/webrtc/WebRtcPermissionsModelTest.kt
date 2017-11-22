package acr.browser.lightning.view.webrtc

import android.net.Uri
import android.webkit.PermissionRequest
import com.nhaarman.mockito_kotlin.*
import org.junit.Test

/**
 * Unit test for [WebRtcPermissionsModel].
 */
class WebRtcPermissionsModelTest {

    private val permissionsView = mock<WebRtcPermissionsView>()

    private fun mockUriForHost(host: String): Uri {
        val mock = mock<Uri>()
        whenever(mock.host).then { host }

        return mock
    }

    @Test
    fun `requestPermission with resource denial denies request`() {
        val model = WebRtcPermissionsModel()
        whenever(permissionsView.requestResources(any(), any(), any())).then {
            (it.arguments[2] as ((Boolean) -> Unit)).invoke(false)
        }

        val request = mock<PermissionRequest>()
        whenever(request.origin).then { mockUriForHost("test.com") }
        whenever(request.resources).then { arrayOf("perm1", "perm2") }

        model.requestPermission(request, permissionsView)

        verify(permissionsView).requestResources(any(), any(), any())
        verifyNoMoreInteractions(permissionsView)

        verify(request).deny()
        verify(request, never()).grant(any())
    }

    @Test
    fun `requestPermission with resource grant and permission denial denies request`() {
        val model = WebRtcPermissionsModel()
        whenever(permissionsView.requestResources(any(), any(), any())).then {
            (it.arguments[2] as ((Boolean) -> Unit)).invoke(true)
        }
        whenever(permissionsView.requestPermissions(any(), any())).then {
            (it.arguments[1] as ((Boolean) -> Unit)).invoke(false)
        }

        val request = mock<PermissionRequest>()
        whenever(request.origin).then { mockUriForHost("test.com") }
        whenever(request.resources).then { arrayOf("perm1", "perm2") }

        model.requestPermission(request, permissionsView)

        verify(permissionsView).requestResources(any(), any(), any())
        verify(permissionsView).requestPermissions(any(), any())
        verifyNoMoreInteractions(permissionsView)

        verify(request).deny()
        verify(request, never()).grant(any())
    }

    @Test
    fun `requestPermission with resource and permission grant grants request`() {
        val model = WebRtcPermissionsModel()
        whenever(permissionsView.requestResources(any(), any(), any())).then {
            (it.arguments[2] as ((Boolean) -> Unit)).invoke(true)
        }
        whenever(permissionsView.requestPermissions(any(), any())).then {
            (it.arguments[1] as ((Boolean) -> Unit)).invoke(true)
        }

        val requestedResources = arrayOf("perm1", "perm2")
        val request = mock<PermissionRequest>()
        whenever(request.origin).then { mockUriForHost("test.com") }
        whenever(request.resources).then { requestedResources }

        model.requestPermission(request, permissionsView)

        verify(permissionsView).requestResources(any(), any(), any())
        verify(permissionsView).requestPermissions(any(), any())
        verifyNoMoreInteractions(permissionsView)

        verify(request).grant(requestedResources)
        verify(request, never()).deny()
    }

    @Test
    fun `requestPermission with pre-granted resources and permission grant grants request`() {
        val model = WebRtcPermissionsModel()
        whenever(permissionsView.requestResources(any(), any(), any())).then {
            (it.arguments[2] as ((Boolean) -> Unit)).invoke(true)
        }
        whenever(permissionsView.requestPermissions(any(), any())).then {
            (it.arguments[1] as ((Boolean) -> Unit)).invoke(true)
        }

        val firstRequestedResources = arrayOf("perm1", "perm2")
        val firstRequest = mock<PermissionRequest>()
        whenever(firstRequest.origin).then { mockUriForHost("test.com") }
        whenever(firstRequest.resources).then { firstRequestedResources }

        model.requestPermission(firstRequest, permissionsView)

        verify(permissionsView).requestResources(any(), any(), any())
        verify(permissionsView).requestPermissions(any(), any())
        verifyNoMoreInteractions(permissionsView)

        verify(firstRequest).grant(firstRequestedResources)
        verify(firstRequest, never()).deny()

        // Set up the second request with a subset of resources
        val secondRequestedResources = arrayOf("perm1")
        val secondRequest = mock<PermissionRequest>()
        whenever(secondRequest.origin).then { mockUriForHost("test.com") }
        whenever(secondRequest.resources).then { secondRequestedResources }

        model.requestPermission(secondRequest, permissionsView)

        verify(permissionsView, times(2)).requestPermissions(any(), any())
        verifyNoMoreInteractions(permissionsView)
        verify(secondRequest).grant(secondRequestedResources)
        verify(secondRequest, never()).deny()
    }

    @Test
    fun `requestPermission with pre-granted resources and permission denial denies request`() {
        val model = WebRtcPermissionsModel()
        whenever(permissionsView.requestResources(any(), any(), any())).then {
            (it.arguments[2] as ((Boolean) -> Unit)).invoke(true)
        }
        whenever(permissionsView.requestPermissions(any(), any())).then {
            (it.arguments[1] as ((Boolean) -> Unit)).invoke(true)
        }

        val firstRequestedResources = arrayOf("perm1", "perm2")
        val firstRequest = mock<PermissionRequest>()
        whenever(firstRequest.origin).then { mockUriForHost("test.com") }
        whenever(firstRequest.resources).then { firstRequestedResources }

        model.requestPermission(firstRequest, permissionsView)

        verify(permissionsView).requestResources(any(), any(), any())
        verify(permissionsView).requestPermissions(any(), any())
        verifyNoMoreInteractions(permissionsView)

        verify(firstRequest).grant(firstRequestedResources)
        verify(firstRequest, never()).deny()

        // Set up the second request with a subset of resources
        val secondRequestedResources = arrayOf("perm1")
        val secondRequest = mock<PermissionRequest>()
        whenever(secondRequest.origin).then { mockUriForHost("test.com") }
        whenever(secondRequest.resources).then { secondRequestedResources }

        whenever(permissionsView.requestPermissions(any(), any())).then {
            (it.arguments[1] as ((Boolean) -> Unit)).invoke(false)
        }

        model.requestPermission(secondRequest, permissionsView)

        verify(permissionsView, times(2)).requestPermissions(any(), any())
        verifyNoMoreInteractions(permissionsView)
        verify(secondRequest).deny()
        verify(secondRequest, never()).grant(any())
    }

    @Test
    fun `requestPermission with permission grant after multiple grants`() {
        val model = WebRtcPermissionsModel()
        whenever(permissionsView.requestResources(any(), any(), any())).then {
            (it.arguments[2] as ((Boolean) -> Unit)).invoke(true)
        }
        whenever(permissionsView.requestPermissions(any(), any())).then {
            (it.arguments[1] as ((Boolean) -> Unit)).invoke(true)
        }

        val firstRequestedResources = arrayOf("perm1")
        val firstRequest = mock<PermissionRequest>()
        whenever(firstRequest.origin).then { mockUriForHost("test.com") }
        whenever(firstRequest.resources).then { firstRequestedResources }

        model.requestPermission(firstRequest, permissionsView)

        verify(permissionsView).requestResources(any(), any(), any())
        verify(permissionsView).requestPermissions(any(), any())
        verifyNoMoreInteractions(permissionsView)

        verify(firstRequest).grant(firstRequestedResources)
        verify(firstRequest, never()).deny()

        // Set up the second request with a superset of resources
        val secondRequestedResources = arrayOf("perm1", "perm2")
        val secondRequest = mock<PermissionRequest>()
        whenever(secondRequest.origin).then { mockUriForHost("test.com") }
        whenever(secondRequest.resources).then { secondRequestedResources }

        model.requestPermission(secondRequest, permissionsView)

        verify(permissionsView, times(2)).requestResources(any(), any(), any())
        verify(permissionsView, times(2)).requestPermissions(any(), any())
        verifyNoMoreInteractions(permissionsView)
        verify(secondRequest).grant(secondRequestedResources)
        verify(secondRequest, never()).deny()

        // Set up the third request with a subset of resources
        val thirdRequestedResources = arrayOf("perm1", "perm2")
        val thirdRequest = mock<PermissionRequest>()
        whenever(thirdRequest.origin).then { mockUriForHost("test.com") }
        whenever(thirdRequest.resources).then { thirdRequestedResources }

        model.requestPermission(thirdRequest, permissionsView)

        verify(permissionsView, times(2)).requestResources(any(), any(), any())
        verify(permissionsView, times(3)).requestPermissions(any(), any())
        verifyNoMoreInteractions(permissionsView)
        verify(thirdRequest).grant(thirdRequestedResources)
        verify(thirdRequest, never()).deny()
    }
}