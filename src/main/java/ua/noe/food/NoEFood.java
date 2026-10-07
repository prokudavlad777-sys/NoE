package ua.noe.food;

import ua.noe.item.ItemData;
import ua.noe.item.NoEItem;

/** A custom edible item. Effects are applied when the item is consumed. */
public class NoEFood extends NoEItem {

    public NoEFood(ItemData data) {
        super(data);
    }

    public ItemData.FoodSpec getFood() {
        return data().food();
    }
}
