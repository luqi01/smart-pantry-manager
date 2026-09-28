package za.ac.richfield.smartpantry.data;

import android.content.Context;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
