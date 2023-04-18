package io.siggi.lasttouch.coordinate;

import java.util.Objects;
import org.bukkit.block.Block;

public final class LTBlockCoordinate {
    public final String world;
    public final int x;
    public final int y;
    public final int z;

    public LTBlockCoordinate(Block block) {
        this(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public LTBlockCoordinate(String world, int x, int y, int z) {
        if (world == null) throw new NullPointerException();
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public LTChunkCoordinate getChunkCoordinate() {
        return new LTChunkCoordinate(world, x >> 4, z >> 4);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LTBlockCoordinate that = (LTBlockCoordinate) o;
        return x == that.x && y == that.y && z == that.z && world.equals(that.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, y, z);
    }
}
