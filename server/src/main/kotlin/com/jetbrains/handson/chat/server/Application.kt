package com.jetbrains.handson.chat.server

import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.util.*


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused")
fun Application.module() {
    install(WebSockets)
    Database.connect("jdbc:postgresql://localhost:5432/postgres", driver = "org.postgresql.Driver",
        user = "postgres", password = "gfhjkm2000")
    transaction {     SchemaUtils.create (Users)
    SchemaUtils.create(Equations)}
    routing {
        val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())
        webSocket("/chat") {
            println("Adding user!")
            var solvingHistory = emptyArray<Polynomial>()
            val thisConnection = Connection(this)
            connections += thisConnection

            try {

//                send("You are connected! There are ${connections.count()} users here.")
                send("You are connected!\n")

                send("Available commands:")
                send("register LOGIN PASSWORD - to register into system")
                send("login LOGIN PASSWORD - to login into system")


                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val receivedText = frame.readText()
                    val params = receivedText.split(" ").toTypedArray()
                    if (params[0] == "register"){
                        val user = findUserByUsername(params[1]);
                        if (user != null){
                            send("User with that username already exists")
                            send("Available commands:")
                            send("register LOGIN PASSWORD - to register into system")
                            send("login LOGIN PASSWORD - to login into system")
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
                            send("You successfully registered")
                            send("Available commands:")
                            send("register LOGIN PASSWORD - to register into system")
                            send("login LOGIN PASSWORD - to login into system")
                        }

                    }
                    else if (params[0] == "login"){
                        val user = findUserByUsername(params[1]);
                        // TODO как-то запомнить в сессию id пользователя
                        if (user == null){
                            send("No user with that username")
                            send("Available commands:")
                            send("register LOGIN PASSWORD - to register into system")
                            send("login LOGIN PASSWORD - to login into system")
                        }
                        else {
                            val password = transaction { user.password };
                            if (BCrypt.checkpw(params[2], password)) {
                                thisConnection.name = params[1];
                                thisConnection.registered = true
                                send("You are logged in")
                                send("Available commands:")
                                send("show - to show solve history")
                                send("find eqNum - to show equation w/ index eqNum")
                                send("Enter polynomial's coeffs to start solving")
                            } else {
                                send("Incorrect password")
                                send("Available commands:")
                                send("register LOGIN PASSWORD - to register into system")
                                send("login LOGIN PASSWORD - to login into system")
                            }
                        }


                    }
                    else if(params[0] == "show"){

                        if (thisConnection.registered) {
                            val currentUser = findUserByUsername(thisConnection.name)
                            val userEquations = transaction { currentUser!!.equations .toList()}

                            if(userEquations.isEmpty()){
                                send("You have no saved equations here :(")
                            }else {
                                userEquations.forEachIndexed { index, equation ->
                                    val currentEquation = transaction { equation.equation }
                                    val currentSolution = transaction { equation.solution }
                                    send("${index+1}. $currentEquation = 0, solution: x=${currentSolution}")
                                }
                            }
                        }

                    }else if(params[0] == "find"){
                        if(solvingHistory.isEmpty()){
                            send("You can't use 'find' command because your solving history is empty!")
                        }else if(kotlin.math.abs(params[1].toInt()) > solvingHistory.size || params[1].toInt() < 1){
                            send("Please, enter a valid equation number!")
                        }else{
                            send("Selected equation: ${solvingHistory[params[1].toInt() - 1].toString()} = 0, solution: x=${solvingHistory[params[1].toInt() - 1].solve()}")

                        }
                    }else{
                        val intParams = params.map { it.toInt() }.toTypedArray()
                        val p1 = Polynomial(intParams)
                        solvingHistory += p1
                        val currentUser = findUserByUsername(thisConnection.name)
                        currentUser?. let {
                            transaction {
                            currentUser.maxEquationNumber += 1
                            Equation.new {
                                number = currentUser.maxEquationNumber
                                equation = "Введённый полином: ${p1.toString()} = 0"
                                solution = "Решение: x=${p1.solve()}"
                                user = currentUser
                            }
                            }
                        }


                        val strPolynom = "Введённый полином: ${p1.toString()} = 0"
                        val solutions = "Решение: x=${p1.solve()}"


                        connections.forEach {
                            it.session.send(strPolynom)
                            it.session.send(solutions)
                        }
                    }

                }
            } catch (e: Exception) {
                println(e.localizedMessage)
            }catch(e: NullPointerException){
                println("Your command is incorrect!")
            } finally {
                println("Removing $thisConnection!")
                connections -= thisConnection
            }
        }

    }
}


