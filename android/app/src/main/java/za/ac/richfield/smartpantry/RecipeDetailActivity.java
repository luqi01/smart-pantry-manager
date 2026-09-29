package za.ac.richfield.smartpantry;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.Map;

import za.ac.richfield.smartpantry.data.ApiClient;
import za.ac.richfield.smartpantry.data.PantryRepository;
import za.ac.richfield.smartpantry.logic.IngredientNormaliser;
import za.ac.richfield.smartpantry.logic.PantryDeduction;
import za.ac.richfield.smartpantry.logic.RecipeMatcher;
import za.ac.richfield.smartpantry.model.MatchResult;
import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.model.Recipe;
import za.ac.richfield.smartpantry.model.RecipeIngredient;

/**
 * Shows one recipe in full: what it needs, how to make it, and - when the pantry
 * covers it - a way to say it has been made.
 *
 * <p>The recipe arrives as a Serializable Intent extra rather than an id, so the
 * screen can draw immediately instead of showing a spinner while it fetches
 * something the previous screen already had.
 *
 * <p>Each ingredient line is ticked against the pantry. On a recipe reached from
 * the suggestions list every line is ticked, which is a quiet demonstration that
 * the strict rule did what it claims; on one reached from Almost There, the
 * single missing line is the one without a tick.
 *
 * <p>Cooking is the one action that changes the pantry from this screen, and it
 * is offered only when the rule says every ingredient is held. That is
 * deliberate: the button's presence is another place the rule is visible rather
 * than merely described, and it means the deduction can never be asked to take
 * away something the user does not have.
 */
public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE = "za.ac.richfield.smartpantry.RECIPE";

    private Recipe recipe;
    private LinearLayout ingredientsContainer;
    private MaterialButton cookedButton;
    private PantryRepository repository;

    /** The pantry as last loaded, kept because the deduction is computed from it. */
    private List<PantryItem> pantry;

    /** What the last cook took off, kept so it can be put back. */
    private List<PantryDeduction.Change> lastCooked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        recipe = (Recipe) getIntent().getSerializableExtra(EXTRA_RECIPE);
        if (recipe == null) {
            // Nothing sensible to show, and carrying on would throw on the next line.
            finish();
            return;
        }
        repository = new PantryRepository(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // The page carries its own heading; the bar repeating it is noise.
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        ((TextView) findViewById(R.id.text_name)).setText(recipe.getName());
        ((TextView) findViewById(R.id.text_description)).setText(recipe.getDescription());
        ((TextView) findViewById(R.id.text_meta)).setText(
                getString(R.string.label_serves, recipe.getServes()) + "  ·  "
                        + getString(R.string.label_minutes, recipe.getPrepMinutes()));

        cookedButton = findViewById(R.id.button_cooked);
        cookedButton.setOnClickListener(v -> cook());

        ingredientsContainer = findViewById(R.id.container_ingredients);
        renderSteps();
        renderIngredients(null);   // draw unticked first so the screen is never blank
        loadPantryAndTick();
    }

    private void renderSteps() {
        LinearLayout container = findViewById(R.id.container_steps);
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        List<String> steps = recipe.getSteps();
        for (int i = 0; i < steps.size(); i++) {
            View row = inflater.inflate(R.layout.item_step_line, container, false);
            // The seed stores steps already numbered; strip it so the numeral
            // is not printed twice once the layout supplies one.
            String text = steps.get(i).replaceFirst("^\\d+[.)]\\s*", "");
            ((TextView) row.findViewById(R.id.text_number)).setText(String.valueOf(i + 1));
            ((TextView) row.findViewById(R.id.text_step)).setText(text);
            container.addView(row);
        }
    }

    /**
     * @param pantryIndex the indexed pantry, or null before it has loaded
     */
    private void renderIngredients(Map<String, PantryItem> pantryIndex) {
        ingredientsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            View row = inflater.inflate(R.layout.item_ingredient_line, ingredientsContainer, false);
            TextView line = row.findViewById(R.id.text_line);
            ImageView icon = row.findViewById(R.id.icon_status);

            line.setText(ingredient.getDisplayLine());

            if (pantryIndex == null) {
                icon.setVisibility(View.INVISIBLE);
            } else {
                boolean held = pantryIndex.containsKey(
                        IngredientNormaliser.normalise(ingredient.getName()));
                icon.setVisibility(View.VISIBLE);
                icon.setImageResource(held ? R.drawable.ic_check : R.drawable.ic_empty_basket);
                icon.setAlpha(held ? 1f : 0.35f);
            }
            ingredientsContainer.addView(row);
        }
    }

    private void loadPantryAndTick() {
        repository.readAll(new ApiClient.Callback<List<PantryItem>>() {
            @Override
            public void onSuccess(List<PantryItem> items) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                pantry = items;
                renderIngredients(RecipeMatcher.indexPantry(items));
                showCookedButtonIfCookable();
            }

            @Override
            public void onFailure(String message) {
                // The method and ingredient list are already on screen, so a
                // failure here costs only the ticks. Not worth an error dialog.
            }
        });
    }

    /**
     * The same rule the suggestions list runs, asked about this one recipe.
     * Calling {@link RecipeMatcher} rather than counting the ticks means the
     * button and the list can never disagree about what is cookable.
     */
    private void showCookedButtonIfCookable() {
        MatchResult result = RecipeMatcher.match(recipe, RecipeMatcher.indexPantry(pantry));
        boolean cookable = result.getStatus() == MatchResult.Status.SUGGESTED;
        cookedButton.setVisibility(cookable ? View.VISIBLE : View.GONE);
        cookedButton.setEnabled(cookable);
        cookedButton.setText(R.string.action_cooked);
    }

    private void cook() {
        final List<PantryDeduction.Change> changes =
                PantryDeduction.forRecipe(recipe, pantry);

        if (changes.isEmpty()) {
            // Every ingredient was measured in something the app could not
            // subtract. Saying so is better than silently doing nothing.
            say(getString(R.string.toast_cooked_nothing, recipe.getName()));
            return;
        }

        cookedButton.setEnabled(false);
        repository.applyDeductions(changes, new ApiClient.Callback<Object>() {
            @Override
            public void onSuccess(Object ignored) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                lastCooked = changes;
                // Reloaded first, so the bar has settled into its final state
                // before the note anchors itself above it.
                loadPantryAndTick();
                note(getString(R.string.toast_cooked, recipe.getName()), Snackbar.LENGTH_LONG)
                        .setAction(R.string.action_undo, v -> undoCook())
                        .show();
            }

            @Override
            public void onFailure(String message) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                cookedButton.setEnabled(true);
                say(message);
            }
        });
    }

    private void undoCook() {
        if (lastCooked == null) {
            return;
        }
        final List<PantryDeduction.Change> changes = lastCooked;
        lastCooked = null;
        repository.undoDeductions(changes, new ApiClient.Callback<Object>() {
            @Override
            public void onSuccess(Object ignored) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                loadPantryAndTick();
            }

            @Override
            public void onFailure(String message) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                say(getString(R.string.error_undo));
            }
        });
    }

    private void say(String message) {
        note(message, Snackbar.LENGTH_LONG).show();
    }

    /**
     * A note anchored above the Cooked it bar rather than laid over it.
     *
     * <p>Left to itself a snackbar covers the bottom of the screen, which is
     * exactly where the button that raised it sits. A tap aimed at Undo that
     * arrives a moment after the note has gone lands on Cooked it instead, and
     * cooks the recipe a second time - the opposite of what was intended, and
     * silently. Anchoring moves the note above the bar so the two never share a
     * place on the screen.
     */
    private Snackbar note(String message, int duration) {
        Snackbar bar = Snackbar.make(findViewById(android.R.id.content), message, duration);
        if (cookedButton.getVisibility() == View.VISIBLE) {
            bar.setAnchorView(cookedButton);
        }
        return bar;
    }
}
