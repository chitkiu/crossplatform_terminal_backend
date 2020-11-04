package backend.providers

import backend.Success
import backend.data.AuthModel
import backend.data.ServerModel
import backend.data.ServerModelRequest
import backend.data.UserModel
import backend.data.database.AuthServer
import backend.data.database.SSHServer
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class SSHServersProvider {

    fun getServers(userModel: UserModel): List<ServerModel> {
        return transaction {
            SchemaUtils.create(SSHServer)

            SSHServer.select {
                SSHServer.userId eq userModel.id.toString()
            }.map {
                val authId = it[SSHServer.authId]
                ServerModel(
                        id = UUID.fromString(it[SSHServer.id]),
                        name = it[SSHServer.name],
                        host = it[SSHServer.host],
                        port = it[SSHServer.port],
                        username = it[SSHServer.username],
                        password = it[SSHServer.password],
                        auth = if(authId.isNullOrEmpty())
                            null
                        else
                            AuthServer.select { AuthServer.id eq authId }
                                    .single()
                                    .let(AuthModel.Companion::getFromDB)
                )
            }
        }
    }

    fun putServer(userModel: UserModel, server: ServerModelRequest) {
        transaction {
            SchemaUtils.create(SSHServer)

            SSHServer.insert {
                it[id] = UUID.randomUUID().toString()
                it[name] = server.name
                it[host] = server.host
                it[port] = server.port
                it[username] = server.username
                it[password] = server.password
                it[userId] = userModel.id.toString()
                it[authId] = server.authId
            }
        }
    }

    fun removeServer(user: UserModel, id: UUID): Success {
        return transaction {
            SchemaUtils.create(SSHServer)
            val deleteCount = SSHServer.deleteWhere { SSHServer.id eq id.toString() and (SSHServer.userId eq user.id.toString()) }
            if(deleteCount > 0) {
                Success(true)
            } else {
                Success(false)
            }
        }
    }
}
