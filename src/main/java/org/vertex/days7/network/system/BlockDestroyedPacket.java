package org.vertex.days7.network.system;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.vertex.days7.client.ClientBlockHPManager;

import java.util.function.Supplier;

public class BlockDestroyedPacket {
    private final BlockPos pos;

    public BlockDestroyedPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(BlockDestroyedPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    public static BlockDestroyedPacket decode(FriendlyByteBuf buf) {
        return new BlockDestroyedPacket(buf.readBlockPos());
    }

    public static void handle(BlockDestroyedPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientBlockHPManager.getInstance().clearBlockHP(msg.pos);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
