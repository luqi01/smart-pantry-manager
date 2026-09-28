package za.ac.richfield.smartpantry.data;

import android.content.Context;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import za.ac.richfield.smartpantry.model.Recipe;

/**
 * Read access to the seeded recipe collection.
 *
 * <p>No create, update or delete: the twenty recipes are reference data loaded by
 * the seed script, not something the user edits. Leaving those methods off is a
 * deliberate statement about who owns the table.
 *
 * <p>The recipe list is cached in memory after the first load. It does not change
 * while the app is running, and the suggestions screen re-runs matching on every
 * pantry edit - refetching twenty recipes each time would waste the user's data
 * to receive an identical answer.
 */
public class RecipeRepository {

    private static final Type LIST_TYPE = new TypeToken<List<Recipe>>() {
    }.getType();
    private static final Type HEALTH_TYPE = new TypeToken<JsonObject>() {
    }.getType();

    private static List<Recipe> cache;

    private final ApiClient api;

    public RecipeRepository(Context context) {
        this.api = ApiClient.getInstance(context);
    }

    /**
     * @param forceReload true to bypass the cache, used by pull-to-refresh
     */
    public void readAll(boolean forceReload, final ApiClient.Callback<List<Recipe>> callback) {
        if (!forceReload && cache != null) {
            callback.onSuccess(new ArrayList<>(cache));
            return;
        }
        api.get("/api/recipes", LIST_TYPE, new ApiClient.Callback<List<Recipe>>() {
            @Override
            public void onSuccess(List<Recipe> result) {
                cache = result == null ? new ArrayList<Recipe>() : result;
                callback.onSuccess(new ArrayList<>(cache));
            }

            @Override
            public void onFailure(String message) {
                callback.onFailure(message);
            }
        });
    }

    public void readAll(ApiClient.Callback<List<Recipe>> callback) {
        readAll(false, callback);
    }

    /** Used by the Settings screen to confirm the API is reachable. */
    public void checkConnection(ApiClient.Callback<JsonObject> callback) {
        api.get("/api/health", HEALTH_TYPE, callback);
    }

    /** Dropped when the API URL changes, so stale data cannot survive the switch. */
    public static void clearCache() {
        cache = null;
    }
}
