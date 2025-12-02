VITech Kotlin Notebook Utilities
==========================

Multi-module Kotlin/JVM utils to be used in Kotlin Notebooks:

- jdbc-wrapper — lightweight JDBC helpers for quick SQL querying
- maestro-wrapper — thin wrapper around the MCP Kotlin SDK to drive Maestro

Coordinates (via JitPack Maven repository):

- Group: `com.github.vitech-team`
- Artifacts: `jdbc-wrapper`, `maestro-wrapper`
- Version: `0.1.0` (or a Git tag)

Using in Kotlin Notebook (Kotlin kernel):

- Declare repository and dependencies at the top of a notebook or in a dedicated cell using `@file:` annotations.

JitPack repository and modules:

```text
@file:Repository("https://jitpack.io")
@file:DependsOn("com.github.vitech-team:jdbc-wrapper:0.1.0")
@file:DependsOn("com.github.vitech-team:maestro-wrapper:0.1.0")
```

Artifacts
---------

1) jdbc-wrapper

Simple extension functions on `java.sql.Connection`:

Notebook-friendly example (split into cells):

Initialization (dependencies + JDBC driver):

```text
@file:Repository("https://jitpack.io")
@file:DependsOn("com.github.vitech-team:jdbc-wrapper:0.1.0")
```

Setup (connection):

```kotlin
val conn = java.sql.DriverManager.getConnection(CONNECTION_STRING, USERNAME, PASSWORD)
```

Execution (run queries):

```kotlin
val one = conn.querySingle("select id, name from users where id = 123")
val many = conn.query("select id, name from users limit 10")
println(one)
println(many)
```

Cleanup:

```kotlin
conn.close()
```

2) maestro-wrapper

Wrapper around the MCP Kotlin SDK to talk to `maestro mcp` over stdio:

Notebook-friendly example (split into cells):

Initialization (dependencies):

```text
@file:Repository("https://jitpack.io")
@file:DependsOn("com.github.vitech-team:maestro-wrapper:0.1.0")
```

Setup (ensure Maestro CLI is installed and start the wrapper):

```kotlin
// Optionally specify a device id, otherwise the first connected device is used
val maestro = MaestroWrapper.start(/* requestedDeviceId = null */)
```

Execution (run a flow):

```kotlin
val yaml = """
appId: com.example.app
steps:
  - launchApp: {}
  - tapOn: { text: "Sign in" }
""".trimIndent()

val result = maestro.runFlow(yaml)
println(result)
```

Cleanup:

```kotlin
maestro.close()
```

License
-------

Apache-2.0
