package backend

import backend.data.*
import backend.data.database.*
import backend.providers.CloudNetV3ServersProvider
import backend.repositories.TeamRepository
import backend.repositories.UserRepository
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.*
import io.ktor.auth.jwt.jwt
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import java.util.*

fun main(args: Array<String>): Unit = io.ktor.server.jetty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    //SQLite
    Database.connect("jdbc:sqlite:./data.db", "org.sqlite.JDBC")
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    transaction {
        SchemaUtils.create(Users, Teams, CloudNetV3)
//        SchemaUtils.createMissingTablesAndColumns(Users, Teams, SSHServer, AuthServer, CloudNetV3)
    }

    install(ContentNegotiation) {
        gson {
        }
    }

    install(CORS) {
        method(HttpMethod.Options)
        header(HttpHeaders.XForwardedProto)
        header(HttpHeaders.Authorization)
        anyHost()
        allowCredentials = true
        allowNonSimpleContentTypes = true
    }

    val userRepository = UserRepository()
    val teamRepository = TeamRepository()

    install(Authentication) {
        /**
         * Setup the JWT authentication to be used in [Routing].
         * If the token is valid, the corresponding [User] is fetched from the database.
         * The [User] can then be accessed in each [ApplicationCall].
         */
        jwt {
            verifier(JwtConfig.verifier)
//            realm = "ktor.io"
            validate {
                userRepository.getUserById(
                        id = UUID.fromString(it.payload.getClaim("id").asString())
                )
            }
        }
    }


    /*val client = HttpClient(Jetty) {
        engine {
            sslContextFactory = SslContextFactory()
        }
    }*/

    routing {

        get("/") {
            call.respond(
                    Success(
                            true
                    )
            )
        }

        post("login") {
            val credentials = call.receive<UserPasswordCredential>()
            val user = userRepository.getUserByCredential(credentials)
            val response = user?.let {
                val token = JwtConfig.makeToken(user)
                JWTToken(token)
            } ?: Error("Cannot auth")

            call.respond(response)
        }

        post("register") {
            val credentials = call.receive<UserPasswordCredential>()
            val user = userRepository.registerUser(credentials.name, credentials.password)
            val response = user?.let {
                val token = JwtConfig.makeToken(user)
                JWTToken(token)
            } ?: Error("User already exist")

            call.respond(response)
        }

        post("validate") {
            val authHeader = call.request.headers["Authorization"]
            if (authHeader.isNullOrBlank()) {
                call.respond(
                        Success(false)
                )
            } else {
                try {
                    val jwt = authHeader.split("Bearer ")[1]
                    if (jwt.isBlank()) {
                        call.respond(
                                Success(false)
                        )
                    } else {
                        JwtConfig.verifier.verify(jwt)
                        call.respond(
                                Success(true)
                        )
                    }
                } catch (e: Throwable) {
                    call.respond(
                            Success(false)
                    )
                }
            }
        }

        authenticate {
            route("teams") {
                post {
                    val user = call.user!!
                    val teamNameWrapper = call.receive<NameWrapper>()
                    val team = teamRepository.saveTeam(user, teamNameWrapper.name)
                    call.respond(
                            team ?: Error("Cannot create team")
                    )
                }

                delete("{teamId}") {
                    val user = call.user!!
                    call.parameters["teamId"]?.let { teamId ->
                        try {
                            if(teamRepository.deleteTeam(user, UUID.fromString(teamId))) {
                                call.respond(
                                        Success(true)
                                )
                            } else {
                                call.respond(
                                        Success(false)
                                )
                            }
                        } catch (e: Throwable) {
                            call.respond(
                                    Success(false)
                            )
                        }
                    } ?: call.respond(
                            Success(false)
                    )
                }
            }

            route("team") {
                get {
                    val user = call.user!!
                    teamRepository.getTeamByUser(user)?.let {
                        call.respond(it)
                    } ?: call.respond(Error("Team is on assign to current user"))
                }

                put {
                    val user = call.user!!
                    try {
                        val teamIdWrapper = call.receive<IDWrapper>()
                        userRepository.setTeamForUser(user, UUID.fromString(teamIdWrapper.id))
                        call.respond(
                                Success(true)
                        )
                    } catch (e: Throwable) {
                        call.respond(
                                Success(false)
                        )
                    }
                }

                delete {
                    val user = call.user!!
                    try {
                        userRepository.removeTeam(user)
                        call.respond(
                                Success(true)
                        )
                    } catch (e: Throwable) {
                        call.respond(
                                Success(false)
                        )
                    }
                }
            }

            route("cloudnet") {
                val cloudNet = CloudNetV3ServersProvider()

                get {
                    val user = call.user!!
                    call.respond(
                        cloudNet.getServers(user)
                    )
                }

                put {
                    val user = call.user!!
                    val cloudNetModel = call.receive<CloudNetV3Model>()
                    cloudNet.putServer(user, cloudNetModel)
                    call.respond(
                        Success(true)
                    )
                }

                delete("{cloudNetId}") {
                    val user = call.user!!
                    call.parameters["cloudNetId"]?.let { serverId ->
                        try {
                            if(cloudNet.removeServer(user, UUID.fromString(serverId))) {
                                call.respond(
                                        Success(true)
                                )
                            } else {
                                call.respond(
                                        Success(false)
                                )
                            }
                        } catch (e: Throwable) {
                            call.respond(
                                Success(false)
                            )
                        }
                    } ?: call.respond(
                        Success(false)
                    )
                }
            }
        }
    }
}

data class Success(
        val success: Boolean
)

data class JWTToken(
        val token: String
)

data class Error(
        val error: String
)

data class IDWrapper(
        val id: String
)

data class NameWrapper(
        val name: String
)

val ApplicationCall.user get() = authentication.principal<UserModel>()
