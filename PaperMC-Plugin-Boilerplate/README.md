# Paper Plugin Template

A modern, reusable template for creating Minecraft Paper plugins targeting **Minecraft 1.21.7**. This template is specifically designed for the latest Minecraft versions and will **not support versions below 1.21**.

## 🎯 Features

- **Latest Paper API**: Built for Minecraft 1.21.7 with the newest Paper API
- **Maven-based**: Uses Maven for dependency management (no Gradle)
- **Java 21**: Leverages the latest Java features and performance improvements
- **Clean Structure**: Well-organized package structure and coding standards
- **Ready-to-use**: Includes basic command handling, configuration, and permissions
- **Template-focused**: Designed to be cloned and modified for your own projects

## 🚀 Why This Template?

### Why I Don’t Use Gradle
> Gradle may be popular, but in my experience, it adds unnecessary complexity to modern Paper plugin projects. Its configuration system is too permissive, often confusing, and when something goes wrong in a build.gradle.kts, it tends to fail silently. Debugging becomes a frustrating and time-consuming task.
> 
> Unlike Maven, Gradle doesn’t enforce a clear structure. It leaves too much room for improvisation, which can hurt readability and long-term maintainability.. and specially when coming back to the project after a few weeks. The build process feels more opaque, and the > error messages are often vague and hard to trace. Maven, on the other hand, provides clear messages, predictable phases, and a transparent lifecycle.
> 
> IDE support, particularly in IntelliJ IDEA, tends to be more stable and reliable with Maven. Projects are recognized and imported without any extra tweaks or obscure scripts, which is not always the case with Gradle-based builds in the Minecraft ecosystem.
> 
> I prefer a strict, clean, and reliable foundation. Maven forces good structure, makes the project easier to understand for other developers, and avoids the kind of "magical" issues that often appear with Gradle. This template reflects that philosophy: no unnecessary abstraction, just a simple, robust, and efficient setup.

### Minecraft 1.21.7+ Only
This template is **exclusively for Minecraft 1.21.7 and above**:
- Takes advantage of the latest Paper API improvements and features
- Uses modern Java 21 features for better performance and cleaner code
- Follows current Minecraft development best practices
- No legacy code or backwards compatibility concerns
- Optimized for the latest server performance enhancements

## 📋 Prerequisites

- **Java 21** or higher (required for Minecraft 1.21.7)
- **Maven 3.6+** for dependency management and building
- **IntelliJ IDEA** (recommended) or any Java IDE with Maven support
- **Paper Server 1.21.7+** for testing your plugin

## 🛠️ Quick Start

### 1. Clone or Download This Template
```bash
git clone https://github.com/loyfael/SkySyncro.git
cd SkySyncro
```

### 2. Customize for Your Project
1. **Rename the project**: Change `SkySyncro` to your plugin name in:
   - `pom.xml` (artifactId and project name)
   - `plugin.yml` (name and description)
   - Main class name and package structure
   - Directory/folder names

2. **Update package structure**: Rename `loyfael` package to your own namespace

3. **Modify plugin details** in `plugin.yml`:
   - `name`: Your plugin's name
   - `description`: What your plugin does
   - `author`: Your name or organization
   - `website`: Your project URL

### 3. Build the Plugin
Using Maven from command line:
```bash
mvn clean compile  # Compile only
mvn clean package  # Compile and create JAR
```

Using IntelliJ IDEA:
- Open the project in IntelliJ
- Use the built-in Maven integration
- Run the `package` goal from the Maven tool window

The compiled `.jar` file will be in the `target/` directory.

### 4. Test Your Plugin
1. Copy the generated `.jar` file to your Paper server's `plugins/` folder
2. Start your server
3. Check the console for successful plugin loading
4. Test commands: `/skysyncro info` and `/skysyncro reload`

## 📁 Project Structure

```
SkySyncro/
├── pom.xml                     # Maven configuration and dependencies
├── README.md                   # This documentation file
├── SkySyncro.iml              # IntelliJ IDEA module file
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── loyfael/
│   │   │       └── Main.java   # Main plugin class
│   │   └── resources/
│   │       ├── plugin.yml      # Plugin metadata and configuration
│   │       └── config.yml      # Plugin configuration file (optional)
│   └── test/
│       └── java/               # Unit tests directory (optional)
└── target/                     # Compiled output (auto-generated)
    ├── classes/                # Compiled Java classes
    └── SkySyncro.jar          # Final plugin JAR file
```

## 🔧 Key Files Explained

### `pom.xml` - Maven Configuration
The heart of the project that:
- Defines project metadata (group, artifact, version)
- Manages dependencies (Paper API 1.21.7)
- Configures build process and plugins
- Sets Java version to 21
- Includes Maven Shade plugin for creating fat JARs if needed

### `plugin.yml` - Paper Plugin Descriptor
Essential file that tells Paper how to load your plugin:
- Defines plugin metadata (name, version, author)
- Specifies the main class entry point
- Sets API version to 1.21 for compatibility
- Declares commands, permissions, and dependencies

### `Main.java` - Plugin Entry Point
The main plugin class that:
- Extends `JavaPlugin` from Paper API
- Handles plugin startup (`onEnable()`) and shutdown (`onDisable()`)
- Can register events, commands, tasks, and listeners
- Manages plugin lifecycle and resources

## 🎮 Default Features Included

### Commands
- `/skysyncro info` - Display plugin information and version
- `/skysyncro reload` - Reload plugin configuration (admin only)

### Permissions System
- `skysyncro.use` - Basic plugin usage (default: true for all players)
- `skysyncro.admin` - Administrative commands (default: op only)

### Configuration Support
- Ready for YAML configuration files
- Automatic resource filtering through Maven
- Version placeholders support

## 🔨 Development Guide

### Adding New Dependencies
Add dependencies to `pom.xml` in the `<dependencies>` section:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>example-library</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope> <!-- Use 'provided' for server APIs -->
</dependency>
```

**Scope Guidelines:**
- `provided` - For APIs provided by the server (Paper API, other plugins)
- `compile` - For libraries you want to include in your JAR
- `test` - For testing frameworks (JUnit, Mockito)

### Creating Commands
1. Create a class implementing `CommandExecutor`:
```java
public class MyCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Command logic here
        return true;
    }
}
```

2. Register it in your main class's `onEnable()` method:
```java
getCommand("mycommand").setExecutor(new MyCommand());
```

3. Add command details to `plugin.yml`:
```yaml
commands:
  mycommand:
    description: Description of my command
    usage: /mycommand [args]
    permission: myplugin.mycommand
```

### Event Handling
1. Create a class implementing `Listener`:
```java
public class MyListener implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Event handling logic
    }
}
```

2. Register the listener in your main class:
```java
getServer().getPluginManager().registerEvents(new MyListener(), this);
```

### Configuration Management
1. Create YAML files in `src/main/resources/`
2. Load configuration:
```java
// Default config.yml
FileConfiguration config = getConfig();

// Custom configuration file
File customConfigFile = new File(getDataFolder(), "custom.yml");
FileConfiguration customConfig = YamlConfiguration.loadConfiguration(customConfigFile);
```

## 🚫 What This Template Doesn't Include

- **Legacy version support** (anything below Minecraft 1.21)
- **Bukkit/Spigot compatibility layers** (Paper-only, modern APIs)
- **Gradle build files** (Maven-only approach)
- **Complex examples** (kept minimal and clean for template purposes)
- **Database integrations** (add as needed for your specific use case)
- **Advanced plugin architectures** (MVC, dependency injection, etc.)

## 💡 Best Practices
### Code Organization
- Keep your main class lightweight (only initialization logic)
- Separate concerns into different classes (commands, listeners, utilities)
- Use packages to organize related functionality
- Follow Java naming conventions consistently

### Performance Considerations
- Use async operations for heavy tasks (`Bukkit.getScheduler().runTaskAsynchronously()`)
- Cache frequently accessed data
- Minimize database queries and network calls
- Use appropriate data structures for your use cases

### Configuration Management
- Provide sensible defaults in your configuration files
- Validate configuration values on plugin load
- Support configuration reloading without server restart
- Document configuration options clearly

## 🤝 Contributing to This Template

This template is designed to be simple, focused, and reusable. If you have suggestions for improvements that:
- Maintain the template's simplicity and clarity
- Add value for most plugin developers
- Follow modern Java and Minecraft development practices
- Don't introduce unnecessary complexity

Feel free to contribute via pull requests or issues.

## 🆘 Getting Help

### Documentation Resources
- **Paper Documentation**: https://docs.papermc.io/
- **Paper API Javadocs**: https://jd.papermc.io/paper/1.21.7/
- **Maven Documentation**: https://maven.apache.org/guides/getting-started/
- **Java 21 Documentation**: https://docs.oracle.com/en/java/javase/21/

### Community Support
- **Paper Discord**: https://discord.gg/papermc
- **SpigotMC Forums**: https://www.spigotmc.org/
- **r/feedthebeast**: Reddit community for modded Minecraft
- **Minecraft Development Communities**: Various Discord servers and forums

### Common Issues
- **"Cannot resolve symbol 'bukkit'"**: Ensure your IDE recognizes the Maven project and has downloaded dependencies
- **"Main class not found"**: Check that the `main` field in `plugin.yml` matches your actual main class path
- **"Unsupported API version"**: Verify you're using Paper 1.21.7+ and not an older server version

## 🏆 Acknowledgments

- **Paper Team** for creating an excellent server platform
- **Minecraft Community** for continuous innovation and support
- **Apache Maven** for reliable build management
- **Java Community** for the robust programming language and ecosystem

---

**⚠️ Important Note**: This template is specifically designed for Minecraft 1.21.7+ and will not work with earlier versions. The Paper API has evolved significantly, and this template takes advantage of modern features not available in older versions. For legacy support, consider using different templates or maintaining separate branches for different Minecraft versions.

**🎯 Template Philosophy**: This template prioritizes simplicity, reliability, and modern development practices over backwards compatibility and feature completeness. It's designed to be a solid foundation that you can build upon, not a comprehensive framework that does everything for you.
