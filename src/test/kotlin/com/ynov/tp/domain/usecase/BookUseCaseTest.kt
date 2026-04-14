package com.ynov.tp.domain.usecase

import com.ynov.tp.domain.model.Book
import com.ynov.tp.domain.port.BookRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.stringPattern
import io.kotest.property.checkAll
import io.kotest.property.arbitrary.bind

class BookUseCaseTest : DescribeSpec({

    describe("BookUseCase") {

        lateinit var bookRepository: BookRepository
        lateinit var bookUseCase: BookUseCase

        beforeEach {
            bookRepository = mockk(relaxed = true)
            bookUseCase = BookUseCase(bookRepository)
        }

        describe("addBook") {

            it("devrait ajouter un livre avec titre et auteur valides") {
                // Given
                val title = "Clean Code"
                val author = "Robert Martin"

                // When
                bookUseCase.addBook(title, author)

                // Then
                verify(exactly = 1) {
                    bookRepository.save(match { book ->
                        book.title == title && book.author == author
                    })
                }
            }

            it("devrait rejeter un livre avec titre vide") {
                // When / Then
                shouldThrow<IllegalArgumentException> {
                    bookUseCase.addBook("", "Robert Martin")
                }
            }

            it("devrait rejeter un livre avec auteur vide") {
                // When / Then
                shouldThrow<IllegalArgumentException> {
                    bookUseCase.addBook("Clean Code", "")
                }
            }
        }

        describe("getAllBooksSortedByTitle") {

            it("devrait retourner tous les livres triés par titre") {
                // Given
                val books = listOf(
                    Book("Zinc Book", "Author C"),
                    Book("Alpha Book", "Author A"),
                    Book("Middle Book", "Author B")
                )
                every { bookRepository.findAll() } returns books

                // When
                val result = bookUseCase.getAllBooksSortedByTitle()

                // Then
                result.size shouldBe 3
                result[0].title shouldBe "Alpha Book"
                result[1].title shouldBe "Middle Book"
                result[2].title shouldBe "Zinc Book"
            }

            it("devrait retourner une liste vide si aucun livre") {
                // Given
                every { bookRepository.findAll() } returns emptyList()

                // When
                val result = bookUseCase.getAllBooksSortedByTitle()

                // Then
                result shouldBe emptyList()
            }
        }

        describe("Tests de propriétés") {

            it("PROPRIÉTÉ : La liste retournée contient tous les éléments de la liste stockée") {
                checkAll(Arb.list(arbBook(), 0..50)) { books ->
                    // Given
                    val repo = mockk<BookRepository>(relaxed = true)
                    val useCase = BookUseCase(repo)
                    every { repo.findAll() } returns books

                    // When
                    val result = useCase.getAllBooksSortedByTitle()

                    // Then - La liste résultat contient tous les livres originaux
                    result shouldHaveSize books.size
                    result shouldContainAll books
                }
            }

            it("PROPRIÉTÉ : La liste retournée est toujours triée par titre (ordre lexicographique)") {
                checkAll(Arb.list(arbBook(), 0..50)) { books ->
                    // Given
                    val repo = mockk<BookRepository>(relaxed = true)
                    val useCase = BookUseCase(repo)
                    every { repo.findAll() } returns books

                    // When
                    val result = useCase.getAllBooksSortedByTitle()

                    // Then - Chaque élément doit être <= au suivant
                    result.zipWithNext().all { (current, next) ->
                        current.title <= next.title
                    } shouldBe true
                }
            }

            it("PROPRIÉTÉ : Le nombre d'éléments retournés est toujours égal au nombre stocké") {
                checkAll(Arb.list(arbBook(), 0..100)) { books ->
                    // Given
                    val repo = mockk<BookRepository>(relaxed = true)
                    val useCase = BookUseCase(repo)
                    every { repo.findAll() } returns books

                    // When
                    val result = useCase.getAllBooksSortedByTitle()

                    // Then
                    result.size shouldBe books.size
                }
            }

            it("PROPRIÉTÉ : Ajouter un livre avec titre et auteur non vides ne lève jamais d'exception") {
                checkAll(arbNonEmptyString(), arbNonEmptyString()) { title, author ->
                    // Given - Mock réinitialisé pour chaque itération
                    val repo = mockk<BookRepository>(relaxed = true)
                    val useCase = BookUseCase(repo)

                    // When / Then - Ne doit pas lancer d'exception
                    useCase.addBook(title, author)
                    verify(exactly = 1) { repo.save(any()) }
                }
            }

            it("PROPRIÉTÉ : Ajouter un livre avec titre vide lève toujours une exception") {
                checkAll(arbBlankString(), arbNonEmptyString()) { emptyTitle, author ->
                    // Given
                    val repo = mockk<BookRepository>(relaxed = true)
                    val useCase = BookUseCase(repo)

                    // When / Then
                    shouldThrow<IllegalArgumentException> {
                        useCase.addBook(emptyTitle, author)
                    }
                }
            }

            it("PROPRIÉTÉ : Ajouter un livre avec auteur vide lève toujours une exception") {
                checkAll(arbNonEmptyString(), arbBlankString()) { title, emptyAuthor ->
                    // Given
                    val repo = mockk<BookRepository>(relaxed = true)
                    val useCase = BookUseCase(repo)

                    // When / Then
                    shouldThrow<IllegalArgumentException> {
                        useCase.addBook(title, emptyAuthor)
                    }
                }
            }
        }
    }
})

// Générateurs personnalisés (Arbitraries)
private fun arbBook(): Arb<Book> = Arb.bind(
    arbNonEmptyString(),
    arbNonEmptyString()
) { title, author ->
    Book(title, author)
}

private fun arbNonEmptyString(): Arb<String> =
    Arb.stringPattern("[a-zA-Z0-9][a-zA-Z0-9 ]{0,49}")

private fun arbBlankString(): Arb<String> =
    Arb.stringPattern("[ ]{0,10}")