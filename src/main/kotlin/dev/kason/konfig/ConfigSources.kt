package dev.kason.konfig

import com.typesafe.config.*
import java.io.File
import java.io.InputStream
import java.io.Reader
import java.net.URL
import java.nio.file.Path
import java.util.Properties
import kotlin.reflect.*

/**
 * A config source can provide a [Config] instance.
 *
 * Config sources are used to load configuration from various sources.
 * */
fun interface ConfigSource {
    fun load(): Config
}

/**
 * Contains various built in [ConfigSource]s.
 *
 * */
@Suppress("FunctionName")
object ConfigSources {
    /**
     * Config source that loads data from System properties.
     *
     * Provides the same functionality as [ConfigFactory.systemProperties].
     *
     * */
    val SystemProperties = ConfigSource { ConfigFactory.systemProperties() }

    /**
     * Config source that loads data from System environment variables.
     *
     * Provides the same functionality as [ConfigFactory.systemEnvironment].
     *
     * */
    val EnvironmentVariables = ConfigSource { ConfigFactory.systemEnvironment() }

    /**
     * Returns a config source that loads data from the `reference.conf` file.
     *
     * A custom class loader can be provided to load the file from a different class loader.
     * */
    fun Reference(optionalClassLoader: ClassLoader? = null): ConfigSource = ConfigSource {
        if (optionalClassLoader != null) {
            ConfigFactory.parseResources(optionalClassLoader, "reference.conf")
        } else {
            ConfigFactory.parseResources("reference.conf")
        }
    }

    /**
     * Returns a config source that loads data from a `application.conf` file.
     *
     * A custom class loader can be provided to load the file from a different class loader.
     *
     * The `config.resource`, `config.file`, and `config.url` system properties can be used
     * to specify the location of the file.
     * */
    fun Application(optionalClassLoader: ClassLoader? = null): ConfigSource = ConfigSource {
        val specifiedOverrideOptions = ConfigParseOptions.defaults().setAllowMissing(false)
        if (System.getProperty("config.resource") != null) {
            if (optionalClassLoader != null) {
                return@ConfigSource ConfigFactory.parseResources(
                    optionalClassLoader,
                    System.getProperty("config.resource"),
                    specifiedOverrideOptions
                )
            } else {
                return@ConfigSource ConfigFactory.parseResources(
                    System.getProperty("config.resource"),
                    specifiedOverrideOptions
                )
            }
        } else if (System.getProperty("config.file") != null) {
            return@ConfigSource ConfigFactory.parseFile(
                File(System.getProperty("config.file")),
                specifiedOverrideOptions
            )
        } else if (System.getProperty("config.url") != null) {
            return@ConfigSource ConfigFactory.parseURL(URL(System.getProperty("config.url")), specifiedOverrideOptions)
        } else {
            if (optionalClassLoader != null) {
                return@ConfigSource ConfigFactory.parseResourcesAnySyntax(optionalClassLoader, "application")
            } else {
                return@ConfigSource ConfigFactory.parseResourcesAnySyntax("application")
            }
        }
    }

    /**
     * Returns the config source that loads data from a specified resource.
     *
     * A custom class loader can be provided to load the file from a different class loader.
     * If [failIfMissing] is true, the config source will throw an exception if the resource is not found.
     * */
    fun Resource(
        resource: String,
        optionalClassLoader: ClassLoader? = null,
        failIfMissing: Boolean = true
    ): ConfigSource = ConfigSource {
        val options = ConfigParseOptions.defaults().setAllowMissing(!failIfMissing)
        return@ConfigSource if (optionalClassLoader != null) {
            ConfigFactory.parseResourcesAnySyntax(optionalClassLoader, resource, options)
        } else {
            ConfigFactory.parseResourcesAnySyntax(resource, options)
        }
    }

    /**
     * Returns the config source that loads data from a specified class resource using the specified [javaClass].
     *
     * If [failIfMissing] is true, the config source will throw an exception if the file is not found.
     * */
    fun ClassResource(resource: String, javaClass: Class<*>, failIfMissing: Boolean = true): ConfigSource =
        ConfigSource {
            val options = ConfigParseOptions.defaults().setAllowMissing(!failIfMissing)
            return@ConfigSource ConfigFactory.parseResourcesAnySyntax(javaClass, resource, options)
        }

    /**
     * Returns the config source that loads data from a specified class resource using the specified [kotlinClass].
     *
     * If [failIfMissing] is true, the config source will throw an exception if the file is not found.
     */
    fun ClassResource(resource: String, kotlinClass: KClass<*>, failIfMissing: Boolean = true): ConfigSource =
        ConfigSource {
            val options = ConfigParseOptions.defaults().setAllowMissing(!failIfMissing)
            return@ConfigSource ConfigFactory.parseResourcesAnySyntax(kotlinClass.java, resource, options)
        }

    /**
     * Returns the config source that loads data from a specified class resource using the reified [T] class.
     *
     * If [failIfMissing] is true, the config source will throw an exception if the file is not found.
     */
    inline fun <reified T> ClassResource(resource: String, failIfMissing: Boolean = true): ConfigSource =
        ClassResource(resource, T::class, failIfMissing)

    /**
     * Returns the config source that loads data from a specified file.
     *
     * If [failIfMissing] is true, the config source will throw an exception if the file is not found.
     */
    fun File(file: File, failIfMissing: Boolean = true): ConfigSource = ConfigSource {
        val options = ConfigParseOptions.defaults().setAllowMissing(!failIfMissing)
        return@ConfigSource ConfigFactory.parseFile(file, options)
    }

    /**
     * Returns the config source that loads data from a specified file path.
     *
     * If [failIfMissing] is true, the config source will throw an exception if the file is not found.
     */
    fun Url(url: URL, failIfMissing: Boolean = true): ConfigSource = ConfigSource {
        val options = ConfigParseOptions.defaults().setAllowMissing(!failIfMissing)
        return@ConfigSource ConfigFactory.parseURL(url, options)
    }

    /**
     * Returns the config source that loads data the specified reader.
     *
     * Same as [ConfigFactory.parseReader].
     */
    fun Read(reader: Reader): ConfigSource = ConfigSource { ConfigFactory.parseReader(reader) }

    /**
     * Returns the config source that loads data the specified input stream.
     *
     * Same as [ConfigFactory.parseReader].
     */
    fun Read(inputStream: InputStream): ConfigSource = ConfigSource { ConfigFactory.parseReader(inputStream.reader()) }

    /**
     * Returns the config source that loads data the specified map.
     *
     * Same as [ConfigFactory.parseMap].
     */
    fun ParseMap(map: Map<String, Any>): ConfigSource = ConfigSource { ConfigFactory.parseMap(map) }

    /**
     * Returns the config source that loads data the specified properties.
     *
     * Same as [ConfigFactory.parseProperties].
     */
    fun ParseProperties(properties: Properties): ConfigSource = ConfigSource {
        ConfigFactory.parseProperties(properties)
    }

    /**
     * Returns the config source that loads data the specified string.
     *
     * Same as [ConfigFactory.parseString].
     */
    fun ParseString(content: String): ConfigSource = ConfigSource { ConfigFactory.parseString(content) }

    /**
     * Returns the config source that loads data from the specified path.
     *
     * Same as [ConfigFactory.parseFile].
     */
    fun Path(path: Path, failIfMissing: Boolean = true): ConfigSource = File(path.toFile(), failIfMissing)

    /**
     * Dsl for creating a config from multiple config sources.
     * */
    class Dsl {
        private val sources = mutableListOf<ConfigSource>()

        /**
         * Adds a config source to the dsl.
         * */
        operator fun ConfigSource.unaryPlus() {
            sources.add(this)
        }

        /**
         * Builds a config from the added config sources.
         *
         * The config sources are loaded in the order they were added.
         * */
        fun build(): Config {
            val config = sources.fold(ConfigFactory.empty()) { acc, source ->
                acc + source.load()
            }
            return config.resolve()
        }
    }
}

/**
 * Returns a config from the specified config sources in the lambda.
 *
 * Config sources are loaded in the order they are added.
 *
 * Usage:
 * ```
 * val config = createConfig {
 *    +Resource("application")
 *    +SystemProperties()
 *    +EnvironmentVariables()
 *    /* etc */
 * }
 * ```
 * */
fun createConfig(block: ConfigSources.Dsl.() -> Unit): Config = ConfigSources.Dsl().apply(block).build()

/**
 * Returns the default config.
 *
 * Uses the following config sources:
 * - SystemProperties
 * - EnvironmentVariables
 * - Reference()
 * - Application()
 *
 * Reference and Application will use the specified [classLoader] if one is provided.
 * */
fun loadDefaultApplicationConfig(classLoader: ClassLoader? = null): Config = createConfig {
    +ConfigSources.SystemProperties
    +ConfigSources.EnvironmentVariables
    +ConfigSources.Reference(classLoader)
    +ConfigSources.Application(classLoader)

    ConfigFactory.load()
}