package org.vertex.days7.mixin.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@OnlyIn(Dist.CLIENT)
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {

    @Shadow
    @Final
    private Minecraft minecraft;
    @Shadow
    private boolean isDestroying;
    @Shadow
    private float destroyProgress;
    @Shadow
    private float destroyTicks;

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void days7$onAttack(Player player, Entity target, CallbackInfo ci) {
        if (player.getAbilities().instabuild) {
            return;
        }
        ci.cancel();
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onStartDestroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer player = this.minecraft.player;
        if (player == null) return;

        if (player.getAbilities().instabuild) {
            return;
        }

        this.isDestroying = false;
        this.destroyProgress = 0;
        this.destroyTicks = 0;

        cir.setReturnValue(false);
        cir.cancel();
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onContinueDestroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer player = this.minecraft.player;
        if (player == null) return;

        if (player.getAbilities().instabuild) {
            return;
        }

        this.isDestroying = false;
        this.destroyProgress = 0;
        this.destroyTicks = 0;

        cir.setReturnValue(false);
        cir.cancel();
    }

    @Inject(method = "stopDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onStopDestroyBlock(CallbackInfo ci) {
        ci.cancel();
        LocalPlayer player = this.minecraft.player;
        if (player == null) return;

        if (player.getAbilities().instabuild) {
            return;
        }

        this.isDestroying = false;
        this.destroyProgress = 0;
        this.destroyTicks = 0;
    }
}
