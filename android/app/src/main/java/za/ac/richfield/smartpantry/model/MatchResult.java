package za.ac.richfield.smartpantry.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The outcome of testing one recipe against the pantry.
 *
 * <p>Carries the reasons as well as the verdict. A user told only that a recipe
 * is unavailable has to work out why themselves; a user told "missing cheese"
 * knows what to buy, and the same detail makes the rule easy to demonstrate.
 */
public class MatchResult {

    public enum Status {
        /** Every ingredient is present in at least the required quantity. */
        SUGGESTED,
        /** Exactly one ingredient is missing or short. */
        ALMOST,
        /** Two or more ingredients are missing or short. */
        EXCLUDED
    }

    private final Recipe recipe;
    private final Status status;
    private final List<String> missing;

    public MatchResult(Recipe recipe, Status status, List<String> missing) {
        this.recipe = recipe;
        this.status = status;
        this.missing = missing == null ? new ArrayList<String>() : missing;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public Status getStatus() {
        return status;
    }

    public List<String> getMissing() {
        return missing;
    }

    /** Comma-separated list of what is missing, for display in a list row. */
    public String getMissingSummary() {
        return String.join(", ", missing);
    }
}
