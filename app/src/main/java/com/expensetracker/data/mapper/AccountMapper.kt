package com.expensetracker.data.mapper

import com.expensetracker.data.local.entities.AccountEntity
import com.expensetracker.domain.model.Account
import com.expensetracker.domain.model.AccountType
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapper for converting between Account domain model and AccountEntity
 */
@Singleton
class AccountMapper @Inject constructor() {

    fun toDomain(entity: AccountEntity): Account {
        return Account(
            id = entity.id,
            bankName = entity.bankName,
            accountType = AccountType.valueOf(entity.accountType),
            accountNumber = entity.accountNumber,
            nickname = entity.nickname,
            currentBalance = BigDecimal(entity.currentBalance),
            isActive = entity.isActive,
            createdAt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(entity.createdAt),
                ZoneId.systemDefault()
            )
        )
    }

    fun toEntity(domain: Account): AccountEntity {
        return AccountEntity(
            id = domain.id,
            bankName = domain.bankName,
            accountType = domain.accountType.name,
            accountNumber = domain.accountNumber,
            nickname = domain.nickname,
            currentBalance = domain.currentBalance.toString(),
            isActive = domain.isActive,
            createdAt = domain.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
    }

    fun toDomainList(entities: List<AccountEntity>): List<Account> {
        return entities.map { toDomain(it) }
    }

    fun toEntityList(domains: List<Account>): List<AccountEntity> {
        return domains.map { toEntity(it) }
    }
}