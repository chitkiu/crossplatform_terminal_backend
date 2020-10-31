package backend.data

import backend.data.database.AuthServer
import org.jetbrains.exposed.sql.ResultRow
import java.util.*

data class AuthModel(
    val id: UUID?,
    val name: String,
    val username: String,
    val password: String?,
    val privateKey: String?
) {
    companion object {
        fun getFromDB(row: ResultRow): AuthModel {
            return AuthModel(
                    id = UUID.fromString(row[AuthServer.id]),
                    name = row[AuthServer.name],
                    username = row[AuthServer.username],
                    password = row[AuthServer.password],
                    privateKey = row[AuthServer.privateKey]
            )
        }
    }
}