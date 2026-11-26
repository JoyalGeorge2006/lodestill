package dev.joyal.lodestill

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @org.junit.Before fun before() = kotlinx.coroutines.runBlocking {
        TestReset.all(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as android.app.Application)
    }

    @Test fun todayShowsAndStartSheetOpens() {
        rule.onAllNodesWithText("TODAY")[0].assertIsDisplayed()
        rule.onNodeWithText("Start a session").assertIsDisplayed()
        rule.onNodeWithText("Start a session").performClick()
        rule.onNodeWithText("Begin").assertIsDisplayed()
    }

    @Test fun settingsShowsTiers() {
        rule.onNodeWithText("SETTINGS").performClick()
        rule.onNodeWithText("FOCUS PROTECTION").assertIsDisplayed()
        rule.onNodeWithText("GUARD").assertIsDisplayed()
        rule.onNodeWithText("BLOCK").assertIsDisplayed()
    }
}
