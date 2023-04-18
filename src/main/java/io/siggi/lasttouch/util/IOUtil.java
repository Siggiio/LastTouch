package io.siggi.lasttouch.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtil {
    private IOUtil() {
    }

    public static long readVarInt(InputStream in) throws IOException {
        long value = 0;
        int shift = 0;
        int read;
        do {
            if (shift >= 64) {
                throw new IndexOutOfBoundsException("VarInt too large");
            }
            read = read(in);
            value |= ((long) (read & 0x7f)) << shift;
            shift += 7;
        } while ((read & 0x80) != 0);
        return value;
    }

    public static int writeVarInt(OutputStream out, long value) throws IOException {
        int count = 0;
        do {
            int outputByte = (int) (value & 0x7F);
            value >>>= 7;
            outputByte |= (value == 0 ? 0 : 0x80);
            if (out != null) out.write(outputByte);
        } while (value != 0);
        return count;
    }

    public static int read(InputStream in) throws IOException {
        int value = in.read();
        if (value == -1) throw new EOFException();
        return value;
    }

    public static long readLong(InputStream in) throws IOException {
        return (((long) read(in)) << 56) | (((long) read(in)) << 48) | (((long) read(in)) << 40) | (((long) read(in)) << 32) |
            (((long) read(in)) << 24) | (((long) read(in)) << 16) | (((long) read(in)) << 8) | ((long) read(in));
    }

    public static void writeLong(OutputStream out, long value) throws IOException {
        out.write((int) ((value >>> 56) & 0xff));
        out.write((int) ((value >>> 48) & 0xff));
        out.write((int) ((value >>> 40) & 0xff));
        out.write((int) ((value >>> 32) & 0xff));
        out.write((int) ((value >>> 24) & 0xff));
        out.write((int) ((value >>> 16) & 0xff));
        out.write((int) ((value >>> 8) & 0xff));
        out.write((int) (value & 0xff));
    }
}
