package org.vertex.days7.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChunkBlockDamageCapability implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<ChunkBlockDamageCapability> CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() {});

    private final LazyOptional<ChunkBlockDamageCapability> holder = LazyOptional.of(() -> this);

    // Урон блоков: localPos -> damage
    private final Map<Long, Integer> blockDamage = new HashMap<>();

    // Неразрушаемые блоки: localPos
    private final Set<Long> indestructibleBlocks = new HashSet<>();

    // ============ Урон ============

    public int getDamage(BlockPos pos) {
        long key = encodeLocalPos(pos);
        return blockDamage.getOrDefault(key, 0);
    }

    public void setDamage(BlockPos pos, int damage) {
        long key = encodeLocalPos(pos);
        if (damage <= 0) {
            blockDamage.remove(key);
        } else {
            blockDamage.put(key, damage);
        }
    }

    public void clearDamage(BlockPos pos) {
        long key = encodeLocalPos(pos);
        blockDamage.remove(key);
    }

    public void clearDamageByKey(long key) {
        blockDamage.remove(key);
    }

    public boolean hasDamage(BlockPos pos) {
        long key = encodeLocalPos(pos);
        return blockDamage.containsKey(key) && blockDamage.get(key) > 0;
    }

    public Map<Long, Integer> getAllDamage() {
        return blockDamage;
    }

    // ============ Неразрушаемость ============

    public boolean isIndestructible(BlockPos pos) {
        long key = encodeLocalPos(pos);
        return indestructibleBlocks.contains(key);
    }

    public void setIndestructible(BlockPos pos, boolean indestructible) {
        long key = encodeLocalPos(pos);
        if (indestructible) {
            indestructibleBlocks.add(key);
        } else {
            indestructibleBlocks.remove(key);
        }
    }

    public Set<Long> getAllIndestructible() {
        return indestructibleBlocks;
    }

    // ============ Утилиты ============

    public boolean isEmpty() {
        return blockDamage.isEmpty() && indestructibleBlocks.isEmpty();
    }

    private long encodeLocalPos(BlockPos pos) {
        int localX = pos.getX() & 15;
        int localZ = pos.getZ() & 15;
        int y = pos.getY();
        return ((long) localX) | ((long) localZ << 4) | ((long) (y + 64) << 8);
    }

    // ============ Capability ============

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == CAPABILITY) {
            return holder.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        // Сохраняем урон
        ListTag damageList = new ListTag();
        for (Map.Entry<Long, Integer> entry : blockDamage.entrySet()) {
            CompoundTag blockTag = new CompoundTag();
            blockTag.putLong("pos", entry.getKey());
            blockTag.putInt("damage", entry.getValue());
            damageList.add(blockTag);
        }
        tag.put("damage", damageList);

        // Сохраняем неразрушаемые блоки
        long[] indestructibleArray = indestructibleBlocks.stream().mapToLong(Long::longValue).toArray();
        tag.putLongArray("indestructible", indestructibleArray);

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        blockDamage.clear();
        indestructibleBlocks.clear();

        // Загружаем урон
        if (tag.contains("damage", Tag.TAG_LIST)) {
            ListTag list = tag.getList("damage", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag blockTag = list.getCompound(i);
                long pos = blockTag.getLong("pos");
                int damage = blockTag.getInt("damage");
                if (damage > 0) {
                    blockDamage.put(pos, damage);
                }
            }
        }

        // Загружаем неразрушаемые блоки
        if (tag.contains("indestructible")) {
            long[] arr = tag.getLongArray("indestructible");
            for (long pos : arr) {
                indestructibleBlocks.add(pos);
            }
        }
    }

    public void invalidate() {
        holder.invalidate();
    }
}