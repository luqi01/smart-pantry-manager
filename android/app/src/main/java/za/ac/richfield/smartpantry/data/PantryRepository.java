package za.ac.richfield.smartpantry.data;

import android.content.Context;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import za.ac.richfield.smartpantry.logic.PantryDeduction;
import za.ac.richfield.smartpantry.model.PantryItem;

/**
 * The four CRUD operations on pantry items, expressed in the app's own terms.
 *
 * <p>The screens call {@code create}, {@code readAll}, {@code update} and
 * {@code delete}. They never build a URL or know that the data lives in
 * PostgreSQL. That separation is what makes the database swappable: replacing
 * the API with an on-device store would change this class and nothing above it.
 */
public class PantryRepository {

    private static final String PATH = "/api/pantry";
    private static final Type LIST_TYPE = new TypeToken<List<PantryItem>>() {
    }.getType();
    private static final Type ITEM_TYPE = new TypeToken<PantryItem>() {
    }.getType();
    private static final Type ANY_TYPE = new TypeToken<Object>() {
    }.getType();

    private final ApiClient api;

    public PantryRepository(Context context) {
        this.api = ApiClient.getInstance(context);
    }

    /** READ - every pantry item, sorted by name on the server. */
    public void readAll(ApiClient.Callback<List<PantryItem>> callback) {
        api.get(PATH, LIST_TYPE, callback);
    }

    /** CREATE - add a new ingredient to the pantry. */
    public void create(PantryItem item, ApiClient.Callback<PantryItem> callback) {
        api.post(PATH, toPayload(item), ITEM_TYPE, callback);
    }

    /** UPDATE - change an existing ingredient. */
    public void update(PantryItem item, ApiClient.Callback<PantryItem> callback) {
        api.put(PATH + "/" + item.getId(), toPayload(item), ITEM_TYPE, callback);
    }

    /** DELETE - remove an ingredient entirely. */
    public void delete(long id, ApiClient.Callback<Object> callback) {
        api.delete(PATH + "/" + id, ANY_TYPE, callback);
    }

    /**
     * Applies everything cooking a recipe takes out of the pantry.
     *
     * <p>Each change is a row to update or a row to remove, so this is several
     * requests rather than one. The API has no endpoint that takes a batch, and
     * adding one for a handful of writes would have meant a server change to
     * save a fraction of a second.
     *
     * <p>The requests go out together rather than one after another, because
     * chaining them would make the wait the sum of every round trip instead of
     * the longest one.
     */
    public void applyDeductions(final List<PantryDeduction.Change> changes,
                                final ApiClient.Callback<Object> callback) {
        runBatch(changes, false, callback);
    }

    /**
     * Puts back exactly what {@link #applyDeductions} took out.
     *
     * <p>Every change carries the item as it stood before, so undoing is a
     * write of a known value rather than an attempt to add the recipe's amounts
     * back on. That difference matters: adding back would compound any rounding
     * and would be wrong entirely if the user had edited the item in between.
     *
     * <p>An item that cooking removed is created again, so it comes back with a
     * new id. Nothing in the app holds onto an id across that boundary, and the
     * alternative - letting the client choose the primary key - would be a
     * worse trade for a cosmetic gain.
     */
    public void undoDeductions(final List<PantryDeduction.Change> changes,
                               final ApiClient.Callback<Object> callback) {
        runBatch(changes, true, callback);
    }

    /**
     * Fires every write at once and reports back when the last one lands.
     *
     * <p>The counter is atomic because the responses arrive on whichever thread
     * the client finishes them on, and two landing at the same moment could
     * otherwise both read the same remaining count and neither could call back.
     * The {@code failed} flag makes sure a batch reports at most one error even
     * when several requests fail.
     */
    private void runBatch(final List<PantryDeduction.Change> changes, final boolean undo,
                          final ApiClient.Callback<Object> callback) {
        if (changes == null || changes.isEmpty()) {
            callback.onSuccess(null);
            return;
        }

        final AtomicInteger outstanding = new AtomicInteger(changes.size());
        final AtomicBoolean failed = new AtomicBoolean(false);

        ApiClient.Callback<Object> step = new ApiClient.Callback<Object>() {
            @Override
            public void onSuccess(Object ignored) {
                if (outstanding.decrementAndGet() == 0 && !failed.get()) {
                    callback.onSuccess(null);
                }
            }

            @Override
            public void onFailure(String message) {
                if (failed.compareAndSet(false, true)) {
                    callback.onFailure(message);
                }
                outstanding.decrementAndGet();
            }
        };

        for (PantryDeduction.Change change : changes) {
            PantryItem before = change.getBefore();
            if (undo) {
                if (change.isExhausted()) {
                    create(before, asObjectCallback(step));
                } else {
                    update(before, asObjectCallback(step));
                }
            } else if (change.isExhausted()) {
                delete(before.getId(), step);
            } else {
                PantryItem reduced = new PantryItem(before.getId(), before.getName(),
                        change.getNewQuantity(), before.getUnit(), before.getExpiryDate());
                update(reduced, asObjectCallback(step));
            }
        }
    }

    /** Adapts a typed item callback onto the untyped batch counter. */
    private ApiClient.Callback<PantryItem> asObjectCallback(final ApiClient.Callback<Object> target) {
        return new ApiClient.Callback<PantryItem>() {
            @Override
            public void onSuccess(PantryItem item) {
                target.onSuccess(item);
            }

            @Override
            public void onFailure(String message) {
                target.onFailure(message);
            }
        };
    }

    /**
     * Builds the request body by hand rather than serialising the model.
     *
     * <p>Sending the model directly would include {@code id} and
     * {@code updated_at}, both of which the server owns. Naming the four fields
     * the client is allowed to set makes that boundary explicit.
     */
    private Map<String, Object> toPayload(PantryItem item) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", item.getName());
        payload.put("quantity", item.getQuantity());
        payload.put("unit", item.getUnit());
        payload.put("expiry_date", item.getExpiryDate());
        return payload;
    }
}
