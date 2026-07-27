package com.impulse.backend.planning

import org.springframework.stereotype.Service

data class PlanningPreparation(
    val intent: PlanIntent,
    val missingContext: List<String>,
    val suggestedSources: List<String>,
)

@Service
class PlanningPreparationService {
    fun prepare(query: String, constraints: Map<String, Any?>): PlanningPreparation {
        val normalized = query.lowercase()
        val intent = INTENT_TERMS.entries
            .maxByOrNull { (_, terms) -> terms.count(normalized::contains) }
            ?.takeIf { (_, terms) -> terms.any(normalized::contains) }
            ?.key
            ?: PlanIntent.GENERAL
        val supplied = constraints.keys.map(String::lowercase).toSet()
        val missing = requiredContext(intent)
            .filterNot { field -> supplied.any { it.contains(field.key) } || field.terms.any(normalized::contains) }
            .map(ContextField::label)
            .take(4)
        return PlanningPreparation(
            intent = intent,
            missingContext = missing,
            suggestedSources = suggestedSources(intent),
        )
    }

    private fun requiredContext(intent: PlanIntent): List<ContextField> = when (intent) {
        PlanIntent.STUDY, PlanIntent.LEARNING -> listOf(
            ContextField("level", "Current skill level", listOf("beginner", "intermediate", "advanced")),
            ContextField("deadline", "Target date or deadline", listOf("day", "week", "month", "deadline", "exam")),
            ContextField("time", "Available study time", listOf("minute", "hour", "daily")),
        )
        PlanIntent.WORKOUT -> listOf(
            ContextField("level", "Training experience", listOf("beginner", "intermediate", "advanced")),
            ContextField("equipment", "Available equipment", listOf("gym", "home", "dumbbell", "equipment")),
            ContextField("schedule", "Days available each week", listOf("day", "week")),
            ContextField("limitation", "Injuries or physical limitations", listOf("injury", "pain", "limitation")),
        )
        PlanIntent.MEAL -> listOf(
            ContextField("diet", "Dietary preference", listOf("vegetarian", "vegan", "non-veg", "keto")),
            ContextField("allergy", "Allergies or foods to avoid", listOf("allergy", "avoid", "intolerant")),
            ContextField("budget", "Food budget", listOf("budget", "₹", "$")),
        )
        PlanIntent.ROOM -> listOf(
            ContextField("budget", "Room budget", listOf("budget", "₹", "$")),
            ContextField("size", "Room size or dimensions", listOf("feet", "meter", "size", "dimension")),
            ContextField("style", "Preferred visual style", listOf("minimal", "warm", "modern", "style")),
        )
        PlanIntent.PRODUCT -> listOf(
            ContextField("budget", "Purchase budget", listOf("budget", "under", "₹", "$")),
            ContextField("priority", "Most important requirements", listOf("need", "priority", "must")),
        )
        PlanIntent.OUTING -> listOf(
            ContextField("location", "Preferred area or starting point", listOf("near", "area", "city", "location")),
            ContextField("budget", "Budget per person", listOf("budget", "under", "₹", "$")),
            ContextField("time", "Available time", listOf("morning", "afternoon", "evening", "hour")),
        )
        else -> listOf(
            ContextField("time", "Available time", listOf("minute", "hour", "day", "week")),
        )
    }

    private fun suggestedSources(intent: PlanIntent): List<String> = when (intent) {
        PlanIntent.STUDY, PlanIntent.LEARNING ->
            listOf("Course syllabus or learning objectives", "Saved tutorials or lecture notes", "Practice questions")
        PlanIntent.WORKOUT ->
            listOf("Preferred exercise routines", "Equipment tutorials", "Mobility or form guidance")
        PlanIntent.MEAL ->
            listOf("Recipes you want to use", "Dietary guidance you trust", "Ingredient or grocery references")
        PlanIntent.ROOM ->
            listOf("Room inspiration images", "Furniture or product references", "Colour and lighting ideas")
        PlanIntent.PRODUCT ->
            listOf("Two or more product pages", "Specification or review pages", "Your must-have requirements")
        PlanIntent.OUTING ->
            listOf("Cafes or places you want to visit", "Saved local recommendations", "Menus or location pages")
        else ->
            listOf("Tutorials or references related to the goal", "Examples you want to follow")
    }

    private data class ContextField(
        val key: String,
        val label: String,
        val terms: List<String>,
    )

    private companion object {
        val INTENT_TERMS = linkedMapOf(
            PlanIntent.STUDY to listOf("study", "exam", "revision", "syllabus", "course"),
            PlanIntent.WORKOUT to listOf("workout", "exercise", "gym", "fitness", "training"),
            PlanIntent.MEAL to listOf("meal", "diet", "recipe", "food", "grocery"),
            PlanIntent.ROOM to listOf("room", "interior", "decor", "furniture", "aesthetic"),
            PlanIntent.PRODUCT to listOf("product", "compare", "buy", "purchase", "shopping"),
            PlanIntent.OUTING to listOf(
                "cafe", "coffee", "restaurant", "outing", "date", "hopping", "places", "itinerary",
            ),
            PlanIntent.LEARNING to listOf("learn", "skill", "tutorial", "practice"),
            PlanIntent.PROJECT to listOf("project", "build", "create", "launch"),
            PlanIntent.ROUTINE to listOf("routine", "habit", "daily", "weekly"),
        )
    }
}
