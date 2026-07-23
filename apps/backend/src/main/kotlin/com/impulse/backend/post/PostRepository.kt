package com.impulse.backend.post

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PostRepository : JpaRepository<Post, UUID> {
    fun findAllByCategory(category: PostCategory, pageable: Pageable): Page<Post>
}

