package org.vertex.days7.event;

import org.vertex.days7.Days7;
import org.vertex.days7.command.BlockHPCommands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Days7.MODID)
public class CommandEvents {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        BlockHPCommands.register(event.getDispatcher());
    }
}