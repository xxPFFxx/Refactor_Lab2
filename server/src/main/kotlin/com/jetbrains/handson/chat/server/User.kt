package com.jetbrains.handson.chat.server

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.transactions.transaction

//data class User(val id: Int? = null, val name: String, val password: String)

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(Users)
    var name     by Users.name
    var password by Users.password
    var maxEquationNumber by Users.maxEquationNumber
    val equations by Equation referrersOn Equations.user
}

object Users: IntIdTable() {
    val name = varchar("name", 50);
    val password = varchar("password", 200);
    val maxEquationNumber = integer("maxEquationNumber");
}

fun findUserByUsername(username : String) =
    transaction {
        User.find { Users.name eq username }.firstOrNull()
    }

