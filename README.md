# Lognumber Maven Plugin

Assigns unique numbers to log statements and keeps them consistent: `log.info("4777;message")`.

## Prerequisites

- Maven 3.9.0 or higher, running on Java 21 or higher (the projects you process may target older Java versions)

## Quick start

Add the range to the `pom.xml` of the project:

```xml
<properties>
    <log-assign.range>20_000,30_000</log-assign.range>
</properties>
```

Write new statements with the placeholder `;` or `0;` and run the plugin from the root directory:

```java
log.info("0;device connected");
```

```shell
mvn lognumber:assign
```

The short prefix needs `<pluginGroup>org.futureaspects.mojo</pluginGroup>` in `~/.m2/settings.xml`. Otherwise call
`mvn org.futureaspects.mojo:lognumber-maven-plugin:assign`.

## Range

The range is mandatory. The first value is the first allowed number, the second value the first excluded number.
Both `20_000,30_000` and `20.000,30.000` are valid.

- A module uses its own or inherited `log-assign.range`, otherwise the one of the reactor root.
- Modules with an identical range are checked together, across module borders.
- Modules without log statements and `pom` modules need no range.
- The run fails if a module with log statements has no range, or if different ranges overlap.
- Without a `pom.xml` the range is given by `-Dlog-assign.range=20_000,30_000`. Only `reset` works without a range.

Always start Maven from the root directory. The goals run once for the whole reactor, a run from a submodule cannot
see the other modules.

Scanned are the compile and test source roots, generated sources in the build directory are skipped. Without a
`pom.xml` the plugin scans `baseDir`.

## Goals

| Goal | Description |
|---|---|
| `lognumber:detect` | Reports the highest number, statements to assign, duplicates, numbers outside of the range and shared blocks. |
| `lognumber:assign` | Assigns numbers to all statements with `;` or `0;` and reports violations afterwards. |
| `lognumber:fix-duplicates` | Keeps one occurrence of every duplicated number and assigns new numbers to the others. |
| `lognumber:fix-range` | Assigns new numbers to everything outside of the range. |
| `lognumber:migrate-blocks` | Resolves blocks shared by several files. |
| `lognumber:rebase` | Renumbers all statements in blocks. |
| `lognumber:reset` | Resets all numbers to `0;`. |
| `lognumber:help` | Displays help. Use `mvn lognumber:help -Ddetail=true -Dgoal=<goal-name>` for parameter details. |

## Parameters

All goals:

| Parameter | Default | Description |
|---|---|---|
| `blockSize` | `10` | Numbers per block |
| `fileExtension` | `java` | Extension of the processed files |
| `loggerNamePattern` | `log\|logger\|s_logger\|LOG\|LOGGER\|Log\|getLog\(\)` | Regular expression of the logger names |
| `logLevelPattern` | `debug\|info\|warn\|trace\|error\|fatal\|v\|d\|i\|w\|e` | Regular expression of the log levels |
| `baseDir` | `.` | Folder to scan without a `pom.xml` |

All goals except `detect`:

| Parameter | Default | Description |
|---|---|---|
| `dryRun` | `false` | Shows the changes without writing them |
| `backupFiles` | `false` | Keeps a copy of every changed file |
| `backupExtension` | `.bak` | Extension of the copies |
| `files` | all | Comma-separated files to write, relative to the start directory. Everything is still analysed, which makes it suitable for pre-commit hooks. |
| `numberUnprefixed` | `false` | Also processes statements without any prefix, such as `log.info("message")` |

`detect`:

| Parameter | Default | Description |
|---|---|---|
| `failOnViolation` | `false` | Fails the build on duplicates or numbers outside of the range |
| `reportFile` | none | Writes the violations to this file (`group;type;number;file;line`) |

`rebase`:

| Parameter | Default | Description |
|---|---|---|
| `newBaseNumber` | start of the range | First number to use, must be inside of the range |

## Blocks

Numbers are grouped in blocks of `blockSize`, for example 20100 to 20109. A file only gets new numbers from blocks no
other file uses:

1. Free numbers in blocks only this file uses
2. The next block nobody uses

This way new numbers cannot collide with other files. Everything is computed before the first file is written. If
the range is too small the run stops with the number of needed and free blocks, and no file is changed.

A block used by several files is no violation, because existing numbers should stay stable. Only `migrate-blocks`
changes them: The file with the most numbers in a block keeps it, ties are resolved by path.

## Recognized statements

| Statement | Result |
|---|---|
| `log.info("message")` | Stays as it is, unless `numberUnprefixed` is set |
| `log.info(";message")`, `log.info("0;message")` | Gets a number |
| `log.info("4777;message")` | Keeps its number |

Multi-line statements are supported. Files keep their encoding (UTF-8 or ISO-8859-1), line endings and last line.

Text blocks and statements with another first argument, such as `Log.d(TAG, "message")`, are not recognized.

## Migrating from 1.x

- Add `log-assign.range` to every project. Pin the old version where this is not possible yet:
  `mvn org.futureaspects.mojo:lognumber-maven-plugin:1.3.5:assign`.
- Statements without a prefix are no longer numbered by default. Use `-DnumberUnprefixed=true` for the old behavior.
- `assign` no longer continues with the highest number but uses free blocks.
- `newBaseNumber` is optional and must be inside of the range.
- Goals run once for the reactor and no longer have a default lifecycle phase.
- Maven must run on Java 21.

To move existing numbers to blocks, run the goals once in this order and check them with `-DdryRun=true` first. They
change numbers which may be referenced by alerts or documentation.

```shell
mvn lognumber:detect
mvn lognumber:fix-range
mvn lognumber:fix-duplicates
mvn lognumber:migrate-blocks
```

Use this in CI to keep it that way:

```shell
mvn lognumber:detect -DfailOnViolation=true -DreportFile=target/lognumber-report.csv
```

## Development

```shell
mvn clean verify             # build and unit tests
mvn -Prun-its verify         # also runs the integration tests in src/it
```

## Contributing

Contributions are welcome. Please open an issue or submit a pull request on the [GitHub repository](https://github.com/smikesmike/lognumber-maven-plugin.git).

## License

This project is licensed under the terms of the [MIT License](https://opensource.org/licenses/MIT).
