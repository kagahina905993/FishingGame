package com.example.fishinggame

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponsiveLayoutTest {
    @Test
    fun inputBottomBar_isHiddenWhileKeyboardIsVisible() {
        assertFalse(shouldShowInputBottomBar(isKeyboardVisible = true))
    }

    @Test
    fun inputBottomBar_isShownAfterKeyboardCloses() {
        assertTrue(shouldShowInputBottomBar(isKeyboardVisible = false))
    }
}
