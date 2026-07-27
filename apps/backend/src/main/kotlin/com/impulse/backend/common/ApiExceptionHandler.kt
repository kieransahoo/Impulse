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
import com.impulse.backend.memory.MemoryNotFoundException
import com.impulse.backend.planning.NoMemoriesForPlanningException
import com.impulse.backend.planning.InvalidPlanCollectionException
import com.impulse.backend.usercollection.UserCollectionNotFoundException
import com.impulse.backend.usercollection.CollectionSourceNotFoundException
import com.impulse.backend.usercollection.CollectionNameConflictException
import com.impulse.backend.usercollection.DefaultCollectionMutationException
import com.impulse.backend.auth.EmailAlreadyRegisteredException
import com.impulse.backend.auth.InvalidCredentialsException
import com.impulse.backend.savedplan.SavedPlanNotFoundException
import com.impulse.backend.savedplan.InvalidPlanMemoryException
import com.impulse.backend.savedplan.IncompletePlanException
import com.impulse.backend.savedplan.SavedPlanStepNotFoundException
import com.impulse.backend.savedplan.CannotReplacePlanException
import com.impulse.backend.savedplan.InvalidPlanTransitionException
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
    @ExceptionHandler(MemoryNotFoundException::class)
    fun handleMemoryNotFound(exception: MemoryNotFoundException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiError(HttpStatus.NOT_FOUND.value(), exception.message ?: "Memory was not found"),
        )

    @ExceptionHandler(SavedPlanNotFoundException::class)
    fun handleSavedPlanNotFound(exception: SavedPlanNotFoundException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiError(HttpStatus.NOT_FOUND.value(), exception.message ?: "Saved plan was not found"),
        )

    @ExceptionHandler(SavedPlanStepNotFoundException::class)
    fun handleSavedPlanStepNotFound(exception: SavedPlanStepNotFoundException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiError(HttpStatus.NOT_FOUND.value(), exception.message ?: "Saved plan step was not found"),
        )

    @ExceptionHandler(IncompletePlanException::class)
    fun handleIncompletePlan(exception: IncompletePlanException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            ApiError(HttpStatus.CONFLICT.value(), exception.message ?: "Plan still has incomplete steps"),
        )

    @ExceptionHandler(CannotReplacePlanException::class)
    fun handleCannotReplacePlan(exception: CannotReplacePlanException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            ApiError(HttpStatus.CONFLICT.value(), exception.message ?: "Saved plan was not changed"),
        )

    @ExceptionHandler(InvalidPlanTransitionException::class)
    fun handleInvalidPlanTransition(exception: InvalidPlanTransitionException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            ApiError(HttpStatus.CONFLICT.value(), exception.message ?: "Invalid plan state"),
        )

    @ExceptionHandler(InvalidPlanMemoryException::class)
    fun handleInvalidPlanMemory(exception: InvalidPlanMemoryException): ResponseEntity<ApiError> =
        ResponseEntity.badRequest().body(
            ApiError(HttpStatus.BAD_REQUEST.value(), exception.message ?: "Invalid cited memory"),
        )

    @ExceptionHandler(CollectionSourceNotFoundException::class)
    fun handleCollectionSourceNotFound(exception: CollectionSourceNotFoundException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiError(HttpStatus.NOT_FOUND.value(), exception.message ?: "Collection source was not found"),
        )

    @ExceptionHandler(CollectionNameConflictException::class)
    fun handleCollectionNameConflict(exception: CollectionNameConflictException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            ApiError(HttpStatus.CONFLICT.value(), exception.message ?: "Collection name already exists"),
        )

    @ExceptionHandler(DefaultCollectionMutationException::class)
    fun handleDefaultCollectionMutation(exception: DefaultCollectionMutationException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            ApiError(HttpStatus.CONFLICT.value(), exception.message ?: "Default collection cannot be changed"),
        )

    @ExceptionHandler(EmailAlreadyRegisteredException::class)
    fun handleEmailConflict(exception: EmailAlreadyRegisteredException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            ApiError(HttpStatus.CONFLICT.value(), exception.message ?: "Email is already registered"),
        )

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(exception: InvalidCredentialsException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ApiError(HttpStatus.UNAUTHORIZED.value(), exception.message ?: "Invalid credentials"),
        )

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

    @ExceptionHandler(InvalidPlanCollectionException::class)
    fun handleInvalidPlanCollection(exception: InvalidPlanCollectionException): ResponseEntity<ApiError> =
        ResponseEntity.badRequest().body(
            ApiError(
                status = HttpStatus.BAD_REQUEST.value(),
                message = exception.message ?: "Invalid selected collection",
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
