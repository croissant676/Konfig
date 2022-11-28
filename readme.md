# Konfig: A simple Kotlin wrapper for [HOCON configuration](https://github.com/lightbend/config)

What is Konfig?

- Provides a simple API for accessing configuration values based on types
- Easier and more concise than using the typesafe config library directly
- Allows users to add their own evaluators for custom types
- Select the sources of configuration values (e.g. environment variables, system properties, etc.)

## Usage

### Creating a config object

```kotlin
val config = createConfig {
    +SystemProperties
    +EnvironmentVariables
}
```

The above code will create a config object that will look for configuration values in the system properties and
environment variables.

```kotlin
val config = createConfig {
    +SystemProperties
    +EnvironmentVariables
    +Application()
}
```

Different functions will be used to create different sources of configuration values. 
The above code will create a config object that will look for configuration values in the system properties,
environment variables, and the application.conf file.

Read the documentation for the different sources of configuration values to learn more about how to use them.

### Getting a configuration value

```kotlin
val port = config.get<Int>("port")

// or

val port: Int? = config["port"]
```

The above code will get the value of the configuration key "port" and convert it to an Int.
Both ways of getting a configuration value will return null if the key is not found.

### Getting a configuration value with a default

```kotlin
val port = config["port"] ?: 8080
```

Concise and easy code can be written using the elvis operator to get a configuration value with a default.

### Getting a configuration value with a custom type

```kotlin
registerEvaluator {
   UUID.fromString(this[it] ?: return@registerEvaluator null)
}

val uuid: UUID? = config["path"]

```

The above code will get the value of the configuration key "path" and convert it to a UUID 
using the custom evaluator registered above.

