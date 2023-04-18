package io.siggi.lasttouch;

import io.siggi.anvilregionformat.AnvilRegion;
import io.siggi.lasttouch.coordinate.LTChunkCoordinate;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Chunk;

public class LTWorld {
    private final LastTouch plugin;
    final String world;
    final int minHeight;
    final int maxHeight;
    private final Map<LTChunkCoordinate, LTChunk> chunks = new HashMap<>();
    private boolean unloaded = false;
    AnvilRegion anvilRegion;

    LTWorld(LastTouch plugin, String world, int minHeight, int maxHeight) {
        this.plugin = plugin;
        if (world == null) throw new NullPointerException();
        this.world = world;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        File file = new File(new File(world), "lasttouch");
        anvilRegion = AnvilRegion.open(file);
    }

    void ltTick(long time) {
        chunks.values().removeIf(chunk -> chunk.ltTick(time));
    }

    void removeChunk(LTChunkCoordinate chunk) {
        LTChunk ltChunk = chunks.remove(chunk);
        if (ltChunk == null) return;
        ltChunk.unload();
    }

    public LTChunk getChunk(Chunk chunk) {
        return getChunk(new LTChunkCoordinate(chunk));
    }

    public LTChunk getChunk(LTChunkCoordinate chunk) {
        return chunks.computeIfAbsent(chunk, c -> new LTChunk(plugin, this, c));
    }

    public void unload() {
        if (unloaded) return;
        unloaded = true;
        List<LTChunkCoordinate> chunks = new ArrayList<>(this.chunks.keySet());
        for (LTChunkCoordinate chunk : chunks) {
            removeChunk(chunk);
        }
        plugin.getWorkerExecutor().execute(() -> {
            try {
                anvilRegion.close();
            } catch (Exception e) {
            }
        });
    }

    public void purge() {
        for (LTChunk chunk : chunks.values()) {
            chunk.save();
        }
    }
}
