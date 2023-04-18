package io.siggi.lasttouch.data;

import java.util.UUID;

public class LTBlockData {
    private final UUID user;
    private final long time;

    public LTBlockData(UUID user, long time) {
        this.user = user;
        this.time = time;
    }

    public UUID getUser() {
        return user;
    }

    public long getTime() {
        return time;
    }
}
