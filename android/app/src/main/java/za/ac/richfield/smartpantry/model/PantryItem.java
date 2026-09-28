package za.ac.richfield.smartpantry.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

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
     * Quantity without a trailing ".0" on whole numbers, because "3 piece" reads
     * better in a list than "3.0 piece".
     */
    public String getDisplayQuantity() {
        if (quantity == Math.floor(quantity) && !Double.isInfinite(quantity)) {
            return String.valueOf((long) quantity);
        }
        return String.valueOf(quantity);
    }

    public String getDisplayAmount() {
        return getDisplayQuantity() + " " + (unit == null ? "" : unit);
    }
}
