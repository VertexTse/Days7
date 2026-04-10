package org.vertex.days7.mixin.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.vertex.days7.accessor.IFoodDataAccessor;

@Mixin(FoodData.class)
public abstract class FoodDataMixin implements IFoodDataAccessor {
    @Shadow
    private int foodLevel;
    @Shadow
    private int tickTimer;
    @Shadow
    private int lastFoodLevel;
    @Shadow
    private float exhaustionLevel;

    @Unique
    private int days7$thirstLevel = 100;
    @Unique
    private float days7$thirstExhaustionLevel = 0.0f;

    @Unique
    private float days7$stamina = 100;

    @Unique
    private int days7$maxThirst = 100;
    @Unique
    private int days7$maxFood = 100;
    @Unique
    private int days7$maxStamina = 100;

    @Override
    public int days7$getThirstLevel() {
        return this.days7$thirstLevel;
    }
    @Override
    public float days7$getThirstExhaustionLevel() {
        return this.days7$thirstExhaustionLevel;
    }
    @Override
    public int days7$getFoodLevel() {
        return this.foodLevel;
    }
    @Override
    public float days7$getStamina() {
        return this.days7$stamina;
    }

    @Override
    public int days7$getMaxThirst() {
        return this.days7$maxThirst;
    }
    @Override
    public int days7$getMaxFood() {
        return this.days7$maxFood;
    }
    @Override
    public int days7$getMaxStamina() {
        return this.days7$maxStamina;
    }

    @Override
    public void days7$setMaxThirst(int maxThirst) {
        this.days7$maxThirst = maxThirst;
    }
    @Override
    public void days7$setMaxFood(int maxFood) {
        this.days7$maxFood = maxFood;
    }
    @Override
    public void days7$setMaxStamina(int maxStamina) {
        this.days7$maxStamina = maxStamina;
    }

    @Override
    public void days7$setThirstExhaustion(float thirstExhaustion) {
        this.days7$thirstExhaustionLevel = thirstExhaustion;
    }
    @Override
    public void days7$setThirst(int thirst) {
        this.days7$thirstLevel = thirst;
    }
    @Override
    public void days7$setFood(int food) {
        this.foodLevel = food;
    }
    @Override
    public void days7$setStamina(float stamina) {
        this.days7$stamina = stamina;
    }

    @Override
    public boolean days7$onNeedsWater() {
        return this.days7$thirstLevel < this.days7$maxThirst;
    }
    @Override
    public void days7$drink(int thirst) {
        this.days7$thirstLevel = Math.min(this.days7$thirstLevel + thirst, this.days7$maxThirst);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(Player player, CallbackInfo ci) {
        ci.cancel();

        if (player.isCreative() || player.isSpectator()) return;
        this.lastFoodLevel = this.foodLevel;
        if (this.exhaustionLevel > 8.0F) {
            this.exhaustionLevel -= 8.0F;
            this.foodLevel = Math.max(this.foodLevel - 1, 0);
        }
        this.exhaustionLevel = this.exhaustionLevel + 0.01f;
        if (this.days7$thirstExhaustionLevel > 8.0F) {
            this.days7$thirstExhaustionLevel -= 8.0F;
            this.days7$thirstLevel = Math.max(this.days7$thirstLevel - 1, 0);
        }
        this.days7$thirstExhaustionLevel = this.days7$thirstExhaustionLevel + 0.01f;

        boolean naturalRegeneration = player.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION);
        if (naturalRegeneration && this.foodLevel >= 5 && this.days7$thirstLevel >= 5 && player.isHurt()) {
            ++this.tickTimer;
            if (this.tickTimer >= 80) {
                player.heal(1.0F);
                this.tickTimer = 0;
            }
        } else if (this.foodLevel <= 0) {
            ++this.tickTimer;
            if (this.tickTimer >= 80) {
                if (player.getHealth() > 0) {
                    player.hurt(player.damageSources().starve(), 1.0F);
                }
                this.tickTimer = 0;
            }
        } else {
            this.tickTimer = 0;
        }
    }

    @Inject(method = "addExhaustion", at = @At("HEAD"), cancellable = true, remap = false)
    private void onAddExhaustion(float exhaustion, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "eat(IF)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void onEat(int value, float p_38709_, CallbackInfo ci) {
        ci.cancel();
        this.foodLevel = Math.min(value + this.foodLevel, days7$maxFood);
    }

    @Inject(method = "needsFood", at = @At("HEAD"), cancellable = true, remap = false)
    private void onNeedsFood(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(this.foodLevel < this.days7$maxFood);
        cir.cancel();
    }

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    private void onAddAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        tag.putInt("MaxFood", days7$maxFood);
        tag.putInt("ThirstLevel", days7$thirstLevel);
        tag.putFloat("ThirstExhaustionLevel", days7$thirstExhaustionLevel);
        tag.putInt("MaxThirst", days7$maxThirst);

        tag.putInt("MaxStamina", days7$maxStamina);
        tag.putFloat("Stamina", days7$stamina);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void onReadAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("MaxFood")) days7$maxFood = tag.getInt("MaxFood");
        if (tag.contains("ThirstExhaustionLevel")) days7$thirstExhaustionLevel = tag.getFloat("ThirstExhaustionLevel");
        if (tag.contains("ThirstLevel")) days7$thirstLevel = tag.getInt("ThirstLevel");
        if (tag.contains("MaxThirst")) days7$maxThirst = tag.getInt("MaxThirst");

        if (tag.contains("MaxStamina")) days7$maxStamina = tag.getInt("MaxStamina");
        if (tag.contains("Stamina")) days7$stamina = tag.getFloat("Stamina");
    }
}
