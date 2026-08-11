# Animations

A Bukkit/Paper plugin for building and playing block **animations** — sequences of frames that paste
into the world in order — with a range of triggers and optional sound. Originally by Ivan1pl;
maintained for [MCME](https://www.mcmiddleearth.com/).

- **Server:** Paper 26.2 (`api-version: 1.21`)
- **Required dependency:** [PluginUtils](https://github.com/MCME/PluginUtils) — pair the **2.0.x**
  release with a 26.2 server. Animations will not enable without it (`depend: [PluginUtils]`).

---

## Features

* Create a **stationary** animation (frames cycle in place) or a **moving** animation (a whole
  selection slides along a step vector — even players riding along, unless flying).
* Add, remove, reorder and update frames.
* Set the interval (in ticks) between frames.
* Attach a trigger so the animation plays on its own.
* Attach a sound.
* Limits to protect the server from lag (see `config.yml`).

## Triggers

| Trigger | Behaviour |
|---|---|
| **Range** | Plays when a player enters the range; reverses when everyone leaves. |
| **Loop** | Keeps playing while at least one player is in range. |
| **Block** | Plays when a player in range clicks a chosen block; reverses when everyone leaves. |
| **Password** | Plays when a player in range types a password in chat; reverses when everyone leaves. |
| **Two-block** | One block starts it, a second block reverses it. |
| **Chain** | Starts when another animation reaches a chosen frame. |

---

## For builders

Everything is under the `/anim` command and requires the `animations.admin` permission.

### Selecting a region

Animations are built from a cuboid **selection**. Hold the **selection wand** (a blaze powder by
default) and **left-click** one corner, **right-click** the opposite corner. Block triggers use a
separate **block selector** (a blaze rod by default).

### Classic editor (stable)

```
/anim create <name>     Start a new animation (opens the in-chat editor)
/anim edit <name>       Re-open an existing animation in the editor
/anim delete <name>     Delete an animation
/anim play <name>       Play an animation now
/anim list [page]       List animations
/anim sound <name> set <sound> <range> <pitch> <begin|end|all>
/anim sound <name> remove | info
```

Inside the in-chat editor, type **`help`** to see the frame/trigger/interval commands, and **`save`**
or **`cancel`** to finish. (Messages that should go to normal chat while editing can be prefixed with
the configured escape string, `!` by default.)

### Plot editor (v2.0 — experimental, in development)

A newer way to build **stationary** animations: instead of rebuilding one region over and over, each
frame gets its **own plot** in a private void world, so you can build frame-by-frame and see them
side by side. Under active development — currently namespaced under `/anim plot` and covers session
setup and navigation; frame capture/save is still being built.

```
/anim plot create <name>   Start a plot session from your current wand selection
/anim plot new             Add the next frame plot (and teleport to it)
/anim plot goto <n>        Teleport to frame n
/anim plot exit            Leave the session (clears your plots)
/anim editworld            Teleport into the edit world (debug)
```

> The plot editor's edit world is scratch space — see the administration notes below.

---

## For server administrators

### Installation

1. Install a compatible **PluginUtils** (2.0.x for a Paper 26.2 server) in `plugins/`.
2. Drop `Animations-<version>.jar` in `plugins/`.
3. Start the server. Animations refuses to enable if PluginUtils is missing (by design).

### Permission

| Node | Grants |
|---|---|
| `animations.admin` | All `/anim` commands and wand use. |

Operators have it by default; grant it via your permissions plugin for non-op staff. Players without
the node cannot see or tab-complete any `/anim` command.

### Configuration (`config.yml`)

```yaml
wand: BLAZE_POWDER            # selection wand material
blockSelectorWand: BLAZE_ROD  # block-trigger selector material

limits:                       # set any to -1 to disable
  maxFrameSize: 50000         # max block volume of one frame
  maxRunningAnimations: 50    # max animations playing at once
  maxProcessedBlocks: 100000  # max blocks processed at once

editor:
  timeout: 600                # in-chat editor inactivity timeout (seconds)
  escapeString: '!'           # prefix to send chat while in the editor ('' disables)
  plot:                       # plot editor (v2.0)
    world: animations_edit    # name of the private void build world
    laneSpacing: 2048         # Z spacing between editors' lanes
    frameGap: 8               # X gap between frame plots
    baseY: 64                 # Y level plots are built at

debug:
  enabled: false              # extra logging
```

### The plot-editor world

The plot editor builds in a **dedicated void world** (`animations_edit` by default), created
automatically on enable. It holds **no real terrain**, so nothing you build there affects the live
map.

**It is scratch space and is regenerated from empty on every server start.** Editing sessions are
held in memory (not yet persisted across restarts), so any plots left in that world from a previous
run are stale and are cleared on boot. Do not store anything of value in `animations_edit`.

### Data

Animations are stored under `plugins/<plugin-data-folder>/animations/<name>/` as an `animation.yml`
plus `frame_N.mcme` frame files. Editing a stationary animation to remove frames automatically prunes
now-orphaned `frame_N.mcme` files on save.

---

## Building from source

Gradle build; requires a **JDK 25** toolchain (Gradle itself must run on it).

```bash
export JAVA_HOME=/path/to/jdk-25
./gradlew build          # compiles, runs tests, formats-check, builds the shaded jar
./gradlew spotlessApply  # apply the palantir-java-format code style
```

The jar is written to `build/libs/`. PluginUtils is resolved from
[JitPack](https://jitpack.io/#MCME/PluginUtils) and is **not** shaded in — it must be present on the
server as its own plugin.
