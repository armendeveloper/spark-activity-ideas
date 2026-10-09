package com.armen.spark.ui.edit

import com.armen.spark.testIdea
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IdeaDraftTest {
    @Test
    fun blankOrWhitespaceTitleCannotBeSaved() {
        assertFalse(IdeaDraft(title = "").canSave)
        assertFalse(IdeaDraft(title = "   \n").canSave)
        assertNull(IdeaDraft(title = "   ").toIdeaOrNull())
    }

    @Test
    fun savingTrimsTextAndMarksCustom() {
        val idea = IdeaDraft(title = "  Bake bread ", description = " Sourdough \n", emoji = "  ").toIdeaOrNull()!!
        assertEquals("Bake bread", idea.title)
        assertEquals("Sourdough", idea.description)
        assertEquals("✨", idea.emoji)
        assertTrue(idea.isCustom)
        assertTrue("new custom ideas are favorited", idea.isFavorite)
    }

    @Test
    fun roundTripsAnExistingIdea() {
        val original = testIdea(9, title = "Kite flying", isCustom = true, isFavorite = false)
        assertEquals(original, IdeaDraft.from(original).toIdeaOrNull())
    }
}
