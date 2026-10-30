package com.ecoalacena.app.data.remote.ai

import kotlinx.serialization.Serializable

/**
 * One food product recognised by Gemini.
 *
 * Every field except [name] has a default so that a slightly incomplete
 * answer from the model does not make the whole parse fail.
 */
@Serializable
data class GeminiAnalysisResult(
    val name: String,
    /** A category code (`FRUIT_VEG`, `DAIRY`, `MEAT`…); mapped with `Category.fromRaw`. */
    val category: String = "OTHER",
    val quantity: Double = 1.0,
    val unit: String = "unit",
    /** Days from today until the product is expected to expire, or `null` if unknown. */
    val estimatedDaysUntilExpiration: Int? = null,
    /** How sure the model is of this item, from 0 to 1. */
    val confidence: Double = 1.0,
    val isOrganic: Boolean = false,
)

/**
 * The JSON document the model is instructed to return.
 * A single product photo yields one item; a supermarket receipt yields many.
 */
@Serializable
data class GeminiAnalysisResponse(
    val items: List<GeminiAnalysisResult> = emptyList(),
)

// --- Envelope returned by the Gemini `generateContent` endpoint (only the fields we use) ---

@Serializable
internal data class GeminiGenerateContentResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
    val promptFeedback: GeminiPromptFeedback? = null,
)

@Serializable
internal data class GeminiCandidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null,
)

@Serializable
internal data class GeminiContent(
    val parts: List<GeminiPart> = emptyList(),
)

@Serializable
internal data class GeminiPart(
    val text: String? = null,
)

@Serializable
internal data class GeminiPromptFeedback(
    val blockReason: String? = null,
)
