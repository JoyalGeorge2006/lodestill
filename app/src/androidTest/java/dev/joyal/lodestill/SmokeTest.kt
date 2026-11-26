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

    @Test fun todayShowsAndStartSheetOpens() {
        rule.onAllNodesWithText("Today")[0].assertIsDisplayed()
        rule.onNodeWithText("Start a session").assertIsDisplayed()
        rule.onNodeWithText("Start a session").performClick()
        rule.onNodeWithText("Begin").assertIsDisplayed()
    }

    @Test fun settingsShowsTiers() {
        rule.onNodeWithText("Settings").performClick()
        rule.onNodeWithText("Focus protection").assertIsDisplayed()
        rule.onNodeWithText("Guard").assertIsDisplayed()
        rule.onNodeWithText("Block").assertIsDisplayed()
    }
}
