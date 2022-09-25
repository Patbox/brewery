package eu.pb4.brewery.duck;

import eu.pb4.brewery.drink.AlcoholManager;

public interface LivingEntityExt {
    AlcoholManager brewery$getAlcoholManager();
    void brewery$setAlcoholManager(AlcoholManager manager);
}
