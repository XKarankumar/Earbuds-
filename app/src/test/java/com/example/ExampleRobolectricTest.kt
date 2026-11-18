package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `app name matches Aether Buds`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Aether Buds", appName)
    }

    @Test
    fun `navigate across all four companion tabs`() {
        composeRule.onNodeWithTag("anc_mode_anc_max").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_sound").performClick()
        composeRule.onNodeWithTag("open_save_profile_button").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_controls").performClick()
        composeRule.onNodeWithTag("earbud_side_left").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_security").performClick()
        composeRule.onNodeWithTag("exclusive_lock_switch").assertIsDisplayed()
    }
}
