package com.javonpeart.ideacompanion.conversation

object EvidenceGuard {
    fun cleanAnswer(raw: String): String {
        val visible = raw.replace(Regex("(?s)<think>.*?</think>"), "").trim()
        val answer = visible.substringAfterLast("Answer:", visible).trim()
        require(answer.isNotBlank() && answer.length <= 800 && "<think" !in answer && "</think>" !in answer && "<|" !in answer) {
            "Model did not produce a finished answer; use the recorded ideas below"
        }
        return answer
    }

    fun hasOnlyKnownCitations(answer: String, ids: Set<Long>): Boolean {
        val cited = Regex("\\[(\\d+)]").findAll(answer).map { it.groupValues[1].toLong() }.toList()
        return cited.isNotEmpty() && cited.all { it in ids }
    }
}
