package backend.data

import java.util.*

data class CloudNetV3Model(
    val id: UUID?,
    val name: String,
    val username: String?,
    val password: String?,
    val serverUrl: String,
    val serverPort: Int,
    val screenPort: Int,
    val useSsl: Boolean
)