package org.vertex.days7.event.player;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import org.vertex.days7.accessor.IFoodDataAccessor;
import org.vertex.days7.network.NetworkHandler;
import org.vertex.days7.network.player.ThirstSyncPacket;

public class ThirstEventHandler {
    @SubscribeEvent
    public void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack stack = event.getItem();

        var foodData = (IFoodDataAccessor)player.getFoodData();

        if (stack.getItem() instanceof PotionItem) {
            if (PotionUtils.getPotion(stack) == Potions.HEALING) {
                foodData.days7$setFood(2);
                return;
            }
            if (PotionUtils.getPotion(stack) == Potions.FIRE_RESISTANCE) {
                foodData.days7$setThirst(2);
                return;
            }
            if (PotionUtils.getPotion(stack) == Potions.WATER) {
                foodData.days7$drink(6);
                return;
            } else {
                foodData.days7$drink(2);
                return;
            }
        }

        if (stack.is(Items.MILK_BUCKET)) {
            foodData.days7$drink(4);
            return;
        }

        if (stack.is(Items.MELON_SLICE)) {
            foodData.days7$drink(2);
            return;
        }

        if (stack.is(Items.MUSHROOM_STEW) ||
                stack.is(Items.BEETROOT_SOUP) ||
                stack.is(Items.RABBIT_STEW) ||
                stack.is(Items.SUSPICIOUS_STEW)) {
            foodData.days7$drink(3);
            return;
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();

        if (!player.getMainHandItem().isEmpty() || !player.isShiftKeyDown()) return;

        var blockPos = event.getPos().relative(event.getFace());
        var blockState = player.level().getBlockState(blockPos);

        if (blockState.getFluidState().isSource()) {
            var fluid = blockState.getFluidState().getType();

            if (fluid == net.minecraft.world.level.material.Fluids.WATER ||
                    fluid == net.minecraft.world.level.material.Fluids.FLOWING_WATER) {

                var foodData = (IFoodDataAccessor)player.getFoodData();

                if (foodData.days7$onNeedsWater()) {
                    foodData.days7$drink(15);

                    player.level().playSound(
                            null,
                            player.getX(), player.getY(), player.getZ(),
                            net.minecraft.sounds.SoundEvents.GENERIC_DRINK,
                            net.minecraft.sounds.SoundSource.PLAYERS,
                            0.5f,
                            player.level().random.nextFloat() * 0.1f + 0.9f
                    );

                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;

        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {

            var foodData = (IFoodDataAccessor)player.getFoodData();

            NetworkHandler.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new ThirstSyncPacket(
                            foodData.days7$getThirstLevel(),
                            foodData.days7$getThirstExhaustionLevel()
                    )
            );
        }
    }
}
