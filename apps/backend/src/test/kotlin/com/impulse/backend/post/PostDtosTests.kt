package com.impulse.backend.post

import kotlin.test.Test
import kotlin.test.assertEquals

class PostDtosTests {
    @Test
    fun `post maps to API response`() {
        val post = Post(
            caption = "Coffee and a slow morning.",
            imageUrl = "https://example.com/coffee.jpg",
            category = PostCategory.CAFE,
            tags = linkedSetOf("Cafe", "Coffee"),
        )

        val response = post.toResponse()

        assertEquals(post.id, response.id)
        assertEquals(post.caption, response.caption)
        assertEquals(post.imageUrl, response.image)
        assertEquals(post.category, response.category)
        assertEquals(post.tags, response.tags)
    }
}
