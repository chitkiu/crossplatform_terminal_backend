package backend.providers

import backend.Success
import backend.data.AuthModel
import backend.data.ServerModel
import backend.data.ServerModelRequest
import backend.data.UserModel
import backend.data.database.AuthServer
import backend.data.database.SSHServer
import backend.data.database.Teams
import backend.data.database.Users
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class SSHServersProvider {

    fun getServers(userModel: UserModel): List<ServerModel> {
        return transaction {
            val userServers = SSHServer.select {
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
                        auth = if (authId.isNullOrEmpty())
                            null
                        else
                            AuthServer.select { AuthServer.id eq authId }
                                    .single()
                                    .let(AuthModel.Companion::getFromDB)
                )
            }
            val teamServers = userModel.teamId?.let { teamId ->
                (Teams innerJoin Users)
                        .slice(Teams.name, Users.id)
                        .select {
                            Teams.id eq teamId.toString() and (Users.teamId eq teamId.toString())
                        }.map { resultUser ->
                            SSHServer.select {
                                SSHServer.userId eq resultUser[Users.id]
                            }.map {
                                ServerModel(
                                        id = UUID.fromString(it[SSHServer.id]),
                                        name = "${it[SSHServer.name]} (team: ${resultUser[Teams.name]})",
                                        host = it[SSHServer.host],
                                        port = it[SSHServer.port],
                                        //Force null because we shouldn't send any auth data from other user(important data)
                                        username = null,
                                        password = null,
                                        auth = null
                                )
                            }
                        }
                        .flatten()
            } ?: emptyList()

            val resultList = userServers.toMutableList()
            teamServers
                    .filter { !resultList.any { item -> item.id == it.id } }
                    .let(resultList::addAll)
            resultList
        }
    }

    fun putServer(userModel: UserModel, server: ServerModelRequest) {
        transaction {
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

    fun removeServer(user: UserModel, id: UUID): Boolean {
        return transaction {
            SSHServer.deleteWhere { SSHServer.id eq id.toString() and (SSHServer.userId eq user.id.toString()) } > 0
        }
    }
}
