package za.ac.richfield.smartpantry.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A recipe with the ingredients it requires and the method for making it.
 *
 * <p>The API returns ingredients nested inside each recipe rather than as a
 * separate call, so one request gives the app everything the matcher needs.
 */
public class Recipe implements Serializable {

    private long id;
    private String name;
    private String description;

    @SerializedName("prep_steps")
    private String prepSteps;

    @SerializedName("prep_minutes")
    private int prepMinutes;

    private int serves;

    private List<RecipeIngredient> ingredients = new ArrayList<>();

    public Recipe() {
        // Required by Gson.
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPrepSteps() {
        return prepSteps;
    }

    public int getPrepMinutes() {
        return prepMinutes;
    }

    public int getServes() {
        return serves;
    }

    /**
     * Never null. A recipe row with no ingredient rows would otherwise crash the
     * matcher, and the matcher runs on every pantry change.
     */
    public List<RecipeIngredient> getIngredients() {
        if (ingredients == null) {
            ingredients = new ArrayList<>();
        }
        return ingredients;
    }

    /** The method, split into the numbered steps stored in one text column. */
    public List<String> getSteps() {
        List<String> steps = new ArrayList<>();
        if (prepSteps == null) {
            return steps;
        }
        for (String line : prepSteps.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                steps.add(trimmed);
            }
        }
        return steps;
    }
}
