package org.vertex.days7.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.vertex.days7.Days7;
import org.vertex.days7.network.player.SwingConfirmPacket;
import org.vertex.days7.network.player.SwingStartPacket;
import org.vertex.days7.network.player.ThirstSyncPacket;
import org.vertex.days7.network.system.BlockDamagePacket;
import org.vertex.days7.network.system.BlockDestroyedPacket;
import org.vertex.days7.network.system.BlockHPSyncPacket;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Days7.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;
    private static boolean registered = false;

    public static void register() {
        if (registered) return;
        registered = true;

        CHANNEL.registerMessage(
                packetId++,
                ThirstSyncPacket.class,
                ThirstSyncPacket::encode,
                ThirstSyncPacket::decode,
                ThirstSyncPacket::handle
        );

        CHANNEL.messageBuilder(SwingStartPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(SwingStartPacket::decode)
                .encoder(SwingStartPacket::encode)
                .consumerMainThread(SwingStartPacket::handle)
                .add();
        CHANNEL.messageBuilder(SwingConfirmPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SwingConfirmPacket::decode)
                .encoder(SwingConfirmPacket::encode)
                .consumerMainThread(SwingConfirmPacket::handle)
                .add();
        CHANNEL.messageBuilder(BlockDamagePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(BlockDamagePacket::decode)
                .encoder(BlockDamagePacket::encode)
                .consumerMainThread(BlockDamagePacket::handle)
                .add();
        CHANNEL.messageBuilder(BlockHPSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(BlockHPSyncPacket::decode)
                .encoder(BlockHPSyncPacket::encode)
                .consumerMainThread(BlockHPSyncPacket::handle)
                .add();
        CHANNEL.messageBuilder(BlockDestroyedPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(BlockDestroyedPacket::decode)
                .encoder(BlockDestroyedPacket::encode)
                .consumerMainThread(BlockDestroyedPacket::handle)
                .add();

        Days7.LOGGER.info("Network packets registered!");
    }

    public static void sendToServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }

    public static void sendToPlayer(Object msg, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void sendToAllTracking(Object msg, ServerPlayer sourcePlayer, net.minecraft.core.BlockPos pos) {
        CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() ->
                sourcePlayer.serverLevel().getChunkAt(pos)), msg);
    }
}
