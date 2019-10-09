# Config

The `org.jitsi.utils.config` package contains classes for defining configuration properties in way that abstracts the property itself away from a particular 'key' and 'source' of the property.

Each configuration property definition (created by subclassing `ConfigProperty`) has 3 properties:
1. A list of suppliers
2. A 'read frequency' strategy
3. A 'not found' strategy

#### Suppliers
A supplier for a configuration property queries some source and returns a value, if found, for that property.  Multiple suppliers can be provided to perform 'fallback' mechanics for a property.

Say we have a property in our application that denotes the port our webserver will listen on.  We might have a config file which contains a default value for the port, but also want the user to be able to override the port via a command-line argument.

Example config file:
```
myapp.webserver.port=8080
```
Example command line argument:
```
--port=8090
```
When we define this property, we'll want to add 2 suppliers: one which will check the command-line argument for the `--port` command-line flag, and another which will check the file for the `myapp.webserver.port` property.  Our definition of the property might look like:

```
class WebserverPortProperty implements AbstractConfigProperty<Integer>
{
    protected static final configFilePropName = "myapp.webserver.port";
    protected static final commandLineArgName = "--port";

    public WebserverPortProperty()
    {
        // We use the PropertyConfig builder to help define it
        super(new PropertyConfig<Integer>()
            // Some function which can get values for command-line args
            .suppliedBy(() -> getCommandLineValueForArg(commandLineArgName))
            // Some function which can get values from the config file
            .suppliedBy(() -> getConfigFileValueForProp(configFilePropName))
            <...snip: see read frequency and not found strategy sections below>
        );
    }
}
```

Now we can use this property in the code:
```
public class Main
{
    private WebserverPortProperty webserverPort = new WebserverPortProperty();

    public static void main(String[] args)
    {
        new Webserver(webserverPort.get());
    }
}
```

#### Read frequency strategy
Some configuration values we want to be able to update if we re-load the config in order to see new values.  To get this behavior, we apply a read frequency strategy to our property definition.  So, if we want the value return from `.get()` to update if the config is reloaded and the value has changed, our property definition would become:

```
class WebserverPortProperty implements AbstractConfigProperty<Integer>
{
    protected static final configFilePropName = "myapp.webserver.port";
    protected static final commandLineArgName = "--port";

    public WebserverPortProperty()
    {
        // We use the PropertyConfig builder to help define it
        super(new PropertyConfig<Integer>()
            // Some function which can get values for command-line args
            .suppliedBy(() -> getCommandLineValueForArg(commandLineArgName))
            // Some function which can get values from the config file
            .suppliedBy(() -> getConfigFileValueForProp(configFilePropName))
            .readEveryTime()
            <...snip: not found strategy sections below>
        );
    }
}
```
And now, the next time we call `.get()` we'll get a new value if the suppliers return a different result than they did before.  To read a property only once (when the property instance is created), use `.readOnce()` instead.

#### Not found strategy
Generally, configuration properties must be defined (this wrapper defines no method to supply a default value) so if the code looks for a property and it isn't found, a `ConfigPropertyNotFound` exception is thrown.  Sometimes, though, an optional property is desired.  The 'not found' strategy defines what happens when none of the suppliers find a value for this property.  If we want to require a property, we'd use: `.throwIfNotFound()`:

```
class WebserverPortProperty implements AbstractConfigProperty<Integer>
{
    protected static final configFilePropName = "myapp.webserver.port";
    protected static final commandLineArgName = "--port";

    public WebserverPortProperty()
    {
        // We use the PropertyConfig builder to help define it
        super(new PropertyConfig<Integer>()
            // Some function which can get values for command-line args
            .suppliedBy(() -> getCommandLineValueForArg(commandLineArgName))
            // Some function which can get values from the config file
            .suppliedBy(() -> getConfigFileValueForProp(configFilePropName))
            .readEveryTime()
            .throwIfNotFound()
        );
    }
}
```
If we're ok with a property not being present, we can use `.returnNullIfNotFound()` instead.

#### Marking a property as obsolete
The `ObsoleteConfig` annotation is provided to mark a property as obsolete.  Code can be written to find property classes with this annotation to see if any value is provided and warn appropriately.
