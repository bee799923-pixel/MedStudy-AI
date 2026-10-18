package com.medstudy.ai.data

object ExportUtil {
    data class FlashcardItem(val front: String, val back: String, val tags: String)

    fun generateAnkiTsv(cards: List<FlashcardItem>): String {
        val sb = StringBuilder()
        sb.append("#separator:tab\n")
        sb.append("#html:false\n")
        sb.append("#tagscolumn:3\n")
        for (card in cards) {
            val cleanFront = card.front.replace("\n", "<br>")
            val cleanBack = card.back.replace("\n", "<br>")
            sb.append("$cleanFront\t$cleanBack\t${card.tags}\n")
        }
        return sb.toString()
    }

    fun generateQuizletText(cards: List<FlashcardItem>): String {
        val sb = StringBuilder()
        for (card in cards) {
            val cleanFront = card.front.replace("\t", " ").replace("\n", " ")
            val cleanBack = card.back.replace("\t", " ").replace("\n", " ")
            sb.append("$cleanFront\t$cleanBack\n")
        }
        return sb.toString()
    }
}
