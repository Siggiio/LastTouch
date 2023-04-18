package io.siggi.lasttouch.data;

import io.siggi.lasttouch.LTChunk;
import io.siggi.lasttouch.coordinate.LTBlockCoordinate;
import io.siggi.lasttouch.util.IOUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LTChunkData {
    private final LTChunk chunk;
    public final LTBlockData[] data;
    private final int minHeight;
    private final int maxHeight;
    private final int totalHeight;

    public LTChunkData(int minHeight, int maxHeight, LTChunk chunk) {
        this.chunk = chunk;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        totalHeight = maxHeight - minHeight;
        data = new LTBlockData[16 * 16 * totalHeight];
    }

    public void merge(Map<LTBlockCoordinate, LTBlockData> map) {
        for (Map.Entry<LTBlockCoordinate, LTBlockData> entry : map.entrySet()) {
            int index = getIndex(entry.getKey());
            if (index < 0 || index >= data.length) continue;
            data[index] = entry.getValue();
        }
    }

    public LTBlockCoordinate fromIndex(int index) {
        int x = index & 0xF;
        int z = (index >>> 4) & 0xF;
        int y = index >>> 8;
        y += minHeight;
        return new LTBlockCoordinate(
            chunk.chunkCoordinate.world,
            (chunk.chunkCoordinate.x * 16) + x,
            y,
            (chunk.chunkCoordinate.z * 16) + z
        );
    }

    private int getIndex(LTBlockCoordinate block) {
        int x = block.x & 0xf;
        int y = block.y;
        int z = block.z & 0xf;
        return getIndex(x, y, z);
    }

    private int getIndex(int x, int y, int z) {
        y -= minHeight;
        return x + (z * 16) + (y * 256);
    }

    public void deserialize(byte[] data) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(data);
            int version = (int) IOUtil.readVarInt(in);
            int userCount = (int) IOUtil.readVarInt(in);
            UUID[] uuids = new UUID[userCount];
            for (int i = 0; i < userCount; i++) {
                long mostSignificant = IOUtil.readLong(in);
                long leastSignificant = IOUtil.readLong(in);
                uuids[i] = new UUID(mostSignificant, leastSignificant);
            }
            for (int i = 0; i < this.data.length; i++) {
                int user = (int) IOUtil.readVarInt(in);
                if (user == 0L) continue;
                long time = IOUtil.readVarInt(in);
                this.data[i] = new LTBlockData(uuids[user - 1], time);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public byte[] serialize() {
        Map<UUID, UUIDInfo> map = new HashMap<>();
        for (LTBlockData data : this.data) {
            if (data == null) continue;
            UUIDInfo info = map.computeIfAbsent(data.getUser(), UUIDInfo::new);
            info.count += 1;
        }
        List<UUIDInfo> list = new ArrayList<>(map.values());
        list.sort((u1, u2) -> Integer.compare(u2.count, u1.count));
        Map<UUID, Integer> uuidMap = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            UUIDInfo info = list.get(i);
            uuidMap.put(info.uuid, i);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            IOUtil.writeVarInt(out, 0); // version
            IOUtil.writeVarInt(out, list.size());
            for (UUIDInfo info : list) {
                IOUtil.writeLong(out, info.uuid.getMostSignificantBits());
                IOUtil.writeLong(out, info.uuid.getLeastSignificantBits());
            }
            for (LTBlockData data : data) {
                if (data == null) {
                    IOUtil.writeVarInt(out, 0);
                    continue;
                }
                IOUtil.writeVarInt(out, uuidMap.get(data.getUser()) + 1);
                IOUtil.writeVarInt(out, data.getTime());
            }
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out.toByteArray();
    }
}
