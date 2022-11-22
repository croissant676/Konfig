package dev.kason.konfig

import com.typesafe.config.Config
import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigList
import com.typesafe.config.ConfigMemorySize
import com.typesafe.config.ConfigObject
import com.typesafe.config.ConfigValue
import java.time.*
import java.time.temporal.TemporalAmount
import java.util.*
import kotlin.reflect.*
import kotlin.reflect.full.*

/**
 * A Config evaluator attempts to convert a config value (represented through a [Config] and
 * path) to a specified type. If an evaluator is unable to do so, it should return `null`.
 * If this evaluator throws an error, it will be interpreted as being unable to convert the
 * given value.
 *
 * Config evaluators can be registered through [registerEvaluator] in order to add support
 * for custom types.
 *
 * @see registerEvaluator
 * */
typealias ConfigEvaluator<T> = Config.(key: String) -> T?

@PublishedApi
internal val evaluators: MutableMap<KType, ConfigEvaluator<*>> = hashMapOf()

@PublishedApi
internal class CompositeEvaluator<T>(val evaluators: MutableList<ConfigEvaluator<T>>) : (Config, String) -> T? {
    override fun invoke(config: Config, key: String): T? = with(config) {
        for (evaluator in evaluators) {
            val result = kotlin.runCatching { evaluator(this, key) }.getOrNull()
            if (result != null) return@with result
        }
        return null
    }
}

/**
 * This function takes in an [evaluator] and registers it to the specified type [T].
 * The evaluator will be called whenever a value of type [T] is needed from the config.
 * Because types are used instead of classes, users don't have to worry about generics.
 *
 * Multiple evaluators can be registered to the same type. In that case, evaluators will be
 * executed in the order that they are registered.
 *
 * Here's an example:
 * ```
 * registerEvaluator {
 *    UUID.fromString(this[it])
 * }
 * ```
 *
 * In this case, Kotlin will be able to infer that the [ConfigEvaluator] type is [UUID] and
 * will register it to that type. Whenever a `UUID` object is needed from the config,
 * users can simply do:
 * ```
 * val uuid: UUID = config["path"]
 * ```
 *
 * This allows for support for custom types, reducing boilerplate code.
 *
 * @see get
 * */
inline fun <reified T> registerEvaluator(noinline evaluator: ConfigEvaluator<T>) {
    val type = typeOf<T>()
    if (evaluators[type] != null) {
        val existingEvaluator = evaluators[type]!!
        if (existingEvaluator is CompositeEvaluator<*>) {
            @Suppress("UNCHECKED_CAST")
            existingEvaluator as CompositeEvaluator<T>
            existingEvaluator.evaluators += evaluator
        } else {
            val newEvaluator = CompositeEvaluator(mutableListOf(existingEvaluator, evaluator))
            evaluators[type] = newEvaluator
        }
        return
    }
    evaluators[type] = evaluator
}

/**
 * This function takes in a given [key] and type [T] and attempts to convert
 * the value from the config for the given [key] into the specified type.
 *
 * By default, types with a getter in place in the [Config] class are supported
 * already ([String], [Long], [Duration], etc.)
 *
 * If you wish to add support for a type that doesn't already exist, consider
 * you'll need to register an evaluator through [registerEvaluator].
 *
 * This function will return `null` if no value can be found in the config, or
 * the value present can't be converted into the specified type.
 * If the value is required, look into [require], which throws an error if the
 * value can't be found.
 *
 * Usage:
 * ```
 *
 * val stringValue: String? = config["path"]
 * val intValue: Int? = config["non-existent-path"] // will return null
 *
 * val unsupportedType: UnsupportedType? = config["path"] // will return null, unless an evaluator is registered.
 *
 * // you can also do this:
 *
 * val stringValue = config.get<String>("path")
 * ```
 *
 * @see registerEvaluator
 * @see require
 * @throws ConfigException.BadPath if the given [key] isn't a valid path.
 * See [Config.hasPath] for more information regarding this exception.
 * */
inline operator fun <reified T> Config.get(key: String): T? {
    if (!hasPath(key)) return null
    val expectedClassType = T::class
    when (expectedClassType) {
        String::class -> return getString(key) as T
        Int::class -> return getInt(key) as T
        Long::class -> return getLong(key) as T
        Double::class -> return getDouble(key) as T
        Boolean::class -> return getBoolean(key) as T
        ConfigMemorySize::class -> return getMemorySize(key) as T
        ConfigObject::class -> return getObject(key) as T
        ConfigList::class -> return getList(key) as T
        ConfigValue::class -> return getValue(key) as T
        Config::class -> return getConfig(key) as T
        Duration::class -> return getDuration(key) as T
        Period::class -> return getPeriod(key) as T
        TemporalAmount::class -> return getTemporal(key) as T
    }
    if (expectedClassType.isSubclassOf(Enum::class)) {
        @Suppress("UNCHECKED_CAST")
        return getEnum(expectedClassType.java as Class<out Enum<*>>, key) as T
    }
    val type = typeOf<T>()
    when (type) {
        typeOf<List<Boolean>>() -> return getBooleanList(key) as T
        typeOf<List<Int>>() -> return getIntList(key) as T
        typeOf<List<Long>>() -> return getLongList(key) as T
        typeOf<List<Double>>() -> return getDoubleList(key) as T
        typeOf<List<String>>() -> return getStringList(key) as T
        typeOf<List<Duration>>() -> return getDurationList(key) as T
        typeOf<List<ConfigMemorySize>>() -> return getMemorySizeList(key) as T
    }
    if (type.isSubtypeOf(typeOf<List<Enum<*>>>())) {
        @Suppress("UNCHECKED_CAST")
        return getEnumList(expectedClassType.java as Class<out Enum<*>>, key) as T
    }
    val evaluator = evaluators[type] ?: return null
    return kotlin.runCatching { evaluator(this, key) as? T }.getOrNull()
}

/**
 * Same as [get] but will throw an [IllegalArgumentException] if the config is missing a value
 * at the specified [path] or the value can't be converted to the type [T].
 *
 * Example:
 * ```
 * // throws exception if debug is not present
 * val isDebug: Boolean = config.require("debug")
 * ```
 *
 * If you wish to use a different message or exception class, you can write
 * your own extension function.
 *
 * @see get
 * */
inline fun <reified T> Config.require(path: String): T {
    return get<T>(path) ?: throw IllegalArgumentException("Missing required config value: $path")
}

/**
 * Shortcut for `root().render()`. It renders the current config as
 * a string with comments detailing where each config value is from.
 *
 * This is mainly used for debugging.
 *
 * @see ConfigObject.render
 * */
fun Config.renderAsString(): String = root().render()

/**
 * Provides a lazy delegate that returns the value at the specified [path] in the specified
 * type [T]. If the value does not exist or cannot be converted, this delegate will return `null`.
 *
 * The return value will be equivalent to calling [get].
 *
 * The [LazyThreadSafetyMode] can also be specified through the [mode] parameter.
 *
 * Sample usage:
 * ```
 *
 * val isDebug: Boolean? by config.lazy("debug")
 * // or
 * val isDebug by config.lazy<Boolean>("debug")
 *
 * ```
 *
 * The value won't be calculated until the property is referenced.
 *
 * @see get
 * */
inline fun <reified T> Config.lazy(
    path: String,
    mode: LazyThreadSafetyMode = LazyThreadSafetyMode.SYNCHRONIZED
): Lazy<T?> = lazy(mode) { get<T>(path) }

/**
 * Extension function that allows values to be retrieved lazily through the config.
 *
 * The path of this function will simply be the name of the property.
 *
 * Unlike [lazy], the return value is guaranteed to be not null. If the value does not exist
 * or cannot be converted into that type, an exception will be thrown, similar to [require].
 *
 * ```
 * val debug: Boolean by config // equal in value to config.require("debug")
 * ```
 *
 * @see lazy
 * @see require
 * */
inline operator fun <reified T> Config.getValue(thisRef: Any?, property: KProperty<*>): T = require(property.name)

/**
 *  Syntactic sugar for [Config.withFallback]. Does the exact same thing.
 *
 *  @see Config.withFallback
 *  */
operator fun Config.plus(other: Config): Config = this.withFallback(other)