package org.vertex.days7.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.vertex.days7.Days7;

@Mod.EventBusSubscriber(modid = Days7.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CapabilityRegistry {

    public static final ResourceLocation BLOCK_DAMAGE_CAP =
            ResourceLocation.fromNamespaceAndPath(Days7.MODID, "block_damage");

    @SubscribeEvent
    public static void attachChunkCapabilities(AttachCapabilitiesEvent<LevelChunk> event) {
        if (!event.getObject().getLevel().isClientSide()) {
            event.addCapability(BLOCK_DAMAGE_CAP, new ChunkBlockDamageCapability());
        }
    }
}
