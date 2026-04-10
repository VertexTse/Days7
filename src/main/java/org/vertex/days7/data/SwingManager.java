package org.vertex.days7.data;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SwingManager {

    private static final SwingManager INSTANCE = new SwingManager();

    private final Map<UUID, SwingData> playerSwings = new ConcurrentHashMap<>();

    public static SwingManager getInstance() {
        return INSTANCE;
    }

    /**
     * Начать замах
     * @return true если замах начат, false если уже в процессе замаха
     */
    public boolean startSwing(ServerPlayer player, ItemStack tool) {
        UUID playerId = player.getUUID();

        SwingData existingSwing = playerSwings.get(playerId);
        if (existingSwing != null && !existingSwing.isCompleted()) {
            return false;
        }

        BlockHPManager manager = BlockHPManager.getInstance();
        int delayTicks = manager.getSwingDelayTicks(tool);

        SwingData swing = new SwingData(tool.copy(), delayTicks);
        playerSwings.put(playerId, swing);

        return true;
    }

    /**
     * Обновить замах (вызывается каждый тик)
     * @return SwingData если замах завершён и нужно нанести урон, null иначе
     */
    public SwingData tickSwing(ServerPlayer player) {
        UUID playerId = player.getUUID();
        SwingData swing = playerSwings.get(playerId);

        if (swing == null) {
            return null;
        }

        swing.tick();

        if (swing.isCompleted()) {
            playerSwings.remove(playerId);
            return swing;
        }

        return null;
    }

    /**
     * Проверить, в процессе ли замаха игрок
     */
    public boolean isSwinging(ServerPlayer player) {
        SwingData swing = playerSwings.get(player.getUUID());
        return swing != null && !swing.isCompleted();
    }

    /**
     * Получить прогресс замаха (0.0 - 1.0)
     */
    public float getSwingProgress(ServerPlayer player) {
        SwingData swing = playerSwings.get(player.getUUID());
        if (swing == null) {
            return 0;
        }
        return swing.getProgress();
    }

    /**
     * Отменить замах
     */
    public void cancelSwing(ServerPlayer player) {
        playerSwings.remove(player.getUUID());
    }

    /**
     * Очистить данные игрока (при выходе)
     */
    public void clearPlayer(ServerPlayer player) {
        playerSwings.remove(player.getUUID());
    }

    /**
     * Данные о замахе
     */
    public static class SwingData {
        private final ItemStack tool;
        private final int totalTicks;
        private int remainingTicks;

        public SwingData(ItemStack tool, int totalTicks) {
            this.tool = tool;
            this.totalTicks = totalTicks;
            this.remainingTicks = totalTicks;
        }

        public void tick() {
            if (remainingTicks > 0) {
                remainingTicks--;
            }
        }

        public boolean isCompleted() {
            return remainingTicks <= 0;
        }

        public float getProgress() {
            if (totalTicks <= 0) return 1.0f;
            return 1.0f - ((float) remainingTicks / totalTicks);
        }

        public ItemStack getTool() {
            return tool;
        }

        public int getRemainingTicks() {
            return remainingTicks;
        }

        public int getTotalTicks() {
            return totalTicks;
        }
    }
}