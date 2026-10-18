package com.medstudy.ai

import com.medstudy.ai.data.ExportUtil
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportTest {
    @Test
    fun testAnkiTsvFormatting() {
        val card = ExportUtil.FlashcardItem("Test Front", "Test Back", "tag1")
        val tsv = ExportUtil.generateAnkiTsv(listOf(card))
        assert(tsv.contains("#separator:tab"))
        assert(tsv.contains("Test Front\tTest Back\ttag1"))
    }
}
