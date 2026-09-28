package za.ac.richfield.smartpantry.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Wrapper around {@link SharedPreferences} for the handful of settings the app
 * remembers between runs.
 *
 * <p>SharedPreferences rather than the database, because these are small
 * single-value settings belonging to this installation, and a network call to
 * read a checkbox would be absurd. The database holds the user data; this holds
 * how the user wants it shown.
 *
 * <p>Wrapping it in one class keeps the string keys in a single place. Scattering
 * {@code getBoolean("alerts", true)} through the activities invites a typo that
 * silently reads a different setting.
 */
public final class Prefs {

    private static final String FILE = "smart_pantry_prefs";

    private static final String KEY_API_BASE = "api_base_url";
    private static final String KEY_EXPIRY_ALERTS = "expiry_alerts";
    private static final String KEY_SHOW_ALMOST = "show_almost_there";
    private static final String KEY_DEFAULT_UNIT = "default_unit";

    /**
     * 10.0.2.2 is the address the Android emulator maps to the host machine's
     * loopback interface. Using "localhost" here would point the app at the
     * emulator itself, where nothing is listening - a confusing failure, so it is
     * worth stating plainly.
     */
    public static final String DEFAULT_API_BASE = "http://10.0.2.2:3000";

    public static final int EXPIRY_WARNING_DAYS = 3;

    private final SharedPreferences preferences;

    public Prefs(Context context) {
        this.preferences = context.getApplicationContext()
                .getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public String getApiBaseUrl() {
        String value = preferences.getString(KEY_API_BASE, DEFAULT_API_BASE);
        if (value == null || value.trim().isEmpty()) {
            return DEFAULT_API_BASE;
        }
        // A trailing slash would produce "…:3000//api/pantry", which some servers
        // reject outright.
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    public void setApiBaseUrl(String url) {
        preferences.edit().putString(KEY_API_BASE, url).apply();
    }

    public boolean isExpiryAlertsEnabled() {
        return preferences.getBoolean(KEY_EXPIRY_ALERTS, true);
    }

    public void setExpiryAlertsEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_EXPIRY_ALERTS, enabled).apply();
    }

    public boolean isShowAlmostThere() {
        return preferences.getBoolean(KEY_SHOW_ALMOST, true);
    }

    public void setShowAlmostThere(boolean enabled) {
        preferences.edit().putBoolean(KEY_SHOW_ALMOST, enabled).apply();
    }

    public String getDefaultUnit() {
        return preferences.getString(KEY_DEFAULT_UNIT, "piece");
    }

    public void setDefaultUnit(String unit) {
        preferences.edit().putString(KEY_DEFAULT_UNIT, unit).apply();
    }
}
