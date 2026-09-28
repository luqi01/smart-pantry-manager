package za.ac.richfield.smartpantry.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
 *
 * <p>The row is one line of a ruled page, so the adapter fills a line rather
 * than a set of columns: the index that sits out in the margin, the name, the
 * quantity the dot leaders run out to, and - on the next line down, when there
 * is one - the note about when it needs using.
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
    private static final SimpleDateFormat SHORT = new SimpleDateFormat("d MMM", Locale.UK);

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

    /**
     * Whether an item falls inside the expiry warning window.
     *
     * <p>Static and public because the masthead counts expiring items and the
     * row highlights them, and the two should agree on what "soon" means. Two
     * copies of the same comparison would eventually drift apart.
     */
    public static boolean isExpiringSoon(@Nullable PantryItem item) {
        if (item == null) {
            return false;
        }
        Date date = parseIso(item.getExpiryDate());
        return date != null && daysUntil(date) <= Prefs.EXPIRY_WARNING_DAYS;
    }

    private static Date parseIso(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            // The server may return a full timestamp; only the date matters.
            return ISO.parse(value.length() > 10 ? value.substring(0, 10) : value);
        } catch (ParseException exception) {
            return null;
        }
    }

    private static long daysUntil(Date date) {
        Calendar midnight = Calendar.getInstance();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);
        return TimeUnit.MILLISECONDS.toDays(date.getTime() - midnight.getTimeInMillis());
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
        holder.bind(items.get(position), position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class PantryViewHolder extends RecyclerView.ViewHolder {

        private final TextView indexView;
        private final TextView nameView;
        private final TextView amountView;
        private final View expiryGroup;
        private final TextView expiryView;
        private final ImageButton deleteButton;

        PantryViewHolder(@NonNull View itemView) {
            super(itemView);
            indexView = itemView.findViewById(R.id.text_index);
            nameView = itemView.findViewById(R.id.text_name);
            amountView = itemView.findViewById(R.id.text_amount);
            expiryGroup = itemView.findViewById(R.id.group_expiry);
            expiryView = itemView.findViewById(R.id.text_expiry);
            deleteButton = itemView.findViewById(R.id.button_delete);
        }

        void bind(final PantryItem item, int position) {
            // Zero padded, so the margin keeps one width the whole way down.
            indexView.setText(String.format(Locale.UK, "%02d", position + 1));
            nameView.setText(item.getName());
            amountView.setText(item.getDisplayAmount());
            bindExpiry(item);

            // The line itself opens the editor. A separate pencil beside the
            // bin was a second way of doing what tapping the line already did.
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onEdit(item);
                }
            });

            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onDelete(item);
                }
            });
        }

        private void bindExpiry(PantryItem item) {
            // The API sends ISO dates. Showing "2026-10-02" to a user is
            // needlessly technical, so it is reformatted for display.
            Date date = parseIso(item.getExpiryDate());
            if (date == null) {
                expiryGroup.setVisibility(View.GONE);
                return;
            }

            expiryGroup.setVisibility(View.VISIBLE);
            Context context = expiryView.getContext();
            boolean warn = prefs.isExpiryAlertsEnabled() && isExpiringSoon(item);

            // Something about to go off is what a person would reach for a red
            // pen to mark, and they would write "use by" rather than a date on
            // its own. A date that is merely known stays in ordinary ink.
            expiryView.setText(warn
                    ? context.getString(R.string.label_use_by, SHORT.format(date))
                    : context.getString(R.string.label_expires, FRIENDLY.format(date)));
            expiryView.setTextColor(ContextCompat.getColor(context,
                    warn ? R.color.pen : R.color.ink_muted));
        }
    }
}
