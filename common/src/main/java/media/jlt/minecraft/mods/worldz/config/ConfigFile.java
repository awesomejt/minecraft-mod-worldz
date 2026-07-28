package media.jlt.minecraft.mods.worldz.config;

import java.util.List;

/**
 * One entry in {@link ConfigLayout}'s file map (DESIGN §43.2/§43.4.1, TODO 25.7a): the root-level
 * keys a given {@code config/jlt_worldz/} file owns, and whether that file is <em>unwrapped</em>.
 *
 * <p>A file that owns exactly one root key may be unwrapped: its root mapping <strong>is</strong>
 * that key's body (e.g. {@code cave.yaml} starts at {@code spawnY: -32}, with no {@code cave:}
 * line). A file owning several keys is wrapped: its root mapping is a partial copy of the config
 * root, keyed as today (e.g. {@code runtime.yaml}'s {@code foreverNight:}/{@code risingLava:}/
 * {@code structureDistance:} blocks). {@link ConfigLayout#FILES} enforces the "unwrapped owns
 * exactly one key" invariant declaratively rather than deriving {@link #unwrapped} from {@code
 * rootKeys.size()} -- {@code strip-world.yaml} owns two keys ({@code strip}/{@code stripWorld},
 * D10) yet is wrapped, so file-count alone can't distinguish the two shapes.
 *
 * @param relativePath path under {@code config/jlt_worldz/}, forward-slash separated (e.g. {@code
 *     "world-types/cave.yaml"})
 * @param rootKeys the {@link media.jlt.minecraft.mods.worldz.config.schema.WorldzRootSchema}
 *     root-level keys this file owns
 * @param unwrapped {@code true} when the file's root mapping is its one owned key's body directly,
 *     rather than a {@code key: {...}}-wrapped partial config root
 */
public record ConfigFile(String relativePath, List<String> rootKeys, boolean unwrapped) {
}
