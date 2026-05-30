package jp.komagata.tweektext

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun editsPlainText() {
        composeRule.onNodeWithTag("editor")
            .performTextInput("alpha\nbeta")

        composeRule.onNodeWithTag("editor")
            .assertTextContains("alpha\nbeta")
        composeRule.onNodeWithText("Unsaved changes")
            .assertExists()
    }

    @Test
    fun searchesText() {
        composeRule.onNodeWithTag("editor")
            .performTextInput("alpha beta alpha")

        composeRule.onNodeWithContentDescription("Search")
            .performClick()
        composeRule.onNodeWithTag("search-query")
            .performTextInput("alpha")

        composeRule.onNodeWithText("2 matches")
            .assertExists()
    }

    @Test
    fun replacesText() {
        composeRule.onNodeWithTag("editor")
            .performTextInput("alpha beta alpha")

        composeRule.onNodeWithContentDescription("More")
            .performClick()
        composeRule.onNodeWithText("Replace")
            .performClick()
        composeRule.onNodeWithTag("replace-find")
            .performTextInput("alpha")
        composeRule.onNodeWithTag("replace-with")
            .performTextInput("gamma")
        composeRule.onNodeWithText("Replace all")
            .performClick()

        composeRule.onNodeWithTag("editor")
            .assertTextContains("gamma beta gamma")
    }

    @Test
    fun saveStartsCreateDocumentPickerForNewFile() {
        intending(hasAction(Intent.ACTION_CREATE_DOCUMENT))
            .respondWith(ActivityResult(Activity.RESULT_CANCELED, null))

        composeRule.onNodeWithContentDescription("Save")
            .performClick()

        intended(hasAction(Intent.ACTION_CREATE_DOCUMENT))
    }

    @Test
    fun openStartsOpenDocumentPicker() {
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT))
            .respondWith(ActivityResult(Activity.RESULT_CANCELED, null))

        composeRule.onNodeWithContentDescription("Open")
            .performClick()

        intended(hasAction(Intent.ACTION_OPEN_DOCUMENT))
    }
}
