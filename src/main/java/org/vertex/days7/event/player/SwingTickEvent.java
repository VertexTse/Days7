package org.vertex.days7.event.player;

import org.vertex.days7.Days7;
import org.vertex.days7.data.BlockHPManager;
import org.vertex.days7.network.system.BlockDestroyedPacket;
import org.vertex.days7.network.system.BlockHPSyncPacket;
import org.vertex.days7.network.NetworkHandler;
import org.vertex.days7.data.SwingManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = Days7.MODID)
public class SwingTickEvent {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        SwingManager swingManager = SwingManager.getInstance();

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            SwingManager.SwingData completedSwing = swingManager.tickSwing(player);

            if (completedSwing != null) {
                executeSwingDamage(player, completedSwing);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            SwingManager.getInstance().clearPlayer(serverPlayer);
        }
    }

    private static void executeSwingDamage(ServerPlayer player, SwingManager.SwingData swing) {
        ServerLevel level = player.serverLevel();
        BlockHPManager manager = BlockHPManager.getInstance();
        ItemStack currentTool = player.getMainHandItem();

        if (!manager.canAttackWithItem(currentTool)) {
            return;
        }

        // Дистанция для атаки сущностей (3 блока стандарт)
        double entityReachDistance = player.getAttributeValue(
                net.minecraftforge.common.ForgeMod.ENTITY_REACH.get());

        // Дистанция для блоков
        double blockReachDistance = player.getAttributeValue(
                net.minecraftforge.common.ForgeMod.BLOCK_REACH.get());

        // Сначала проверяем сущности
        EntityHitResult entityHit = getTargetEntity(player, entityReachDistance);

        if (entityHit != null && entityHit.getEntity() != null) {
            executeEntityDamage(player, entityHit.getEntity(), currentTool);
            return;
        }

        // Затем проверяем блоки
        BlockHitResult blockHit = getTargetBlock(player, blockReachDistance);

        if (blockHit != null && blockHit.getType() == HitResult.Type.BLOCK) {
            executeBlockDamage(player, blockHit.getBlockPos(), currentTool, manager);
        }
    }

    /**
     * Нанести урон сущности
     */
    private static void executeEntityDamage(ServerPlayer player, Entity target, ItemStack tool) {
        if (target == player) return;

        if (!(target instanceof LivingEntity livingTarget)) {
            return;
        }

        if (!livingTarget.isAlive() || livingTarget.isInvulnerable()) {
            return;
        }

        // Проверка что цель не в периоде неуязвимости
        if (livingTarget.invulnerableTime > 10) {
            return;
        }

        BlockHPManager manager = BlockHPManager.getInstance();

        // Базовый урон инструмента
        float baseDamage = manager.getToolDamage(tool);

        // Добавляем бонус от силы атаки (enchantments)
        float enchantBonus = EnchantmentHelper.getDamageBonus(tool, livingTarget.getMobType());

        float damage = baseDamage + enchantBonus;

        // ============================================
        // ТЕСТОВЫЙ МНОЖИТЕЛЬ - УМЕНЬШАЕМ УРОН В 5 РАЗ
        // TODO: Убрать или настроить для продакшена
        damage = damage / 5.0f;
        // ============================================

        if (damage <= 0) {
            return;
        }

        // Критический удар (если падает)
        boolean isCritical = player.fallDistance > 0.0F &&
                !player.onGround() &&
                !player.onClimbable() &&
                !player.isInWater() &&
                !player.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS) &&
                !player.isPassenger();

        if (isCritical) {
            damage *= 1.5f;
        }

        // Наносим урон
        DamageSource damageSource = player.damageSources().playerAttack(player);
        boolean hurt = livingTarget.hurt(damageSource, damage);

        if (hurt) {
            // Отбрасывание
            float knockbackStrength = EnchantmentHelper.getKnockbackBonus(player);
            if (knockbackStrength > 0) {
                livingTarget.knockback(
                        knockbackStrength * 0.5f,
                        Math.sin(player.getYRot() * ((float) Math.PI / 180F)),
                        -Math.cos(player.getYRot() * ((float) Math.PI / 180F))
                );
            }

            // Огонь от зачарования
            int fireAspect = EnchantmentHelper.getFireAspect(player);
            if (fireAspect > 0) {
                livingTarget.setSecondsOnFire(fireAspect * 4);
            }

            // Урон инструменту
            if (!tool.isEmpty()) {
                tool.hurtAndBreak(1, player, (p) ->
                        p.broadcastBreakEvent(player.getUsedItemHand()));
            }

            // Статистика и триггеры
            player.awardStat(net.minecraft.stats.Stats.DAMAGE_DEALT, Math.round(damage * 10.0F));

            // Партиклы критического удара
            if (isCritical) {
                player.crit(livingTarget);
            }
        }

        Days7.LOGGER.debug("Player {} hit {} for {} damage (crit: {})",
                player.getName().getString(),
                target.getName().getString(),
                damage,
                isCritical);
    }

    /**
     * Нанести урон блоку
     */
    private static void executeBlockDamage(ServerPlayer player, BlockPos pos, ItemStack tool, BlockHPManager manager) {
        ServerLevel level = player.serverLevel();
        BlockState state = level.getBlockState(pos);

        if (state.isAir()) {
            return;
        }

        if (manager.isIndestructible(level, pos)) {
            NetworkHandler.sendToPlayer(new BlockHPSyncPacket(pos, -1, -1), player);
            return;
        }

        int damage = manager.calculateDamage(tool, state);
        if (damage <= 0) {
            return;
        }

        int currentDamage = manager.getBlockDamage(level, pos);
        int maxHP = manager.getBlockMaxHP(state);

        currentDamage += damage;
        int currentHP = maxHP - currentDamage;
        int breakerId = manager.getBlockBreakerId(pos);

        SoundType soundType = state.getSoundType(level, pos, player);
        level.playSound(null, pos, soundType.getHitSound(), SoundSource.BLOCKS,
                (soundType.getVolume() + 1.0F) / 8.0F,
                soundType.getPitch() * 0.5F);

        if (currentHP <= 0) {
            manager.clearBlockDamage(level, pos);

            sendDestructionPacketToAll(level, breakerId, pos, -1);
            level.destroyBlock(pos, true, player);

            if (!tool.isEmpty()) {
                tool.hurtAndBreak(1, player, (p) ->
                        p.broadcastBreakEvent(player.getUsedItemHand()));
            }

            sendHPToNearbyPlayers(level, pos, 0, maxHP);
            NetworkHandler.sendToPlayer(new BlockDestroyedPacket(pos), player);

        } else {
            manager.setBlockDamage(level, pos, currentDamage);

            float damagePercent = (float) currentDamage / maxHP;
            int destroyStage = (int) (damagePercent * 10.0f);
            destroyStage = Math.min(9, Math.max(0, destroyStage));

            sendDestructionPacketToAll(level, breakerId, pos, destroyStage);
            sendHPToNearbyPlayers(level, pos, currentHP, maxHP);
        }
    }

    /**
     * Получить сущность на которую смотрит игрок
     */
    private static EntityHitResult getTargetEntity(ServerPlayer player, double reachDistance) {
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(reachDistance));

        AABB searchBox = player.getBoundingBox()
                .expandTowards(lookVec.scale(reachDistance))
                .inflate(1.0);

        Predicate<Entity> filter = entity ->
                !entity.isSpectator() &&
                        entity.isPickable() &&
                        entity != player &&
                        entity.isAlive();

        Entity closestEntity = null;
        double closestDistance = reachDistance;
        Vec3 closestHitPos = null;

        for (Entity entity : player.level().getEntities(player, searchBox, filter)) {
            AABB entityBox = entity.getBoundingBox().inflate(entity.getPickRadius());
            Optional<Vec3> hitResult = entityBox.clip(eyePos, endPos);

            if (hitResult.isPresent()) {
                double distance = eyePos.distanceTo(hitResult.get());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestEntity = entity;
                    closestHitPos = hitResult.get();
                }
            } else if (entityBox.contains(eyePos)) {
                if (closestDistance > 0) {
                    closestDistance = 0;
                    closestEntity = entity;
                    closestHitPos = eyePos;
                }
            }
        }

        if (closestEntity != null && closestHitPos != null) {
            BlockHitResult blockHit = player.level().clip(new ClipContext(
                    eyePos,
                    closestHitPos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player
            ));

            if (blockHit.getType() == HitResult.Type.BLOCK) {
                double blockDist = eyePos.distanceTo(blockHit.getLocation());
                if (blockDist < closestDistance) {
                    return null;
                }
            }

            return new EntityHitResult(closestEntity, closestHitPos);
        }

        return null;
    }

    /**
     * Получить блок на который смотрит игрок
     */
    private static BlockHitResult getTargetBlock(ServerPlayer player, double reachDistance) {
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(reachDistance));

        ClipContext context = new ClipContext(
                eyePos,
                endPos,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        );

        return player.level().clip(context);
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