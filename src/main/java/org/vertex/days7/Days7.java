package org.vertex.days7;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.vertex.days7.client.ClientEvents;
import org.vertex.days7.data.BlockHPManager;
import org.vertex.days7.event.player.ThirstEventHandler;
import org.vertex.days7.network.NetworkHandler;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Days7.MODID)
public class Days7 {
    public static final String MODID = "days7";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation THIRST_CAP = ResourceLocation.fromNamespaceAndPath(MODID, "thirst");

    public Days7(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::clientSetup);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ThirstEventHandler());
        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListener);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            MinecraftForge.EVENT_BUS.register(ClientEvents.class);
        });
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::register);
        LOGGER.info("ThirstMod initialized!");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
    }

    private void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(BlockHPManager.getInstance());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
