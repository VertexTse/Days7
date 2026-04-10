package org.vertex.days7.mixin.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vertex.days7.accessor.IFoodDataAccessor;

import java.util.Objects;

@Mixin(ForgeGui.class)
public abstract class ForgeGuiMixin {
    @Inject(method = "renderHealth", at = @At("HEAD"), cancellable = true, remap = false)
    private void onRenderHealth(int width, int height, GuiGraphics guiGraphics, CallbackInfo ci) {
        ci.cancel();
        var th = (GuiAccessor) this;
        var minecraft = th.days$getMinecraft();
        minecraft.getProfiler().push("health");
        RenderSystem.enableBlend();

        Player player = (Player) minecraft.getCameraEntity();

        int barWidth = 40;
        int barHeight = 12;
        int padding = 4;
        int left = width / 2 - 91 - barWidth - padding;
        int top = height - barHeight - 2;

        int health = Mth.ceil(Objects.requireNonNull(player).getHealth());
        int healthMax = (int) Objects.requireNonNull(player.getAttribute(Attributes.MAX_HEALTH)).getValue();
        days7$renderBar(guiGraphics, th.days$getFont(), left, top, barWidth, barHeight,
                health, healthMax, 0xFF200000, 0xFFCC0000, 0xFFFF4444);

        assert minecraft.player != null;
        int thirst = 10;
        int thirstMax = 20;
        days7$renderBar(guiGraphics, th.days$getFont(), left, top - barHeight - 2, barWidth, barHeight,
                thirst, thirstMax, 0xFF000020, 0xFF0044CC, 0xFF4488FF);

        RenderSystem.disableBlend();
        minecraft.getProfiler().pop();
    }

    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true, remap = false)
    private void onRenderFood(int width, int height, GuiGraphics guiGraphics, CallbackInfo ci) {
        ci.cancel();
        var th = (GuiAccessor) this;
        var minecraft = th.days$getMinecraft();
        minecraft.getProfiler().push("health");
        RenderSystem.enableBlend();

        Player player = (Player) minecraft.getCameraEntity();

        int barWidth = 40;
        int barHeight = 12;
        int padding = 4;

        int left = width / 2 + 91 + padding;
        int top = height - barHeight - 2;

        // Здоровье
        var foodData = (IFoodDataAccessor)Objects.requireNonNull(player).getFoodData();
        int food = foodData.days7$getFoodLevel();
        int foodMax = foodData.days7$getMaxFood();
        days7$renderBar(guiGraphics, th.days$getFont(), left, top, barWidth, barHeight,
                food, foodMax, 0xFF201500, 0xFF17b000, 0xFF1ddb00);

        int thirstLevel = foodData.days7$getThirstLevel();
        int thirstMax = foodData.days7$getMaxThirst();
        days7$renderBar(guiGraphics, th.days$getFont(), left, top - barHeight - 2, barWidth, barHeight,
                thirstLevel, thirstMax, 0xFF000020, 0xFF0044CC, 0xFF4488FF);

        RenderSystem.disableBlend();
        minecraft.getProfiler().pop();
    }

    @Unique
    private static void days7$renderBar(GuiGraphics gui, Font font, int x, int y, int w, int h,
                                            int value, int max, int bgColor, int fillColor, int highlightColor) {
        // Рамка и фон
        gui.fill(x, y, x + w, y + h, 0xFF444444);
        gui.fill(x + 1, y + 1, x + w - 1, y + h - 1, bgColor);

        // Заполнение
        int fillW = (int) ((w - 2) * Math.min(value / (float)max, 1f));
        if (fillW > 0) {
            gui.fill(x + 1, y + 1, x + 1 + fillW, y + h - 1, fillColor);
            gui.fill(x + 1, y + 1, x + 1 + fillW, y + 2, highlightColor); // блик
        }

        // Текст с тенью
        String text = String.valueOf(value);
        int tx = x + (w - font.width(text)) / 2;
        int ty = y + (h - 8) / 2 + 1;

        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                if (dx != 0 || dy != 0)
                    gui.drawString(font, text, tx + dx, ty + dy, 0xFF000000, false);

        gui.drawString(font, text, tx, ty, 0xFFFFFFFF, false);
    }
}
