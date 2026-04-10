package org.vertex.days7.mixin;

import com.tacz.guns.GunMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GunMod.class)
public abstract class GunModMixin {
    @Inject(
            method = "registerDefaultExtraGunPack",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void onRegisterDefaultExtraGunPack(CallbackInfo ci) {
        ci.cancel();
        GunMod.LOGGER.info("Custom gun pack registered!");
    }
}
