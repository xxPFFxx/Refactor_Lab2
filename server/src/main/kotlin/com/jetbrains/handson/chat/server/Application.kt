package com.jetbrains.handson.chat.server

import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.lang.NumberFormatException
import java.util.*


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused")
fun Application.module() {
    install(WebSockets)
    Database.connect("jdbc:postgresql://localhost:5432/postgres", driver = "org.postgresql.Driver",
        user = "", password = "")
    transaction {     SchemaUtils.create (Users)
    SchemaUtils.create(Equations)}
    routing {
        val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())
        webSocket("/solver") {
            println("Adding user!")
            val thisConnection = Connection(this)
            connections += thisConnection
            try {
                send("You are connected! There are ${connections.count()} users here.")
                sendAvailableCommandsNotLoggedIn(thisConnection)
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val receivedText = frame.readText()
                    val params = receivedText.split(" ").toTypedArray()
                    when (params[0]){
                        "register" -> registerCommand(params, thisConnection)
                        "login" -> loginCommand(params, thisConnection)
                        "show" -> showCommand(thisConnection)
                        "find" -> findCommand(params, thisConnection)
                        else -> {
                            solveCommand(params, thisConnection)
                        }
                    }
                }
            }
            catch (e: Exception) {
                println(e.localizedMessage)
            }catch(e: NullPointerException){
                println("Your command is incorrect!")
            }
        }

    }
}
