package com.javonpeart.ideacompanion.conversation

object EvidenceGuard {
    fun hasOnlyKnownCitations(answer: String, ids: Set<Long>): Boolean {
        val cited = Regex("\\[(\\d+)]").findAll(answer).map { it.groupValues[1].toLong() }.toList()
        return cited.isNotEmpty() && cited.all { it in ids }
    }
}
