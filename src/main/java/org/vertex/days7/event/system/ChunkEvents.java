// event/ChunkEvents.java
package org.vertex.days7.event.system;

import org.vertex.days7.Days7;
import org.vertex.days7.capability.ChunkBlockDamageCapability;
import org.vertex.days7.data.BlockHPManager;
import org.vertex.days7.network.system.BlockHPSyncPacket;
import org.vertex.days7.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Days7.MODID)
public class ChunkEvents {

    @SubscribeEvent
    public static void onChunkWatch(ChunkWatchEvent.Watch event) {
        ServerPlayer player = event.getPlayer();
        LevelChunk chunk = event.getChunk();
        ChunkPos chunkPos = chunk.getPos();

        chunk.getCapability(ChunkBlockDamageCapability.CAPABILITY).ifPresent(cap -> {
            if (cap.isEmpty()) return;

            BlockHPManager manager = BlockHPManager.getInstance();
            List<Long> toRemove = new ArrayList<>();

            // Отправляем повреждённые блоки
            for (Map.Entry<Long, Integer> entry : cap.getAllDamage().entrySet()) {
                BlockPos pos = decodeLocalPos(entry.getKey(), chunkPos.x, chunkPos.z);
                int damage = entry.getValue();

                BlockState state = player.serverLevel().getBlockState(pos);

                if (state.isAir()) {
                    toRemove.add(entry.getKey());
                    continue;
                }

                if (damage > 0) {
                    int maxHP = manager.getBlockMaxHP(state);
                    int currentHP = maxHP - damage;

                    float damagePercent = (float) damage / maxHP;
                    int destroyStage = (int) (damagePercent * 10.0f);
                    destroyStage = Math.min(9, Math.max(0, destroyStage));

                    // Трещины
                    int breakerId = manager.getBlockBreakerId(pos);
                    player.connection.send(new ClientboundBlockDestructionPacket(
                            breakerId, pos, destroyStage
                    ));

                    // HP данные (currentHP, maxHP)
                    NetworkHandler.sendToPlayer(new BlockHPSyncPacket(pos, currentHP, maxHP), player);
                }
            }

            // Отправляем неразрушаемые блоки
            for (Long key : cap.getAllIndestructible()) {
                BlockPos pos = decodeLocalPos(key, chunkPos.x, chunkPos.z);
                BlockState state = player.serverLevel().getBlockState(pos);

                if (state.isAir()) {
                    continue;
                }

                // Отправляем как неразрушаемый (-1, -1)
                NetworkHandler.sendToPlayer(new BlockHPSyncPacket(pos, -1, -1), player);
            }

            // Очищаем невалидные
            if (!toRemove.isEmpty()) {
                for (Long key : toRemove) {
                    cap.clearDamageByKey(key);
                }
                chunk.setUnsaved(true);
            }
        });
    }

    private static BlockPos decodeLocalPos(long encoded, int chunkX, int chunkZ) {
        int localX = (int) (encoded & 15);
        int localZ = (int) ((encoded >> 4) & 15);
        int y = (int) ((encoded >> 8) & 1023) - 64;
        return new BlockPos(chunkX * 16 + localX, y, chunkZ * 16 + localZ);
    }
}