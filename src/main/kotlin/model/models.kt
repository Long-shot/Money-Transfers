package model

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
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
data class Account(
    val number: String,
    val user_id: String,
    val currency: Currency,
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    override val id: UUID = UUID.randomUUID()!!, val bank: String
) : IndexedModel

/**
 * Set of supported transfer currencies
 */
enum class TransactionStatus {
    PENDING,
    SUCCESS,
    FAILED,
}

data class Transaction(
    val accountFrom: UUID,
    val accountTo: UUID,
    val amount: BigDecimal,
    val currency: Currency,
    var status: TransactionStatus = TransactionStatus.PENDING,
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    override val id: UUID = UUID.randomUUID()!!,
    val error: String = "",
    val createdAt: Date = Date()
) : IndexedModel {
    val updatedAt = Date()
}