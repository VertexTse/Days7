package org.vertex.days7.util;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.vertex.days7.data.BlockHPData;
import org.vertex.days7.data.BlockHPManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

public class ItemAttributeHelper {

    private static final UUID TOOL_ATTACK_SPEED_UUID =
            UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

    /**
     * Модифицирует атрибуты инструмента если есть настройки в JSON
     * @return модифицированные атрибуты или null если не нужно менять
     */
    public static Multimap<Attribute, AttributeModifier> modifyAttributes(
            Item item,
            Multimap<Attribute, AttributeModifier> originalModifiers) {

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
        BlockHPManager manager = BlockHPManager.getInstance();

        if (!manager.hasToolStats(itemId)) {
            return null;
        }

        BlockHPData.ToolStats stats = manager.getToolStats(itemId);
        if (stats == null) {
            return null;
        }

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

        // Копируем все атрибуты кроме скорости атаки
        for (var entry : originalModifiers.entries()) {
            if (entry.getKey() != Attributes.ATTACK_SPEED) {
                builder.put(entry.getKey(), entry.getValue());
            }
        }

        // Добавляем новую скорость атаки
        double speedModifier = stats.getAttackSpeed() - 4.0;
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                TOOL_ATTACK_SPEED_UUID,
                "Tool modifier",
                speedModifier,
                AttributeModifier.Operation.ADDITION
        ));

        return builder.build();
    }
}