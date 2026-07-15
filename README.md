# JLT Worldz

Limit newly created Minecraft worlds to one or more chosen biomes while keeping
vanilla terrain shapes, caves, rivers, mountains, Nether, and End. An optional
circular starter biome can be forced around the world origin. Supports Fabric
and NeoForge for Minecraft 26.2.

## Supported loaders

| Loader | Status |
|---|---|
| Fabric | supported; requires Fabric API |
| NeoForge | supported |

Use the jar built for your loader; Fabric and NeoForge jars are not
interchangeable. Install the mod on the game or server that creates and hosts
the world. Clients that need to create a Worldz singleplayer world also need the
mod installed.

## Using Worldz

Worldz only affects worlds that explicitly select its preset. Vanilla and other
world types are untouched.

For singleplayer:

1. Start Minecraft once so the default config is created.
2. Edit `config/jlt_worldz.yaml` if desired, then restart Minecraft.
3. Create a world and select **Worldz** under **World Type**.

For a dedicated server, set these values before creating the world:

```properties
level-type=jlt_worldz:worldz
```

Delete or rename an existing `level-name` world only when you intentionally want
the server to create a new one. Worldz never converts an existing world.

## Configuration

The mod reads `config/jlt_worldz.yaml` at startup. A complete documented example
is available at [`config/jlt_worldz.example.yaml`](config/jlt_worldz.example.yaml).
If an older `jlt_worldz.json` exists and no YAML config exists yet, Worldz
migrates it automatically and retains the original as `jlt_worldz.json.bak`.

| Setting | Default | Description |
|---|---|---|
| `allowedBiomes` | `["minecraft:plains"]` | Biome ids and/or `#` biome-tag ids. A single biome produces a single-biome overworld. |
| `starterBiome` | `""` | Biome id forced around the origin; empty disables the starter zone. Tags are not accepted here. |
| `starterRadiusBlocks` | `512` | Inclusive circular radius, clamped to `64..4096` blocks. |
| `overworldBorder` | disabled | Optional square overworld border and resize schedule. |
| `netherBorder` | disabled | Optional independent Nether border and resize schedule. |

Short ids use the `minecraft` namespace, so `plains` and `minecraft:plains` are
equivalent. Examples:

```yaml
allowedBiomes:
  - 'minecraft:plains'
  - 'minecraft:desert'
  - 'minecraft:snowy_plains'
starterBiome: 'minecraft:cherry_grove'
starterRadiusBlocks: 512
```

```yaml
allowedBiomes:
  - '#minecraft:is_overworld'
starterBiome: ''
starterRadiusBlocks: 512
```

Quote biome tags in YAML because an unquoted `#` begins a comment.

### Limited-world borders

Borders use Minecraft's visible square world border centered at `(0, 0)`.
Radius values are the distance from the center to each side, so a radius of
`512` creates a `1024 × 1024` playable square. Borders affect only newly
created Worldz worlds.

```yaml
overworldBorder:
  enabled: true
  initialRadiusBlocks: 512
  finalRadiusBlocks: 2048
  resizeDays: 100
  ensureEndPortal: true
netherBorder:
  enabled: true
  initialRadiusBlocks: 256
  finalRadiusBlocks: 512
  resizeDays: 100
  ensureBlazeAccess: true
```

Equal initial and final radii make a static border. A larger final radius grows
linearly; a smaller one shrinks. `resizeDays: 0` applies the final radius
immediately. The transition uses elapsed Minecraft game time and resumes rather
than restarting when the save is reopened.

When a progression guarantee is enabled, Worldz uses a natural stronghold or
Nether fortress if one fits safely inside the final border. Otherwise it creates
a compact fallback near `(32, 0)`: a visible surface End-portal frame in the
overworld, or an enclosed nether-brick blaze-spawner room at approximately
`(32, 64, 0)` in the Nether. The fallback portal contains no eyes. Exact
coordinates are written to the game log. Fallback sites are placed within the
smallest supported border, so they may become reachable before a growing border
finishes its schedule.

Syntax errors use safe defaults and leave the broken file untouched. Invalid
list entries are logged and removed; unknown biome or tag ids are logged when a
new Worldz world is created. If none of the configured biomes can be used, the
mod falls back to the full vanilla overworld biome list so world creation still
succeeds.

Configuration is baked into a Worldz world's saved biome source when that world
is created. Later config edits affect only newly created worlds; reopening an
existing Worldz world keeps its original biome list, starter biome, starter
radius, and border schedules.

## How biome limiting works

Worldz filters vanilla's overworld multi-noise climate map to the allowed
biomes. Minecraft still chooses the closest climate entry at every position,
which preserves natural-looking regions when several biomes are allowed. The
starter biome overrides the entire vertical column inside its circular zone.

## Caveats

- Structures follow their allowed biomes. A plains-only world has no ocean
  monuments or jungle temples; villages, strongholds, and other structures can
  appear only where their own biome rules permit.
- Allowed biomes need vanilla overworld climate entries. Nether, End, and
  special biomes in `allowedBiomes` are ignored with a warning. Fixed-source
  support for those biomes is deferred.
- Existing worlds are never modified, and config changes do not alter worlds
  already created with Worldz.
- Nether and End generation remain vanilla in version 0.1.0.
- Worldz does not currently provide an in-game config screen or reload command.
- Shrinking a border does not delete chunks that were previously generated; it
  makes the area outside the new border inaccessible.

## Building and testing

Requires Java 25. Build both loader artifacts and run all tests with:

```bash
./gradlew build
```

Artifacts are written to:

- `fabric/build/libs/jlt_worldz-fabric-26.2-<version>.jar`
- `neoforge/build/libs/jlt_worldz-neoforge-26.2-<version>.jar`

The common test suite covers config handling, biome/tag syntax, climate-entry
filtering, starter-zone boundary math, and preset resource structure. Runtime
smoke testing should also create fresh Worldz worlds on both loaders and confirm
biome behavior in-game; see [`TODO.md`](TODO.md) for the current checklist.

## License

MIT
