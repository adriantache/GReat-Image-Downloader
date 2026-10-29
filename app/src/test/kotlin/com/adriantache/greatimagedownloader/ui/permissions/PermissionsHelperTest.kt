package com.adriantache.greatimagedownloader.ui.permissions

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class PermissionsHelperTest {
    private lateinit var context: Context
    private lateinit var app: Application

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        app = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `hasAllRequiredPermissions returns false when location permission is missing`() {
        shadowOf(app).grantPermissions(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        shadowOf(app).denyPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION)

        assertThat(PermissionsHelper.hasAllRequiredPermissions(context)).isFalse
    }

    @Test
    fun `hasAllRequiredPermissions returns false when storage permission is missing`() {
        shadowOf(app).grantPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION)
        shadowOf(app).denyPermissions(android.Manifest.permission.READ_EXTERNAL_STORAGE)

        assertThat(PermissionsHelper.hasAllRequiredPermissions(context)).isFalse
    }

    @Test
    fun `hasAllRequiredPermissions returns true when all required permissions are granted on SDK 30`() {
        shadowOf(app).grantPermissions(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
        )

        assertThat(PermissionsHelper.hasAllRequiredPermissions(context)).isTrue
    }
}
