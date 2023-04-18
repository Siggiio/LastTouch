package io.siggi.lasttouch;

import io.siggi.anvilregionformat.AnvilUtil;
import io.siggi.anvilregionformat.ChunkData;
import io.siggi.lasttouch.coordinate.LTBlockCoordinate;
import io.siggi.lasttouch.coordinate.LTChunkCoordinate;
import io.siggi.lasttouch.data.LTBlockData;
import io.siggi.lasttouch.data.LTChunkData;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.block.Block;

public class LTChunk {
    private final LastTouch plugin;
    private final LTWorld world;
    public final LTChunkCoordinate chunkCoordinate;
    private boolean hasCachedData = false;
    private long lastRead = 0L;
    private boolean unloaded = false;
    private final Map<LTBlockCoordinate, LTBlockData> changes = new HashMap<>();
    private long lastChange = 0L;

    LTChunk(LastTouch plugin, LTWorld world, LTChunkCoordinate chunkCoordinate) {
        this.plugin = plugin;
        this.world = world;
        this.chunkCoordinate = chunkCoordinate;
    }

    boolean ltTick(long time) {
        if (changes.isEmpty() || time - lastRead <= 30000L || time - lastChange <= 30000L) {
            return false;
        }
        unload();
        return true;
    }

    public CompletableFuture<LTBlockData> getData(Block block) {
        return getData(new LTBlockCoordinate(block));
    }

    public CompletableFuture<LTBlockData> getData(LTBlockCoordinate block) {
        if (unloaded)
            throw new IllegalStateException("This LTChunk instance has been unloaded. Do not cache LTChunk instances, obtain a new one for each use.");
        lastRead = System.currentTimeMillis();
        if (hasCachedData) {
            LTBlockData ltBlockData = changes.get(block);
            return CompletableFuture.completedFuture(ltBlockData);
        }
        CompletableFuture<LTBlockData> result = new CompletableFuture<>();
        plugin.getWorkerExecutor().execute(() -> {
            try {
                ChunkData read = world.anvilRegion.read(chunkCoordinate.toAnvilChunkCoordinate());
                LTChunkData chunkData = newLTChunkData();
                if (read != null) {
                    chunkData.deserialize(read.getDecompressedData());
                }
                plugin.getBukkitExecutor().execute(() -> {
                    if (unloaded) {
                        result.complete(null);
                        return;
                    }
                    if (!hasCachedData) {
                        hasCachedData = true;
                        for (int i = 0; i < chunkData.data.length; i++) {
                            LTBlockData data = chunkData.data[i];
                            if (data != null) {
                                changes.putIfAbsent(chunkData.fromIndex(i), data);
                            }
                        }
                    }
                    result.complete(changes.get(block));
                });
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        });
        return result;
    }

    public void recordChange(LTBlockCoordinate block, UUID player) {
        if (unloaded)
            throw new IllegalStateException("This LTChunk instance has been unloaded. Do not cache LTChunk instances, obtain a new one for each use.");
        long now = System.currentTimeMillis();
        lastChange = System.currentTimeMillis();
        changes.put(block, new LTBlockData(player, now));
    }

    public void save() {
        if (unloaded)
            throw new IllegalStateException("This LTChunk instance has been unloaded. Do not cache LTChunk instances, obtain a new one for each use.");
        if (changes.isEmpty()) return;
        if (lastChange == 0L) {
            hasCachedData = false;
            changes.clear();
            return;
        }
        Map<LTBlockCoordinate, LTBlockData> changesToWrite = new HashMap<>(changes);
        hasCachedData = false;
        changes.clear();
        lastRead = 0L;
        lastChange = 0L;
        plugin.getWorkerExecutor().execute(() -> {
            try {
                LTChunkData chunkData = newLTChunkData();
                ChunkData read = world.anvilRegion.read(chunkCoordinate.toAnvilChunkCoordinate());
                if (read != null) {
                    try {
                        chunkData.deserialize(read.getDecompressedData());
                    } catch (Exception e) {
                    }
                }
                chunkData.merge(changesToWrite);
                byte[] data = chunkData.serialize();
                byte[] compressedData = AnvilUtil.zlibCompress(data);
                world.anvilRegion.write(chunkCoordinate.toAnvilChunkCoordinate(), ChunkData.create(compressedData));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private LTChunkData newLTChunkData() {
        return new LTChunkData(world.minHeight, world.maxHeight, this);
    }

    void unload() {
        if (unloaded) return;
        save();
        unloaded = true;
    }
}
