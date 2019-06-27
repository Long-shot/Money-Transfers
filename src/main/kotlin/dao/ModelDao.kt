package dao

import model.IndexedModel
import java.lang.IllegalArgumentException
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Storage that is able to store and access Indexed data
 */
interface IndexedDao<T: IndexedModel> {
    fun create(entry: T)
    fun delete(entry: T)
    fun update(entry: T)
    fun getAll(): List<T>
    fun filter(filter: (T) -> Boolean) : List<T>
    fun findById(id: UUID): T?
}

/**
 * Stores information in RAM, without any persistent storage
 */
class InMemoryIndexedDao<T: IndexedModel>: IndexedDao<T> {
    private val storage = ConcurrentHashMap<UUID, T>()

    override fun create(entry: T) {
        storage[entry.id] = entry
    }

    override fun delete(entry: T) {
        storage.remove(entry.id) ?: throw IllegalArgumentException("Entity with id [${entry.id}] not found") // TODO: proper errors
    }

    override fun update(entry: T) {
        if (!storage.contains(entry)) {
            throw IllegalArgumentException("Entity with id [${entry.id}] not found") // TODO: proper errors
        }
        storage.remove(entry.id)
    }

    override fun filter(filter: (T) -> Boolean) = storage.values.filter(filter)

    override fun findById(id: UUID) : T? = storage[id]

    override fun getAll() = storage.values.toList()
}
