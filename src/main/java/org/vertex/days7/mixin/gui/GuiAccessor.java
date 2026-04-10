package org.vertex.days7.mixin.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@OnlyIn(Dist.CLIENT)
@Mixin(Gui.class)
public interface GuiAccessor {
    @Accessor("minecraft")
    Minecraft days$getMinecraft();

    @Accessor("screenWidth")
    int days$getScreenWidth();
    @Accessor("screenHeight")
    int days$getScreenHeight();

    @Accessor("tickCount")
    int days$getTickCount();

    @Invoker("getCameraPlayer")
    Player days$getCameraPlayer();
    @Invoker("getFont")
    Font days$getFont();

    @Accessor("GUI_ICONS_LOCATION")
    static ResourceLocation getGUI_ICONS_LOCATION() {
        throw new AssertionError();
    }
    @Accessor("WIDGETS_LOCATION")
    static ResourceLocation getWIDGETS_LOCATION() {
        throw new AssertionError();
    }
}
