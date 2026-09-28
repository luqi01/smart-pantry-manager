package za.ac.richfield.smartpantry;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import za.ac.richfield.smartpantry.data.ApiClient;
import za.ac.richfield.smartpantry.data.PantryRepository;
import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.util.Prefs;

/**
 * Adds a new ingredient or edits an existing one.
 *
 * <p>One activity serves both jobs because the form is identical; the only
 * difference is whether an item arrived in the Intent. Splitting it into two
 * activities would duplicate the validation, and validation duplicated in two
 * places is validation that will eventually disagree with itself.
 *
 * <p>The result is returned with {@code setResult} so the pantry list knows to
 * reload and can show a confirmation.
 */
public class AddEditIngredientActivity extends AppCompatActivity {

    /** Serializable {@link PantryItem}; absent when adding a new ingredient. */
    public static final String EXTRA_ITEM = "za.ac.richfield.smartpantry.ITEM";

    /** Confirmation sentence handed back to the pantry list. */
    public static final String EXTRA_MESSAGE = "za.ac.richfield.smartpantry.MESSAGE";

    private static final String[] UNITS = {
            "piece", "slice", "clove", "can", "packet", "g", "kg", "ml", "l", "tsp", "tbsp", "cup"
    };

    private static final SimpleDateFormat ISO = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
    private static final double MAX_QUANTITY = 100000;

    private PantryRepository repository;
    private PantryItem editing;

    private TextInputLayout nameLayout;
    private TextInputLayout quantityLayout;
    private TextInputEditText nameInput;
    private TextInputEditText quantityInput;
    private TextInputEditText expiryInput;
    private Spinner unitSpinner;
    private MaterialButton saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_ingredient);

        repository = new PantryRepository(this);
        editing = (PantryItem) getIntent().getSerializableExtra(EXTRA_ITEM);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // The page carries its own heading; the bar repeating it is noise.
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // The heading sits in the page rather than the bar, so it can carry the
        // display face at a size a toolbar title could not.
        ((android.widget.TextView) findViewById(R.id.text_form_title)).setText(
                editing == null ? R.string.title_add_ingredient : R.string.title_edit_ingredient);

        bindViews();
        populateUnits();

        if (editing != null) {
            fillForm(editing);
        }

        saveButton.setOnClickListener(v -> save());
    }

    private void bindViews() {
        nameLayout = findViewById(R.id.layout_name);
        quantityLayout = findViewById(R.id.layout_quantity);
        nameInput = findViewById(R.id.input_name);
        quantityInput = findViewById(R.id.input_quantity);
        expiryInput = findViewById(R.id.input_expiry);
        unitSpinner = findViewById(R.id.spinner_unit);
        saveButton = findViewById(R.id.button_save);

        expiryInput.setOnClickListener(v -> showDatePicker());
        findViewById(R.id.button_clear_date).setOnClickListener(v -> expiryInput.setText(""));

        MaterialButton delete = findViewById(R.id.button_delete);
        if (editing != null) {
            delete.setVisibility(View.VISIBLE);
            delete.setOnClickListener(v -> confirmDelete());
        }
    }

    private void populateUnits() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, UNITS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(adapter);
        unitSpinner.setSelection(indexOfUnit(new Prefs(this).getDefaultUnit()));
    }

    private int indexOfUnit(String unit) {
        for (int i = 0; i < UNITS.length; i++) {
            if (UNITS[i].equalsIgnoreCase(unit)) {
                return i;
            }
        }
        return 0;
    }

    private void fillForm(PantryItem item) {
        nameInput.setText(item.getName());
        quantityInput.setText(item.getDisplayQuantity());
        unitSpinner.setSelection(indexOfUnit(item.getUnit()));
        if (item.getExpiryDate() != null && item.getExpiryDate().length() >= 10) {
            expiryInput.setText(item.getExpiryDate().substring(0, 10));
        }
    }

    private void showDatePicker() {
        Calendar now = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int day) {
                        Calendar chosen = Calendar.getInstance();
                        chosen.set(year, month, day);
                        expiryInput.setText(ISO.format(chosen.getTime()));
                    }
                },
                now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        // Food already past its date does not belong in a pantry, so the picker
        // will not offer yesterday.
        dialog.getDatePicker().setMinDate(now.getTimeInMillis() - 1000);
        dialog.show();
    }

    // ------------------------------------------------------------ validation

    /**
     * Checks the form and reports every problem it finds against the field that
     * caused it.
     *
     * <p>Errors are attached to the input rather than shown in one toast, because
     * a toast saying "invalid input" leaves the user hunting for which field is
     * wrong. Validation runs before any network call: rejecting bad input here is
     * instant, where a round trip is not.
     *
     * @return the item to save, or null when the form is not valid
     */
    private PantryItem validate() {
        nameLayout.setError(null);
        quantityLayout.setError(null);

        String name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
        String quantityText = quantityInput.getText() == null
                ? "" : quantityInput.getText().toString().trim();
        String expiry = expiryInput.getText() == null ? "" : expiryInput.getText().toString().trim();

        boolean valid = true;

        if (name.isEmpty()) {
            nameLayout.setError(getString(R.string.error_name_required));
            valid = false;
        } else if (name.length() > 80) {
            nameLayout.setError(getString(R.string.error_name_too_long));
            valid = false;
        } else if (!name.matches("[A-Za-z0-9 ,'()\\-/.]+")) {
            nameLayout.setError(getString(R.string.error_name_chars));
            valid = false;
        }

        double quantity = 0;
        if (quantityText.isEmpty()) {
            quantityLayout.setError(getString(R.string.error_quantity_required));
            valid = false;
        } else {
            try {
                quantity = Double.parseDouble(quantityText);
                if (quantity <= 0) {
                    quantityLayout.setError(getString(R.string.error_quantity_positive));
                    valid = false;
                } else if (quantity > MAX_QUANTITY) {
                    quantityLayout.setError(getString(R.string.error_quantity_large));
                    valid = false;
                }
            } catch (NumberFormatException exception) {
                // Reached when the field holds something like "2.5.1" or "two".
                quantityLayout.setError(getString(R.string.error_quantity_number));
                valid = false;
            }
        }

        if (!valid) {
            return null;
        }

        PantryItem item = new PantryItem(
                editing == null ? 0 : editing.getId(),
                name,
                quantity,
                (String) unitSpinner.getSelectedItem(),
                expiry.isEmpty() ? null : expiry);
        return item;
    }

    // ---------------------------------------------------------------- saving

    private void save() {
        final PantryItem item = validate();
        if (item == null) {
            return;
        }

        saveButton.setEnabled(false);   // stops a double tap creating two rows

        ApiClient.Callback<PantryItem> callback = new ApiClient.Callback<PantryItem>() {
            @Override
            public void onSuccess(PantryItem saved) {
                finishWith(getString(editing == null ? R.string.toast_added : R.string.toast_updated,
                        saved.getName()));
            }

            @Override
            public void onFailure(String message) {
                saveButton.setEnabled(true);
                Snackbar.make(saveButton, message, Snackbar.LENGTH_LONG).show();
            }
        };

        if (editing == null) {
            repository.create(item, callback);
        } else {
            repository.update(item, callback);
        }
    }

    private void confirmDelete() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.confirm_delete_title, editing.getName()))
                .setMessage(R.string.confirm_delete_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) ->
                        repository.delete(editing.getId(), new ApiClient.Callback<Object>() {
                            @Override
                            public void onSuccess(Object ignored) {
                                finishWith(getString(R.string.toast_deleted, editing.getName()));
                            }

                            @Override
                            public void onFailure(String message) {
                                Snackbar.make(saveButton, message, Snackbar.LENGTH_LONG).show();
                            }
                        }))
                .show();
    }

    private void finishWith(String message) {
        Intent result = new Intent();
        result.putExtra(EXTRA_MESSAGE, message);
        setResult(RESULT_OK, result);
        finish();
    }
}
