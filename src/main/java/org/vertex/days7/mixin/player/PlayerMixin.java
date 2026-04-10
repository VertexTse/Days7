package org.vertex.days7.mixin.player;

import net.minecraft.world.item.Item;
import org.vertex.days7.data.BlockHPManager;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(Player.class)
public class PlayerMixin {
    @Unique
    private static final UUID HAND_ATTACK_SPEED_UUID =
            UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA4");

    @Unique
    private Item days7$lastHeldItemType = null;

    @Unique
    private boolean days7$wasEmpty = false;

    @Unique
    private boolean days7$initialized = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void days7$onTick(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        ItemStack currentItem = player.getMainHandItem();

        boolean isEmpty = currentItem.isEmpty();
        Item currentType = isEmpty ? null : currentItem.getItem();

        // Первый тик - принудительное обновление
        if (!days7$initialized) {
            days7$initialized = true;
            days7$wasEmpty = isEmpty;
            days7$lastHeldItemType = currentType;
            days7$updateHandAttackSpeed(player, currentItem);
            return;
        }

        // Проверяем изменился ли тип предмета
        if (isEmpty == days7$wasEmpty && currentType == days7$lastHeldItemType) {
            return;
        }

        days7$wasEmpty = isEmpty;
        days7$lastHeldItemType = currentType;

        days7$updateHandAttackSpeed(player, currentItem);
    }

    @Unique
    private void days7$updateHandAttackSpeed(Player player, ItemStack heldItem) {
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed == null) return;

        attackSpeed.removeModifier(HAND_ATTACK_SPEED_UUID);

        if (heldItem.isEmpty()) {
            BlockHPManager manager = BlockHPManager.getInstance();
            float handSpeed = manager.getHandAttackSpeed();
            double modifier = handSpeed - 4.0;

            if (Math.abs(modifier) > 0.001) {
                attackSpeed.addTransientModifier(new AttributeModifier(
                        HAND_ATTACK_SPEED_UUID,
                        "Hand attack speed",
                        modifier,
                        AttributeModifier.Operation.ADDITION
                ));
            }
        }
    }
}