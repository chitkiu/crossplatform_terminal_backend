package backend.repositories

import backend.data.UserModel
import backend.data.database.Users
import io.ktor.auth.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.MessageDigest
import java.util.*

class UserRepository {

    fun getUserByCredential(userCredential: UserPasswordCredential): UserModel? {
        return transaction {
            Users.select {
                Users.name eq userCredential.name and (Users.password eq hashPassword(userCredential.password))
            }.singleOrNull()?.let { result ->
                UserModel(
                        id = UUID.fromString(result[Users.id]),
                        name = result[Users.name],
                        passwordHash = result[Users.password],
                        teamId = result[Users.teamId]?.let(UUID::fromString)
                )
            }
        }
    }

    fun getUserById(id: UUID): UserModel? {
        return transaction {
            Users.select {
                Users.id eq id.toString()
            }.singleOrNull()?.let { result ->
                UserModel(
                        id = UUID.fromString(result[Users.id]),
                        name = result[Users.name],
                        passwordHash = result[Users.password],
                        teamId = result[Users.teamId]?.let(UUID::fromString)
                )
            }
        }
    }

    //TODO: Add check is team exist
    fun setTeamForUser(userModel: UserModel, teamId: UUID) {
        return transaction {
            Users.update({ Users.id eq userModel.id.toString() }) {
                it[Users.teamId] = stringLiteral(teamId.toString())
            }
        }
    }

    fun removeTeam(userModel: UserModel) {
        return transaction {
            Users.update({ Users.id eq userModel.id.toString() }) {
                it[teamId] = null
            }
        }
    }

    fun registerUser(username: String, pass: String): UserModel? {
        return transaction {
            val empty = Users.select { Users.name eq username }.toList().isEmpty()
            if(empty) {
                val user = Users.insert {
                    it[id] = UUID.randomUUID().toString()
                    it[name] = username
                    it[password] = hashPassword(pass)
                }
                UserModel(
                        id = UUID.fromString(user[Users.id]),
                        name = user[Users.name],
                        passwordHash = user[Users.password],
                        teamId = user[Users.teamId]?.let(UUID::fromString)
                )
            } else {
                null
            }
        }
    }

    private fun hashPassword(pass: String): String {
        val bytes = pass.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("", { str, it -> str + "%02x".format(it) })
    }
}
