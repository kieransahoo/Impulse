package com.impulse.backend.common

import com.impulse.backend.collection.DuplicateCollectionException
import com.impulse.backend.collection.UnsupportedCollectionSourceException
import com.impulse.backend.collection.YouTubeCollectionException
import com.impulse.backend.collection.YouTubeConfigurationException
import com.impulse.backend.collection.LinkCollectionNotFoundException
import com.impulse.backend.post.PostNotFoundException
import com.impulse.backend.memory.AiProcessingException
import com.impulse.backend.memory.DuplicateMemoryException
import com.impulse.backend.memory.UnsupportedMemoryUrlException
import com.impulse.backend.planning.NoMemoriesForPlanningException
import com.impulse.backend.usercollection.UserCollectionNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

data class ApiError(
    val status: Int,
    val message: String,
    val errors: Map<String, String> = emptyMap(),
    val timestamp: Instant = Instant.now(),
)

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(UserCollectionNotFoundException::class)
    fun handleUserCollectionNotFound(exception: UserCollectionNotFoundException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiError(
                status = HttpStatus.NOT_FOUND.value(),
                message = exception.message ?: "Collection was not found",
            ),
        )

    @ExceptionHandler(NoMemoriesForPlanningException::class)
    fun handleNoMemories(exception: NoMemoriesForPlanningException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
            ApiError(
                status = HttpStatus.UNPROCESSABLE_ENTITY.value(),
                message = exception.message ?: "No memories are available for planning",
            ),
        )

    @ExceptionHandler(LinkCollectionNotFoundException::class)
    fun handleCollectionNotFound(exception: LinkCollectionNotFoundException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiError(
                status = HttpStatus.NOT_FOUND.value(),
                message = exception.message ?: "Link collection was not found",
            ),
        )

    @ExceptionHandler(UnsupportedCollectionSourceException::class)
    fun handleUnsupportedCollection(exception: UnsupportedCollectionSourceException): ResponseEntity<ApiError> =
        ResponseEntity.badRequest().body(
            ApiError(
                status = HttpStatus.BAD_REQUEST.value(),
                message = exception.message ?: "Unsupported collection source",
            ),
        )

    @ExceptionHandler(DuplicateCollectionException::class)
    fun handleDuplicateCollection(exception: DuplicateCollectionException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            ApiError(
                status = HttpStatus.CONFLICT.value(),
                message = exception.message ?: "Collection already exists",
            ),
        )

    @ExceptionHandler(YouTubeConfigurationException::class)
    fun handleYouTubeConfiguration(exception: YouTubeConfigurationException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            ApiError(
                status = HttpStatus.SERVICE_UNAVAILABLE.value(),
                message = exception.message ?: "YouTube collection is not configured",
            ),
        )

    @ExceptionHandler(YouTubeCollectionException::class)
    fun handleYouTubeCollection(exception: YouTubeCollectionException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
            ApiError(
                status = HttpStatus.BAD_GATEWAY.value(),
                message = exception.message ?: "YouTube collection failed",
            ),
        )

    @ExceptionHandler(UnsupportedMemoryUrlException::class)
    fun handleUnsupportedUrl(exception: UnsupportedMemoryUrlException): ResponseEntity<ApiError> =
        ResponseEntity.badRequest().body(
            ApiError(
                status = HttpStatus.BAD_REQUEST.value(),
                message = exception.message ?: "Unsupported URL",
            ),
        )

    @ExceptionHandler(DuplicateMemoryException::class)
    fun handleDuplicateMemory(exception: DuplicateMemoryException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            ApiError(
                status = HttpStatus.CONFLICT.value(),
                message = exception.message ?: "Memory already exists",
            ),
        )

    @ExceptionHandler(AiProcessingException::class)
    fun handleAiProcessor(exception: AiProcessingException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
            ApiError(
                status = HttpStatus.BAD_GATEWAY.value(),
                message = exception.message ?: "AI processor failed",
            ),
        )

    @ExceptionHandler(PostNotFoundException::class)
    fun handleNotFound(exception: PostNotFoundException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiError(
                status = HttpStatus.NOT_FOUND.value(),
                message = exception.message ?: "Post was not found",
            ),
        )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(exception: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val errors = exception.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "Invalid value")
        }
        return ResponseEntity.badRequest().body(
            ApiError(
                status = HttpStatus.BAD_REQUEST.value(),
                message = "Validation failed",
                errors = errors,
            ),
        )
    }
}
