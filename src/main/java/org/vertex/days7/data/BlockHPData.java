// data/BlockHPData.java
package org.vertex.days7.data;

import java.util.Map;

public class BlockHPData {
    private int defaultHP = 100;
    private float handDamage = 1.0f;
    private float handAttackSpeed = 4.0f;
    private boolean allowHandAttack = true;

    // Задержка перед ударом в тиках (1 тик = 50мс)
    private int swingDelayTicks = 4;

    private Map<String, Integer> blocks;
    private Map<String, ToolStats> tools;
    private Map<String, Map<String, Float>> toolEffectiveness;

    public int getDefaultHP() {
        return defaultHP;
    }

    public float getHandDamage() {
        return handDamage;
    }

    public float getHandAttackSpeed() {
        return handAttackSpeed;
    }

    public boolean isAllowHandAttack() {
        return allowHandAttack;
    }

    public int getSwingDelayTicks() {
        return swingDelayTicks;
    }

    public Map<String, Integer> getBlocks() {
        return blocks;
    }

    public Map<String, ToolStats> getTools() {
        return tools;
    }

    public Map<String, Map<String, Float>> getToolEffectiveness() {
        return toolEffectiveness;
    }

    public static class ToolStats {
        private float damage = 10.0f;
        private float attackSpeed = 4.0f;
        // Индивидуальная задержка замаха для инструмента (опционально)
        private int swingDelayTicks = -1; // -1 = использовать глобальное значение

        public float getDamage() {
            return damage;
        }

        public float getAttackSpeed() {
            return attackSpeed;
        }

        public int getSwingDelayTicks() {
            return swingDelayTicks;
        }
    }
}