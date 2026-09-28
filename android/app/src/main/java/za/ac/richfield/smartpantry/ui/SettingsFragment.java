package za.ac.richfield.smartpantry.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;

import za.ac.richfield.smartpantry.R;
import za.ac.richfield.smartpantry.data.ApiClient;
import za.ac.richfield.smartpantry.data.RecipeRepository;
import za.ac.richfield.smartpantry.util.Prefs;

/**
 * Settings, stored in SharedPreferences rather than the database.
 *
 * <p>Four things are adjustable: where the API lives, whether expiring items are
 * highlighted, whether the Almost There list appears, and the unit new
 * ingredients default to.
 *
 * <p>The API URL is here for a practical reason. The emulator reaches a server on
 * the host at 10.0.2.2, a physical phone needs the machine's address on the
 * network, and a deployed API needs neither. Hard-coding one of those would make
 * the other two require a rebuild.
 */
public class SettingsFragment extends Fragment {

    private static final String[] UNITS = {
            "piece", "slice", "clove", "can", "packet", "g", "kg", "ml", "l", "tsp", "tbsp", "cup"
    };

    private Prefs prefs;

    private TextInputEditText apiInput;
    private MaterialSwitch expirySwitch;
    private MaterialSwitch almostSwitch;
    private Spinner unitSpinner;
    private TextView status;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = new Prefs(requireContext());

        apiInput = view.findViewById(R.id.input_api_url);
        expirySwitch = view.findViewById(R.id.switch_expiry);
        almostSwitch = view.findViewById(R.id.switch_almost);
        unitSpinner = view.findViewById(R.id.spinner_unit);
        status = view.findViewById(R.id.text_status);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, UNITS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(adapter);

        loadCurrentValues();

        view.findViewById(R.id.button_save_settings).setOnClickListener(v -> save());
        ((MaterialButton) view.findViewById(R.id.button_test)).setOnClickListener(v -> testConnection());
    }

    private void loadCurrentValues() {
        apiInput.setText(prefs.getApiBaseUrl());
        expirySwitch.setChecked(prefs.isExpiryAlertsEnabled());
        almostSwitch.setChecked(prefs.isShowAlmostThere());

        String unit = prefs.getDefaultUnit();
        for (int i = 0; i < UNITS.length; i++) {
            if (UNITS[i].equalsIgnoreCase(unit)) {
                unitSpinner.setSelection(i);
                break;
            }
        }
    }

    private void save() {
        String url = apiInput.getText() == null ? "" : apiInput.getText().toString().trim();
        String previous = prefs.getApiBaseUrl();

        prefs.setApiBaseUrl(url.isEmpty() ? Prefs.DEFAULT_API_BASE : url);
        prefs.setExpiryAlertsEnabled(expirySwitch.isChecked());
        prefs.setShowAlmostThere(almostSwitch.isChecked());
        prefs.setDefaultUnit((String) unitSpinner.getSelectedItem());

        // Recipes cached from the old server would otherwise survive the switch
        // and be matched against the new server's pantry.
        if (!prefs.getApiBaseUrl().equals(previous)) {
            RecipeRepository.clearCache();
        }

        apiInput.setText(prefs.getApiBaseUrl());
        Toast.makeText(requireContext(), R.string.settings_saved, Toast.LENGTH_SHORT).show();
    }

    /**
     * Calls the health endpoint so a misconfigured URL is caught here, on a
     * screen that explains it, rather than as a failure on the pantry list.
     */
    private void testConnection() {
        save();
        status.setVisibility(View.VISIBLE);
        status.setText("Checking " + prefs.getApiBaseUrl() + " ...");
        status.setTextColor(ContextCompat.getColor(requireContext(), R.color.ink_muted));

        new RecipeRepository(requireContext()).checkConnection(new ApiClient.Callback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject result) {
                if (!isAdded()) {
                    return;
                }
                int recipes = result != null && result.has("recipes")
                        ? result.get("recipes").getAsInt() : 0;
                status.setText(getString(R.string.settings_connection_ok, recipes));
                status.setTextColor(ContextCompat.getColor(requireContext(), R.color.ink));
            }

            @Override
            public void onFailure(String message) {
                if (!isAdded()) {
                    return;
                }
                status.setText(message);
                status.setTextColor(ContextCompat.getColor(requireContext(), R.color.pen));
            }
        });
    }
}
