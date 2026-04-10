package org.vertex.days7.network.player;

import org.vertex.days7.client.BlockAttackHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SwingConfirmPacket {
    private final int swingTicks;

    public SwingConfirmPacket(int swingTicks) {
        this.swingTicks = swingTicks;
    }

    public static void encode(SwingConfirmPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.swingTicks);
    }

    public static SwingConfirmPacket decode(FriendlyByteBuf buf) {
        return new SwingConfirmPacket(buf.readInt());
    }

    public static void handle(SwingConfirmPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                BlockAttackHandler.onSwingConfirmed(msg.swingTicks);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}