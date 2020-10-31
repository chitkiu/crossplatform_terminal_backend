package backend.data.database

import backend.data.database.CloudNetV3.references
import backend.data.database.SSHServer.references
import org.jetbrains.exposed.sql.Table

object Users: Table() {
    val id = varchar("id", length = 36)
    val name = varchar("name", length = 32)
    val password = varchar("password", length = 64)

    override val primaryKey = PrimaryKey(id, name = "PK_Users_ID")
}

object CloudNetV3: Table() {
    val id = varchar("id", length = 36)
    val name = varchar("name", length = 32)
    val username = varchar("username", length = 32)
    val password = varchar("password", length = 32)
    val serverUrl = varchar("serverUrl", length = 32)
    val serverPort = integer("serverPort")
    val screenPort = integer("screenPort")
    val useSsl = bool("useSsl")

    val userId = (varchar("userId", length = 36) references Users.id)

    override val primaryKey = PrimaryKey(id, name = "PK_CloudNetV3_ID")
}

object AuthServer: Table() {
    val id = varchar("id", length = 36)
    val name = varchar("name", length = 32)
    val username = varchar("username", length = 32)
    val password = varchar("password", length = 32).nullable()
    val privateKey = varchar("privateKey", length = 2200).nullable()
    val userId = (varchar("userId", length = 36) references Users.id)

    override val primaryKey = PrimaryKey(id, name = "PK_AuthServer_ID")
}

object SSHServer: Table() {
    val id = varchar("id", length = 36)
    val name = varchar("name", length = 32)
    val host = varchar("host", length = 32)
    val port = integer("port")
    val username = varchar("username", length = 32).nullable()
    val password = varchar("password", length = 32).nullable()
    val authId = (varchar("authId", length = 36) references AuthServer.id).nullable()
    val userId = (varchar("userId", length = 36) references Users.id)

    override val primaryKey = PrimaryKey(id, name = "PK_SSHServer_ID")
}
