package ua.noe.block;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Remembers which placed blocks are NoE custom blocks. Data lives in the chunk's PersistentDataContainer,
 * is cached while the chunk is loaded and written through on every change (changes are rare).
 */
public final class CustomBlockStore {

    private record Entry(int x, int y, int z, String id) {
    }

    private final NamespacedKey key;
    private final Map<UUID, Map<Long, Map<Long, Entry>>> cache = new HashMap<>();

    public CustomBlockStore(NamespacedKey key) {
        this.key = key;
    }

    /** @return the NoE id of the block at this position, or {@code null} for ordinary blocks. */
    public String getId(Block block) {
        Entry e = chunkData(block.getChunk()).get(blockKey(block.getX(), block.getY(), block.getZ()));
        return e == null ? null : e.id();
    }

    public boolean isCustom(Block block) {
        return getId(block) != null;
    }

    public void put(Block block, String id) {
        Chunk chunk = block.getChunk();
        Map<Long, Entry> data = chunkData(chunk);
        data.put(blockKey(block.getX(), block.getY(), block.getZ()), new Entry(block.getX(), block.getY(), block.getZ(), id));
        save(chunk, data);
    }

    public void remove(Block block) {
        Chunk chunk = block.getChunk();
        Map<Long, Entry> data = chunkData(chunk);
        if (data.remove(blockKey(block.getX(), block.getY(), block.getZ())) != null) {
            save(chunk, data);
        }
    }

    /** Drops the cache of an unloaded chunk. */
    public void unload(Chunk chunk) {
        Map<Long, Map<Long, Entry>> world = cache.get(chunk.getWorld().getUID());
        if (world != null) {
            world.remove(chunkKey(chunk.getX(), chunk.getZ()));
        }
    }

    public void unload(World world) {
        cache.remove(world.getUID());
    }

    public void clear() {
        cache.clear();
    }

    private Map<Long, Entry> chunkData(Chunk chunk) {
        Map<Long, Map<Long, Entry>> world = cache.computeIfAbsent(chunk.getWorld().getUID(), u -> new HashMap<>());
        return world.computeIfAbsent(chunkKey(chunk.getX(), chunk.getZ()), k -> read(chunk));
    }

    private Map<Long, Entry> read(Chunk chunk) {
        Map<Long, Entry> data = new LinkedHashMap<>();
        String raw = chunk.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (raw == null || raw.isEmpty()) {
            return data;
        }
        for (String part : raw.split(";")) {
            String[] f = part.split(",", 4);
            if (f.length != 4) {
                continue;
            }
            try {
                int x = Integer.parseInt(f[0]);
                int y = Integer.parseInt(f[1]);
                int z = Integer.parseInt(f[2]);
                data.put(blockKey(x, y, z), new Entry(x, y, z, f[3]));
            } catch (NumberFormatException ignored) {
                // skip a corrupted entry, keep the rest
            }
        }
        return data;
    }

    private void save(Chunk chunk, Map<Long, Entry> data) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        if (data.isEmpty()) {
            pdc.remove(key);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Entry e : data.values()) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(e.x()).append(',').append(e.y()).append(',').append(e.z()).append(',').append(e.id());
        }
        pdc.set(key, PersistentDataType.STRING, sb.toString());
    }

    static long chunkKey(int cx, int cz) {
        return ((long) cx & 0xFFFFFFFFL) | ((long) cz << 32);
    }

    static long blockKey(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFFL);
    }
}
