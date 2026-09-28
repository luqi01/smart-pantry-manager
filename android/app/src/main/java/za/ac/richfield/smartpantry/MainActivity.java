package za.ac.richfield.smartpantry;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import za.ac.richfield.smartpantry.ui.PantryFragment;
import za.ac.richfield.smartpantry.ui.SettingsFragment;
import za.ac.richfield.smartpantry.ui.SuggestionsFragment;

/**
 * The host activity. Holds the toolbar and the bottom navigation bar, and swaps
 * the three main screens in and out of a single fragment container.
 *
 * <p>Fragments rather than three separate activities for these screens, because
 * they share one navigation bar. Three activities would each have to host their
 * own copy and keep the selected item in step, and moving between them would
 * push entries onto the back stack that the user never asked for. The two
 * screens that genuinely are separate tasks - adding an ingredient and reading a
 * recipe - are activities, launched with explicit Intents.
 *
 * <p>Lifecycle note: the selected tab is saved in {@link #onSaveInstanceState}
 * and restored in {@link #onCreate}. Without that, rotating the device recreates
 * the activity and drops the user back on the pantry tab, because a new instance
 * has no memory of what came before.
 */
public class MainActivity extends AppCompatActivity {

    private static final String STATE_SELECTED_TAB = "selected_tab";

    private BottomNavigationView navigation;
    private int selectedTab = R.id.nav_pantry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        navigation = findViewById(R.id.bottom_navigation);
        navigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
                selectedTab = item.getItemId();
                showFragment(selectedTab);
                return true;
            }
        });

        if (savedInstanceState != null) {
            selectedTab = savedInstanceState.getInt(STATE_SELECTED_TAB, R.id.nav_pantry);
        }
        navigation.setSelectedItemId(selectedTab);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_TAB, selectedTab);
    }

    private void showFragment(int itemId) {
        Fragment fragment;
        int titleRes;

        if (itemId == R.id.nav_suggestions) {
            fragment = new SuggestionsFragment();
            titleRes = R.string.nav_suggestions;
        } else if (itemId == R.id.nav_settings) {
            fragment = new SettingsFragment();
            titleRes = R.string.nav_settings;
        } else {
            fragment = new PantryFragment();
            titleRes = R.string.nav_pantry;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(titleRes);
        }
    }

    /**
     * Sends the user to the recipes tab. Called from the pantry screen after an
     * edit, so the effect of a change on the suggestions is one tap away.
     */
    public void showSuggestions() {
        navigation.setSelectedItemId(R.id.nav_suggestions);
    }
}
