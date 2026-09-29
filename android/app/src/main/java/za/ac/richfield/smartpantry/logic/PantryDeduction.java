package za.ac.richfield.smartpantry.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.model.Recipe;
import za.ac.richfield.smartpantry.model.RecipeIngredient;

/**
 * Works out what cooking a recipe takes out of the pantry.
 *
 * <p>The application suggests what can be cooked and then, until now, forgot
 * about it. Cooking is the one thing that actually changes a pantry, so a
 * suggestion the user acted on left the list wrong: the maize meal was still
 * recorded as 2 kg after half of it had gone into the pot, and every suggestion
 * made afterwards was based on stock that no longer existed.
 *
 * <p>The deduction reuses {@link RecipeMatcher#compare} rather than working the
 * units out again. That matters more than it looks. If the two ever disagreed,
 * the application could suggest a recipe the rule says is cookable and then fail
 * to subtract it, or subtract more than the user had. One comparison, used by
 * both, cannot drift.
 *
 * <p>Nothing here touches the network. It returns a plain list of intended
 * changes, which makes it testable on the JVM and makes the undo in
 * {@link za.ac.richfield.smartpantry.data.PantryRepository} straightforward:
 * every change carries the item exactly as it was beforehand.
 */
public final class PantryDeduction {

    /** Below this much left, an ingredient is treated as finished. */
    private static final double EXHAUSTED = 1e-6;

    /** One pantry item, and what cooking leaves of it. */
    public static final class Change {

        private final PantryItem before;
        private final double newQuantity;
        private final boolean exhausted;

        Change(PantryItem before, double newQuantity, boolean exhausted) {
            this.before = before;
            this.newQuantity = newQuantity;
            this.exhausted = exhausted;
        }

        /** The item as it stood before cooking. Undo restores exactly this. */
        public PantryItem getBefore() {
            return before;
        }

        /** What is left, in the item's own unit. Meaningless when exhausted. */
        public double getNewQuantity() {
            return newQuantity;
        }

        /** Whether the recipe uses the item up, so the row should be removed. */
        public boolean isExhausted() {
            return exhausted;
        }
    }

    private PantryDeduction() {
        // Utility class; never instantiated.
    }

    /**
     * @param recipe the recipe being cooked
     * @param pantry every item currently held
     * @return one change per ingredient the pantry actually holds. Ingredients
     *         that cannot be found or cannot be compared are skipped rather
     *         than guessed at: taking away an amount the application is not sure
     *         about would corrupt the user's own record of their kitchen.
     */
    public static List<Change> forRecipe(Recipe recipe, List<PantryItem> pantry) {
        List<Change> changes = new ArrayList<>();
        if (recipe == null) {
            return changes;
        }
        Map<String, PantryItem> index = RecipeMatcher.indexPantry(pantry);

        for (RecipeIngredient required : recipe.getIngredients()) {
            String key = IngredientNormaliser.normalise(required.getName());
            PantryItem held = index.get(key);
            if (held == null) {
                continue;
            }

            RecipeMatcher.Comparison comparison = RecipeMatcher.compare(key, held, required);
            if (comparison == null || comparison.getHave() <= 0) {
                continue;
            }

            double remaining = comparison.getHave() - comparison.getNeed();
            if (remaining <= EXHAUSTED) {
                changes.add(new Change(held, 0, true));
                continue;
            }

            // Back out of the common unit by proportion rather than by looking
            // the conversion factor up a second time. The item keeps the unit
            // the user entered it in: 2 kg of maize meal less 250 g is
            // 1.75 kg, not 1750 g.
            double scaled = held.getQuantity() * (remaining / comparison.getHave());
            changes.add(new Change(held, round(scaled), false));
        }
        return changes;
    }

    /**
     * The database column is {@code NUMERIC(10,2)}, so a quantity is stored to
     * two places whatever is sent. Rounding here means the number the user is
     * shown is the number that was saved.
     */
    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
