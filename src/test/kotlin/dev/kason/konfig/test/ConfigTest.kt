package dev.kason.konfig.test

import com.typesafe.config.Config
import dev.kason.konfig.*
import java.util.*
import kotlin.reflect.KProperty

fun main() {
    val config = createConfig {
        +Parse(
            """
            uuid = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
        """.trimIndent()
        )
    }

    val uuid: UUID by config
    println(uuid)
}