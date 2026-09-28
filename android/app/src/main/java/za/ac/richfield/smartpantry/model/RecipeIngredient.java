package za.ac.richfield.smartpantry.model;

import java.io.Serializable;

/**
 * One line of a recipe: what it needs, how much, and in which unit.
 *
 * <p>Deliberately a separate class from {@link PantryItem} even though the
 * fields look similar. They mean different things - a pantry item is a quantity
 * the user holds, a recipe ingredient is a quantity a recipe demands - and the
 * strict-matching rule compares one against the other. Merging them would make
 * the direction of that comparison easy to get backwards.
 */
public class RecipeIngredient implements Serializable {

    private String name;
    private double quantity;
    private String unit;

    public RecipeIngredient() {
        // Required by Gson.
    }

    public RecipeIngredient(String name, double quantity, String unit) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
    }

    public String getName() {
        return name;
    }

    public double getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    public String getDisplayQuantity() {
        if (quantity == Math.floor(quantity) && !Double.isInfinite(quantity)) {
            return String.valueOf((long) quantity);
        }
        return String.valueOf(quantity);
    }

    /** For example "3 piece eggs", as shown on the recipe detail screen. */
    public String getDisplayLine() {
        return getDisplayQuantity() + " " + (unit == null ? "" : unit) + "  " + name;
    }
}
