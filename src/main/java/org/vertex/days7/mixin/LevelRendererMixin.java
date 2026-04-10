package org.vertex.days7.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.server.level.BlockDestructionProgress;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Shadow
    @Final
    private Int2ObjectMap<BlockDestructionProgress> destroyingBlocks;

    @Shadow
    private int ticks;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void blockhp$preventRemoval(CallbackInfo ci) {
        ci.cancel();
        // НЕ отменяем весь метод!
        // Мы просто заранее обновляем updatedRenderTick
        // для наших breakerId

        for (BlockDestructionProgress progress : destroyingBlocks.values()) {

            // Наши ID начинаются с 1_000_000
            if (progress.getId() >= 1_000_000) {

                // Просто обновляем tick на текущий
                progress.updateTick(this.ticks);
            }
        }
    }
}
