package za.ac.richfield.smartpantry.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import za.ac.richfield.smartpantry.R;
import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.util.Prefs;

/**
 * Binds pantry items to rows in the pantry list.
 *
 * <p>A RecyclerView needs three things from an adapter: how many rows there are,
 * how to create a row view, and how to fill one in. The ViewHolder pattern is
 * what makes that efficient - {@code findViewById} is expensive, so each row
 * looks its views up once when it is created and the RecyclerView then recycles
 * that holder as rows scroll off screen. Without it, scrolling a long pantry
 * would stutter.
 */
public class PantryAdapter extends RecyclerView.Adapter<PantryAdapter.PantryViewHolder> {

    /**
     * Lets the fragment decide what a tap means without the adapter knowing
     * anything about activities or navigation.
     */
    public interface OnItemAction {
        void onEdit(PantryItem item);

        void onDelete(PantryItem item);
    }

    private static final SimpleDateFormat ISO = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
    private static final SimpleDateFormat FRIENDLY = new SimpleDateFormat("d MMM yyyy", Locale.UK);

    private final List<PantryItem> items = new ArrayList<>();
    private final OnItemAction listener;
    private final Prefs prefs;

    public PantryAdapter(Context context, OnItemAction listener) {
        this.listener = listener;
        this.prefs = new Prefs(context);
    }

    public void replaceAll(List<PantryItem> replacement) {
        items.clear();
        if (replacement != null) {
            items.addAll(replacement);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PantryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pantry, parent, false);
        return new PantryViewHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull PantryViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class PantryViewHolder extends RecyclerView.ViewHolder {

        private final TextView nameView;
        private final TextView amountView;
        private final TextView expiryView;
        private final ImageButton editButton;
        private final ImageButton deleteButton;

        PantryViewHolder(@NonNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.text_name);
            amountView = itemView.findViewById(R.id.text_amount);
            expiryView = itemView.findViewById(R.id.text_expiry);
            editButton = itemView.findViewById(R.id.button_edit);
            deleteButton = itemView.findViewById(R.id.button_delete);
        }

        void bind(final PantryItem item) {
            nameView.setText(item.getName());
            amountView.setText(item.getDisplayAmount());
            bindExpiry(item);

            View.OnClickListener edit = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onEdit(item);
                }
            };
            // The whole row opens the editor as well as the pencil: a 48dp icon
            // is a small target, and the row is the obvious thing to tap.
            itemView.setOnClickListener(edit);
            editButton.setOnClickListener(edit);

            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onDelete(item);
                }
            });
        }

        private void bindExpiry(PantryItem item) {
            String expiry = item.getExpiryDate();
            if (expiry == null || expiry.isEmpty()) {
                expiryView.setVisibility(View.GONE);
                return;
            }

            // The API sends ISO dates. Showing "2026-10-02" to a user is
            // needlessly technical, so it is reformatted for display.
            Date date = parseIso(expiry);
            if (date == null) {
                expiryView.setVisibility(View.GONE);
                return;
            }

            expiryView.setVisibility(View.VISIBLE);
            long daysLeft = daysUntil(date);
            Context context = expiryView.getContext();

            if (prefs.isExpiryAlertsEnabled() && daysLeft <= Prefs.EXPIRY_WARNING_DAYS) {
                expiryView.setText(context.getString(R.string.label_expiring_soon,
                        FRIENDLY.format(date)));
                expiryView.setTextColor(ContextCompat.getColor(context, R.color.red_700));
            } else {
                expiryView.setText(context.getString(R.string.label_expires,
                        FRIENDLY.format(date)));
                expiryView.setTextColor(ContextCompat.getColor(context, R.color.grey_600));
            }
        }

        private Date parseIso(String value) {
            try {
                // The server may return a full timestamp; only the date matters.
                return ISO.parse(value.length() > 10 ? value.substring(0, 10) : value);
            } catch (ParseException exception) {
                return null;
            }
        }

        private long daysUntil(Date date) {
            Calendar midnight = Calendar.getInstance();
            midnight.set(Calendar.HOUR_OF_DAY, 0);
            midnight.set(Calendar.MINUTE, 0);
            midnight.set(Calendar.SECOND, 0);
            midnight.set(Calendar.MILLISECOND, 0);
            return TimeUnit.MILLISECONDS.toDays(date.getTime() - midnight.getTimeInMillis());
        }
    }
}
