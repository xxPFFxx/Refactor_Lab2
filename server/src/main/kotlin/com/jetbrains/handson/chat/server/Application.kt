package com.jetbrains.handson.chat.server

import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.util.Identity.decode
import io.ktor.websocket.*
import java.util.*



fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused")
fun Application.module() {
    install(WebSockets)
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
                send("show - to show solve history")
                send("Enter polynomial's coeffs to start solving")

                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val receivedText = frame.readText()
                    val params = receivedText.split(" ").toTypedArray()

                    if(params[0] == "show"){
                        if(solvingHistory.isEmpty()){
                            send("You have no saved equations here :(")
                        }else {
                            solvingHistory.forEachIndexed { index, equation ->
                                send("${index+1}. ${equation.toString()} = 0, solution: x=${equation.solve()}")
                            }
                        }
                    }else{
                        val intParams = params.map { it.toInt() }.toTypedArray()
                        val p1 = Polynomial(intParams)

                        solvingHistory += p1

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
            } finally {
                println("Removing $thisConnection!")
                connections -= thisConnection
            }
        }
    }
}
