package dev.kason.konfig

import com.typesafe.config.Config
import com.typesafe.config.ConfigList
import com.typesafe.config.ConfigMemorySize
import com.typesafe.config.ConfigObject
import com.typesafe.config.ConfigValue
import java.time.*
import java.time.temporal.TemporalAmount
import kotlin.reflect.*
import kotlin.reflect.full.*

/**
 * Evaluators are used to convert a value at a path for a [Config] to a type.
 * If they are unable to convert the value, they should return null.
 *
 * Custom evaluators can be registered using [addConfigEvaluator].
 * */
typealias Evaluator = Config.(path: String) -> Any?

/**
 * Evaluator priority determines the order in which evaluators are called.
 *
 * By default, evaluators are registered with [Medium] priority.
 *
 * The way in which the evaluators are called is as follows:
 *
 * ```
 *
 * get()
 *      -> <checks existence of value>
 *      -> [Highest]
 *      -> <default>
 *      -> [Medium]
 *      -> <enums, lists>
 *      -> Lowest
 *
 * ```
 * */
enum class EvaluatorPriority {
    /**
     * Evaluators are evaluated immediately.
     * */
    Highest,

    /**
     * Evaluators are evaluated after default evaluators.
     * */
    Medium,

    /**
     * Evaluators are evaluated after every other evaluator.
     * */
    Lowest;

    // ignore this... internal values
    internal val listOfEvaluators = mutableListOf<Evaluator>()

    @Deprecated("This is an internal function", level = DeprecationLevel.WARNING)
    fun <T> execute(config: Config, path: String): T? {
        fun Evaluator.execute(): Any? {
            val value: Any?
            try {
                value = this(config, path)
            } catch (e: Exception) {
                return null
            }
            return value
        }

        for (evaluator in listOfEvaluators) {
            val value = evaluator.execute()
            if (value != null) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    return value as T
                } catch (e: Exception) {
                    continue
                }
            }
        }
        return null
    }
}

/**
 * Adds a new evaluator with the specified priority.
 *
 * See [EvaluatorPriority] for more information.
 *
 * */
fun addConfigEvaluator(
    evaluatorPriority: EvaluatorPriority = EvaluatorPriority.Medium,
    block: Evaluator
) {
    evaluatorPriority.listOfEvaluators.add(block)
}

/**
 * Returns the value at the specified [path] in the specified type [T].
 *
 * All default [Config] types, such as [String], [Long], [Duration], etc. are supported.
 *
 * If you want to add support for custom types, you can use [addConfigEvaluator].
 *
 * This function returns null if the value is not present or if the value cannot be converted to the specified type.
 *
 * Usage:
 * ```
 *
 * val isDebug: Boolean = config["debug"]
 * val port: Int = config["port"]
 * ```
 *
 * */
@Suppress("UNCHECKED_CAST")
inline operator fun <reified T> Config.get(path: String): T? {
    if (!hasPath(path)) return null
    val expectedClassType = T::class
    EvaluatorPriority.Highest.execute<T>(this, path)?.let { return it }
    // default
    when (expectedClassType) {
        String::class -> return getString(path) as T
        Int::class -> return getInt(path) as T
        Long::class -> return getLong(path) as T
        Double::class -> return getDouble(path) as T
        Boolean::class -> return getBoolean(path) as T
        Duration::class -> return getDuration(path) as T
        ConfigMemorySize::class -> return getMemorySize(path) as T
        Period::class -> return getPeriod(path) as T
        // interfaces or not concrete classes
        Number::class -> return getNumber(path) as T
        Config::class -> return getConfig(path) as T
        ConfigObject::class -> return getObject(path) as T
        ConfigValue::class -> return getValue(path) as T
        TemporalAmount::class -> return getTemporal(path) as T
        ConfigList::class -> return getList(path) as T
    }
    EvaluatorPriority.Medium.execute<T>(this, path)?.let { return it }
    if (expectedClassType.isSubclassOf(Enum::class)) {
        val enum = getEnum(expectedClassType.java as Class<out Enum<*>>, path)
        return enum as T
    }
    val type = typeOf<T>()
    when (type) {
        typeOf<List<Boolean>>() -> return getBooleanList(path) as T
        typeOf<List<Int>>() -> return getIntList(path) as T
        typeOf<List<Long>>() -> return getLongList(path) as T
        typeOf<List<Double>>() -> return getDoubleList(path) as T
        typeOf<List<String>>() -> return getStringList(path) as T
        typeOf<List<Duration>>() -> return getDurationList(path) as T
        typeOf<List<ConfigMemorySize>>() -> return getMemorySizeList(path) as T
    }
    if (type.isSubtypeOf(typeOf<List<Enum<*>>>())) {
        val enumList = getEnumList(expectedClassType.java as Class<out Enum<*>>, path)
        return enumList as T
    }
    EvaluatorPriority.Lowest.execute<T>(this, path)?.let { return it }
    return null
}

/**
 * Same as [Config.get] but throws an exception if the value is not present or if the value cannot be converted to the specified type.
 *
 * Usage:
 * ```
 *
 * val isDebug: Boolean = config.require("debug") // throws exception if debug is not present
 *
 * ```
 * */
inline fun <reified T> Config.require(path: String): T {
    return get<T>(path) ?: throw IllegalArgumentException("Missing required configuration: $path")
}

/**
 * Renders the configuration as a string. Read [ConfigValue.render] for more information.
 *
 * Mostly used for debugging purposes.
 * */
fun Config.renderAsString(): String = root().render()

/**
 * Returns a [Lazy] delegate that will return the value at the specified [path] in the specified type [T].
 *
 * Supports everything that [Config.get] supports.
 *
 * Usage:
 * ```
 * val isDebug: Boolean by config.lazy("debug") // will not be evaluated until isDebug is accessed
 * ```
 * */
inline fun <reified T> Config.lazy(path: String): Lazy<T?> = lazy { get<T>(path) }

/**
 * Returns a [Lazy] delegate that will return the value at the specified [path] in the specified type [T].
 *
 * Supports everything that [Config.get] supports.
 *
 * If the value is not present or if the value cannot be converted to the specified type, the specified [defaultValue] value will be returned.
 *
 * Usage:
 * ```
 * val isDebug: Boolean by config.lazy("debug", false) // default value is false
 * ```
 *
 * */
inline fun <reified T> Config.lazy(path: String, defaultValue: T): Lazy<T?> = lazy { get<T>(path) ?: defaultValue }

/**
 * Returns the value of the config at the path determined by the name of the property.
 *
 * Usage:
 * ```
 * val value: String by config // same value as config["value"]
 * ```
 * */
inline operator fun <reified T> Config.getValue(thisRef: Any?, property: KProperty<*>): T = require(property.name)

/**
 * Returns a config that contains the values of both this config and the specified [other] config.
 *
 * Read [Config.withFallback] for more information.
 *
 * Usage:
 * ```
 * val config = config1 + config2
 * ```
 * */
operator fun Config.plus(other: Config): Config = this.withFallback(other)
