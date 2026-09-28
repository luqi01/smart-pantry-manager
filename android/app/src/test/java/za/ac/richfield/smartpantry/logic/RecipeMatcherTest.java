package za.ac.richfield.smartpantry.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import za.ac.richfield.smartpantry.model.MatchResult;
import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.model.Recipe;

/**
 * Tests for the strict-matching rule.
 *
 * <p>Plain JUnit, so these run on the JVM with {@code ./gradlew test} and need no
 * emulator. That matters: the rule is the heart of the app, and a test that
 * needs a device attached is a test that stops being run.
 *
 * <p>The recipes are built from JSON through Gson rather than constructed
 * directly, which exercises the same deserialisation path the real API responses
 * take. A mistake in a {@code SerializedName} would fail here rather than at
 * runtime.
 */
public class RecipeMatcherTest {

    private static final Gson GSON = new Gson();

    /** The four-ingredient omelette from the seed data. */
    private static final String OMELETTE_JSON = "{"
            + "\"id\":1,\"name\":\"Cheese Omelette\",\"description\":\"d\","
            + "\"prep_steps\":\"1. Beat\\n2. Fry\",\"prep_minutes\":10,\"serves\":1,"
            + "\"ingredients\":["
            + "{\"name\":\"eggs\",\"quantity\":3,\"unit\":\"piece\"},"
            + "{\"name\":\"cheese\",\"quantity\":40,\"unit\":\"g\"},"
            + "{\"name\":\"butter\",\"quantity\":10,\"unit\":\"g\"},"
            + "{\"name\":\"salt\",\"quantity\":1,\"unit\":\"tsp\"}]}";

    private Recipe omelette() {
        return GSON.fromJson(OMELETTE_JSON, Recipe.class);
    }

    private PantryItem item(String name, double quantity, String unit) {
        return new PantryItem(0, name, quantity, unit, null);
    }

    private Map<String, PantryItem> pantry(PantryItem... items) {
        return RecipeMatcher.indexPantry(new ArrayList<>(Arrays.asList(items)));
    }

    // ------------------------------------------------------------ the rule

    @Test
    public void suggestsWhenEveryIngredientIsPresentInEnoughQuantity() {
        MatchResult result = RecipeMatcher.match(omelette(), pantry(
                item("Eggs", 6, "piece"),
                item("Cheese", 200, "g"),
                item("Butter", 250, "g"),
                item("Salt", 100, "tsp")));

        assertEquals(MatchResult.Status.SUGGESTED, result.getStatus());
        assertTrue(result.getMissing().isEmpty());
    }

    /**
     * The brief's own worked example: four of five held must not be suggested.
     * This is the single most important assertion in the suite.
     */
    @Test
    public void doesNotSuggestWhenOneIngredientIsMissing() {
        MatchResult result = RecipeMatcher.match(omelette(), pantry(
                item("Eggs", 6, "piece"),
                item("Butter", 250, "g"),
                item("Salt", 100, "tsp")));

        assertNotEquals(MatchResult.Status.SUGGESTED, result.getStatus());
        assertEquals(MatchResult.Status.ALMOST, result.getStatus());
        assertEquals(1, result.getMissing().size());
        assertTrue(result.getMissing().get(0).contains("cheese"));
    }

    @Test
    public void excludesWhenTwoOrMoreAreMissing() {
        MatchResult result = RecipeMatcher.match(omelette(), pantry(
                item("Eggs", 6, "piece"),
                item("Butter", 250, "g")));

        assertEquals(MatchResult.Status.EXCLUDED, result.getStatus());
    }

    @Test
    public void doesNotSuggestWhenAnIngredientIsPresentButShort() {
        MatchResult result = RecipeMatcher.match(omelette(), pantry(
                item("Eggs", 2, "piece"),        // recipe needs 3
                item("Cheese", 200, "g"),
                item("Butter", 250, "g"),
                item("Salt", 100, "tsp")));

        assertEquals(MatchResult.Status.ALMOST, result.getStatus());
        assertTrue(result.getMissing().get(0).contains("eggs"));
    }

    @Test
    public void exactQuantityIsEnough() {
        MatchResult result = RecipeMatcher.match(omelette(), pantry(
                item("Eggs", 3, "piece"),
                item("Cheese", 40, "g"),
                item("Butter", 10, "g"),
                item("Salt", 1, "tsp")));

        assertEquals(MatchResult.Status.SUGGESTED, result.getStatus());
    }

    @Test
    public void emptyPantrySuggestsNothing() {
        MatchResult result = RecipeMatcher.match(omelette(), pantry());
        assertEquals(MatchResult.Status.EXCLUDED, result.getStatus());
    }

    // ------------------------------------------------- real-world messiness

    @Test
    public void pluralAndSingularNamesMatch() {
        // Pantry holds "egg", recipe asks for "eggs". A naive equals() fails here.
        MatchResult result = RecipeMatcher.match(omelette(), pantry(
                item("egg", 12, "piece"),
                item("Cheese", 1, "kg"),
                item("butter", 1, "kg"),
                item("salt", 2, "cup")));

        assertEquals(MatchResult.Status.SUGGESTED, result.getStatus());
    }

    @Test
    public void caseAndDescriptorsAreIgnored() {
        assertEquals("tomato", IngredientNormaliser.normalise("Tomatoes"));
        assertEquals("tomato", IngredientNormaliser.normalise("  2 large RIPE tomatoes "));
        assertEquals("potato", IngredientNormaliser.normalise("Potatoes"));
        assertEquals("cheese", IngredientNormaliser.normalise("Grated Cheddar Cheese"));
        assertEquals("milk", IngredientNormaliser.normalise("Milk (full cream)"));
    }

    @Test
    public void synonymsResolveBeforeDescriptorsAreStripped() {
        // "ground" is a descriptor, so stripping it first would leave "beef".
        assertEquals("mince", IngredientNormaliser.normalise("ground beef"));
        assertEquals("mince", IngredientNormaliser.normalise("Beef Mince"));
        assertEquals("pasta", IngredientNormaliser.normalise("Spaghetti"));
        assertEquals("maize meal", IngredientNormaliser.normalise("Mielie Meal"));
    }

    @Test
    public void unitsConvertWithinTheSameDimension() {
        assertEquals(1000.0, UnitConverter.toBase(1, "kg").amount, 0.001);
        assertEquals(30.0, UnitConverter.toBase(2, "tbsp").amount, 0.001);
        assertEquals(UnitConverter.Dimension.MASS, UnitConverter.toBase(1, "g").dimension);
        assertEquals(UnitConverter.Dimension.COUNT, UnitConverter.toBase(1, "slice").dimension);
    }

    @Test
    public void kilogramsSatisfyAGramRequirement() {
        MatchResult result = RecipeMatcher.match(omelette(), pantry(
                item("Eggs", 3, "piece"),
                item("Cheese", 1, "kg"),          // recipe asks for 40 g
                item("Butter", 1, "kg"),
                item("Salt", 1, "tsp")));

        assertEquals(MatchResult.Status.SUGGESTED, result.getStatus());
    }

    @Test
    public void wholeItemsAreWeighedAgainstAMassRequirement() {
        // A recipe wanting 150 g of onion is satisfied by two whole onions.
        String json = "{\"name\":\"Onion Test\",\"prep_steps\":\"\",\"ingredients\":"
                + "[{\"name\":\"onion\",\"quantity\":150,\"unit\":\"g\"}]}";
        Recipe recipe = GSON.fromJson(json, Recipe.class);

        assertEquals(MatchResult.Status.SUGGESTED,
                RecipeMatcher.match(recipe, pantry(item("Onions", 2, "piece"))).getStatus());
        assertNotEquals(MatchResult.Status.SUGGESTED,
                RecipeMatcher.match(recipe, pantry(item("Onions", 0.5, "piece"))).getStatus());
    }

    @Test
    public void incomparableUnitsAreNotSilentlyAccepted() {
        // Salt held by mass cannot be checked against a teaspoon requirement, and
        // guessing in the user's favour would break the strict rule.
        MatchResult result = RecipeMatcher.match(omelette(), pantry(
                item("Eggs", 6, "piece"),
                item("Cheese", 200, "g"),
                item("Butter", 250, "g"),
                item("Salt", 500, "g")));

        assertNotEquals(MatchResult.Status.SUGGESTED, result.getStatus());
    }

    // ---------------------------------------------------------- list level

    @Test
    public void suggestedListExcludesAlmostThereRecipes() {
        List<Recipe> recipes = new ArrayList<>();
        recipes.add(omelette());

        List<PantryItem> short1 = Arrays.asList(
                item("Eggs", 6, "piece"),
                item("Butter", 250, "g"),
                item("Salt", 100, "tsp"));

        assertTrue(RecipeMatcher.suggested(recipes, short1).isEmpty());
        assertEquals(1, RecipeMatcher.almostThere(recipes, short1).size());
    }

    @Test
    public void duplicatePantryRowsKeepTheLargerQuantity() {
        Map<String, PantryItem> index = pantry(
                item("Eggs", 2, "piece"),
                item("eggs", 9, "piece"));

        assertEquals(1, index.size());
        assertEquals(9.0, index.get("egg").getQuantity(), 0.001);
    }
}
