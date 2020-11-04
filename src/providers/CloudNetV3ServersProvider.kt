package backend.providers

import backend.Success
import backend.data.CloudNetV3Model
import backend.data.UserModel
import backend.data.database.CloudNetV3
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class CloudNetV3ServersProvider {

    fun getServers(user: UserModel): List<CloudNetV3Model> {
        return transaction {
            CloudNetV3.select {
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
        }
    }

    fun putServer(user: UserModel, server: CloudNetV3Model) {
        transaction {
            CloudNetV3.insert {
                it[id] = UUID.randomUUID().toString()
                it[name] = server.name
                it[username] = server.username
                it[password] = server.password
                it[serverUrl] = server.serverUrl
                it[serverPort] = server.serverPort
                it[screenPort] = server.screenPort
                it[useSsl] = server.useSsl
                it[userId] = user.id.toString()
            }
        }
    }

    fun removeServer(user: UserModel, id: UUID): Success {
        return transaction {
            val deleteCount = CloudNetV3.deleteWhere { CloudNetV3.id eq id.toString() and (CloudNetV3.userId eq user.id.toString()) }
            if(deleteCount > 0) {
                Success(true)
            } else {
                Success(false)
            }
        }
    }

}