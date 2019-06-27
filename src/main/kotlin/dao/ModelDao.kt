package dao

import model.IndexedModel
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList

/**
 * Storage that is able to store and access Indexed data
 */
interface IndexedDao<T: IndexedModel> {
    /**
     * Store given entry
     */
    fun create(entry: T)

    /**
     * delete given entry from storage
     */
    fun delete(entry: T)

    /**
     * Update corresponding underlying entry
     */
    fun update(entry: T)

    /**
     * Fetch all stored data
     * @return List of all entries
     */
    fun getAll(): List<T>

    /**
     * Fetch entries matching given predicate
     * @return List of all matching entries
     */
    fun filter(filter: (T) -> Boolean) : List<T>

    /**
     * Fetches entry with matching Index
     * @return entry with matching id if found, or null otherwise
     */
    fun findById(id: UUID): T?
}

/**
 * Thread-safe in-memory storage for indexed models
 */
class InMemoryIndexedDao<T: IndexedModel>: IndexedDao<T> {
    private val storage = ConcurrentHashMap<UUID, T>()

    /**
     * Stores model based on it's Index (id)
     */
    override fun create(entry: T) {
        storage[entry.id] = entry
    }

    /**
     * Deletes model based on it's Index (id)
     * @throws EntryNotFoundException if specified it was not present
     */
    override fun delete(entry: T) {
        storage.remove(entry.id) ?: throw EntryNotFoundException(entry)
    }

    /**
     * Updates model with given Index (id)
     * @throws EntryNotFoundException if specified it was not present
     */
    override fun update(entry: T) {
        if (!storage.containsKey(entry.id)) {
            throw EntryNotFoundException(entry)
        }
        storage[entry.id] = entry
    }

    /**
     * Fetches all stored models matching predicate
     * @return List of all matching models
     */
    override fun filter(filter: (T) -> Boolean) = storage.values.filter(filter)

    /**
     * Fetches model with given Index (id)
     * @return model with given id if found, null otherwise
     */
    override fun findById(id: UUID) : T? = storage.getOrDefault(id, null)

    /**
     * Fetches all stored models
     * @return List of all models
     */
    override fun getAll() = storage.values.toList()
}
