package dao

import model.IndexedModel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*
import kotlin.test.assertFailsWith

data class MockIndexedModel(override val id: UUID, val data: Int) : IndexedModel {
    constructor() : this(UUID.randomUUID()!!, (0..100).random())
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InMemoryIndexedDaoTest {
    private val indexedDao = InMemoryIndexedDao<MockIndexedModel>()

    @BeforeEach
    fun clear() {
        for (entry in indexedDao.getAll()) {
            indexedDao.deleteByID(entry.id)
        }
    }

    @Nested
    inner class Create {
        @Test
        fun `entries are successfully created`() {
            for (i in 0..10) {
                val entry = createEntry()
                indexedDao.create(entry)
                assertTrue(indexedDao.getAll().contains(entry))
            }
        }

        @Test
        fun `creating entries with duplicate id fails`() {
            val entry = createEntry()
            indexedDao.create(entry)
            assertFailsWith<EntryAlreadyExistsException>{indexedDao.create(entry)}
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `existing entries are successfully deleted`() {
            val entry = createEntry()
            indexedDao.create(entry)
            assertTrue(indexedDao.getAll().contains(entry))

            indexedDao.deleteByID(entry.id)
            assertFalse(indexedDao.getAll().contains(entry))
        }

        @Test
        fun `deleting missing entry returns null`() {
            val entry = createEntry()

            assertNull(indexedDao.deleteByID(entry.id))
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `updating existing entry works`() {
            val entry = createEntry()
            indexedDao.create(entry)

            val newEntry = MockIndexedModel(entry.id, entry.data + 1)
            indexedDao.update(newEntry)

            assertEquals(newEntry, indexedDao.findById(entry.id))
        }

        @Test
        fun `updating non-existent entry fails`() {
            val entry = createEntry()

            assertFailsWith<EntryNotFoundException> { indexedDao.update(entry) }
        }
    }

    @Nested
    inner class Filter {
        @Test
        fun `test filtering on data works`() {
            val entry1 = createEntry()
            val entry2 = createEntry()

            indexedDao.create(entry1)
            indexedDao.create(entry2)

            assertIterableEquals(indexedDao.filter { it.data == entry1.data }, listOf(entry1))
        }
    }

    @Nested
    inner class FindById {
        @Test
        fun `test null on fetching non-existing entry`() {
            val entry = createEntry()
            assertNull(indexedDao.findById(entry.id))
        }

        @Test
        fun `test success on fetching existing entry`() {
            val entry = createEntry()
            indexedDao.create(entry)
            assertEquals(entry, indexedDao.findById(entry.id))
        }
    }

    @Nested
    inner class GetAll {
        @Test
        fun `test get all entries`() {
            for (i in 0..10) {
                assertEquals(indexedDao.getAll().size, i)

                val entry = createEntry()
                indexedDao.create(entry)
                assertTrue(indexedDao.getAll().contains(entry))
            }
        }
    }

    private fun createEntry(): MockIndexedModel {
        return MockIndexedModel()
    }
}