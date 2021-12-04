package com.jetbrains.handson.chat.server

import io.ktor.http.cio.websocket.*

suspend fun sendAvailableCommandsLoggedIn(connection: Connection){
    connection.session.send("Available commands:")
    connection.session.send("show - to show solve history")
    connection.session.send("find eqNum - to show equation w/ index eqNum")
    connection.session.send("Enter polynomial's coeffs to start solving")
}

suspend fun sendAvailableCommandsNotLoggedIn(connection: Connection){
    connection.session.send("Available commands:")
    connection.session.send("register LOGIN PASSWORD - to register into system")
    connection.session.send("login LOGIN PASSWORD - to login into system")
}
