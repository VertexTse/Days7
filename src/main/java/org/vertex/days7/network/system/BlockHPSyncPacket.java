// network/BlockHPSyncPacket.java
package org.vertex.days7.network.system;

import org.vertex.days7.client.ClientBlockHPManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class BlockHPSyncPacket {
    private final BlockPos pos;
    private final int currentHP;
    private final int maxHP; // -1 = неразрушаемый

    public BlockHPSyncPacket(BlockPos pos, int currentHP, int maxHP) {
        this.pos = pos;
        this.currentHP = currentHP;
        this.maxHP = maxHP;
    }

    public static void encode(BlockHPSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeInt(msg.currentHP);
        buf.writeInt(msg.maxHP);
    }

    public static BlockHPSyncPacket decode(FriendlyByteBuf buf) {
        return new BlockHPSyncPacket(
                buf.readBlockPos(),
                buf.readInt(),
                buf.readInt()
        );
    }

    public static void handle(BlockHPSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientBlockHPManager.getInstance().setBlockHP(
                        msg.pos,
                        msg.currentHP,
                        msg.maxHP
                );
            });
        });
        ctx.get().setPacketHandled(true);
    }
}