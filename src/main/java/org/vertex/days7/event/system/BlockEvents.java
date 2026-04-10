package org.vertex.days7.event.system;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.vertex.days7.Days7;
import org.vertex.days7.data.BlockHPManager;

@Mod.EventBusSubscriber(modid = Days7.MODID)
public class BlockEvents {

    /**
     * Очищаем урон когда блок ломается другим способом
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;

        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();

        clearBlockDamageAndNotify(level, pos);
    }

    /**
     * Очищаем урон когда блок заменяется
     */
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;

        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();

        clearBlockDamageAndNotify(level, pos);
    }

    private static void clearBlockDamageAndNotify(Level level, BlockPos pos) {
        BlockHPManager manager = BlockHPManager.getInstance();

        if (manager.hasBlockDamage(level, pos)) {
            int breakerId = manager.getBlockBreakerId(pos);
            manager.clearBlockDamage(level, pos);

            // Уведомляем всех игроков об удалении трещин
            if (level instanceof ServerLevel serverLevel) {
                ClientboundBlockDestructionPacket packet =
                        new ClientboundBlockDestructionPacket(breakerId, pos, -1);

                for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
                    if (player.level() == level) {
                        double dist = player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
                        if (dist < 4096) {
                            player.connection.send(packet);
                        }
                    }
                }
            }
        }
    }
}
