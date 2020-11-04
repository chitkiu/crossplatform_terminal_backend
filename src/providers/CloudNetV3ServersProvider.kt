package backend.providers

import backend.data.CloudNetV3Model
import backend.data.UserModel
import backend.data.database.CloudNetV3
import backend.data.database.Teams
import backend.data.database.Users
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class CloudNetV3ServersProvider {

    fun getServers(user: UserModel): List<CloudNetV3Model> {
        return transaction {
            val userServers = CloudNetV3.select {
                CloudNetV3.userId eq user.id.toString()
            }.map {
                CloudNetV3Model(
                        id = UUID.fromString(it[CloudNetV3.id]),
                        name = it[CloudNetV3.name],
                        username = it[CloudNetV3.username],
                        password = it[CloudNetV3.password],
                        serverUrl = it[CloudNetV3.serverUrl],
                        serverPort = it[CloudNetV3.serverPort],
                        screenPort = it[CloudNetV3.screenPort],
                        useSsl = it[CloudNetV3.useSsl],
                )
            }
            val teamServers = user.teamId?.let { teamId ->
                (Teams innerJoin Users)
                        .slice(Teams.name, Users.id)
                        .select {
                            Teams.id eq teamId.toString() and (Users.teamId eq teamId.toString())
                        }.map { resultUser ->
                            CloudNetV3.select {
                                CloudNetV3.userId eq resultUser[Users.id]
                            }.map {
                                CloudNetV3Model(
                                        id = UUID.fromString(it[CloudNetV3.id]),
                                        name = "${it[CloudNetV3.name]} (team: ${resultUser[Teams.name]})",
                                        serverUrl = it[CloudNetV3.serverUrl],
                                        serverPort = it[CloudNetV3.serverPort],
                                        screenPort = it[CloudNetV3.screenPort],
                                        useSsl = it[CloudNetV3.useSsl],
                                        //Force null because we shouldn't send any auth data from other user(important data)
                                        username = null,
                                        password = null
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

    fun putServer(user: UserModel, server: CloudNetV3Model) {
        transaction {
            CloudNetV3.insert {
                it[id] = UUID.randomUUID().toString()
                it[name] = server.name
                it[username] = server.username ?: ""
                it[password] = server.password ?: ""
                it[serverUrl] = server.serverUrl
                it[serverPort] = server.serverPort
                it[screenPort] = server.screenPort
                it[useSsl] = server.useSsl
                it[userId] = user.id.toString()
            }
        }
    }

    fun removeServer(user: UserModel, id: UUID): Boolean {
        return transaction {
            CloudNetV3.deleteWhere { CloudNetV3.id eq id.toString() and (CloudNetV3.userId eq user.id.toString()) } > 0
        }
    }

}