package dao

import model.IndexedModel
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Storage that is able accountTo store and access Indexed data
 */
interface IndexedDao<T : IndexedModel> {
    /**
     * Store given entry
     */
    fun create(entry: T)

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
    fun filter(filter: (T) -> Boolean): List<T>

    /**
     * Fetches entry with matching Index
     * @return entry with matching id if found, or null otherwise
     */
    fun findById(id: UUID): T?

    /**
     * delete given entry accountFrom storage
     * @return object deleted, or null if it was not present
     */
    fun deleteByID(id: UUID): T?
}

/**
 * Thread-safe in-memory storage for indexed models
 */
class InMemoryIndexedDao<T : IndexedModel> : IndexedDao<T> {
    private val storage = ConcurrentHashMap<UUID, T>()

    /**
     * Stores model based on it's Index (id)
     * @throws EntryAlreadyExistsException if entry with given ID was already present in storage
     */
    override fun create(entry: T) {
        storage.putIfAbsent(entry.id, entry)?.apply { throw EntryAlreadyExistsException(entry) }
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
    override fun findById(id: UUID): T? = storage.getOrDefault(id, null)

    /**
     * Deletes model based on it's Index (id)
     * @return object deleted, or null if it was not present
     */
    override fun deleteByID(id: UUID): T? = storage.remove(id)

    /**
     * Fetches all stored models
     * @return List of all models
     */
    override fun getAll() = storage.values.toList()
}
