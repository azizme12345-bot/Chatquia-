package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.containsRtlScript
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ChatQivo", appName)
    }

    @Test
    fun `hero title preserves spaces around you`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fullTitle = context.getString(R.string.hero_title_prefix) +
            context.getString(R.string.hero_title_em) +
            context.getString(R.string.hero_title_suffix)
        assertEquals("What can I help you with?", fullTitle)
    }

    @Test
    fun `rtl script detection works for Urdu and Arabic`() {
        assertTrue(containsRtlScript("سلام دنیا"))
        assertTrue(containsRtlScript("مرحبا بالعالم"))
        assertFalse(containsRtlScript("Hello ChatQivo"))
    }
}
