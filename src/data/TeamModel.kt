package backend.data

import java.util.*

data class TeamModel(
        val id: UUID,
        val name: String,
        val ownerId: UUID
)