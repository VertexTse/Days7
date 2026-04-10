package org.vertex.days7.network.player;

import org.vertex.days7.data.BlockHPManager;
import org.vertex.days7.data.SwingManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.vertex.days7.network.NetworkHandler;

import java.util.function.Supplier;

public class SwingStartPacket {
    public static final SwingStartPacket INSTANCE = new SwingStartPacket();

    public SwingStartPacket() {
    }

    public static void encode(SwingStartPacket msg, FriendlyByteBuf buf) {
        // Пустой пакет - просто сигнал начала замаха
    }

    public static SwingStartPacket decode(FriendlyByteBuf buf) {
        return INSTANCE;
    }

    public static void handle(SwingStartPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            BlockHPManager manager = BlockHPManager.getInstance();
            ItemStack tool = player.getMainHandItem();

            // Проверка разрешённого предмета
            if (!manager.canAttackWithItem(tool)) {
                return;
            }

            // Проверка кулдауна атаки
            float attackStrength = player.getAttackStrengthScale(0.0f);
            if (attackStrength < 0.9f) {
                return;
            }

            // Креатив - не используем систему замаха
            if (player.gameMode.getGameModeForPlayer().isCreative()) {
                return;
            }

            if (!player.gameMode.getGameModeForPlayer().isSurvival()) {
                return;
            }

            // Начинаем замах
            SwingManager swingManager = SwingManager.getInstance();
            if (swingManager.startSwing(player, tool)) {
                // Сбрасываем кулдаун атаки
                player.resetAttackStrengthTicker();

                // Анимация замаха на всех клиентах
                player.swing(InteractionHand.MAIN_HAND, true);

                // Отправляем подтверждение клиенту
                NetworkHandler.sendToPlayer(
                        new SwingConfirmPacket(manager.getSwingDelayTicks(tool)),
                        player
                );
            }
        });
        ctx.get().setPacketHandled(true);
    }
}