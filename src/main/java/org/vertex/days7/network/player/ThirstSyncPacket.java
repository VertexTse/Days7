package org.vertex.days7.network.player;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.vertex.days7.accessor.IFoodDataAccessor;

import java.util.function.Supplier;

public class ThirstSyncPacket {
    private final int thirstLevel;
    private final float exhaustionLevel;

    public ThirstSyncPacket(int thirstLevel, float exhaustionLevel) {
        this.thirstLevel = thirstLevel;
        this.exhaustionLevel = exhaustionLevel;
    }

    public static void encode(ThirstSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.thirstLevel);
        buffer.writeFloat(packet.exhaustionLevel);
    }

    public static ThirstSyncPacket decode(FriendlyByteBuf buffer) {
        return new ThirstSyncPacket(
                buffer.readInt(),
                buffer.readFloat()
        );
    }

    public static void handle(ThirstSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // ВАЖНО: выполняем на клиенте
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleOnClient(packet));
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleOnClient(ThirstSyncPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            var food = (IFoodDataAccessor)mc.player.getFoodData();
            food.days7$setThirst(packet.thirstLevel);
            food.days7$setThirstExhaustion(packet.exhaustionLevel);
        }
    }
}
