package dev.kason.konfig

import com.typesafe.config.*
import java.io.*
import java.net.URL
import java.nio.file.Path
import java.util.*

/**
 * A config source represents an object or function that can provide a [Config] object.
 *
 * ConfigSources can be added together in through [ConfigSource.Dsl] in order to create
 * a single [Config] object containing values from all sources.
 *
 * You can access several built-in config sources through a [ConfigSource.Companion] object.
 *
 * @see createConfig
 * */
fun interface ConfigSource {
    /**
     * Creates a [Config] instance.
     *
     * The result is not cached or saved in any way. If you need to cache the result,
     * you can do so yourself.
     *
     * This function should not throw any errors. If a config source is unable to
     * provide a config, it should return an empty config ([ConfigFactory.empty]).
     *
     * @see ConfigSource.Dsl
     * */
    fun load(): Config

    companion object {

        /**
         * A [ConfigSource] that loads a [Config] from system properties.
         * The resulting [Config] will be identical to the one returned by
         * [ConfigFactory.systemProperties].
         *
         * @see ConfigFactory.systemProperties
         * */
        val SystemProperties = ConfigSource { ConfigFactory.systemProperties() }

        /**
         * A [ConfigSource] that loads a [Config] from environment variables.
         * The resulting [Config] will be identical to the one returned by
         * [ConfigFactory.systemEnvironment].
         *
         * @see ConfigFactory.systemEnvironment
         * */
        val EnvironmentVariables = ConfigSource { ConfigFactory.systemEnvironment() }

        /**
         * Returns a [ConfigSource] that loads a [Config] from the specified [file].
         * If [failIfMissing] is set to true, the Config Source will throw an error
         * if any IO errors occur.
         *
         * The resulting [Config] will be identical to the one returned by
         * [ConfigFactory.parseFile].
         *
         * @see ConfigFactory.parseFile
         * */
        fun File(file: File, failIfMissing: Boolean = true): ConfigSource = ConfigSource {
            ConfigFactory.parseFile(file, ConfigParseOptions.defaults().setAllowMissing(!failIfMissing))
        }

        /**
         * Returns a [ConfigSource] that loads a [Config] from the specified [url].
         * If [failIfMissing] is set to true, the Config Source will throw an error
         * if any IO errors occur.
         *
         * The resulting [Config] will be identical to the one returned by
         * [ConfigFactory.parseFile].
         *
         * @see ConfigFactory.parseFile
         * */
        fun Url(url: URL, failIfMissing: Boolean = true): ConfigSource = ConfigSource {
            ConfigFactory.parseURL(url, ConfigParseOptions.defaults().setAllowMissing(!failIfMissing))
        }

        // reader
        /**
         * Returns a [ConfigSource] that loads a [Config] from the specified [reader].
         * The Config will be constructed through the contents of the reader.
         *
         * The resulting [Config] will be identical to the one returned by
         * [ConfigFactory.parseReader].
         *
         * The data in the reader should be in valid HOCON or JSON format.
         *
         * @see ConfigFactory.parseReader
         * */
        fun Read(reader: Reader): ConfigSource = ConfigSource { ConfigFactory.parseReader(reader) }

        /**
         * Returns a [ConfigSource] that loads a [Config] from the specified [inputStream].
         * The Config will be constructed through the contents of the input stream.
         * The input stream will be first converted to a [Reader] via [InputStreamReader],
         * and then parsed.
         *
         * The data in the input stream should contain a valid HOCON or JSON string.
         *
         * @see ConfigFactory.parseReader
         * */
        fun Read(inputStream: InputStream): ConfigSource =
            ConfigSource { ConfigFactory.parseReader(inputStream.reader()) }

        /**
         * Returns a [ConfigSource] that creates a [Config] from specified [map].
         *
         * The resulting [Config] will be identical to the one returned by
         * [ConfigFactory.parseMap].
         *
         * @see ConfigFactory.parseMap
         */
        fun Parse(map: Map<String, Any>): ConfigSource = ConfigSource { ConfigFactory.parseMap(map) }

        /**
         * Returns a [ConfigSource] that creates a [Config] from specified [properties].
         *
         * The resulting [Config] will be identical to the one returned by
         * [ConfigFactory.parseProperties].
         *
         * @see ConfigFactory.parseProperties
         */
        fun Parse(properties: Properties): ConfigSource = ConfigSource { ConfigFactory.parseProperties(properties) }

        /**
         * Returns a [ConfigSource] that creates a [Config] from specified [content] String.
         * The input string should be in the HOCON format.
         *
         * The resulting [Config] will be identical to the one returned by
         * [ConfigFactory.parseString].
         *
         * @see ConfigFactory.parseString
         */
        fun Parse(content: String): ConfigSource = ConfigSource { ConfigFactory.parseString(content) }

        /**
         * Returns a [ConfigSource] that creates a [Config] from specified [path].
         * If [failIfMissing] is set to true, the Config Source will throw an error
         * if any IO errors occur.
         *
         * The resulting [Config] will be identical to the one returned by
         * [ConfigFactory.parseFile].
         *
         * @see ConfigFactory.parseFile
         */
        fun Path(path: Path, failIfMissing: Boolean = true): ConfigSource = File(path.toFile(), failIfMissing)

        /**
         * Returns a [ConfigSource] that returns the specified [config].
         *
         * Useful if you have an existing [Config] object that you want to use as a config source.
         */
        fun FromConfig(config: Config): ConfigSource = ConfigSource { config }

        /**
         * Returns a [ConfigSource] that provides a [Config] from the specified class resource.
         *
         * The class type [T] will provide the package in which the resource is located and also
         * the class loader to use to load the resource. The resource name is specified by [resource].
         *
         * If [failIfMissing] is set to true, the Config Source will throw an error
         * if any IO errors occur.
         *
         * The resulting [Config] will be identical to the one returned by
         * [ConfigFactory.parseResourcesAnySyntax] with the specified [T] class type and [resource].
         *
         * @see ConfigFactory.parseResourcesAnySyntax
         */
        inline fun <reified T> ClassResource(resource: String, failIfMissing: Boolean = true): ConfigSource =
            ConfigSource {
                val options = ConfigParseOptions.defaults().setAllowMissing(!failIfMissing)
                ConfigFactory.parseResourcesAnySyntax(T::class.java, resource, options)
            }

        /**
         * Returns a [ConfigSource] that provides a [Config] from the specified resource.
         *
         * The resource name is specified by [resource]. The class loader to use to load the resource
         * is specified by [classLoader].
         *
         * If [failIfMissing] is set to true, the Config Source will throw an error
         * if any IO errors occur.
         *
         * The resulting [Config] will be identical to the one returned by
         * [ConfigFactory.parseResourcesAnySyntax] with the specified [classLoader] and [resource].
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

        /**
         * Returns a [ConfigSource] that provides a [Config] from the `reference.conf`
         * resource.
         *
         * An optional [classLoader] can be specified to load the resource.
         *
         * The resulting [Config] will be identical to the one returned by
         * [ConfigFactory.defaultReference] with the specified [classLoader].
         *
         * @see ConfigFactory.defaultReference
         */
        fun Reference(classLoader: ClassLoader? = null): ConfigSource = ConfigSource {
            if (classLoader != null) {
                ConfigFactory.defaultReference(classLoader)
            } else {
                ConfigFactory.defaultReference()
            }
        }

        /**
         * Returns a [ConfigSource] that provides a [Config] follow the same rules as
         * [ConfigFactory.defaultApplication].
         *
         * These rules are as follows:
         *
         *  - If the system property `config.resources` is set, read from that resource.
         *  - If the system property `config.file` is set, read from that file.
         *  - If the system property `config.url` is set, read from that URL.
         *
         *  If none of these conditions are met, this source will return
         *  the Config from the `application.conf` file.
         *
         * An optional [classLoader] can be specified to load the resource.
         *
         * The resulting [Config] will be identical to the one returned by
         * [ConfigFactory.defaultApplication] with the specified [classLoader].
         *
         * @see ConfigFactory.defaultApplication
         */
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
    }

    /**
     * A DSL used to create a [Config] object from multiple [ConfigSource] objects.
     *
     * Several functions provide built-in config sources. You can also create your own
     * config sources by implementing the [ConfigSource] interface.
     *
     * @see createConfig
     * */
    class Dsl {
        private val sources = mutableListOf<ConfigSource>()

        /**
         * Adds a [ConfigSource] to the list of config sources.
         */
        operator fun ConfigSource.unaryPlus() {
            sources.add(this)
        }

        /**
         * Returns a [Config] object that is the result of merging all the config sources together.
         *
         * The order in which the config sources are merged is the same as the order in which
         * they were added to the DSL.
         *
         * The resulting [Config] object will be resolved if [resolve] is set to true.
         * */
        fun build(resolve: Boolean = true): Config {
            val config = sources.fold(ConfigFactory.empty()) { config, source ->
                config + source.load()
            }
            return if (resolve) config.resolve() else config
        }
    }
}

/**
 * Creates a [Config] object from multiple [ConfigSource] objects, which should be listed
 * in the [block] parameter. The [ConfigSource.Dsl] provides several functions that can be used
 * to create config sources.
 *
 * Example:
 *
 * ```
 * val config = createConfig {
 *    +SystemProperties
 *    +EnvironmentVariables
 *    // etc
 * }
 * ```
 *
 * This provides a convenient way to create a [Config] object from multiple sources.
 * If you want to simply use the default method of creating a [Config] object, you can
 * use [loadDefaultConfig].
 *
 * By default, the resulting [Config] object will be resolved. If you do not want the
 * resulting [Config] object to be resolved, you can set [resolve] to false.
 *
 * @see ConfigSource
 * */
fun createConfig(resolve: Boolean = true, block: ConfigSource.Dsl.() -> Unit): Config =
    ConfigSource.Dsl().apply(block).build(resolve)

/**
 * This loads the default configuration for the application. This is equivalent to calling
 * [ConfigFactory.load].
 *
 * @see ConfigFactory.load
 * */
fun loadDefaultConfig(): Config = createConfig {
    +ConfigSource.SystemProperties
    +ConfigSource.EnvironmentVariables
    +ConfigSource.Application()
    +ConfigSource.Reference()
}