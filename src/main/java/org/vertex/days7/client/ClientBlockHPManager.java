// client/ClientBlockHPManager.java
package org.vertex.days7.client;

import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientBlockHPManager {
    private static final ClientBlockHPManager INSTANCE = new ClientBlockHPManager();

    // pos -> [currentHP, maxHP]
    // maxHP = -1 означает неразрушаемый блок
    private final Map<Long, int[]> blockHPData = new ConcurrentHashMap<>();

    public static ClientBlockHPManager getInstance() {
        return INSTANCE;
    }

    /**
     * Устанавливает HP блока
     * @param currentHP текущее HP
     * @param maxHP максимальное HP (-1 = неразрушаемый)
     */
    public void setBlockHP(BlockPos pos, int currentHP, int maxHP) {
        long key = pos.asLong();
        blockHPData.put(key, new int[]{currentHP, maxHP});
    }

    /**
     * @return [currentHP, maxHP] или null если нет данных
     */
    public int[] getBlockHP(BlockPos pos) {
        return blockHPData.get(pos.asLong());
    }

    public void clearBlockHP(BlockPos pos) {
        blockHPData.remove(pos.asLong());
    }

    /**
     * Проверяет, повреждён ли блок (HP < MaxHP)
     */
    public boolean isDamaged(BlockPos pos) {
        int[] hp = blockHPData.get(pos.asLong());
        if (hp == null) return false;
        if (hp[1] == -1) return false; // Неразрушаемый
        return hp[0] < hp[1];
    }

    /**
     * Проверяет, является ли блок неразрушаемым
     */
    public boolean isIndestructible(BlockPos pos) {
        int[] hp = blockHPData.get(pos.asLong());
        return hp != null && hp[1] == -1;
    }

    public void clearAll() {
        blockHPData.clear();
    }
}