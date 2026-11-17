package com.javonpeart.ideacompanion

import com.javonpeart.ideacompanion.conversation.EvidenceGuard
import org.junit.Assert.*
import org.junit.Test

class EvidenceGuardTest {
    @Test fun rejectsMissingAndUnknownCitations() {
        assertFalse(EvidenceGuard.hasOnlyKnownCitations("A new memory", setOf(7)))
        assertFalse(EvidenceGuard.hasOnlyKnownCitations("A memory [7] and another [8]", setOf(7)))
    }
    @Test fun acceptsKnownIdeaCitations() {
        assertTrue(EvidenceGuard.hasOnlyKnownCitations("A recorded idea [7]", setOf(7)))
    }
    @Test fun removesFinishedThinkingAndRejectsUnfinishedThinking() {
        assertEquals("Idea [7]", EvidenceGuard.cleanAnswer("<think>private reasoning</think>Answer: Idea [7]"))
        assertThrows(IllegalArgumentException::class.java) {
            EvidenceGuard.cleanAnswer("<think>unfinished [7]")
        }
    }
    @Test fun keepsOnlyFinalAnswerAfterModelAnalysis() {
        assertEquals("Idea [7]", EvidenceGuard.cleanAnswer("Analysis mentions [8]. Answer: Idea [7]"))
    }
}
