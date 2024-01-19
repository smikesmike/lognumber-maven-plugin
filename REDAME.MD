This is the Lognumber Maven Plugin, a tool designed to assist developers in managing and manipulating log numbers in their Java projects.

## Prerequisites

- Java 1.8 or higher
- Maven 3.9.0 or higher

## Installation

To install the plugin, add the following to your `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.futureaspects.mojo</groupId>
            <artifactId>lognumber-maven-plugin</artifactId>
            <version>1.3.3-SNAPSHOT</version>
        </plugin>
    </plugins>
</build>
```

## Goals

The plugin has the following goals:

- `lognumber:assign`: Searches for the highest lognumber and adds a lognumber to all missing positions or statements beginning with '0;'.
- `lognumber:detect`: Detects the highest lognumber in all given files.
- `lognumber:help`: Display help information on lognumber-maven-plugin. Call `mvn lognumber:help -Ddetail=true -Dgoal=<goal-name>` to display parameter details.
- `lognumber:rebase`: Reassign all logging statements to be based on a new number set.
- `lognumber:reset`: Resets all logging statements to start with '0;'.

## Contributing

Contributions are welcome. Please open an issue or submit a pull request on the [GitHub repository](https://github.com/smikesmike/lognumber-maven-plugin.git).

## License

This project is licensed under the terms of the [MIT License](https://opensource.org/licenses/MIT).
