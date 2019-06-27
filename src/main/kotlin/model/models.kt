package model

import java.util.*

/**
 * Describes models that support indexing.
 * To prevent enumeration, everything is indexed with UUID
 */
interface IndexedModel {
    val id: UUID
}

/**
 * Account represents an Indexed models for storing user account data
 */
data class Account(val bank: String, val number: String, val user_id: String, val currency: Currency) : IndexedModel {
    override val id = UUID.randomUUID()!!
}