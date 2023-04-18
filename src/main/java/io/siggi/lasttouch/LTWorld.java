package io.siggi.lasttouch;

import io.siggi.anvilregionformat.AnvilRegion;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Chunk;
import org.bukkit.World;

public class LTWorld {
    private final LastTouch plugin;
    private final World world;
    private final Map<Chunk, LTChunk> chunks = new HashMap<>();
    private boolean unloaded = false;
    AnvilRegion anvilRegion;

    LTWorld(LastTouch plugin, World world) {
        this.plugin = plugin;
        this.world = world;
        File file = new File(new File(world.getName()), "lasttouch");
        anvilRegion = AnvilRegion.open(file);
        for (Chunk chunk : world.getLoadedChunks()) {
            addChunk(chunk);
        }
    }

    void ltTick(long time) {
        for (LTChunk chunk : chunks.values()) {
            chunk.ltTick(time);
        }
    }

    void addChunk(Chunk chunk) {
        if (unloaded) return;
        LTChunk ltChunk = chunks.get(chunk);
        if (ltChunk != null) return;
        ltChunk = new LTChunk(plugin, world, this, chunk);
        chunks.put(chunk, ltChunk);
    }

    void removeChunk(Chunk chunk) {
        LTChunk ltChunk = chunks.get(chunk);
        if (ltChunk == null) return;
        ltChunk.unload();
        chunks.remove(chunk);
    }

    public LTChunk getChunk(Chunk chunk) {
        return chunks.get(chunk);
    }

    public void unload() {
        if (unloaded) return;
        unloaded = true;
        List<Chunk> chunks = new ArrayList<>(this.chunks.keySet());
        for (Chunk chunk : chunks) {
            removeChunk(chunk);
        }
        plugin.getWorkerExecutor().execute(() -> {
            try {
                anvilRegion.close();
            } catch (Exception e) {
            }
        });
    }
}
