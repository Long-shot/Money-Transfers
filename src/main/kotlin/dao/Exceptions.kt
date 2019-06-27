package dao

import model.IndexedModel
import java.lang.Exception

class EntryNotFoundException(message: String): Exception(message) {
    constructor(entry: IndexedModel): this("Entity with id [${entry.id}] not found")
}