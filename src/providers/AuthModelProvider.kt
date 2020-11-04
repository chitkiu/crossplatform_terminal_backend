package backend.providers

import backend.data.AuthModel
import backend.data.UserModel
import backend.data.database.AuthServer
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class AuthModelProvider {

    fun getAuths(userModel: UserModel): List<AuthModel> {
        return transaction {
            AuthServer.select {
                AuthServer.userId eq userModel.id.toString()
            }
                    .map(AuthModel.Companion::getFromDB)
        }
    }

    fun putAuth(user: UserModel, authModel: AuthModel) {
        return transaction {
            AuthServer.insert {
                it[id] = UUID.randomUUID().toString()
                it[name] = authModel.name
                it[username] = authModel.username
                it[password] = authModel.password
                it[privateKey] = authModel.privateKey
                it[userId] = user.id.toString()
            }
        }
    }

    fun removeAuth(user: UserModel, id: UUID): Boolean {
        return transaction {
            AuthServer.deleteWhere { AuthServer.id eq id.toString() and (AuthServer.userId eq user.id.toString()) } > 0
        }
    }

}