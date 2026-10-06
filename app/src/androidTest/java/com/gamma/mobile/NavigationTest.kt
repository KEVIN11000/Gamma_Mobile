package com.gamma.mobile

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class NavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNavigation_clickTabs_changesScreen() {
        // Starts at Dashboard
        composeTestRule.onNodeWithText("Gamma Dashboard").assertExists()

        // Click on Diario
        composeTestRule.onNodeWithContentDescription("Transacciones").performClick()
        composeTestRule.onNodeWithText("Libro Diario").assertExists()

        // Click on Obligaciones
        composeTestRule.onNodeWithContentDescription("Obligaciones").performClick()
        composeTestRule.onNodeWithText("Obligaciones").assertExists()

        // Click on Avisos
        composeTestRule.onNodeWithContentDescription("Avisos").performClick()
        composeTestRule.onNodeWithText("Pantalla de Avisos").assertExists()

        // Click on Sistema
        composeTestRule.onNodeWithContentDescription("Sistema").performClick()
        composeTestRule.onNodeWithText("Pantalla de Sistema").assertExists()

        // Go back to Dashboard
        composeTestRule.onNodeWithContentDescription("Dashboard").performClick()
        composeTestRule.onNodeWithText("Gamma Dashboard").assertExists()
    }
}
