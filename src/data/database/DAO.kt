package backend.data.database

import org.jetbrains.exposed.sql.Table

object Users: Table() {
    val id = varchar("id", length = 36)
    val name = varchar("name", length = 32)
    val password = varchar("password", length = 64)
    val teamId = (varchar("team_id", length = 36) references Teams.id).nullable()

    override val primaryKey = PrimaryKey(id, name = "PK_Users_ID")
}

object Teams: Table() {
    val id = varchar("id", length = 36)
    val name = varchar("name", length = 32)
    val ownerId = (varchar("ownerId", length = 36) references Users.id)

    override val primaryKey = PrimaryKey(id, name = "PK_Teams_ID")
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
