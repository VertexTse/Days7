// data/BlockHPManager.java
package org.vertex.days7.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.vertex.days7.Days7;
import org.vertex.days7.capability.ChunkBlockDamageCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BlockHPManager extends SimplePreparableReloadListener<BlockHPData> {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ResourceLocation DATA_LOCATION =
            ResourceLocation.fromNamespaceAndPath(Days7.MODID, "block_hp.json");

    private static BlockHPManager INSTANCE;

    private int defaultHP = 100;
    private float handDamage = 1.0f;
    private float handAttackSpeed = 4.0f;
    private boolean allowHandAttack = true;
    private int defaultSwingDelayTicks = 4;

    private final Map<ResourceLocation, Integer> blockHealthMap = new HashMap<>();
    private final Map<ResourceLocation, BlockHPData.ToolStats> toolStatsMap = new HashMap<>();
    private final Map<ResourceLocation, Map<TagKey<Block>, Float>> toolEffectivenessMap = new HashMap<>();
    private final Set<ResourceLocation> allowedItems = new HashSet<>();

    private final Map<Long, Integer> blockBreakerIds = new ConcurrentHashMap<>();
    private int nextBreakerId = 1_000_000;

    public static BlockHPManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new BlockHPManager();
        }
        return INSTANCE;
    }

    @Override
    protected BlockHPData prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        try {
            Resource resource = resourceManager.getResource(DATA_LOCATION).orElse(null);
            if (resource != null) {
                try (InputStreamReader reader = new InputStreamReader(resource.open())) {
                    return GSON.fromJson(reader, BlockHPData.class);
                }
            }
        } catch (Exception e) {
            Days7.LOGGER.error("Failed to load block HP data", e);
        }
        return new BlockHPData();
    }

    @Override
    protected void apply(BlockHPData data, ResourceManager resourceManager, ProfilerFiller profiler) {
        blockHealthMap.clear();
        toolStatsMap.clear();
        toolEffectivenessMap.clear();
        allowedItems.clear();

        if (data == null) {
            Days7.LOGGER.warn("Block HP data is null, using defaults");
            return;
        }

        this.defaultHP = data.getDefaultHP();
        this.handDamage = data.getHandDamage();
        this.handAttackSpeed = data.getHandAttackSpeed();
        this.allowHandAttack = data.isAllowHandAttack();
        this.defaultSwingDelayTicks = data.getSwingDelayTicks();

        if (data.getBlocks() != null) {
            data.getBlocks().forEach((blockId, hp) -> {
                ResourceLocation loc = ResourceLocation.parse(blockId);
                blockHealthMap.put(loc, hp);
            });
        }

        if (data.getTools() != null) {
            data.getTools().forEach((toolId, stats) -> {
                ResourceLocation loc = ResourceLocation.parse(toolId);
                toolStatsMap.put(loc, stats);
                allowedItems.add(loc);
            });
        }

        if (data.getToolEffectiveness() != null) {
            data.getToolEffectiveness().forEach((toolId, effectiveness) -> {
                ResourceLocation toolLoc = ResourceLocation.parse(toolId);
                Map<TagKey<Block>, Float> tagMap = new HashMap<>();

                effectiveness.forEach((tagId, multiplier) -> {
                    TagKey<Block> tag = BlockTags.create(ResourceLocation.parse(tagId));
                    tagMap.put(tag, multiplier);
                });

                toolEffectivenessMap.put(toolLoc, tagMap);
            });
        }

        Days7.LOGGER.info("Loaded {} block HP values, {} allowed tools",
                blockHealthMap.size(), allowedItems.size());
    }

    // ============ Swing Delay ============

    /**
     * Получить задержку замаха для предмета в тиках
     */
    public int getSwingDelayTicks(ItemStack tool) {
        if (tool.isEmpty()) {
            return defaultSwingDelayTicks;
        }

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(tool.getItem());
        BlockHPData.ToolStats stats = toolStatsMap.get(itemId);

        if (stats != null && stats.getSwingDelayTicks() >= 0) {
            return stats.getSwingDelayTicks();
        }

        return defaultSwingDelayTicks;
    }

    // ============ Проверка разрешённых предметов ============

    public boolean canAttackWithItem(ItemStack item) {
        if (item.isEmpty()) {
            return allowHandAttack;
        }

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item.getItem());
        return allowedItems.contains(itemId);
    }

    public boolean isHandAttackAllowed() {
        return allowHandAttack;
    }

    // ============ HP блоков ============

    public int getBlockMaxHP(BlockState state) {
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return blockHealthMap.getOrDefault(blockId, defaultHP);
    }

    public boolean isIndestructible(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        return chunk.getCapability(ChunkBlockDamageCapability.CAPABILITY)
                .map(cap -> cap.isIndestructible(pos))
                .orElse(false);
    }

    public void setIndestructible(Level level, BlockPos pos, boolean indestructible) {
        if (level.isClientSide()) return;

        LevelChunk chunk = level.getChunkAt(pos);
        chunk.getCapability(ChunkBlockDamageCapability.CAPABILITY)
                .ifPresent(cap -> {
                    cap.setIndestructible(pos, indestructible);
                    if (indestructible) {
                        cap.clearDamage(pos);
                    }
                    chunk.setUnsaved(true);
                });
    }

    // ============ Статистика инструментов ============

    public float getToolDamage(ItemStack tool) {
        if (tool.isEmpty()) {
            return allowHandAttack ? handDamage : 0;
        }

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(tool.getItem());

        if (!allowedItems.contains(itemId)) {
            return 0;
        }

        BlockHPData.ToolStats stats = toolStatsMap.get(itemId);
        if (stats != null) {
            return stats.getDamage();
        }

        return 0;
    }

    public float getToolAttackSpeed(ItemStack tool) {
        if (tool.isEmpty()) {
            return handAttackSpeed;
        }

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(tool.getItem());
        BlockHPData.ToolStats stats = toolStatsMap.get(itemId);

        if (stats != null) {
            return stats.getAttackSpeed();
        }

        return 4.0f;
    }

    public boolean hasToolStats(ResourceLocation itemId) {
        return toolStatsMap.containsKey(itemId);
    }

    public BlockHPData.ToolStats getToolStats(ResourceLocation itemId) {
        return toolStatsMap.get(itemId);
    }

    // ============ Эффективность ============

    public float getEffectivenessMultiplier(ItemStack tool, BlockState state) {
        if (tool.isEmpty()) {
            return 1.0f;
        }

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(tool.getItem());

        if (!allowedItems.contains(itemId)) {
            return 0;
        }

        if (toolEffectivenessMap.containsKey(itemId)) {
            Map<TagKey<Block>, Float> effectiveness = toolEffectivenessMap.get(itemId);
            for (Map.Entry<TagKey<Block>, Float> entry : effectiveness.entrySet()) {
                if (state.is(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }

        if (tool.isCorrectToolForDrops(state)) {
            return 2.0f;
        }

        return 1.0f;
    }

    public int calculateDamage(ItemStack tool, BlockState state) {
        float baseDamage = getToolDamage(tool);
        if (baseDamage <= 0) {
            return 0;
        }
        float effectiveness = getEffectivenessMultiplier(tool, state);
        return (int) Math.ceil(baseDamage * effectiveness);
    }

    // ============ Block Breaker ID ============

    public int getBlockBreakerId(BlockPos pos) {
        long key = pos.asLong();
        return blockBreakerIds.computeIfAbsent(key, k -> nextBreakerId++);
    }

    public void clearBlockBreakerId(BlockPos pos) {
        blockBreakerIds.remove(pos.asLong());
    }

    // ============ Работа с HP ============

    public int getBlockCurrentHP(Level level, BlockPos pos) {
        if (level.isClientSide()) return 0;

        if (isIndestructible(level, pos)) {
            return -1;
        }

        BlockState state = level.getBlockState(pos);
        int maxHP = getBlockMaxHP(state);
        int damage = getBlockDamage(level, pos);

        return maxHP - damage;
    }

    public int getBlockDamage(Level level, BlockPos pos) {
        if (level.isClientSide()) return 0;

        LevelChunk chunk = level.getChunkAt(pos);
        return chunk.getCapability(ChunkBlockDamageCapability.CAPABILITY)
                .map(cap -> cap.getDamage(pos))
                .orElse(0);
    }

    public void setBlockDamage(Level level, BlockPos pos, int damage) {
        if (level.isClientSide()) return;

        LevelChunk chunk = level.getChunkAt(pos);
        chunk.getCapability(ChunkBlockDamageCapability.CAPABILITY)
                .ifPresent(cap -> {
                    cap.setDamage(pos, damage);
                    chunk.setUnsaved(true);
                });
    }

    public void setBlockHP(Level level, BlockPos pos, int currentHP) {
        if (level.isClientSide()) return;

        BlockState state = level.getBlockState(pos);
        int maxHP = getBlockMaxHP(state);
        int damage = maxHP - currentHP;

        setBlockDamage(level, pos, Math.max(0, damage));
    }

    public void clearBlockDamage(Level level, BlockPos pos) {
        if (level.isClientSide()) return;

        LevelChunk chunk = level.getChunkAt(pos);
        chunk.getCapability(ChunkBlockDamageCapability.CAPABILITY)
                .ifPresent(cap -> {
                    cap.clearDamage(pos);
                    chunk.setUnsaved(true);
                });

        clearBlockBreakerId(pos);
    }

    public boolean hasBlockDamage(Level level, BlockPos pos) {
        if (level.isClientSide()) return false;

        LevelChunk chunk = level.getChunkAt(pos);
        return chunk.getCapability(ChunkBlockDamageCapability.CAPABILITY)
                .map(cap -> cap.hasDamage(pos))
                .orElse(false);
    }

    // ============ Геттеры ============

    public float getHandDamage() {
        return handDamage;
    }

    public float getHandAttackSpeed() {
        return handAttackSpeed;
    }

    public int getDefaultSwingDelayTicks() {
        return defaultSwingDelayTicks;
    }
}