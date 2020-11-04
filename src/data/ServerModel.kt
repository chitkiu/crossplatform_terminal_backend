package backend.data

import java.util.*

data class ServerModel(
    val id: UUID?,
    val name: String,
    val host: String,
    val port: Int,
    val username: String?,
    val password: String?,
    val auth: AuthModel?
)

data class ServerModelRequest(
    val name: String,
    val host: String,
    val port: Int,
    val username: String?,
    val password: String?,
    val authId: String?
)