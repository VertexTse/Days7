package org.vertex.days7.mixin.items;

import com.google.common.collect.Multimap;
import org.vertex.days7.util.ItemAttributeHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SwordItem.class)
public class SwordItemMixin {
    @Inject(method = "getDefaultAttributeModifiers", at = @At("RETURN"), cancellable = true)
    private void days7$modifyAttackSpeed(EquipmentSlot slot, CallbackInfoReturnable<Multimap<Attribute, AttributeModifier>> cir) {
        if (slot != EquipmentSlot.MAINHAND) return;

        Item self = (Item) (Object) this;
        Multimap<Attribute, AttributeModifier> modified = ItemAttributeHelper.modifyAttributes(self, cir.getReturnValue());

        if (modified != null) {
            cir.setReturnValue(modified);
        }
    }
}
