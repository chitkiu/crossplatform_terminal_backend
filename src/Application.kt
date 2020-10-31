package backend

import backend.data.*
import backend.providers.AuthModelProvider
import backend.providers.CloudNetV3ServersProvider
import backend.providers.SSHServersProvider
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
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.sql.Connection
import java.util.*

fun main(args: Array<String>): Unit = io.ktor.server.jetty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    //SQLite
    Database.connect("jdbc:sqlite:./data.db", "org.sqlite.JDBC")
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

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

        authenticate {
            val cloudNet = CloudNetV3ServersProvider()

            route("cloudnet") {
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
                            call.respond(
                                cloudNet.removeServer(user, UUID.fromString(serverId))
                            )
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

            route("auth") {
                val auths = AuthModelProvider()

                get {
                    val user = call.user!!
                    call.respond(
                            auths.getAuths(user)
                    )
                }

                put {
                    val user = call.user!!
                    val authModel = call.receive<AuthModel>()
                    auths.putAuth(user, authModel)
                    call.respond(
                            Success(true)
                    )
                }

                delete("{authId}") {
                    val user = call.user!!
                    call.parameters["authId"]?.let { authId ->
                        try {
                            call.respond(
                                    auths.removeAuth(user, UUID.fromString(authId))
                            )
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

            route("sshserver") {
                val servers = SSHServersProvider()
                get {
                    val user = call.user!!
                    call.respond(
                            servers.getServers(user)
                    )
                }

                put {
                    val user = call.user!!
                    val serverModel = call.receive<ServerModelRequest>()
                    servers.putServer(user, serverModel)
                    call.respond(
                            Success(true)
                    )
                }

                delete("{serverId}") {
                    val user = call.user!!
                    call.parameters["serverId"]?.let { authId ->
                        try {
                            call.respond(
                                    servers.removeServer(user, UUID.fromString(authId))
                            )
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

            /*get("/servers") {
                call.respond(
                        servers.getServersListWithAuthModel(0)
                )
            }
            */
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

val ApplicationCall.user get() = authentication.principal<UserModel>()
