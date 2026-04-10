package org.vertex.days7.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.vertex.days7.client.BlockAttackHandler;

import javax.annotation.Nullable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow @Nullable public LocalPlayer player;
    @Shadow @Nullable public MultiPlayerGameMode gameMode;
    @Shadow @Nullable public HitResult hitResult;
    @Shadow private int rightClickDelay;

    // Задержка для размещения блоков (4 тика = 0.2 сек)
    @Unique
    private static final int PLACE_DELAY_TICKS = 4;

    @Unique
    private int days7$placeDelayCounter = 0;

    /**
     * Перехватываем ВСЕ атаки
     */
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void days7$onStartAttack(CallbackInfoReturnable<Boolean> cir) {
        if (player == null || gameMode == null) return;

        // В креативе - стандартное поведение для блоков, но замах для мобов
        if (player.getAbilities().instabuild) {
            // Если смотрим на сущность - используем нашу систему
            if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
                boolean handled = BlockAttackHandler.handleAttack((Minecraft) (Object) this);
                if (handled) {
                    cir.setReturnValue(true);
                }
            }
            return;
        }

        // Наша система обрабатывает ВСЕ атаки
        boolean handled = BlockAttackHandler.handleAttack((Minecraft) (Object) this);
        if (handled) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Перехватываем продолжение атаки
     */
    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void days7$onContinueAttack(boolean leftClick, CallbackInfo ci) {
        if (!leftClick) {
            BlockAttackHandler.stopAttack();
            return;
        }

        if (player == null || gameMode == null) return;

        // В креативе - только для сущностей
        if (player.getAbilities().instabuild) {
            if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
                BlockAttackHandler.handleContinueAttack((Minecraft) (Object) this);
                ci.cancel();
            }
            return;
        }

        // Наша система обрабатывает
        BlockAttackHandler.handleContinueAttack((Minecraft) (Object) this);
        ci.cancel();
    }

    /**
     * Добавляем задержку для размещения блоков
     */
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void days7$onStartUseItem(CallbackInfo ci) {
        if (player == null || gameMode == null) return;

        // В креативе - без задержки
        if (player.getAbilities().instabuild) {
            return;
        }

        // Проверяем задержку
        if (days7$placeDelayCounter > 0) {
            ci.cancel();
            return;
        }

        // Устанавливаем задержку после успешного использования
        // Задержка применится в tick
    }

    /**
     * Обрабатываем задержку размещения в тике
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void days7$onTick(CallbackInfo ci) {
        // Уменьшаем счётчик задержки размещения
        if (days7$placeDelayCounter > 0) {
            days7$placeDelayCounter--;
        }
    }

    /**
     * После успешного использования предмета - устанавливаем задержку
     */
    @Inject(method = "startUseItem", at = @At("RETURN"))
    private void days7$afterUseItem(CallbackInfo ci) {
        if (player == null) return;

        // В креативе - без задержки
        if (player.getAbilities().instabuild) {
            return;
        }

        // Если было размещение блока - добавляем задержку
        if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            days7$placeDelayCounter = PLACE_DELAY_TICKS;
        }
    }
}
