package io.mr_w98.justvoid.platform;

import pumpkin.common.BlockPos;
import pumpkin.world.ChunkBuffer;

public record Platform(long minX, long maxX, long y, long minZ, long maxZ) {

    public static Platform under(BlockPos spawn, int size) {
        long minX = (long) spawn.x() - size / 2;
        long minZ = (long) spawn.z() - size / 2;
        return new Platform(minX, minX + size - 1, (long) spawn.y() - 1, minZ, minZ + size - 1);
    }

    public void place(ChunkBuffer chunk, int stateId) {
        long startX = chunk.getX() * 16L;
        long startZ = chunk.getZ() * 16L;
        long fromX = Math.max(minX, startX);
        long toX = Math.min(maxX, startX + 15);
        long fromZ = Math.max(minZ, startZ);
        long toZ = Math.min(maxZ, startZ + 15);
        if (fromX > toX || fromZ > toZ) return;

        long minY = chunk.getMinY();
        if (y < minY || y >= minY + chunk.getHeight()) return;

        chunk.fillCuboid((int) (fromX - startX), (int) y, (int) (fromZ - startZ), (int) (toX - startX), (int) y, (int) (toZ - startZ), stateId);
    }
}
