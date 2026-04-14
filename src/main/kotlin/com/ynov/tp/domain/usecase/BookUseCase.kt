package com.ynov.tp.domain.usecase

import com.ynov.tp.domain.model.Book
import com.ynov.tp.domain.port.BookRepository

class BookUseCase(
    private val bookRepository: BookRepository
) {
    fun addBook(title: String, author: String) {
        val book = Book(title, author)
        bookRepository.save(book)
    }

    fun getAllBooksSortedByTitle(): List<Book> {
        return bookRepository.findAll()
            .sortedBy { it.title }
    }
}