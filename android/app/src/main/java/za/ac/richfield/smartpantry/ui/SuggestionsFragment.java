package za.ac.richfield.smartpantry.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import za.ac.richfield.smartpantry.R;
import za.ac.richfield.smartpantry.RecipeDetailActivity;
import za.ac.richfield.smartpantry.adapter.RecipeAdapter;
import za.ac.richfield.smartpantry.data.ApiClient;
import za.ac.richfield.smartpantry.data.PantryRepository;
import za.ac.richfield.smartpantry.data.RecipeRepository;
import za.ac.richfield.smartpantry.logic.RecipeMatcher;
import za.ac.richfield.smartpantry.model.MatchResult;
import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.model.Recipe;

/**
 * Shows the recipes the user can cook right now.
 *
 * <p>The screen needs both halves of the comparison before it can show anything,
 * so it loads the recipes and the pantry and only runs
 * {@link RecipeMatcher} once both have arrived. Rendering after the first
 * response would match against an empty pantry and briefly show nothing, which
 * looks like a bug.
 *
 * <p>Two lists are rendered from the same pass: the strict suggestions, and the
 * optional Almost There list for recipes exactly one ingredient short. They live
 * under separate headings so nothing in the top list is ever anything other than
 * a recipe the user can make now.
 */
public class SuggestionsFragment extends Fragment implements RecipeAdapter.OnRecipeClick {

    private RecipeRepository recipeRepository;
    private PantryRepository pantryRepository;

    private RecipeAdapter suggestedAdapter;
    private RecipeAdapter almostAdapter;

    private SwipeRefreshLayout refreshLayout;
    private ProgressBar progress;
    private TextView emptySuggested;
    private TextView emptyAlmost;
    private View headingAlmost;
    private RecyclerView almostRecycler;

    private List<Recipe> recipes;
    private List<PantryItem> pantry;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recipeRepository = new RecipeRepository(requireContext());
        pantryRepository = new PantryRepository(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_suggestions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        refreshLayout = view.findViewById(R.id.swipe_refresh);
        progress = view.findViewById(R.id.progress);
        emptySuggested = view.findViewById(R.id.text_empty_suggested);
        emptyAlmost = view.findViewById(R.id.text_empty_almost);
        headingAlmost = view.findViewById(R.id.heading_almost);
        almostRecycler = view.findViewById(R.id.recycler_almost);

        suggestedAdapter = new RecipeAdapter(this);
        RecyclerView suggested = view.findViewById(R.id.recycler_suggested);
        suggested.setLayoutManager(new LinearLayoutManager(requireContext()));
        suggested.setAdapter(suggestedAdapter);
        suggested.addItemDecoration(new RuledPaperDecoration(requireContext()));

        almostAdapter = new RecipeAdapter(this);
        almostRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        almostRecycler.setAdapter(almostAdapter);
        almostRecycler.addItemDecoration(new RuledPaperDecoration(requireContext()));

        refreshLayout.setOnRefreshListener(() -> load(true));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recomputed on every return to this tab, so an ingredient added on the
        // pantry screen is reflected here immediately.
        load(false);
    }

    private void load(final boolean forceReload) {
        recipes = null;
        pantry = null;
        if (!refreshLayout.isRefreshing()) {
            progress.setVisibility(View.VISIBLE);
        }

        recipeRepository.readAll(forceReload, new ApiClient.Callback<List<Recipe>>() {
            @Override
            public void onSuccess(List<Recipe> result) {
                recipes = result;
                renderWhenReady();
            }

            @Override
            public void onFailure(String message) {
                fail(message);
            }
        });

        pantryRepository.readAll(new ApiClient.Callback<List<PantryItem>>() {
            @Override
            public void onSuccess(List<PantryItem> result) {
                pantry = result;
                renderWhenReady();
            }

            @Override
            public void onFailure(String message) {
                fail(message);
            }
        });
    }

    private void fail(String message) {
        if (!isAdded()) {
            return;
        }
        progress.setVisibility(View.GONE);
        refreshLayout.setRefreshing(false);
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.action_retry, v -> load(true))
                    .show();
        }
    }

    /** Runs the match only once both requests have returned. */
    private void renderWhenReady() {
        if (!isAdded() || recipes == null || pantry == null) {
            return;
        }

        progress.setVisibility(View.GONE);
        refreshLayout.setRefreshing(false);

        List<MatchResult> canCook = RecipeMatcher.suggested(recipes, pantry);
        suggestedAdapter.replaceAll(canCook);
        emptySuggested.setVisibility(canCook.isEmpty() ? View.VISIBLE : View.GONE);

        List<MatchResult> almost = RecipeMatcher.almostThere(recipes, pantry);

        // The second list is optional, and its heading goes with it.
        boolean showAlmost = new za.ac.richfield.smartpantry.util.Prefs(requireContext())
                .isShowAlmostThere();
        headingAlmost.setVisibility(showAlmost ? View.VISIBLE : View.GONE);
        if (showAlmost) {
            almostAdapter.replaceAll(almost);
            boolean bare = almost.isEmpty();
            emptyAlmost.setVisibility(bare ? View.VISIBLE : View.GONE);
            almostRecycler.setVisibility(bare ? View.GONE : View.VISIBLE);
        } else {
            almostAdapter.replaceAll(null);
            emptyAlmost.setVisibility(View.GONE);
            almostRecycler.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRecipeSelected(Recipe recipe) {
        Intent intent = new Intent(requireContext(), RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE, recipe);
        startActivity(intent);
    }
}
