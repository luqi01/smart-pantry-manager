package za.ac.richfield.smartpantry.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import za.ac.richfield.smartpantry.model.MatchResult;
import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.model.Recipe;
import za.ac.richfield.smartpantry.model.RecipeIngredient;

/**
 * Decides which recipes the user can cook right now.
 *
 * <p>This is the rule the whole application is built around. A recipe may only
 * be suggested when <em>every</em> ingredient it requires is present in the
 * pantry in at least the required quantity. A recipe needing five ingredients
 * where the user holds four is not suggested, however close it is - that is what
 * "strictly" means, and it is the difference between an app that saves someone a
 * trip to the shop and one that sends them on it.
 *
 * <p>Recipes that fall one ingredient short go to a separate "Almost There"
 * list. That list is kept deliberately apart from the suggestions, because
 * folding the two together would quietly undo the rule.
 *
 * <p>The matching runs on the device rather than in SQL. Two reasons: the
 * normalisation and unit rules are easier to read, test and explain as Java than
 * as a query, and the recompute happens on every pantry edit, where a round trip
 * to the server would make the list feel slow.
 */
public final class RecipeMatcher {

    /** Tolerance for floating-point comparison. 250g stored as 249.9999 still counts. */
    private static final double EPSILON = 1e-9;

    private RecipeMatcher() {
        // Utility class; never instantiated.
    }

    /**
     * Builds a lookup of normalised ingredient name to pantry item.
     *
     * <p>The database has a unique index on the name, so duplicates should not
     * arise. If two rows ever do normalise to the same key, the larger quantity
     * wins: under-reporting what the user holds would hide a recipe they can
     * actually make.
     */
    public static Map<String, PantryItem> indexPantry(List<PantryItem> items) {
        Map<String, PantryItem> index = new HashMap<>();
        if (items == null) {
            return index;
        }
        for (PantryItem item : items) {
            String key = IngredientNormaliser.normalise(item.getName());
            if (key.isEmpty()) {
                continue;
            }
            PantryItem existing = index.get(key);
            if (existing == null) {
                index.put(key, item);
                continue;
            }
            UnitConverter.Quantity held = UnitConverter.toBase(existing.getQuantity(), existing.getUnit());
            UnitConverter.Quantity candidate = UnitConverter.toBase(item.getQuantity(), item.getUnit());
            if (held != null && candidate != null
                    && held.dimension == candidate.dimension
                    && candidate.amount > held.amount) {
                index.put(key, item);
            }
        }
        return index;
    }

    /**
     * The two sides of one ingredient check, reduced to a single common unit.
     *
     * <p>Which unit that is does not matter to the caller and is deliberately
     * not exposed. What matters is that both numbers are in the same one, so
     * they can be compared and subtracted.
     */
    public static final class Comparison {

        private final double have;
        private final double need;

        Comparison(double have, double need) {
            this.have = have;
            this.need = need;
        }

        /** How much the pantry holds, in the common unit. */
        public double getHave() {
            return have;
        }

        /** How much the recipe calls for, in the common unit. */
        public double getNeed() {
            return need;
        }

        /** Whether the pantry covers the recipe, within floating-point slack. */
        public boolean isSatisfied() {
            return have + EPSILON >= need;
        }
    }

    /**
     * Puts a held amount and a required amount into one unit.
     *
     * <p>Extracted from {@link #match} so that
     * {@link PantryDeduction} can subtract using exactly the arithmetic the rule
     * used to decide the recipe was cookable. Two copies of this would
     * eventually disagree, and the app would either suggest a recipe it then
     * could not deduct or take away more than the user had.
     *
     * @param key      the normalised ingredient name, used to look up an average
     *                 item weight when a count meets a mass
     * @return {@code null} when no honest comparison is possible - an unknown
     *         unit, or a mass against a volume with no density to bridge them.
     *         Refusing is the safer failure: it keeps a recipe out of the
     *         suggestions, which is what "strictly" is supposed to mean.
     */
    public static Comparison compare(String key, PantryItem held, RecipeIngredient required) {
        UnitConverter.Quantity have = UnitConverter.toBase(held.getQuantity(), held.getUnit());
        UnitConverter.Quantity need = UnitConverter.toBase(required.getQuantity(), required.getUnit());

        if (have == null || need == null) {
            return null;
        }

        double haveAmount = have.amount;
        double needAmount = need.amount;

        if (have.dimension != need.dimension) {
            double gramsEach = UnitConverter.pieceWeightGrams(key);
            if (gramsEach <= 0) {
                return null;
            }
            if (have.dimension == UnitConverter.Dimension.COUNT
                    && need.dimension == UnitConverter.Dimension.MASS) {
                haveAmount = have.amount * gramsEach;
            } else if (have.dimension == UnitConverter.Dimension.MASS
                    && need.dimension == UnitConverter.Dimension.COUNT) {
                needAmount = need.amount * gramsEach;
            } else {
                // Mass against volume, with no density to convert through.
                return null;
            }
        }
        return new Comparison(haveAmount, needAmount);
    }

    /**
     * Applies the strict rule to one recipe.
     *
     * @param recipe the recipe under test
     * @param pantry the output of {@link #indexPantry(List)}
     * @return the outcome, including a human-readable reason for each ingredient
     *         that fell short
     */
    public static MatchResult match(Recipe recipe, Map<String, PantryItem> pantry) {
        List<String> missing = new ArrayList<>();

        for (RecipeIngredient required : recipe.getIngredients()) {
            String key = IngredientNormaliser.normalise(required.getName());
            PantryItem held = pantry.get(key);

            if (held == null) {
                missing.add(required.getName());
                continue;
            }

            Comparison comparison = compare(key, held, required);
            if (comparison == null) {
                // The two amounts cannot honestly be compared, so the
                // ingredient counts as not satisfied.
                missing.add(required.getName());
                continue;
            }

            if (!comparison.isSatisfied()) {
                missing.add(String.format(Locale.UK, "%s (need %s %s, you have %s %s)",
                        required.getName(),
                        required.getDisplayQuantity(), required.getUnit(),
                        held.getDisplayQuantity(), held.getUnit()));
            }
        }

        MatchResult.Status status;
        if (missing.isEmpty()) {
            status = MatchResult.Status.SUGGESTED;
        } else if (missing.size() == 1) {
            status = MatchResult.Status.ALMOST;
        } else {
            status = MatchResult.Status.EXCLUDED;
        }
        return new MatchResult(recipe, status, missing);
    }

    /**
     * Runs the rule over every recipe and returns only those the user can cook.
     * Sorted by fewest ingredients first, so the quickest option is at the top.
     */
    public static List<MatchResult> suggested(List<Recipe> recipes, List<PantryItem> pantry) {
        return collect(recipes, pantry, MatchResult.Status.SUGGESTED);
    }

    /** Recipes that are short by exactly one ingredient. */
    public static List<MatchResult> almostThere(List<Recipe> recipes, List<PantryItem> pantry) {
        return collect(recipes, pantry, MatchResult.Status.ALMOST);
    }

    private static List<MatchResult> collect(List<Recipe> recipes, List<PantryItem> pantry,
                                             MatchResult.Status wanted) {
        List<MatchResult> results = new ArrayList<>();
        if (recipes == null) {
            return results;
        }
        Map<String, PantryItem> index = indexPantry(pantry);
        for (Recipe recipe : recipes) {
            MatchResult result = match(recipe, index);
            if (result.getStatus() == wanted) {
                results.add(result);
            }
        }
        results.sort((left, right) -> {
            int bySize = Integer.compare(left.getRecipe().getIngredients().size(),
                    right.getRecipe().getIngredients().size());
            if (bySize != 0) {
                return bySize;
            }
            return left.getRecipe().getName().compareToIgnoreCase(right.getRecipe().getName());
        });
        return results;
    }
}
