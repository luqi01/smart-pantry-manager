package za.ac.richfield.smartpantry.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import za.ac.richfield.smartpantry.AddEditIngredientActivity;
import za.ac.richfield.smartpantry.MainActivity;
import za.ac.richfield.smartpantry.R;
import za.ac.richfield.smartpantry.adapter.PantryAdapter;
import za.ac.richfield.smartpantry.data.ApiClient;
import za.ac.richfield.smartpantry.data.PantryRepository;
import za.ac.richfield.smartpantry.data.RecipeRepository;
import za.ac.richfield.smartpantry.logic.RecipeMatcher;
import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.model.Recipe;

/**
 * The pantry list: everything the user currently has at home.
 *
 * <p>This is the Read half of CRUD. Create, Update and Delete happen through
 * {@link AddEditIngredientActivity}, which this screen launches with an Intent
 * and listens to for a result.
 *
 * <p>The list reloads in {@link #onResume} rather than {@link #onCreate}, which
 * matters for correctness. onCreate runs once; onResume runs every time the
 * screen comes back to the foreground, including on return from the edit screen
 * and after the app has been backgrounded. Loading in onCreate would leave the
 * user looking at data that had already changed.
 */
public class PantryFragment extends Fragment implements PantryAdapter.OnItemAction {

    private PantryRepository repository;
    private RecipeRepository recipeRepository;
    private PantryAdapter adapter;

    private RecyclerView recycler;
    private View emptyState;
    private ProgressBar progress;
    private View readyStrip;
    private TextView readyCount;

    private ActivityResultLauncher<Intent> editLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new PantryRepository(requireContext());
        recipeRepository = new RecipeRepository(requireContext());

        // Registered here rather than at the call site because the framework
        // requires registration before the fragment reaches STARTED.
        editLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                        load();
                        announce(result.getData());
                    }
                });
    }

    /**
     * Reports what the editor did, and offers a deletion back.
     *
     * <p>The editor finishes as soon as it has deleted, so there is nowhere on
     * that screen to put an undo. It hands the item back instead and the offer
     * is made here, which is also where the user is looking.
     */
    private void announce(@Nullable Intent data) {
        if (data == null || getView() == null) {
            return;
        }
        String message = data.getStringExtra(AddEditIngredientActivity.EXTRA_MESSAGE);
        if (message == null) {
            return;
        }
        Snackbar bar = note(message, Snackbar.LENGTH_LONG);
        PantryItem undoable = (PantryItem)
                data.getSerializableExtra(AddEditIngredientActivity.EXTRA_UNDO_ITEM);
        if (undoable != null) {
            bar.setAction(R.string.action_undo, v -> restore(undoable));
        }
        bar.show();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pantry, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recycler = view.findViewById(R.id.recycler_pantry);
        emptyState = view.findViewById(R.id.layout_empty);
        progress = view.findViewById(R.id.progress);

        adapter = new PantryAdapter(requireContext(), this);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);
        // The ruling belongs to the page rather than to any row, so it is drawn
        // across the whole list and carries on below the last line.
        recycler.addItemDecoration(new RuledPaperDecoration(requireContext()));

        MaterialButton add = view.findViewById(R.id.fab_add);
        add.setOnClickListener(v -> openEditor(null));

        readyStrip = view.findViewById(R.id.strip_ready);
        readyCount = view.findViewById(R.id.text_ready_count);
        readyStrip.setOnClickListener(v -> ((MainActivity) requireActivity()).showSuggestions());
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        progress.setVisibility(View.VISIBLE);
        repository.readAll(new ApiClient.Callback<List<PantryItem>>() {
            @Override
            public void onSuccess(List<PantryItem> items) {
                if (!isAdded()) {
                    // The user navigated away while the request was in flight.
                    // Touching views now would throw.
                    return;
                }
                progress.setVisibility(View.GONE);
                adapter.replaceAll(items);
                boolean empty = items == null || items.isEmpty();
                emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                // The list stays on screen when it is empty. It draws the
                // ruling, and hiding it left the user looking at blank paper
                // instead of an empty page waiting to be written on.
                summarise(items);
            }

            @Override
            public void onFailure(String message) {
                if (!isAdded()) {
                    return;
                }
                progress.setVisibility(View.GONE);
                showError(message);
            }
        });
    }

    /**
     * Writes the note at the foot of the list.
     *
     * <p>How many recipes the pantry unlocks is the question the app exists to
     * answer, so it is worth a second request to say it on this screen rather
     * than making the user change tab to find out. The recipes are cached after
     * the first fetch, so in practice this is one network call on a cold start
     * and none afterwards.
     */
    private void summarise(final List<PantryItem> items) {
        recipeRepository.readAll(false, new ApiClient.Callback<List<Recipe>>() {
            @Override
            public void onSuccess(List<Recipe> recipes) {
                if (!isAdded()) {
                    return;
                }
                int ready = RecipeMatcher.suggested(recipes, items).size();
                readyStrip.setVisibility(ready > 0 ? View.VISIBLE : View.GONE);
                readyCount.setText(getResources()
                        .getQuantityString(R.plurals.strip_ready, ready, ready));
            }

            @Override
            public void onFailure(String message) {
                if (!isAdded()) {
                    return;
                }
                // The pantry itself loaded. Saying nothing about the recipes is
                // better than claiming there are none.
                readyStrip.setVisibility(View.GONE);
            }
        });
    }

    private void showError(String message) {
        if (getView() == null) {
            return;
        }
        note(message, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.action_retry, v -> load())
                .show();
    }

    /**
     * A note anchored above the Add ingredient bar rather than laid over it.
     *
     * <p>A snackbar covers the foot of the screen, which is where this screen
     * keeps its one button. A tap meant for Undo that arrived just after the
     * note had gone would open the add form instead. Anchoring keeps the two
     * apart.
     */
    private Snackbar note(String message, int duration) {
        Snackbar bar = Snackbar.make(requireView(), message, duration);
        View anchor = requireView().findViewById(R.id.fab_add);
        if (anchor != null && anchor.getVisibility() == View.VISIBLE) {
            bar.setAnchorView(anchor);
        }
        return bar;
    }

    private void openEditor(@Nullable PantryItem item) {
        Intent intent = new Intent(requireContext(), AddEditIngredientActivity.class);
        if (item != null) {
            // The whole item travels as one Serializable extra, so the editor
            // can populate every field without a second network call.
            intent.putExtra(AddEditIngredientActivity.EXTRA_ITEM, item);
        }
        editLauncher.launch(intent);
    }

    @Override
    public void onEdit(PantryItem item) {
        openEditor(item);
    }

    /**
     * Deletes straight away and offers the deletion back.
     *
     * <p>This used to open a confirmation dialog, on the reasoning that
     * deletion could not be undone. Now that it can, the dialog was asking the
     * user to stop and think about something recoverable - which costs a tap on
     * every intentional deletion to protect against the rare accidental one.
     * An undo costs nothing unless it is needed, and it is the better answer to
     * the same problem: the user finds out what happened and can reverse it,
     * rather than being asked to predict it.
     */
    @Override
    public void onDelete(final PantryItem item) {
        progress.setVisibility(View.VISIBLE);
        repository.delete(item.getId(), new ApiClient.Callback<Object>() {
            @Override
            public void onSuccess(Object ignored) {
                if (!isAdded()) {
                    return;
                }
                load();
                if (getView() != null) {
                    note(getString(R.string.toast_deleted, item.getName()),
                            Snackbar.LENGTH_LONG)
                            .setAction(R.string.action_undo, v -> restore(item))
                            .show();
                }
            }

            @Override
            public void onFailure(String message) {
                if (!isAdded()) {
                    return;
                }
                progress.setVisibility(View.GONE);
                showError(message);
            }
        });
    }

    /**
     * Writes the item back exactly as it was.
     *
     * <p>It returns with a new id, because the row it had is gone. Nothing in
     * the app holds an id across this, and letting the client choose the primary
     * key would be a poor trade for making the number match.
     */
    private void restore(final PantryItem item) {
        progress.setVisibility(View.VISIBLE);
        repository.create(item, new ApiClient.Callback<PantryItem>() {
            @Override
            public void onSuccess(PantryItem restored) {
                if (!isAdded()) {
                    return;
                }
                load();
            }

            @Override
            public void onFailure(String message) {
                if (!isAdded()) {
                    return;
                }
                progress.setVisibility(View.GONE);
                showError(getString(R.string.error_undo));
            }
        });
    }
}
