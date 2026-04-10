package org.vertex.days7.network.system;

import org.vertex.days7.Days7;
import org.vertex.days7.data.BlockHPManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;
import org.vertex.days7.network.NetworkHandler;

import java.util.function.Supplier;

public class BlockDamagePacket {
    private final BlockPos pos;

    public BlockDamagePacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(BlockDamagePacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    public static BlockDamagePacket decode(FriendlyByteBuf buf) {
        return new BlockDamagePacket(buf.readBlockPos());
    }

    public static void handle(BlockDamagePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            BlockPos pos = msg.pos;
            BlockHPManager manager = BlockHPManager.getInstance();
            ItemStack tool = player.getMainHandItem();

            // Проверка разрешённого предмета
            if (!manager.canAttackWithItem(tool)) {
                Days7.LOGGER.debug("Player {} tried to attack with non-allowed item", player.getName().getString());
                return;
            }

            // Проверка дистанции
            double reachDistance = player.getAttributeValue(
                    net.minecraftforge.common.ForgeMod.BLOCK_REACH.get());
            double distSq = player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

            if (distSq > (reachDistance + 1) * (reachDistance + 1)) {
                Days7.LOGGER.debug("Player {} too far from block", player.getName().getString());
                return;
            }

            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                return;
            }

            // Проверяем неразрушаемость
            if (manager.isIndestructible(level, pos)) {
                NetworkHandler.sendToPlayer(new BlockHPSyncPacket(pos, -1, -1), player);
                return;
            }

            // Креатив
            if (player.gameMode.getGameModeForPlayer().isCreative()) {
                level.destroyBlock(pos, false, player);
                return;
            }

            if (!player.gameMode.getGameModeForPlayer().isSurvival()) {
                return;
            }

            // Сбрасываем кулдаун атаки на сервере тоже
            player.resetAttackStrengthTicker();

            int damage = manager.calculateDamage(tool, state);

            // Проверка что урон > 0
            if (damage <= 0) {
                Days7.LOGGER.debug("Damage is 0 for tool {}", tool);
                return;
            }

            int currentDamage = manager.getBlockDamage(level, pos);
            int maxHP = manager.getBlockMaxHP(state);

            currentDamage += damage;

            int currentHP = maxHP - currentDamage;
            int breakerId = manager.getBlockBreakerId(pos);

            Days7.LOGGER.debug("Block {} hit: damage={}, total={}, HP={}/{}",
                    pos, damage, currentDamage, currentHP, maxHP);

            // Звук удара
            SoundType soundType = state.getSoundType(level, pos, player);
            level.playSound(null, pos, soundType.getHitSound(), SoundSource.BLOCKS,
                    (soundType.getVolume() + 1.0F) / 8.0F,
                    soundType.getPitch() * 0.5F);

            if (currentHP <= 0) {
                // === БЛОК СЛОМАН ===
                manager.clearBlockDamage(level, pos);

                sendDestructionPacketToAll(level, breakerId, pos, -1);

                level.destroyBlock(pos, true, player);

                if (!tool.isEmpty()) {
                    tool.hurtAndBreak(1, player, (p) ->
                            p.broadcastBreakEvent(player.getUsedItemHand()));
                }

                NetworkHandler.sendToPlayer(new BlockDestroyedPacket(pos), player);

            } else {
                // === БЛОК ПОВРЕЖДЁН ===
                manager.setBlockDamage(level, pos, currentDamage);

                float damagePercent = (float) currentDamage / maxHP;
                int destroyStage = (int) (damagePercent * 10.0f);
                destroyStage = Math.min(9, Math.max(0, destroyStage));

                sendDestructionPacketToAll(level, breakerId, pos, destroyStage);

                // Отправляем HP всем ближайшим игрокам
                sendHPToNearbyPlayers(level, pos, currentHP, maxHP);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void sendDestructionPacketToAll(ServerLevel level, int breakerId, BlockPos pos, int stage) {
        ClientboundBlockDestructionPacket packet = new ClientboundBlockDestructionPacket(breakerId, pos, stage);

        for (ServerPlayer serverPlayer : level.getServer().getPlayerList().getPlayers()) {
            if (serverPlayer.level() == level) {
                double dx = pos.getX() - serverPlayer.getX();
                double dy = pos.getY() - serverPlayer.getY();
                double dz = pos.getZ() - serverPlayer.getZ();

                if (dx * dx + dy * dy + dz * dz < 4096.0) {
                    serverPlayer.connection.send(packet);
                }
            }
        }
    }

    private static void sendHPToNearbyPlayers(ServerLevel level, BlockPos pos, int currentHP, int maxHP) {
        BlockHPSyncPacket packet = new BlockHPSyncPacket(pos, currentHP, maxHP);

        for (ServerPlayer serverPlayer : level.getServer().getPlayerList().getPlayers()) {
            if (serverPlayer.level() == level) {
                double dx = pos.getX() - serverPlayer.getX();
                double dy = pos.getY() - serverPlayer.getY();
                double dz = pos.getZ() - serverPlayer.getZ();

                if (dx * dx + dy * dy + dz * dz < 4096.0) {
                    NetworkHandler.sendToPlayer(packet, serverPlayer);
                }
            }
        }
    }
}