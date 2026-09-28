package za.ac.richfield.smartpantry.logic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Converts quantities into a common base so two amounts can be compared.
 *
 * <p>The strict-matching rule needs to decide whether a pantry holds "at least
 * the required quantity". That question is meaningless until both sides are in
 * the same unit: 1 kg of flour against 250 g of flour is a comparison, 1 kg
 * against 2 cups is not.
 *
 * <p>Units fall into three dimensions, each with a base unit:
 *
 * <ul>
 *   <li>{@code MASS} in grams</li>
 *   <li>{@code VOLUME} in millilitres</li>
 *   <li>{@code COUNT} in items, where a slice, a clove and a tin all count as
 *       one, because a user who types "piece" instead of "can" means the same
 *       thing and should not be punished for it</li>
 * </ul>
 *
 * <p>When the two sides land in different dimensions there is one bridge:
 * {@link #pieceWeightGrams(String)} gives an average weight for common whole
 * items, so "2 onions" can be checked against "150 g onion". Where no weight is
 * known the comparison is refused rather than guessed. Refusing is the safer
 * failure: it keeps a recipe out of the suggestions list, which is what
 * "strictly" is supposed to mean.
 */
public final class UnitConverter {

    public enum Dimension {
        MASS, VOLUME, COUNT
    }

    /** An amount expressed in the base unit of its dimension. */
    public static final class Quantity {
        public final double amount;
        public final Dimension dimension;

        Quantity(double amount, Dimension dimension) {
            this.amount = amount;
            this.dimension = dimension;
        }
    }

    private static final Map<String, Quantity> UNITS = buildUnits();
    private static final Map<String, Double> PIECE_WEIGHTS = buildPieceWeights();

    private UnitConverter() {
        // Utility class; never instantiated.
    }

    private static Map<String, Quantity> buildUnits() {
        Map<String, Quantity> map = new HashMap<>();

        map.put("mg", new Quantity(0.001, Dimension.MASS));
        map.put("g", new Quantity(1.0, Dimension.MASS));
        map.put("gram", new Quantity(1.0, Dimension.MASS));
        map.put("kg", new Quantity(1000.0, Dimension.MASS));
        map.put("oz", new Quantity(28.3495, Dimension.MASS));
        map.put("lb", new Quantity(453.592, Dimension.MASS));

        map.put("ml", new Quantity(1.0, Dimension.VOLUME));
        map.put("l", new Quantity(1000.0, Dimension.VOLUME));
        map.put("litre", new Quantity(1000.0, Dimension.VOLUME));
        map.put("tsp", new Quantity(5.0, Dimension.VOLUME));
        map.put("tbsp", new Quantity(15.0, Dimension.VOLUME));
        map.put("cup", new Quantity(250.0, Dimension.VOLUME));   // metric cup

        map.put("piece", new Quantity(1.0, Dimension.COUNT));
        map.put("pc", new Quantity(1.0, Dimension.COUNT));
        map.put("unit", new Quantity(1.0, Dimension.COUNT));
        map.put("slice", new Quantity(1.0, Dimension.COUNT));
        map.put("clove", new Quantity(1.0, Dimension.COUNT));
        map.put("can", new Quantity(1.0, Dimension.COUNT));
        map.put("tin", new Quantity(1.0, Dimension.COUNT));
        map.put("packet", new Quantity(1.0, Dimension.COUNT));
        map.put("punnet", new Quantity(1.0, Dimension.COUNT));
        map.put("bunch", new Quantity(1.0, Dimension.COUNT));
        map.put("", new Quantity(1.0, Dimension.COUNT));

        return Collections.unmodifiableMap(map);
    }

    /**
     * Average edible weight of one whole item, in grams.
     *
     * <p>These are approximations and are meant to be. They exist only to bridge
     * a count against a mass, and the alternative - refusing every such
     * comparison - would exclude sensible recipes for no good reason. Keeping
     * them in a named, visible table makes the approximation reviewable instead
     * of hiding it inside the matcher.
     */
    private static Map<String, Double> buildPieceWeights() {
        Map<String, Double> map = new HashMap<>();
        map.put("onion", 150.0);
        map.put("tomato", 120.0);
        map.put("potato", 170.0);
        map.put("carrot", 60.0);
        map.put("egg", 50.0);
        map.put("banana", 120.0);
        map.put("lemon", 100.0);
        map.put("garlic", 5.0);        // one clove
        map.put("butternut", 900.0);
        map.put("chicken", 170.0);     // one breast
        map.put("bread", 30.0);        // one slice
        map.put("apple", 180.0);
        return Collections.unmodifiableMap(map);
    }

    /**
     * Converts an amount to its dimension base unit.
     *
     * @return the converted quantity, or {@code null} when the unit is not one
     *         the app knows, which callers must treat as "cannot compare"
     */
    public static Quantity toBase(double amount, String unit) {
        String key = unit == null ? "" : unit.trim().toLowerCase(Locale.ROOT);
        Quantity definition = UNITS.get(key);
        if (definition == null) {
            // "pieces" and "grams" are the same units the user meant.
            definition = UNITS.get(IngredientNormaliser.singularise(key));
        }
        if (definition == null) {
            return null;
        }
        return new Quantity(amount * definition.amount, definition.dimension);
    }

    /**
     * @param normalisedName an ingredient key from {@link IngredientNormaliser}
     * @return grams per item, or 0 when no average is known for this ingredient
     */
    public static double pieceWeightGrams(String normalisedName) {
        Double weight = PIECE_WEIGHTS.get(normalisedName);
        return weight == null ? 0.0 : weight;
    }

    public static boolean isKnownUnit(String unit) {
        String key = unit == null ? "" : unit.trim().toLowerCase(Locale.ROOT);
        return UNITS.containsKey(key) || UNITS.containsKey(IngredientNormaliser.singularise(key));
    }
}
