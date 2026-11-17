package com.javonpeart.ideacompanion.conversation

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.Content
import com.javonpeart.ideacompanion.data.Idea
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class GemmaEngine {
    suspend fun summarize(model: File, question: String, evidence: List<Idea>): String = withContext(Dispatchers.Default) {
        require(model.isFile) { "Import a LiteRT-LM Gemma model first" }
        require(evidence.isNotEmpty()) { "No recorded ideas match this request" }
        val source = evidence.take(5).joinToString("\n") { idea ->
            "[${idea.id}] ${idea.title.orEmpty()} ${idea.transcript.ifBlank { "(no transcript)" }}".take(1200)
        }
        val prompt = """Answer the user's question using only these recorded ideas. If the evidence does not answer it, say so. Cite every factual claim using [idea ID]. Do not invent memories.
Question: $question
Recorded ideas:
$source"""
        Engine(EngineConfig(modelPath = model.absolutePath, backend = Backend.CPU)).use { engine ->
            engine.initialize()
            engine.createConversation().use { conversation ->
                val answer = conversation.sendMessage(Message.of(prompt)).contents
                    .filterIsInstance<Content.Text>().joinToString("") { it.text }.trim()
                require(EvidenceGuard.hasOnlyKnownCitations(answer, evidence.map { it.id }.toSet())) {
                    "Gemma response had no valid idea citations; review the recorded evidence instead"
                }
                answer
            }
        }
    }
}
