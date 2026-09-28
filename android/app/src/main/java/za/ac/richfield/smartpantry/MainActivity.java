package za.ac.richfield.smartpantry;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import za.ac.richfield.smartpantry.ui.PantryFragment;
import za.ac.richfield.smartpantry.ui.SettingsFragment;
import za.ac.richfield.smartpantry.ui.SuggestionsFragment;

/**
 * The host activity. Holds the masthead and the bottom navigation bar, and swaps
 * the three main screens in and out of a single fragment container.
 *
 * <p>The masthead lives here rather than in each fragment so that it does not
 * flicker out and back in on every tab change, and so the three screens cannot
 * drift apart in how their heading is set. The fragments own its contents: they
 * are the ones holding the data the counts are drawn from, so each calls
 * {@link #setMasthead} and {@link #setStats} once its load has returned.
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

        if (itemId == R.id.nav_suggestions) {
            fragment = new SuggestionsFragment();
            setMasthead(R.string.eyebrow_suggestions, R.string.nav_suggestions);
        } else if (itemId == R.id.nav_settings) {
            fragment = new SettingsFragment();
            setMasthead(R.string.eyebrow_settings, R.string.nav_settings);
        } else {
            fragment = new PantryFragment();
            setMasthead(R.string.stamp_in_the_house, R.string.nav_pantry);
        }

        // The counts belong to the screen being left behind. Clearing them here
        // means the new screen never shows the old screen's figures for the
        // moment before its own request returns.
        hideStats();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    /** Sets the small label above the screen name, and the name itself. */
    public void setMasthead(@StringRes int eyebrow, @StringRes int title) {
        ((TextView) findViewById(R.id.masthead_eyebrow)).setText(eyebrow);
        ((TextView) findViewById(R.id.masthead_title)).setText(title);
    }

    /**
     * Fills the three counts under the screen name. Called by the fragment once
     * its data has arrived, because the activity has none of its own.
     *
     * <p>Takes text rather than numbers so a screen can print a dash for a
     * count it could not obtain. Printing a zero there would claim something
     * the app does not know.
     */
    public void setStats(CharSequence one, @StringRes int oneLabel,
                         CharSequence two, @StringRes int twoLabel,
                         CharSequence three, @StringRes int threeLabel) {
        findViewById(R.id.masthead_stats).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.stat_one_value)).setText(one);
        ((TextView) findViewById(R.id.stat_one_label)).setText(oneLabel);
        ((TextView) findViewById(R.id.stat_two_value)).setText(two);
        ((TextView) findViewById(R.id.stat_two_label)).setText(twoLabel);
        ((TextView) findViewById(R.id.stat_three_value)).setText(three);
        ((TextView) findViewById(R.id.stat_three_label)).setText(threeLabel);
    }

    /** Used by Settings, which has nothing to count. */
    public void hideStats() {
        findViewById(R.id.masthead_stats).setVisibility(View.GONE);
    }

    /**
     * Sends the user to the recipes tab. Called from the pantry screen after an
     * edit, so the effect of a change on the suggestions is one tap away.
     */
    public void showSuggestions() {
        navigation.setSelectedItemId(R.id.nav_suggestions);
    }
}
