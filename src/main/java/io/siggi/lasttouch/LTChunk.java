package io.siggi.lasttouch;

import io.siggi.anvilregionformat.AnvilUtil;
import io.siggi.anvilregionformat.ChunkCoordinate;
import io.siggi.anvilregionformat.ChunkData;
import io.siggi.lasttouch.data.LTBlockData;
import io.siggi.lasttouch.data.LTChunkData;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;

public class LTChunk {
    private final LastTouch plugin;
    private final World bukkitWorld;
    private final LTWorld world;
    private final Chunk chunk;
    private final ChunkCoordinate chunkCoordinate;
    private boolean hasCachedData = false;
    private long lastRead = 0L;
    private boolean unloaded = false;
    private final Map<Block, LTBlockData> changes = new HashMap<>();
    private long lastChange = 0L;

    LTChunk(LastTouch plugin, World bukkitWorld, LTWorld world, Chunk chunk) {
        this.plugin = plugin;
        this.bukkitWorld = bukkitWorld;
        this.world = world;
        this.chunk = chunk;
        this.chunkCoordinate = new ChunkCoordinate(chunk.getX(), chunk.getZ());
    }

    void ltTick(long time) {
        if (changes.isEmpty() || time - lastRead <= 30000L || time - lastChange <= 30000L) {
            return;
        }
        save();
    }

    public CompletableFuture<LTBlockData> getData(Block block) {
        lastRead = System.currentTimeMillis();
        if (hasCachedData) {
            LTBlockData ltBlockData = changes.get(block);
            return CompletableFuture.completedFuture(ltBlockData);
        }
        CompletableFuture<LTBlockData> result = new CompletableFuture<>();
        plugin.getWorkerExecutor().execute(() -> {
            try {
                ChunkData read = world.anvilRegion.read(chunkCoordinate);
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

    public void recordChange(Block block, UUID player) {
        long now = System.currentTimeMillis();
        lastChange = System.currentTimeMillis();
        changes.put(block, new LTBlockData(player, now));
    }

    void save() {
        if (changes.isEmpty()) return;
        if (lastChange == 0L) {
            hasCachedData = false;
            changes.clear();
            return;
        }
        Map<Block, LTBlockData> changesToWrite = new HashMap<>(changes);
        hasCachedData = false;
        changes.clear();
        lastRead = 0L;
        lastChange = 0L;
        plugin.getWorkerExecutor().execute(() -> {
            try {
                LTChunkData chunkData = newLTChunkData();
                ChunkData read = world.anvilRegion.read(chunkCoordinate);
                if (read != null) {
                    try {
                        chunkData.deserialize(read.getDecompressedData());
                    } catch (Exception e) {
                    }
                }
                chunkData.merge(changesToWrite);
                byte[] data = chunkData.serialize();
                byte[] compressedData = AnvilUtil.zlibCompress(data);
                world.anvilRegion.write(chunkCoordinate, ChunkData.create(compressedData));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private LTChunkData newLTChunkData() {
        return new LTChunkData(bukkitWorld, world, chunk, this);
    }

    public void unload() {
        if (unloaded) return;
        unloaded = true;
        save();
    }
}
