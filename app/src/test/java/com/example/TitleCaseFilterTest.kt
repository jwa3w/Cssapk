package com.example

import com.example.ui.screens.isTitleCaseExceptConjunctions
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TitleCaseFilterTest {

    @Test
    fun testTitleCaseWithConjunctions() {
        // True Title Case where every phrase/word is capitalized, and conjunction is lowercase
        assertTrue(isTitleCaseExceptConjunctions("Web Developer and Graphic Designer"))
        
        // Title Case with uppercase conjunction
        assertTrue(isTitleCaseExceptConjunctions("Web Developer AND Graphic Designer"))
        
        // Normal phrase/sentence case is not considered title case
        assertFalse(isTitleCaseExceptConjunctions("Need a web designer with wordpress skills"))
        
        // Lowercase starting sentence
        assertFalse(isTitleCaseExceptConjunctions("looking for Shopify developer"))
    }

    @Test
    fun testTitleCaseWithSpecialCharactersAndNumbers() {
        // Star decorations and caps
        assertTrue(isTitleCaseExceptConjunctions("***WEB DEVELOPER WANTED***"))
        assertTrue(isTitleCaseExceptConjunctions("⚡ Web Developer Wanted ⚡"))
        
        // Numbers inside
        assertTrue(isTitleCaseExceptConjunctions("Looking for 10 Web Developers"))
    }

    @Test
    fun testEmptyAndShortTitles() {
        assertFalse(isTitleCaseExceptConjunctions(""))
        assertFalse(isTitleCaseExceptConjunctions("   "))
    }
}
