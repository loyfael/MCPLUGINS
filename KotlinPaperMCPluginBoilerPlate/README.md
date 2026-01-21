# 🚀 Kotlin PaperMC Plugin Boilerplate

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-blue.svg)](https://kotlinlang.org/)
[![PaperMC](https://img.shields.io/badge/PaperMC-1.21.8-green.svg)](https://papermc.io/)
[![Maven](https://img.shields.io/badge/Maven-3.6+-red.svg)](https://maven.apache.org/)

A **production-ready** boilerplate for creating high-quality PaperMC plugins in Kotlin. This template includes everything you need to get started with modern Minecraft plugin development, featuring database management, configuration handling, command systems, and more.

## ✨ Features

### 🏗️ **Core Architecture**
- ✅ **Modern Kotlin** (1.9.24) with full Java 21 support
- ✅ **Plugin lifecycle management** with proper initialization and cleanup
- ✅ **Modular design** with separated concerns
- ✅ **Error handling** and logging best practices

### 🛠️ **Built-in Systems**
- ✅ **Configuration Manager** - YAML config handling with type safety
- ✅ **Database Manager** - MySQL & SQLite support with HikariCP connection pooling
- ✅ **Command System** - Advanced command handling with permissions and help
- ✅ **Event Listeners** - Player connection tracking example
- ✅ **Utility Classes** - Common functions for plugin development
- ✅ **Data Models** - Player data management example

### 🔧 **Development Tools**
- ✅ **Maven Shade Plugin** - Dependency packaging with relocations
- ✅ **Hot reload friendly** - Development-optimized configuration
- ✅ **Debug mode** - Built-in debugging and logging utilities
- ✅ **Modern API** - Uses latest PaperMC features (`pluginMeta` instead of deprecated `description`)

## 📋 Prerequisites

- **Java 21 JDK** ([Download](https://adoptium.net/temurin/releases/))
- **Maven 3.6+** ([Download](https://maven.apache.org/download.cgi))
- **IntelliJ IDEA** (Community or Ultimate) - **Recommended IDE**
- **PaperMC Server 1.21.8+** ([Download](https://papermc.io/downloads))

> ⚠️ **Note**: Eclipse is not recommended for Kotlin development. Use IntelliJ IDEA for the best experience.

## 🚀 Quick Start

### 1. **Clone the Repository**
```bash
git clone https://github.com/loyfael/KotlinPaperMCPluginBoilerPlate.git
cd KotlinPaperMCPluginBoilerPlate
```

### 2. **Customize the Plugin**
Edit the following files to match your project:

**`pom.xml`**:
```xml
<groupId>kotlin.loyfael</groupId>
<artifactId>your-plugin-name</artifactId>
<version>1.0.0</version>
```

**`src/main/resources/plugin.yml`**:
```yaml
name: YourPluginName
main: loyfael.MyPlugin
author: YourName
description: Your plugin description
```

### 3. **Update Package Names**
Rename the package from `loyfael` to your own:
- Right-click on the package in IntelliJ IDEA
- Select "Refactor → Rename"
- Update all references

### 4. **Build the Plugin**
```bash
mvn clean package
```

### 5. **Installation**
1. Copy `target/your-plugin-name-1.0.0-SNAPSHOT.jar` to your server's `plugins/` folder
2. Restart your server
3. Configure the plugin in `plugins/YourPluginName/config.yml`

## 📁 Project Structure

```
KotlinPaperMCPluginBoilerPlate/
├── src/main/
│   ├── kotlin/loyfael/
│   │   ├── MyPlugin.kt                 # Main plugin class
│   │   ├── ConfigManager.kt            # Configuration handler
│   │   ├── DatabaseManager.kt          # Database connection manager
│   │   ├── MainCommand.kt              # Main command executor
│   │   ├── PlayerConnectionListener.kt # Event listener example
│   │   ├── data/
│   │   │   └── PlayerData.kt          # Data model example
│   │   └── utils/
│   │       └── PluginUtils.kt         # Utility functions
│   └── resources/
│       ├── config.yml                  # Default configuration
│       └── plugin.yml                  # Plugin metadata
├── pom.xml                            # Maven configuration
├── README.md                          # This file
└── .gitignore                         # Git ignore rules
```

## ⚙️ Configuration

### **Default Configuration** (`config.yml`)
```yaml
# Plugin Settings
plugin:
  debug: false
  language: "en"
  auto-save-interval: 5  # minutes (0 to disable)

# Database Configuration
database:
  type: "sqlite"  # sqlite or mysql
  mysql:
    host: "localhost"
    port: 3306
    database: "minecraft"
    username: "user"
    password: "password"
  sqlite:
    file: "database.db"

# Messages
messages:
  prefix: "&8[&bMyPlugin&8] &f"
  no-permission: "&cYou don't have permission to use this command!"
  reload-success: "&aConfiguration reloaded successfully!"
```

### **Configuration Manager Features**
```kotlin
// Get configuration values with defaults
val debugMode = configManager.isDebugEnabled()
val autoSaveInterval = configManager.getAutoSaveInterval()
val message = configManager.getMessage("no-permission")

// Reload configuration
configManager.reloadConfig()
```

## 🗃️ Database Support

### **Supported Databases**
- **SQLite** - File-based, perfect for small servers
- **MySQL** - Network database for larger deployments

### **Features**
- **HikariCP Connection Pooling** - High-performance connection management
- **Automatic table creation** - Database schema is created automatically
- **Migration support** - Easy database updates
- **Connection validation** - Automatic connection health checks

### **Usage Example**
```kotlin
// Initialize database
databaseManager.initializeDatabase()

// Execute queries
val playerData = databaseManager.getPlayerData(playerUuid)
databaseManager.savePlayerData(playerUuid, data)
```

## 🎮 Command System

### **Built-in Commands**
- `/myplugin` - Main command with help system
- `/myplugin help` - Display available commands
- `/myplugin reload` - Reload plugin configuration
- `/myplugin info` - Display plugin information
- `/myplugin debug` - Toggle debug mode

### **Command Features**
- **Permission-based** access control
- **Tab completion** for better UX
- **Help system** with usage examples
- **Error handling** with user-friendly messages

### **Adding New Commands**
```kotlin
override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
    when (args.firstOrNull()?.lowercase()) {
        "yourcommand" -> {
            // Your command logic here
            return true
        }
    }
    return false
}
```

## � Event System

### **Built-in Listeners**
- **PlayerConnectionListener** - Tracks player join/quit events
- **Automatic registration** - All listeners are registered automatically

### **Adding New Listeners**
```kotlin
@EventHandler
fun onPlayerJoin(event: PlayerJoinEvent) {
    // Your event handling logic
}
```

## 🔧 Development

### **Useful Maven Commands**
```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Create JAR with dependencies
mvn clean package

# Install to local Maven repository
mvn install

# Skip tests during build
mvn clean package -DskipTests
```

### **IDE Configuration**
1. **Import Project**: Open the `pom.xml` file in IntelliJ IDEA
2. **JDK Settings**: Set Project SDK to Java 21
3. **Maven Settings**: Enable auto-import for Maven projects
4. **Kotlin Plugin**: Ensure Kotlin plugin is enabled

### **Debugging**
```kotlin
// Enable debug mode in config.yml or use command
if (configManager.isDebugEnabled()) {
    logger.info("Debug: Player ${player.name} performed action")
}
```

## 🏗️ Building for Production

### **Maven Profiles** (Optional Enhancement)
Add different profiles for development and production:

```xml
<profiles>
    <profile>
        <id>dev</id>
        <properties>
            <plugin.debug>true</plugin.debug>
        </properties>
    </profile>
    <profile>
        <id>prod</id>
        <properties>
            <plugin.debug>false</plugin.debug>
        </properties>
    </profile>
</profiles>
```

### **Building**
```bash
# Development build
mvn clean package -Pdev

# Production build  
mvn clean package -Pprod
```

## � Dependencies

### **Included Libraries**
| Library | Version | Purpose |
|---------|---------|---------|
| Paper API | 1.21.8 | Minecraft server API |
| Kotlin Stdlib | 1.9.24 | Kotlin standard library |
| HikariCP | 5.0.1 | Database connection pooling |
| MySQL Connector | 8.0.33 | MySQL database driver |
| PlaceholderAPI | 2.11.5 | Placeholder support (optional) |

### **Shaded Dependencies**
The following dependencies are included in the final JAR:
- Kotlin Standard Library
- HikariCP
- MySQL Connector

## 🧪 Testing

### **Unit Tests** (Ready for implementation)
```kotlin
@Test
fun testConfigManager() {
    // Test configuration loading
    val config = ConfigManager(plugin)
    assertNotNull(config.getPrefix())
}
```

### **Integration Tests**
```bash
# Run with test server
mvn test -Dtest.server.version=1.21.8
```

## 🚀 Performance Optimizations

### **Built-in Optimizations**
- **Async database operations** - Non-blocking database calls
- **Connection pooling** - Efficient database connection management
- **Configuration caching** - Reduced file I/O operations
- **Event optimization** - Efficient event handling

### **Memory Management**
- **Proper resource cleanup** on plugin disable
- **WeakReference usage** where appropriate
- **Cache cleanup** on reload

## 🔍 Troubleshooting

### **Common Issues**

**Issue**: "Plugin failed to load"
- **Solution**: Check that Java 21 is being used
- **Check**: Verify `api-version` matches server version

**Issue**: "Database connection failed"
- **Solution**: Verify database credentials in `config.yml`
- **Check**: Ensure database server is running

**Issue**: "Commands not working"
- **Solution**: Check permissions in `plugin.yml`
- **Check**: Verify command registration in main class

### **Debug Mode**
Enable debug mode to get detailed logging:
```yaml
plugin:
  debug: true
```

## � Useful Resources

### **Documentation**
- [PaperMC API Documentation](https://docs.papermc.io/)
- [PaperMC Javadocs](https://papermc.io/javadocs/)
- [Kotlin Documentation](https://kotlinlang.org/docs/)
- [Maven Getting Started Guide](https://maven.apache.org/guides/getting-started/)

### **Tools**
- [Maven Repository Search](https://mvnrepository.com/)
- [PaperMC Downloads](https://papermc.io/downloads)
- [Kotlin Playground](https://play.kotlinlang.org/)

## 🤝 Contributing

We welcome contributions! Here's how you can help:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

### **Contribution Guidelines**
- Follow Kotlin coding conventions
- Add tests for new features
- Update documentation as needed
- Use meaningful commit messages

## 📄 License

This project is released under the **MIT License** - see the [LICENSE](LICENSE) file for details.

## 🎯 Roadmap

- [ ] **GUI System** - Inventory-based menus
- [ ] **Economy Integration** - Vault support
- [ ] **Redis Support** - For multi-server setups  
- [ ] **Metrics Integration** - bStats integration
- [ ] **Update Checker** - Automatic update notifications

## 💝 Acknowledgments

- **PaperMC Team** - For the excellent server software
- **Kotlin Team** - For the amazing programming language
- **JetBrains** - For IntelliJ IDEA and Kotlin support

---

<div align="center">

**⭐ If this boilerplate helped you, please consider giving it a star! ⭐**

*This boilerplate saves you hours of setup time and provides a solid foundation for your PaperMC plugins!*

</div>
