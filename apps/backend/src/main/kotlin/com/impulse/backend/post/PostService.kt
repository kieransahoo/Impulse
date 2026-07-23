package com.impulse.backend.post

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PostService(
    private val repository: PostRepository,
) {
    @Transactional
    fun create(request: CreatePostRequest): PostResponse =
        repository.save(
            Post(
                caption = request.caption.trim(),
                imageUrl = request.image.trim(),
                category = request.category,
                tags = request.tags.normalizedTags(),
            ),
        ).toResponse()

    @Transactional(readOnly = true)
    fun findAll(category: PostCategory?, pageable: Pageable): Page<PostResponse> {
        val posts = if (category == null) {
            repository.findAll(pageable)
        } else {
            repository.findAllByCategory(category, pageable)
        }
        return posts.map(Post::toResponse)
    }

    @Transactional(readOnly = true)
    fun findById(id: UUID): PostResponse = findPost(id).toResponse()

    @Transactional
    fun update(id: UUID, request: UpdatePostRequest): PostResponse {
        val post = findPost(id)
        post.caption = request.caption.trim()
        post.imageUrl = request.image.trim()
        post.category = request.category
        post.tags = request.tags.normalizedTags()
        return repository.save(post).toResponse()
    }

    @Transactional
    fun delete(id: UUID) {
        repository.delete(findPost(id))
    }

    private fun findPost(id: UUID): Post =
        repository.findById(id).orElseThrow { PostNotFoundException(id) }
}

private fun Set<String>.normalizedTags(): MutableSet<String> =
    map(String::trim).filter(String::isNotEmpty).toCollection(linkedSetOf())

class PostNotFoundException(id: UUID) : RuntimeException("Post $id was not found")

