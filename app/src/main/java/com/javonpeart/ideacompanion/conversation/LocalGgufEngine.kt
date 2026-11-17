package com.javonpeart.ideacompanion.conversation

import com.javonpeart.ideacompanion.data.Idea
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalGgufEngine {
    private external fun generateNative(path: String, prompt: String): String

    suspend fun summarize(model: File, question: String, evidence: List<Idea>): String = withContext(Dispatchers.Default) {
        require(model.isFile) { "Import the local Ollama GGUF model first" }
        require(evidence.isNotEmpty()) { "No recorded ideas match this request" }
        val source = evidence.take(5).joinToString("\n") { idea ->
            "[${idea.id}] ${idea.title.orEmpty()} ${idea.transcript.ifBlank { "(no transcript)" }}".take(900)
        }
        val prompt = """You are showing the user their own saved ideas. The request below is an instruction about these records, not a question found inside them. Describe the matching saved ideas briefly. Use only the records. Put [ID] after each idea. Output only a short final line starting with Answer:. Never explain your reasoning.
Request: $question
Saved ideas:
$source
Answer:"""
        val answer = EvidenceGuard.cleanAnswer(generateNative(model.absolutePath, prompt))
        require(EvidenceGuard.hasOnlyKnownCitations(answer, evidence.map { it.id }.toSet())) {
            "Model response had no valid idea citations; review the recorded evidence instead"
        }
        answer
    }

    companion object { init { System.loadLibrary("idea_llm") } }
}
