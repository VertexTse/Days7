package org.vertex.days7.capability;

import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.vertex.days7.Days7;

@Mod.EventBusSubscriber(modid = Days7.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CapabilitySetup {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(ChunkBlockDamageCapability.class);
    }
}
