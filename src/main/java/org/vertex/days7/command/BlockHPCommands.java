// command/BlockHPCommands.java
package org.vertex.days7.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import org.vertex.days7.data.BlockHPManager;
import org.vertex.days7.network.system.BlockHPSyncPacket;
import org.vertex.days7.network.NetworkHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

public class BlockHPCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("blockhp")
                .requires(source -> source.hasPermission(2))

                // /blockhp set <pos> <hp>
                .then(Commands.literal("set")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("hp", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                                            int hp = IntegerArgumentType.getInteger(ctx, "hp");
                                            return setBlockHP(ctx.getSource(), pos, hp);
                                        }))))

                // /blockhp heal <pos>
                .then(Commands.literal("heal")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(ctx -> {
                                    BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                                    return healBlock(ctx.getSource(), pos);
                                })))

                // /blockhp indestructible <pos> <true/false>
                .then(Commands.literal("indestructible")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.literal("true")
                                        .executes(ctx -> {
                                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                                            return setIndestructible(ctx.getSource(), pos, true);
                                        }))
                                .then(Commands.literal("false")
                                        .executes(ctx -> {
                                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                                            return setIndestructible(ctx.getSource(), pos, false);
                                        }))))

                // /blockhp info <pos>
                .then(Commands.literal("info")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(ctx -> {
                                    BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                                    return showInfo(ctx.getSource(), pos);
                                })))
        );
    }

    private static int setBlockHP(CommandSourceStack source, BlockPos pos, int hp) {
        ServerLevel level = source.getLevel();
        BlockState state = level.getBlockState(pos);

        if (state.isAir()) {
            source.sendFailure(Component.literal("Block at position is air"));
            return 0;
        }

        BlockHPManager manager = BlockHPManager.getInstance();
        int maxHP = manager.getBlockMaxHP(state);

        hp = Math.min(hp, maxHP);
        manager.setBlockHP(level, pos, hp);

        // Обновляем трещины
        int damage = maxHP - hp;
        float damagePercent = (float) damage / maxHP;
        int destroyStage = (int) (damagePercent * 10.0f);
        destroyStage = Math.min(9, Math.max(0, destroyStage));

        int breakerId = manager.getBlockBreakerId(pos);
        sendToNearbyPlayers(level, pos, breakerId, destroyStage, hp, maxHP);

        var message = String.format("Set block HP to %d/%d at %s", hp, maxHP, pos.toShortString());
        source.sendSuccess(() -> Component.literal(message), true);
        return 1;
    }

    private static int healBlock(CommandSourceStack source, BlockPos pos) {
        ServerLevel level = source.getLevel();
        BlockState state = level.getBlockState(pos);

        if (state.isAir()) {
            source.sendFailure(Component.literal("Block at position is air"));
            return 0;
        }

        BlockHPManager manager = BlockHPManager.getInstance();
        manager.clearBlockDamage(level, pos);

        int maxHP = manager.getBlockMaxHP(state);
        int breakerId = manager.getBlockBreakerId(pos);

        // Убираем трещины
        sendToNearbyPlayers(level, pos, breakerId, -1, maxHP, maxHP);

        source.sendSuccess(() -> Component.literal(
                String.format("Healed block at %s to full HP", pos.toShortString())), true);
        return 1;
    }

    private static int setIndestructible(CommandSourceStack source, BlockPos pos, boolean indestructible) {
        ServerLevel level = source.getLevel();
        BlockState state = level.getBlockState(pos);

        if (state.isAir()) {
            source.sendFailure(Component.literal("Block at position is air"));
            return 0;
        }

        BlockHPManager manager = BlockHPManager.getInstance();
        manager.setIndestructible(level, pos, indestructible);

        // Отправляем обновление
        if (indestructible) {
            int breakerId = manager.getBlockBreakerId(pos);
            // Убираем трещины
            for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                if (player.level() == level && player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 4096) {
                    player.connection.send(new ClientboundBlockDestructionPacket(breakerId, pos, -1));
                    NetworkHandler.sendToPlayer(new BlockHPSyncPacket(pos, -1, -1), player);
                }
            }
        }

        source.sendSuccess(() -> Component.literal(
                String.format("Block at %s is now %s",
                        pos.toShortString(),
                        indestructible ? "indestructible" : "destructible")), true);
        return 1;
    }

    private static int showInfo(CommandSourceStack source, BlockPos pos) {
        ServerLevel level = source.getLevel();
        BlockState state = level.getBlockState(pos);

        if (state.isAir()) {
            source.sendFailure(Component.literal("Block at position is air"));
            return 0;
        }

        BlockHPManager manager = BlockHPManager.getInstance();

        boolean indestructible = manager.isIndestructible(level, pos);
        int maxHP = manager.getBlockMaxHP(state);
        int damage = manager.getBlockDamage(level, pos);
        int currentHP = maxHP - damage;

        String info = String.format(
                "Block: %s\nPosition: %s\nHP: %d/%d\nIndestructible: %s",
                state.getBlock().getName().getString(),
                pos.toShortString(),
                indestructible ? -1 : currentHP,
                indestructible ? -1 : maxHP,
                indestructible ? "Yes" : "No"
        );

        source.sendSuccess(() -> Component.literal(info), false);
        return 1;
    }

    private static void sendToNearbyPlayers(ServerLevel level, BlockPos pos, int breakerId, int stage, int currentHP, int maxHP) {
        ClientboundBlockDestructionPacket destructionPacket = new ClientboundBlockDestructionPacket(breakerId, pos, stage);
        BlockHPSyncPacket hpPacket = new BlockHPSyncPacket(pos, currentHP, maxHP);

        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() == level && player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 4096) {
                player.connection.send(destructionPacket);
                NetworkHandler.sendToPlayer(hpPacket, player);
            }
        }
    }
}