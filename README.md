# Brigadier Inception

A simple but powerful Paper library for intercepting Brigadier commands at the node level — cleanly, reliably, and without touching raw command strings.

---

## The Problem

Imagine you're building a minigame plugin and you want to restrict the `/msg` command so that players inside the minigame can't use it. Simple enough — but how would you actually implement that?

---

### Option 1: Permissions

You could revoke the `minecraft.command.msg` permission from the player and restore it later. That works in theory, but it's stateful and brittle. You're responsible for restoring the permission at exactly the right moment, across every possible way a player might leave the game. Edge cases pile up fast.

---

### Option 2: `PlayerCommandPreprocessEvent`

`PlayerCommandPreprocessEvent` fires when a player sends a command, before Brigadier parses it. Sounds ideal — but it has serious limitations.

**You only have the raw input string.** So you check whether it starts with `/msg` and cancel if the player is in a minigame. But you also need to cover `/tell`, `/w`, `/minecraft:msg`, `/minecraft:tell`, and `/minecraft:w`. Miss one, and interception silently breaks.

Now the harder part: what if you also want to block *other* players from sending messages *to* someone in the minigame? The syntax is `/msg <targets> <message>`, where `targets` is a Brigadier player selector that can be a name, `@a`, `@p`, and more. Parsing that correctly without Brigadier's own parser is a significant undertaking.

And then there's `/execute`. The event *is* fired — but the raw string starts with `execute`, not `msg`. To determine what command is actually being run, by whom, and in what context, you'd have to parse the entire `/execute` chain yourself. That's a rabbit hole you don't want to go down.

---

### Option 3: Overriding `/msg`

You could register your own `/msg` command that mirrors the vanilla one. But this duplicates vanilla logic, conflicts with other plugins that do the same, and risks breaking whenever the vanilla command signature changes.

---

### Option 4: Brigadier Inception ✅

```kotlin
lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) {
    BrigadierInterceptor.build(it.registrar().dispatcher) {
        path("msg", "targets", "message")

        intercept {
            val sender = context.source.sender

            if (isSenderBlocked(sender)) {
                sender.sendMessage("You cannot use this command right now.")
                return@intercept 0
            }

            return@intercept runOriginal()
        }
    }
}
```

That's it.

---

## How It Works

We register a handler for the [`COMMANDS` lifecycle event](https://docs.papermc.io/paper/dev/lifecycle/), then call `BrigadierInterceptor.build` with the dispatcher from the command registrar.

`path("msg", "targets", "message")` navigates the Brigadier command tree: the first element is the command name, the rest are its argument nodes. For `/msg <targets> <message>`, the executable handler lives at the `message` node — and that's exactly where we install the interception.

You can call `path` multiple times. The interception block will be installed on every path you provide.

Inside the `intercept` block, `context` exposes the full `CommandContext`, including the command source and all parsed arguments. Call `runOriginal()` to let the command proceed normally, or return early with a result code to block it.

> **Note on argument types:** For intercepted vanilla commands, the arguments are the underlying NMS types that Paper wraps internally. This means Paper-specific resolvers like `PlayerSelectorArgumentResolver` are not available for those arguments. For vanilla commands like `/msg`, you need to use the corresponding NMS argument type directly — in this case, `net.minecraft.commands.arguments.EntityArgument`.

---

## Why Brigadier Inception

|                               | `PlayerCommandPreprocessEvent`                  | Brigadier Inception           |
|-------------------------------|-------------------------------------------------|-------------------------------|
| Works with `/execute`         | ⚠️ Fired, but you must parse the chain yourself | ✅ Always intercepted cleanly  |
| Targets exact Brigadier node  | ❌ String matching only                          | ✅ Node-level precision        |
| Access to parsed arguments    | ❌ Raw string only                               | ✅ Full `CommandContext`       |
| Aliases handled automatically | ❌ Must cover each one                           | ✅ Via node redirect           |

**On aliases:** In vanilla Minecraft, `/tell` and `/w` redirect internally to the same Brigadier node as `/msg`. Because Brigadier Inception replaces the handler directly on that node, all redirecting aliases automatically inherit the change — no duplicate interceptions needed.

---

## Finding the Right Path

Before you can intercept a command, you need to know how its Brigadier node tree is structured. This depends on how the command was registered.

### Native Brigadier Commands

Native Brigadier commands have an explicit tree structure. For `/msg <targets> <message>`, the path is `"msg", "targets", "message"` — each segment is the name of the corresponding node. You can explore any command's tree by inspecting the dispatcher directly or by using a debugging tool.

### Legacy Bukkit Commands (`CommandExecutor` / `BukkitCommand`)

Legacy commands are not built with Brigadier natively. However, to be able to use the command on the client side, Paper converts them into a minimal Brigadier node that looks like this on the client:

```
/<commandlabel> <args: greedy_string>
```

For a legacy command registered as `foo`, the interception path would be `"foo", "args"`.

### `BasicCommand`

Paper's [`BasicCommand`](https://docs.papermc.io/paper/dev/command-api/misc/basic-command/) follows the same pattern — it is also represented as `/<commandlabel> <args: greedy_string>` in the Brigadier tree, so the same path structure applies.

---

## API

### `BrigadierInterceptor`

The entry point. Use `build` for a concise one-shot setup:

```kotlin
BrigadierInterceptor.build(dispatcher) {
    path("msg", "targets", "message")

    intercept {
        runOriginal()
    }
}
```

Use `builder` for more explicit control, or when building interceptions programmatically:

```kotlin
val builder = BrigadierInterceptor.builder(dispatcher)

builder.path("msg", "targets", "message")
builder.intercept {
    runOriginal()
}

builder.install()
```

Note that `build` calls `install()` automatically — you only need to call it manually when using the `builder`.

---

### `InceptionContext`

Provided to every interception block. Exposes:

- `context: CommandContext<CommandSourceStack>` — the full Brigadier context, including source and parsed arguments
- `runOriginal(): Int` — executes the original command handler and returns its result

---

## Error Handling

Both errors are thrown during `install()`, so misconfigured paths are caught immediately at startup rather than failing silently at runtime.

- **`IllegalArgumentException`** — a segment in the provided path does not exist in the command tree
- **`IllegalStateException`** — the target node exists but has no command handler (i.e. it is not executable)

---

## Compatibility

| Platform | Status         |
|----------|----------------|
| Paper    | ✅ Supported    |
| Spigot   | ❌ Unsupported  |
| Folia    | ⚠️ Untested    |

---

## Example Project

Brigadier Inception is used in [Among Us in Minecraft](https://github.com/Fantamomo/among-us-in-minecraft/blob/main/src/main/kotlin/com/fantamomo/mc/amongus/command/AmongUsCommands.kt#L65-L145) to block `/msg` while a game is running — both to prevent players in the game from chatting privately, and to prevent outside players from messaging them.

---

## License

[MIT](LICENSE)

---

## Author

[Fantamomo](https://github.com/Fantamomo)