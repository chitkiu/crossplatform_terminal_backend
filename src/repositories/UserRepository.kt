package backend.repositories

import backend.data.UserModel
import backend.data.database.Users
import io.ktor.auth.UserPasswordCredential
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.MessageDigest
import java.util.*

class UserRepository {

    fun getUserByCredential(userCredential: UserPasswordCredential): UserModel? {
        return transaction {
            SchemaUtils.create(Users)
            Users.select {
                Users.name eq userCredential.name and (Users.password eq hashPassword(userCredential.password))
            }.singleOrNull()?.let { result ->
                UserModel(
                    id = UUID.fromString(result[Users.id]),
                    name =  result[Users.name],
                    passwordHash = result[Users.password]
                )
            }
        }
    }

    fun getUserById(id: UUID): UserModel? {
        return transaction {
            SchemaUtils.create(Users)
            Users.select {
                Users.id eq id.toString()
            }.singleOrNull()?.let { result ->
                UserModel(
                    id = UUID.fromString(result[Users.id]),
                    name = result[Users.name],
                    passwordHash = result[Users.password]
                )
            }
        }
    }

    fun registerUser(username: String, pass: String): UserModel? {
        return transaction {
            SchemaUtils.create(Users)
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
