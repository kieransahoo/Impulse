package com.impulse.backend.planning

import com.impulse.backend.memory.Memory
import com.impulse.backend.memory.MemoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.math.sqrt

data class RetrievedMemory(
    val memory: Memory,
    val score: Double,
    val semanticScore: Double,
    val keywordScore: Double,
    val recencyScore: Double,
)

@Service
class MemoryRetrievalService(
    private val memoryRepository: MemoryRepository,
    private val aiClient: AiPlanningClient,
) {
    @Transactional(readOnly = true)
    fun retrieve(
        userId: UUID,
        query: String,
        limit: Int = DEFAULT_LIMIT,
        allowedMemoryIds: Set<UUID>? = null,
    ): List<RetrievedMemory> {
        val queryEmbedding = aiClient.embedQuery(query)
        val queryTerms = query.lowercase()
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length > 2 }
            .toSet()

        return memoryRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
            .asSequence()
            .filter { allowedMemoryIds == null || it.id in allowedMemoryIds }
            .map { score(it, queryEmbedding, queryTerms) }
            .sortedByDescending(RetrievedMemory::score)
            .take(limit.coerceIn(1, MAX_LIMIT))
            .toList()
    }

    @Transactional(readOnly = true)
    fun retrieveRelevant(
        userId: UUID,
        query: String,
        limit: Int = PLANNING_LIMIT,
        intent: PlanIntent? = null,
        allowedMemoryIds: Set<UUID>? = null,
    ): List<RetrievedMemory> =
        retrieve(userId, query, MAX_LIMIT, allowedMemoryIds)
            .asSequence()
            .filter {
                it.score >= MIN_RELEVANCE_SCORE &&
                    (it.semanticScore >= MIN_SEMANTIC_SCORE || it.keywordScore > 0.0) &&
                    isIntentCompatible(it, intent)
            }
            .distinctBy { it.memory.sourceUrl }
            .take(limit.coerceIn(1, PLANNING_LIMIT))
            .toList()

    private fun isIntentCompatible(result: RetrievedMemory, intent: PlanIntent?): Boolean {
        if (intent == null) return true
        if (intent == PlanIntent.GENERAL) {
            return result.keywordScore > 0.0 || result.semanticScore >= GENERAL_SEMANTIC_SCORE
        }
        val searchable = buildString {
            append(result.memory.title).append(' ')
            append(result.memory.summary).append(' ')
            append(result.memory.category).append(' ')
            append(result.memory.tags.joinToString(" ")).append(' ')
            append(result.memory.topics.joinToString(" "))
        }.lowercase()
        return INTENT_RELEVANCE_TERMS.getValue(intent).any(searchable::contains)
    }

    private fun score(
        memory: Memory,
        queryEmbedding: FloatArray,
        queryTerms: Set<String>,
    ): RetrievedMemory {
        val semantic = cosineSimilarity(memory.embedding, queryEmbedding)
        val searchable = buildString {
            append(memory.title).append(' ')
            append(memory.summary).append(' ')
            append(memory.category).append(' ')
            append(memory.tags.joinToString(" ")).append(' ')
            append(memory.topics.joinToString(" ")).append(' ')
            append(memory.actions.joinToString(" ") { "${it.action} ${it.useWhen.orEmpty()}" })
        }.lowercase()
        val keyword = if (queryTerms.isEmpty()) 0.0 else {
            queryTerms.count(searchable::contains).toDouble() / queryTerms.size
        }
        val ageDays = Duration.between(memory.createdAt, Instant.now()).toDays().coerceAtLeast(0)
        val recency = 1.0 / (1.0 + ageDays / 30.0)
        val actionBoost = if (memory.actions.isEmpty()) 0.0 else 1.0
        val total = 0.60 * semantic + 0.20 * keyword + 0.15 * recency + 0.05 * actionBoost
        return RetrievedMemory(memory, total, semantic, keyword, recency)
    }

    private fun cosineSimilarity(left: FloatArray, right: FloatArray): Double {
        if (left.size != right.size || left.isEmpty()) return 0.0
        var dot = 0.0
        var leftNorm = 0.0
        var rightNorm = 0.0
        for (index in left.indices) {
            dot += left[index] * right[index]
            leftNorm += left[index] * left[index]
            rightNorm += right[index] * right[index]
        }
        if (leftNorm == 0.0 || rightNorm == 0.0) return 0.0
        return dot / (sqrt(leftNorm) * sqrt(rightNorm))
    }

    private companion object {
        const val DEFAULT_LIMIT = 8
        const val MAX_LIMIT = 20
        const val PLANNING_LIMIT = 6
        const val MIN_RELEVANCE_SCORE = 0.30
        const val MIN_SEMANTIC_SCORE = 0.25
        const val GENERAL_SEMANTIC_SCORE = 0.60
        val INTENT_RELEVANCE_TERMS = mapOf(
            PlanIntent.STUDY to listOf("study", "exam", "revision", "syllabus", "course", "education", "lecture"),
            PlanIntent.WORKOUT to listOf("workout", "exercise", "gym", "fitness", "training", "strength", "mobility"),
            PlanIntent.MEAL to listOf("meal", "diet", "recipe", "food", "grocery", "nutrition", "cook"),
            PlanIntent.ROOM to listOf("room", "interior", "decor", "furniture", "aesthetic", "lighting"),
            PlanIntent.PRODUCT to listOf("product", "review", "buy", "purchase", "shopping", "specification"),
            PlanIntent.OUTING to listOf("cafe", "coffee", "restaurant", "outing", "date", "place", "itinerary", "travel"),
            PlanIntent.LEARNING to listOf("learn", "skill", "tutorial", "practice", "lesson"),
            PlanIntent.PROJECT to listOf("project", "build", "create", "launch", "develop"),
            PlanIntent.ROUTINE to listOf("routine", "habit", "daily", "weekly", "schedule"),
            PlanIntent.GENERAL to emptyList(),
        )
    }
}
