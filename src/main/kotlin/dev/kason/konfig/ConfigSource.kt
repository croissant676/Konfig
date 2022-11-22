package dev.kason.konfig

import com.typesafe.config.*
import java.io.*
import java.net.URL
import java.nio.file.Path
import java.util.*

/**
 * A config source represents an object or function that can provide a [Config] object.
 *
 * ConfigSources can be added together in the [createConfig] to create a [Config].
 *
 * @see createConfig
 * */
fun interface ConfigSource {
    /**
     * Creates a [Config] instance.
     * */
    fun load(): Config

    /**
     * DSL for creating a [Config] easily.
     *
     * @see createConfig
     * */
    class Dsl {
        private val sources = mutableListOf<ConfigSource>()

        /**
         * Adds this [ConfigSource] to the list of sources used in this DSL.
         *
         * When the DSL is built, the [ConfigSource]s will be evaluated in the order that
         * they are added.
         * */
        operator fun ConfigSource.unaryPlus() {
            sources.add(this)
        }

        /**
         * A [ConfigSource] that provides a [Config] from System properties.
         *
         * @see ConfigFactory.systemProperties
         * */
        val SystemProperties = ConfigSource { ConfigFactory.systemProperties() }

        /**
         * A [ConfigSource] that provides a [Config] from System environment variables.
         *
         * @see ConfigFactory.systemEnvironment
         * */
        val EnvironmentVariables = ConfigSource { ConfigFactory.systemEnvironment() }

        /**
         * A [ConfigSource] that provides a [Config] from a [File].
         *
         * @see ConfigFactory.parseFile
         * */
        fun File(file: File, failIfMissing: Boolean = true): ConfigSource = ConfigSource {
            ConfigFactory.parseFile(file, ConfigParseOptions.defaults().setAllowMissing(!failIfMissing))
        }

        /**
         * A [ConfigSource] that provides a [Config] from a [URL].
         *
         * @see ConfigFactory.parseURL
         * */
        fun Url(url: URL, failIfMissing: Boolean = true): ConfigSource = ConfigSource {
            ConfigFactory.parseURL(url, ConfigParseOptions.defaults().setAllowMissing(!failIfMissing))
        }

        // reader
        /**
         * A [ConfigSource] that provides a [Config] from a [Reader].
         *
         * @see ConfigFactory.parseReader
         * */
        fun Read(reader: Reader): ConfigSource = ConfigSource { ConfigFactory.parseReader(reader) }

        /**
         * A [ConfigSource] that provides a [Config] from a [InputStream].
         * First converts the [inputStream] into a [Reader] using [InputStreamReader].
         *
         * @see ConfigFactory.parseReader
         * */
        fun Read(inputStream: InputStream): ConfigSource =
            ConfigSource { ConfigFactory.parseReader(inputStream.reader()) }

        /**
         * A [ConfigSource] that provides a [Config] from a [Map].
         *
         * @see ConfigFactory.parseMap
         */
        fun Parse(map: Map<String, Any>): ConfigSource = ConfigSource { ConfigFactory.parseMap(map) }

        /**
         * A [ConfigSource] that provides a [Config] from a [Properties].
         *
         * @see ConfigFactory.parseProperties
         * */
        fun Parse(properties: Properties): ConfigSource = ConfigSource { ConfigFactory.parseProperties(properties) }

        /**
         * A [ConfigSource] that provides a [Config] from a [String].
         *
         * @see ConfigFactory.parseString
         * */
        fun Parse(content: String): ConfigSource = ConfigSource { ConfigFactory.parseString(content) }

        /**
         * A [ConfigSource] that provides a [Config] from a [Path].
         *
         * @see ConfigFactory.parseFile
         * */
        fun Path(path: Path, failIfMissing: Boolean = true): ConfigSource = File(path.toFile(), failIfMissing)

        /**
         * A [ConfigSource] that provides the given [config].
         * */
        fun Config(config: Config): ConfigSource = ConfigSource { config }

        /**
         * A [ConfigSource] that provides a [Config] from the given class resource.
         * Uses the reified [T] parameter to get the class.
         *
         * @see ConfigFactory.parseResourcesAnySyntax
         * */
        inline fun <reified T> ClassResource(resource: String, failIfMissing: Boolean = true): ConfigSource =
            ConfigSource {
                val options = ConfigParseOptions.defaults().setAllowMissing(!failIfMissing)
                ConfigFactory.parseResourcesAnySyntax(T::class.java, resource, options)
            }

        /**
         * A [ConfigSource] that provides a [Config] from the specified [resource].
         *
         * @see ConfigFactory.parseResourcesAnySyntax
         */
        fun Resource(
            resource: String,
            classLoader: ClassLoader? = null,
            failIfMissing: Boolean = true
        ): ConfigSource = ConfigSource {
            val options = ConfigParseOptions.defaults().setAllowMissing(!failIfMissing)
            if (classLoader != null) {
                ConfigFactory.parseResourcesAnySyntax(classLoader, resource, options)
            } else {
                ConfigFactory.parseResourcesAnySyntax(resource, options)
            }
        }

        // reference
        /**
         * A [ConfigSource] that provides a [Config] from `reference.conf`.
         *
         * @see ConfigFactory.parseResourcesAnySyntax
         * */
        fun Reference(classLoader: ClassLoader? = null): ConfigSource = ConfigSource {
            if (classLoader != null) {
                ConfigFactory.parseResources(classLoader, "reference.conf")
            } else {
                ConfigFactory.parseResources("reference.conf")
            }
        }

        /**
         * A [ConfigSource] that provides a [Config] from `application.conf`.
         *
         * This function first checks whether the system properties `config.resource`, `config.file`, `config.url` is set.
         *
         * If any of them are, then the [ConfigSource] will be created from the value of the system property.
         *
         * @see ConfigFactory.parseResourcesAnySyntax
         * */
        fun Application(classLoader: ClassLoader? = null): ConfigSource = ConfigSource {
            val options = ConfigParseOptions.defaults().setAllowMissing(false)
            when {
                System.getProperty("config.resource") != null -> if (classLoader != null) {
                    ConfigFactory.parseResources(
                        classLoader,
                        System.getProperty("config.resource"),
                        options
                    )
                } else {
                    ConfigFactory.parseResources(
                        System.getProperty("config.resource"),
                        options
                    )
                }
                System.getProperty("config.file") != null -> ConfigFactory.parseFile(
                    File(System.getProperty("config.file")),
                    options
                )
                System.getProperty("config.url") != null -> ConfigFactory.parseURL(
                    URL(System.getProperty("config.url")),
                    options
                )
                else -> if (classLoader != null) {
                    ConfigFactory.parseResourcesAnySyntax(classLoader, "application")
                } else {
                    ConfigFactory.parseResourcesAnySyntax("application")
                }
            }
        }

        /**
         * Returns a [Config] object that is the result of merging all the config sources together.
         * */
        fun build(): Config = sources.fold(ConfigFactory.empty()) { config, source ->
            config + source.load()
        }.resolve()
    }
}

/**
 * Creates a [Config] object by merging all specified [ConfigSource]s together.
 * The [ConfigSource]s are merged in the order they are specified.
 *
 * Usage:
 *
 * ```
 * val config = createConfig {
 *    +SystemProperties
 *    +EnvironmentVariables
 *    // etc
 * }
 * ```
 *
 * This makes it a lot easier to create a specific [Config] object.
 *
 * @see ConfigSource
 * */
fun createConfig(block: ConfigSource.Dsl.() -> Unit): Config = ConfigSource.Dsl().apply(block).build()