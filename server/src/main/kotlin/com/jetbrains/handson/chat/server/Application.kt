package com.jetbrains.handson.chat.server

import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.util.Identity.decode
import io.ktor.websocket.*
import java.lang.Math.abs
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
                send("find eqNum - to show equation w/ index eqNum")
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
