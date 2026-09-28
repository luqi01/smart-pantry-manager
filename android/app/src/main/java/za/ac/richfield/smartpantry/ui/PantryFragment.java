package za.ac.richfield.smartpantry.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import za.ac.richfield.smartpantry.AddEditIngredientActivity;
import za.ac.richfield.smartpantry.R;
import za.ac.richfield.smartpantry.adapter.PantryAdapter;
import za.ac.richfield.smartpantry.data.ApiClient;
import za.ac.richfield.smartpantry.data.PantryRepository;
import za.ac.richfield.smartpantry.model.PantryItem;

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
    private PantryAdapter adapter;

    private RecyclerView recycler;
    private View emptyState;
    private ProgressBar progress;

    private ActivityResultLauncher<Intent> editLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new PantryRepository(requireContext());

        // Registered here rather than at the call site because the framework
        // requires registration before the fragment reaches STARTED.
        editLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                        String message = result.getData() == null ? null
                                : result.getData().getStringExtra(AddEditIngredientActivity.EXTRA_MESSAGE);
                        if (message != null && getView() != null) {
                            Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
                        }
                        load();
                    }
                });
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

        FloatingActionButton add = view.findViewById(R.id.fab_add);
        add.setOnClickListener(v -> openEditor(null));
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
                recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
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

    private void showError(String message) {
        if (getView() == null) {
            return;
        }
        Snackbar.make(getView(), message, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.action_retry, v -> load())
                .show();
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

    @Override
    public void onDelete(final PantryItem item) {
        // Deletion cannot be undone, so it is worth one tap to confirm.
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.confirm_delete_title, item.getName()))
                .setMessage(R.string.confirm_delete_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> performDelete(item))
                .show();
    }

    private void performDelete(final PantryItem item) {
        progress.setVisibility(View.VISIBLE);
        repository.delete(item.getId(), new ApiClient.Callback<Object>() {
            @Override
            public void onSuccess(Object ignored) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(requireContext(),
                        getString(R.string.toast_deleted, item.getName()),
                        Toast.LENGTH_SHORT).show();
                load();
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
}
