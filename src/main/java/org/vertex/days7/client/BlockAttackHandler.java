package org.vertex.days7.client;

import org.vertex.days7.data.BlockHPManager;
import org.vertex.days7.network.NetworkHandler;
import org.vertex.days7.network.player.SwingStartPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

public class BlockAttackHandler {

    // Состояние замаха
    private static boolean isSwinging = false;
    private static int swingTicksRemaining = 0;
    private static int totalSwingTicks = 0;

    // Для автоматических ударов при удержании
    private static boolean isHoldingAttack = false;

    /**
     * Вызывается каждый клиентский тик
     */
    public static void onClientTick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.screen != null) {
            reset();
            return;
        }

        // Обработка замаха
        if (isSwinging) {
            swingTicksRemaining--;
            if (swingTicksRemaining <= 0) {
                isSwinging = false;
            }
        }

        // Автоматическое продолжение атаки при удержании
        if (isHoldingAttack && !isSwinging) {
            if (canStartNewSwing(mc.player)) {
                requestSwing(mc);
            }
        }
    }

    /**
     * Обработка начала атаки (нажатие ЛКМ)
     * Вызывается независимо от того куда смотрит игрок
     */
    public static boolean handleAttack(Minecraft mc) {
        LocalPlayer player = mc.player;

        if (player == null || mc.level == null) {
            return false;
        }

        ItemStack tool = player.getMainHandItem();
        BlockHPManager manager = BlockHPManager.getInstance();

        if (!manager.canAttackWithItem(tool)) {
            return false;
        }

        isHoldingAttack = true;

        // Начинаем замах если готовы
        if (canStartNewSwing(player) && !isSwinging) {
            requestSwing(mc);
        }

        return true;
    }

    /**
     * Обработка продолжения атаки (удержание ЛКМ)
     */
    public static void handleContinueAttack(Minecraft mc) {
        LocalPlayer player = mc.player;

        if (player == null || mc.level == null) {
            isHoldingAttack = false;
            return;
        }

        ItemStack tool = player.getMainHandItem();
        BlockHPManager manager = BlockHPManager.getInstance();

        if (!manager.canAttackWithItem(tool)) {
            isHoldingAttack = false;
            return;
        }

        isHoldingAttack = true;
    }

    /**
     * Отправить запрос на замах серверу
     */
    private static void requestSwing(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null) return;

        // Отправляем пакет на сервер
        NetworkHandler.sendToServer(SwingStartPacket.INSTANCE);

        // Клиентская предикция
        BlockHPManager manager = BlockHPManager.getInstance();
        int delayTicks = manager.getSwingDelayTicks(player.getMainHandItem());

        isSwinging = true;
        swingTicksRemaining = delayTicks;
        totalSwingTicks = delayTicks;
    }

    /**
     * Вызывается когда сервер подтверждает замах
     */
    public static void onSwingConfirmed(int swingTicks) {
        isSwinging = true;
        swingTicksRemaining = swingTicks;
        totalSwingTicks = swingTicks;
    }

    /**
     * Проверяет, можно ли начать новый замах
     */
    private static boolean canStartNewSwing(LocalPlayer player) {
        if (isSwinging) {
            return false;
        }

        float attackStrength = player.getAttackStrengthScale(0.0f);
        return attackStrength >= 0.9f;
    }

    /**
     * Вызывается когда отпускаем кнопку атаки
     */
    public static void stopAttack() {
        isHoldingAttack = false;
    }

    /**
     * Сброс состояния
     */
    public static void reset() {
        isSwinging = false;
        swingTicksRemaining = 0;
        totalSwingTicks = 0;
        isHoldingAttack = false;
    }

    public static boolean isSwinging() {
        return isSwinging;
    }

    public static boolean isHoldingAttack() {
        return isHoldingAttack;
    }

    public static float getSwingProgress() {
        if (!isSwinging || totalSwingTicks <= 0) return 0;
        return 1.0f - ((float) swingTicksRemaining / totalSwingTicks);
    }
}