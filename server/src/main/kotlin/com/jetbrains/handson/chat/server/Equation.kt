package com.jetbrains.handson.chat.server

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class Equation(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Equation>(Equations)
    var number     by Equations.number
    var equation   by Equations.equation
    var solution   by Equations.solution
    var user       by User referencedOn Equations.user
}

object Equations: IntIdTable() {
    val number = integer("number");
    val equation = varchar("equation", 200);
    val solution = varchar("solution", 200);
    val user = reference("user", Users);
}

fun findEquationByNumber(number : Int) =
    transaction {
        Equations.select{ Equations.number eq number}
    }
