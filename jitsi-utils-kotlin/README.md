# Config

Many aspects of configuration change throughout an application's lifetime:
1. The names of properties may change
2. The value type of a property may change
3. Properties may become deprecated
4. Underlying config libraries change
5. The location of the config file changes

A change in any one of those areas affects backwards compatibility, so even if the change is easy to enact, remaining compatible with old deployments can be difficult.

The `org.jitsi.utils.config` package contains classes for defining configuration properties in a way that abstracts the property itself away from a particular 'key' and 'source' of the property and makes it easy to implement configuration-related changes in a way that doesn't break backwards compatibility.  It sits in between the application code and one or more configuration sources.


### Defining a property

A `ConfigProperty` has a set of attributes which include:
1. `keyPath` : the "key" we use to look up this property (a.k.a. the property's name)
2. `valueType` : the type as which we should parse the property's value
3. `readOnce`: whether the property's value should be read a single time at startup or re-read on every access (to support changing configuration values at runtime)
4. `configSource`: the `ConfigSource` in which this property can be found
5. `deprecationNotice` (optional): to denote that a property is deprecated

(See the `ConfigPropertyAttributes` class for the code)

There are a few ways to define a property.  Let's say we have a configuration property for a timeout which we hold as a `Duration`, we could define it in the following ways:

Creating a value using the `property` helper:
```
val timeout: ConfigProperty<Duration> = property<Duration> {
  name("client.requestTimeout")
  readOnce()
  fromConfig(myConfigSource)
}
```
Define a class and then an instance of that class:
```
class ClientRequestTimeout : ConfigPropertyImpl<Duration>(
    ConfigPropertyAttributes(
      keyPath = "client.requestTimeout",
      valueType = Duration::class,
      readOnce = true,
      configSource = myConfigSource
    )
}
val timeout: ConfigProperty<Duration> = ClientRequestTimeout()
```

To retrieve it's value, we'd call `timeout.value`:
```
println("Got client request timeout ${timeout.value}")
```
If no value was found for the property in the `ConfigSource`, an exception will be thrown.

### ConfigSource
A configuration source is anything that holds configuration properties we want to query.  We assume that all configuration sources are implemented such that a property consists of a "key" and a "value".  A key is often referred to as the property's name and is assumed to be able to be represented by a `String`.  A value is the value associated with that key and although it's likely represented as a `String` (in the case of a text-based file), it is usually parsed by the underlying configuration library to a specific type (`Int`, `Boolean`, `Duration`, etc.).

Since different applications pull configuration from different places and use different configuration libraries to do so, we provide a `ConfigSource` interface you must implement to add a configuration source.  The key part of that interface is the method:

`fun <T : Any>getterFor(valueType: KClass<T>): (String) -> T`

This method allows a hook for classes to return a method which retrieves a value as a given type `T`.  For example, if a property has been configured as having a value type of `Long`, we'll ask the `ConfigSource` for a function which takes in a `String` (property's path) and returns a `Long`.  You can have a property be any type you want--even your own custom type--as long as your `ConfigSource` returns a getter function for it when asked.


### Examples
Say we have a property in our application that denotes the port our webserver will listen on and its set by a config file.

Example config file:
```
myapp.webserver.port=8080
```

We'll need a `ConfigSource` which reads from the file:
```
// Whatever config file parsing library we're using
import some.config.parser.library.ConfigParser

class PropertiesFileConfigSource(configFilePath: String) : ConfigSource {
  override val name = "PropsFile-$configFilePath"
  private val configParser = ConfigParser(configFilePath)

  override fun <T : Any> getterFor(valueType: KClass<T>): (String) -> T {
    return when(valueType) {
      Int::class -> { path -> configParser.getInt(path) as T }
      Long::class -> { path -> configParser.getLong(path) as T }
      Duration::class -> { path -> configParser.getDuration(path) as T }
      // etc...
    }
  }
}
```
In our application, we'll define the property:
```
val configSource = PropertiesFileConfigSource("/path/to/config/file")
val webserverPort = property<Int> {
  name("myapp.webserver.port")
  readOnce()
  fromConfig(configSource)
}
```
and access it when creating the server:
```
val server = Server(port.value)
```
 Great.  Now what if we want to let the user override this port via a command-line argument?

Example command line argument:
```
--port=8090
```
We'll need to add a new `ConfigSource` which pulls from the command-line arguments:
```
// Whatever command line arg parsing library we're using
import some.commandline.parser.library.CommandLineParser

class CommandLineArgsConfigSource(commandLineArgs: String[] : ConfigSource {
  override val name = "CommandLineArgs"
  private val commandLineParser = CommandLineParser(commandLineArgs)

  override fun <T : Any> getterFor(valueType: KClass<T>): (String) -> T {
    return when(valueType) {
      Int::class -> { path -> commandLineParser.getInt(path) as T }
      Long::class -> { path -> commandLineParser.getLong(path) as T }
      Duration::class -> { path -> commandLineParser.getDuration(path) as T }
      // etc...
    }
  }
}
```
and tweak our property implementation:
```
val configFileSource = PropertiesFileConfigSource("/path/to/config/file")
val commandLineArgsSource = CommandLineArgsConfigSource(commandLineArgs)

val webserverPort = multiProperty<Int> {
  property {
    name("--port")
    readOnce()
    fromConfig(commandLineArgsSource)
  }
  property {
    name("myapp.webserver.port")
    readOnce()
    fromConfig(configFileSource)
  }
}
```
Now, the property will first check for the `--port` arg from the command-line source.  If it finds it there, it will return that value.  If no command-line arg was provided, it'll fallback to checking the config file.

Now let's say we've realized the property name in the config file is poorly named: we called it "port", but really it's the _https_ port, so we want to rename it:
```
myapp.webserver.https_port=8080
```
We can change our config files, but we don't want users to be bit by the change, or older deployments to break.  We can just have the property definition check for both fields and mark the old one as deprecated:
```
val webserverPort = multiProperty<Int> {
  property {
    name("--port")
    readOnce()
    fromConfig(commandLineArgsSource)
  }
  property {
    name("myapp.webserver.https_port")
    readOnce()
    fromConfig(configFileSource)
  }
  property {
    name("myapp.webserver.port")
    readOnce()
    fromConfig(configFileSource)
    deprecated("This property is now deprecated, use " +
        " 'myapp.webserver.https_port' instead")
  }
}
```
This way, we can still fall back to the old property name, but if we do we'll print a deprecation warning.

As another means of backwards/forwards compatibility, we can also convert the value type of a retrieved value.  Let's say we have a timeout property:
```
# Timeout in milliseconds
myapp.timeout=10000
```
With the config property definition:
```
val timeout = property<Long> {
  name("myapp.timeout")
  readOnce()
  fromConfig(myConfigSource)
}
```
And we want to start using it as a `Duration` in the code.  We can change the property definition to convert it:
```
val timeout = property<Duration> {
  name("myapp.timeout")
  readOnce()
  fromConfig(myConfigSource)
  retrievedAs<Long>() convertedBy { Duration.ofMillis(it) }
}
```
Now we'll still read the `Long` value from the config file, but we'll model the property as a `Duration`.  If we change the format of the config file to match:
```
myapp.timeout=10 seconds
```
We can change the property again to still be compatible:
```
val timeout = multiProperty<Duration> {
  property {
    // If the config file still holds a Long, we'll convert it
    // If it's been updated to the new format, this will fail
    // and fall through
    name("myapp.timeout")
    readOnce()
    fromConfig(myConfigSource)
    retrievedAs<Long>() convertedBy { Duration.ofMillis(it) }
  }
  property {
    // Parse as a Duration directly
    name("myapp.timeout")
    readOnce()
    fromConfig(myConfigSource)
  }
}
```

More property examples can be found in `org.jitsi.utils.config.examples` package.
