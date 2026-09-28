package za.ac.richfield.smartpantry;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Map;

import za.ac.richfield.smartpantry.data.ApiClient;
import za.ac.richfield.smartpantry.data.PantryRepository;
import za.ac.richfield.smartpantry.logic.IngredientNormaliser;
import za.ac.richfield.smartpantry.logic.RecipeMatcher;
import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.model.Recipe;
import za.ac.richfield.smartpantry.model.RecipeIngredient;

/**
 * Shows one recipe in full: what it needs and how to make it.
 *
 * <p>The recipe arrives as a Serializable Intent extra rather than an id, so the
 * screen can draw immediately instead of showing a spinner while it fetches
 * something the previous screen already had.
 *
 * <p>Each ingredient line is ticked against the pantry. On a recipe reached from
 * the suggestions list every line is ticked, which is a quiet demonstration that
 * the strict rule did what it claims; on one reached from Almost There, the
 * single missing line is the one without a tick.
 */
public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE = "za.ac.richfield.smartpantry.RECIPE";

    private Recipe recipe;
    private LinearLayout ingredientsContainer;

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

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_recipe);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        ((TextView) findViewById(R.id.text_name)).setText(recipe.getName());
        ((TextView) findViewById(R.id.text_description)).setText(recipe.getDescription());
        ((TextView) findViewById(R.id.text_meta)).setText(
                getString(R.string.label_serves, recipe.getServes()) + "  ·  "
                        + getString(R.string.label_minutes, recipe.getPrepMinutes()));

        ingredientsContainer = findViewById(R.id.container_ingredients);
        renderSteps();
        renderIngredients(null);   // draw unticked first so the screen is never blank
        loadPantryAndTick();
    }

    private void renderSteps() {
        LinearLayout container = findViewById(R.id.container_steps);
        container.removeAllViews();
        List<String> steps = recipe.getSteps();
        for (String step : steps) {
            TextView view = new TextView(this);
            view.setText(step);
            view.setTextSize(15);
            view.setTextColor(ContextCompat.getColor(this, R.color.grey_900));
            view.setPadding(0, 0, 0, 20);
            view.setLineSpacing(4f, 1f);
            container.addView(view);
        }
    }

    /**
     * @param pantry the indexed pantry, or null before it has loaded
     */
    private void renderIngredients(Map<String, PantryItem> pantry) {
        ingredientsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            View row = inflater.inflate(R.layout.item_ingredient_line, ingredientsContainer, false);
            TextView line = row.findViewById(R.id.text_line);
            ImageView icon = row.findViewById(R.id.icon_status);

            line.setText(ingredient.getDisplayLine());

            if (pantry == null) {
                icon.setVisibility(View.INVISIBLE);
            } else {
                boolean held = pantry.containsKey(
                        IngredientNormaliser.normalise(ingredient.getName()));
                icon.setVisibility(View.VISIBLE);
                icon.setImageResource(held ? R.drawable.ic_check : R.drawable.ic_empty_basket);
                icon.setAlpha(held ? 1f : 0.35f);
            }
            ingredientsContainer.addView(row);
        }
    }

    private void loadPantryAndTick() {
        new PantryRepository(this).readAll(new ApiClient.Callback<List<PantryItem>>() {
            @Override
            public void onSuccess(List<PantryItem> items) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                renderIngredients(RecipeMatcher.indexPantry(items));
            }

            @Override
            public void onFailure(String message) {
                // The method and ingredient list are already on screen, so a
                // failure here costs only the ticks. Not worth an error dialog.
            }
        });
    }
}
