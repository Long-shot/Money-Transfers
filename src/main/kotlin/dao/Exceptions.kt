package dao

import model.IndexedModel

class EntryNotFoundException(message: String) : Exception(message) {
    constructor(entry: IndexedModel) : this("Entity with id [${entry.id}] not found")
}

class EntryAlreadyExistsException(message: String) : Exception(message) {
    constructor(entry: IndexedModel) : this("Entity with id [${entry.id}] already exists")
}