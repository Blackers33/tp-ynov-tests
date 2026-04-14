package com.ynov.tp.domain.port

import com.ynov.tp.domain.model.Book

interface BookRepository {
    fun save(book: Book)
    fun findAll(): List<Book>
}