package org.vertex.days7.mixin.player;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vertex.days7.data.BlockHPManager;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {

    @Final
    @Shadow protected ServerPlayer player;
    @Shadow protected ServerLevel level;

    /**
     * Полностью блокируем стандартное разрушение блоков
     */
    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"), cancellable = true)
    private void onHandleBlockBreakAction(BlockPos pos, ServerboundPlayerActionPacket.Action action,
                                          Direction direction, int maxBuildHeight, int sequence, CallbackInfo ci) {

        // В креативе разрешаем стандартное поведение
        if (player.gameMode.getGameModeForPlayer().isCreative()) {
            return;
        }

        // Spectator - блокируем полностью
        if (player.isSpectator()) {
            player.connection.send(new ClientboundBlockChangedAckPacket(sequence));
            ci.cancel();
            return;
        }

        // Для survival/adventure блокируем все действия разрушения
        switch (action) {
            case START_DESTROY_BLOCK:
            case STOP_DESTROY_BLOCK:
            case ABORT_DESTROY_BLOCK:
                // При отмене - сбрасываем урон и визуал
                if (action == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK ||
                        action == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
                    BlockHPManager.getInstance().clearBlockDamage(level, pos);
                    level.destroyBlockProgress(player.getId(), pos, -1);
                }

                // Отправляем подтверждение клиенту
                player.connection.send(new ClientboundBlockChangedAckPacket(sequence));
                ci.cancel();
                break;
        }
    }
}
