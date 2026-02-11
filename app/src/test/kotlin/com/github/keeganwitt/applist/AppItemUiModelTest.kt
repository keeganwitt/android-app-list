package com.github.keeganwitt.applist

import android.graphics.drawable.Drawable
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AppItemUiModelTest {
    @Test
    fun `given AppItemUiModel, when created with all fields, then all fields are set correctly`() {
        val model =
            AppItemUiModel(
                packageName = "com.test.app",
                appName = "Test App",
                infoText = "1.0.0",
            )

        assertEquals("com.test.app", model.packageName)
        assertEquals("Test App", model.appName)
        assertEquals("1.0.0", model.infoText)
    }

    @Test
    fun `given two AppItemUiModel instances with same data, when compared, then they are equal`() {
        val model1 =
            AppItemUiModel(
                packageName = "com.test.app",
                appName = "Test App",
                infoText = "1.0.0",
            )

        val model2 =
            AppItemUiModel(
                packageName = "com.test.app",
                appName = "Test App",
                infoText = "1.0.0",
            )

        assertEquals(model1.packageName, model2.packageName)
        assertEquals(model1.appName, model2.appName)
        assertEquals(model1.infoText, model2.infoText)
    }

    @Test
    fun `given AppItemUiModel, when copy with modified field, then only that field changes`() {
        val model =
            AppItemUiModel(
                packageName = "com.test.app",
                appName = "Test App",
                infoText = "1.0.0",
            )

        val modifiedModel = model.copy(infoText = "2.0.0")

        assertEquals("2.0.0", modifiedModel.infoText)
        assertEquals(model.packageName, modifiedModel.packageName)
        assertEquals(model.appName, modifiedModel.appName)
    }

    @Test
    fun `given AppItemUiModel with empty strings, when created, then empty strings are preserved`() {
        val model =
            AppItemUiModel(
                packageName = "",
                appName = "",
                infoText = "",
            )

        assertEquals("", model.packageName)
        assertEquals("", model.appName)
        assertEquals("", model.infoText)
    }

    @Test
    fun `given AppItemUiModel with special characters, when created, then special characters are preserved`() {
        val model =
            AppItemUiModel(
                packageName = "com.test.app-special_123",
                appName = "Test & App <tag>",
                infoText = "Version \"1.0\" & 'more'",
            )

        assertEquals("com.test.app-special_123", model.packageName)
        assertEquals("Test & App <tag>", model.appName)
        assertEquals("Version \"1.0\" & 'more'", model.infoText)
    }
}
