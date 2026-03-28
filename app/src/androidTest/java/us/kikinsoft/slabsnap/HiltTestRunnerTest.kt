package us.kikinsoft.slabsnap

import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class HiltTestRunnerTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Test
    fun GIVEN_custom_test_runner_WHEN_app_starts_THEN_application_is_HiltTestApplication() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(context.applicationContext is HiltTestApplication)
    }
}
