package backend.data

import io.ktor.auth.Principal
import java.util.*

data class UserModel(
        val id: UUID,
        val name: String,
        val passwordHash: String,
        val teamId: UUID?
) : Principal
