package backend.repositories

import backend.data.TeamModel
import backend.data.UserModel
import backend.data.database.Teams
import backend.data.database.Users
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class TeamRepository {

    fun getTeamByUser(userModel: UserModel): TeamModel? {
        return transaction {
            Users.select {
                Users.id eq userModel.id.toString()
            }.singleOrNull()?.let { result ->
                result[Users.teamId]?.let { teamId ->
                    Teams.select {
                        Teams.id eq teamId
                    }.singleOrNull()?.let { result ->
                        return@transaction TeamModel(
                                id = UUID.fromString(result[Teams.id]),
                                name = result[Teams.name],
                                ownerId = UUID.fromString(result[Teams.ownerId])
                        )
                    }
                }
            }
            return@transaction null
        }
    }

    fun saveTeam(userModel: UserModel, teamName: String): TeamModel? {
        return transaction {
            try {
                val team = Teams.insert {
                    it[id] = UUID.randomUUID().toString()
                    it[name] = teamName
                    it[ownerId] = userModel.id.toString()
                }
                Users.update({ Users.id eq userModel.id.toString()}) {
                    it[teamId] = team[Teams.id]
                }
                TeamModel(
                        id = UUID.fromString(team[Teams.id]),
                        name = team[Teams.name],
                        ownerId = UUID.fromString(team[Teams.ownerId])
                )
            } catch (e: Throwable) {
                null
            }
        }
    }

    fun deleteTeam(userModel: UserModel, teamId: UUID): Boolean {
        return transaction {
            val deleteTeamResult = Teams.deleteWhere { Teams.id eq teamId.toString() and (Teams.ownerId eq userModel.id.toString()) } > 0
            if(deleteTeamResult) {
                Users.update({ Users.teamId eq teamId.toString() }) {
                    it[Users.teamId] = null
                }
            }
            deleteTeamResult
        }
    }

}