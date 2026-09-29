package za.ac.richfield.smartpantry.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * One ingredient the user currently has at home.
 *
 * <p>The field names carry {@link SerializedName} annotations because the REST
 * API uses snake_case while Java convention is camelCase. Without them Gson
 * would silently leave {@code expiryDate} null on every item, which is the kind
 * of bug that only shows up once expiry warnings stop appearing.
 *
 * <p>Implements {@link Serializable} so a whole item can be passed to the
 * edit screen in a single Intent extra rather than four separate ones.
 */
public class PantryItem implements Serializable {

    private long id;
    private String name;
    private double quantity;
    private String unit;

    @SerializedName("expiry_date")
    private String expiryDate;

    @SerializedName("updated_at")
    private String updatedAt;

    public PantryItem() {
        // Gson needs a no-argument constructor to build instances by reflection.
    }

    public PantryItem(long id, String name, double quantity, String unit, String expiryDate) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.expiryDate = expiryDate;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Units that count things rather than measure them, and so take a plural.
     *
     * <p>Grams and millilitres do not: "250 gs" is not English. Splitting the
     * two is the only way to get both "12 eggs" and "250 g" out of one line of
     * formatting, and the list read "12 piece" until it was.
     */
    private static final Set<String> COUNTING_UNITS = new HashSet<>(Arrays.asList(
            "piece", "slice", "clove", "can", "packet", "cup"));

    /**
     * Quantity without a trailing ".0" on whole numbers, because "3 pieces"
     * reads better in a list than "3.0 pieces".
     */
    public String getDisplayQuantity() {
        if (quantity == Math.floor(quantity) && !Double.isInfinite(quantity)) {
            return String.valueOf((long) quantity);
        }
        return String.valueOf(quantity);
    }

    public String getDisplayAmount() {
        if (unit == null || unit.isEmpty()) {
            return getDisplayQuantity();
        }
        String shown = unit;
        if (COUNTING_UNITS.contains(unit.toLowerCase(Locale.UK)) && quantity != 1.0) {
            shown = unit + "s";
        }
        return getDisplayQuantity() + " " + shown;
    }
}
