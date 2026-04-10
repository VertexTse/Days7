package org.vertex.days7.accessor;

public interface IFoodDataAccessor {
    int days7$getThirstLevel();
    float days7$getThirstExhaustionLevel();
    int days7$getFoodLevel();
    float days7$getStamina();

    int days7$getMaxThirst();
    int days7$getMaxFood();
    int days7$getMaxStamina();

    void days7$setMaxThirst(int maxThirst);
    void days7$setMaxFood(int maxFood);
    void days7$setMaxStamina(int maxStamina);

    void days7$setThirstExhaustion(float thirstExhaustion);
    void days7$setThirst(int thirst);
    void days7$setFood(int food);
    void days7$setStamina(float stamina);

    boolean days7$onNeedsWater();
    void days7$drink(int level);
}
