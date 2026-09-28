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

            UnitConverter.Quantity have = UnitConverter.toBase(held.getQuantity(), held.getUnit());
            UnitConverter.Quantity need = UnitConverter.toBase(required.getQuantity(), required.getUnit());

            if (have == null || need == null) {
                // An unrecognised unit means the comparison cannot be trusted, so
                // the ingredient counts as not satisfied.
                missing.add(required.getName());
                continue;
            }

            double haveAmount = have.amount;
            double needAmount = need.amount;

            if (have.dimension != need.dimension) {
                double gramsEach = UnitConverter.pieceWeightGrams(key);
                if (gramsEach <= 0) {
                    missing.add(required.getName());
                    continue;
                }
                if (have.dimension == UnitConverter.Dimension.COUNT
                        && need.dimension == UnitConverter.Dimension.MASS) {
                    haveAmount = have.amount * gramsEach;
                } else if (have.dimension == UnitConverter.Dimension.MASS
                        && need.dimension == UnitConverter.Dimension.COUNT) {
                    needAmount = need.amount * gramsEach;
                } else {
                    // Mass against volume, with no density to convert through.
                    missing.add(required.getName());
                    continue;
                }
            }

            if (haveAmount + EPSILON < needAmount) {
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
