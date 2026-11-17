package com.javonpeart.ideacompanion.conversation

import java.text.Normalizer

sealed interface Command {
    data object Recent : Command
    data class Find(val term: String) : Command
    data object Today : Command
    data object Yesterday : Command
    data object Latest : Command
    data object Unsupported : Command
}

object CommandRouter {
    private fun normalize(value: String) = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFKC)
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim().replace(Regex("\\s+"), " ")

    fun parse(speech: String, activationPhrase: String): Command? {
        val phrase = normalize(activationPhrase)
        val spoken = normalize(speech)
        if (phrase.isBlank() || !(spoken == phrase || spoken.startsWith("$phrase "))) return null
        val request = spoken.removePrefix(phrase).trim()
        return when {
            request == "show my recent ideas" -> Command.Recent
            request.startsWith("find ideas about ") && request.length > "find ideas about ".length -> Command.Find(request.removePrefix("find ideas about "))
            request == "what did i record today" -> Command.Today
            request == "what did i record yesterday" -> Command.Yesterday
            request == "read my latest idea" -> Command.Latest
            else -> Command.Unsupported
        }
    }
}
