package dev.kason.konfig.test

import dev.kason.konfig.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Duration

class ConfigTest : StringSpec({
    "get test" {
        val config = createConfig {
            +ConfigSource.Parse(
                """
                {
                    "a": 1,
                    "b": "hello",
                    "c": {
                        "d": 2
                    }
                }
            """.trimIndent()
            )
        }

        config.get<Int>("a") shouldBe 1
        config.get<String>("b") shouldBe "hello"
        config.get<Int>("c.d") shouldBe 2
        config.get<Int>("c.e") shouldBe null
    }

    "require test" {
        val config = createConfig {
            +ConfigSource.Parse(
                """
                {
                    "a": 1,
                    "b": "hello",
                    "c": {
                        "d": 2
                    }
                }
            """.trimIndent()
            )
        }

        config.require<Int>("a") shouldBe 1
        config.require<String>("b") shouldBe "hello"
        config.require<Int>("c.d") shouldBe 2
        shouldThrow<IllegalArgumentException> {
            config.require<Int>("c.e")
        }
    }

    "get type testing" {
        // test Int, String, Double, Boolean, Long, Duration
        val config = createConfig {
            +ConfigSource.Parse(
                """
                {
                    "a": 1,
                    "b": "hello",
                    "c": 1.0,
                    "d": true,
                    "e": 1,
                    "f": "1s"
                }
            """.trimIndent()
            )
        }

        config.get<Int>("a") shouldBe 1
        config.get<String>("b") shouldBe "hello"
        config.get<Double>("c") shouldBe 1.0
        config.get<Boolean>("d") shouldBe true
        config.get<Long>("e") shouldBe 1L
        config.get<Duration>("f") shouldBe Duration.ofSeconds(1)
    }

    "create config" {
        val mapConfig = createConfig {
            +ConfigSource.Parse(
                mapOf(
                    "a" to 1,
                    "b" to "hello",
                    "c" to mapOf(
                        "d" to 2
                    )
                )
            )
        }
        val stringConfig = createConfig {
            +ConfigSource.Parse(
                """
                {
                    "a": 1,
                    "b": "hello",
                    "c": {
                        "d": 2
                    }
                }
            """.trimIndent()
            )
        }
        mapConfig shouldBe stringConfig
    }
    "config reader" {
        val reader = """
            {
                "a": 1,
                "b": "hello",
                "c": {
                    "d": 2
                }
            }
        """.trimIndent().reader()
        val config = createConfig {
            +ConfigSource.Read(reader)
        }

        config.get<Int>("a") shouldBe 1
        config.get<String>("b") shouldBe "hello"
        config.get<Int>("c.d") shouldBe 2
        config.get<Int>("c.e") shouldBe null

    }

    "config list" {
        val config = createConfig {
            +ConfigSource.Parse(
                """
                    a = [sdf, 23d2s, 3as4]
                """.trimIndent()
            )
        }
        config.get<List<Int>>("a") shouldBe null
        config.get<List<String>>("a") shouldBe listOf("sdf", "23d2s", "3as4")
    }
})