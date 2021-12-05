package com.jetbrains.handson.chat.server

import io.ktor.http.cio.websocket.*
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

suspend fun registerCommand(params : Array<String>, thisConnection: Connection){
    if (params.size != 3) {
        thisConnection.session.send("Bad format of register command, example: register pff 123")
        sendAvailableCommandsNotLoggedIn(thisConnection)
        return
    }
    val user = findUserByUsername(params[1]);
    if (user != null){
        thisConnection.session.send("User with that username already exists")
        sendAvailableCommandsNotLoggedIn(thisConnection)
    }
    else{
        transaction {
            addLogger(StdOutSqlLogger)
            val user = User.new{
                name = params[1]
                password = BCrypt.hashpw(params[2], BCrypt.gensalt())
                maxEquationNumber = 0
            }
        }
        thisConnection.session.send("You successfully registered")
        sendAvailableCommandsNotLoggedIn(thisConnection)
    }
}

suspend fun loginCommand(params : Array<String>, thisConnection: Connection){
    if (params.size != 3){
        thisConnection.session.send("Bad format of login command, example: login pff 123")
        sendAvailableCommandsNotLoggedIn(thisConnection)
        return
    }
    val user = findUserByUsername(params[1]);
    if (user == null){
        thisConnection.session.send("No user with that username")
        sendAvailableCommandsNotLoggedIn(thisConnection)
    }
    else {
        val password = transaction { user.password };
        if (BCrypt.checkpw(params[2], password)) {
            thisConnection.name = params[1];
            thisConnection.registered = true
            thisConnection.session.send("You are logged in")
            sendAvailableCommandsLoggedIn(thisConnection)
        } else {
            thisConnection.session.send("Incorrect password")
            sendAvailableCommandsNotLoggedIn(thisConnection)
        }
    }
}

suspend fun showCommand(thisConnection: Connection){
    if (thisConnection.registered) {
        val currentUser = findUserByUsername(thisConnection.name)
        val userEquations = transaction { currentUser!!.equations .toList()}

        if(userEquations.isEmpty()){
            thisConnection.session.send("You have no saved equations here :(")
        }else {
            userEquations.forEachIndexed { index, equation ->
                val currentEquation = transaction { equation.equation }
                val currentSolution = transaction { equation.solution }
                thisConnection.session.send("${index+1}. $currentEquation = 0, ${currentSolution}")
            }
        }
        sendAvailableCommandsLoggedIn(thisConnection)
    }
    else{
        thisConnection.session.send("You are not logged in. Register first or enter a valid username and password to perform this operation")
        sendAvailableCommandsNotLoggedIn(thisConnection)
    }
}

suspend fun findCommand(params: Array<String>, thisConnection: Connection){
    if (thisConnection.registered){
        val currentUser = findUserByUsername(thisConnection.name)
        val userEquations = transaction { currentUser!!.equations .toList()}
        if(userEquations.isEmpty()){
            thisConnection.session.send("You can't use 'find' command because your solving history is empty!")
        }else if(kotlin.math.abs(params[1].toInt()) > userEquations.size || params[1].toInt() < 1){
            thisConnection.session.send("Please, enter a valid equation number!")
        }else{
            userEquations.forEachIndexed { index, equation ->
                if (equation.number == params[1].toInt())
                    thisConnection.session.send("Selected equation: ${equation.equation}, ${equation.solution}")
            }
        }
        sendAvailableCommandsLoggedIn(thisConnection)
    }
    else{
        thisConnection.session.send("You are not logged in. Register first or enter a valid username and password to perform this operation")
        sendAvailableCommandsNotLoggedIn(thisConnection)
    }
}

suspend fun solveCommand(params: Array<String>, thisConnection: Connection) {
    if (thisConnection.registered) {
        val intParams = params.map {
            if (!isNumeric(it)){
                thisConnection.session.send("Bad format of coeffs, the all should be integer values, separated with space (1 2 3)")
                sendAvailableCommandsLoggedIn(thisConnection)
                return
            }
            it.toInt()
        }.toTypedArray()
        val p1 = Polynomial(intParams)
        val currentUser = findUserByUsername(thisConnection.name)
        currentUser?.let {
            transaction {
                currentUser.maxEquationNumber += 1
                Equation.new {
                    number = currentUser.maxEquationNumber
                    equation = "Введённый полином: $p1 = 0"
                    solution = "Решение: x=${p1.solve()}"
                    user = currentUser
                }
            }
        }
        thisConnection.session.send("Введённый полином: $p1 = 0")
        thisConnection.session.send("Решение: x=${p1.solve()}")
        sendAvailableCommandsLoggedIn(thisConnection)
    } else {
        thisConnection.session.send("You are not logged in. Register first or enter a valid username and password to perform this operation")
        sendAvailableCommandsNotLoggedIn(thisConnection)
    }
}

fun isNumeric(str: String): Boolean = str
    .removePrefix("-")
    .removePrefix("+")
    .all { it in '0'..'9' }