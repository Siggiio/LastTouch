package io.siggi.lasttouch.coordinate;

import io.siggi.anvilregionformat.ChunkCoordinate;
import java.util.Objects;
import org.bukkit.Chunk;

public final class LTChunkCoordinate {
    public final String world;
    public final int x;
    public final int z;

    public LTChunkCoordinate(Chunk chunk) {
        this(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    public LTChunkCoordinate(String world, int x, int z) {
        if (world == null) throw new NullPointerException();
        this.world = world;
        this.x = x;
        this.z = z;
    }

    public ChunkCoordinate toAnvilChunkCoordinate() {
        return new ChunkCoordinate(x, z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LTChunkCoordinate that = (LTChunkCoordinate) o;
        return x == that.x && z == that.z && world.equals(that.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, z);
    }
}
