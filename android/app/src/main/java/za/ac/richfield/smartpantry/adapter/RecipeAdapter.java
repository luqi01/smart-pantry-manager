package za.ac.richfield.smartpantry.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import za.ac.richfield.smartpantry.R;
import za.ac.richfield.smartpantry.model.MatchResult;
import za.ac.richfield.smartpantry.model.Recipe;

/**
 * Binds match results to lines on the Suggested Recipes page.
 *
 * <p>Takes {@link MatchResult} rather than {@link Recipe} because the line needs
 * the verdict as well as the recipe: a suggested line reads "ready", a line in
 * the second list names what is still wanted. Passing the recipe alone would
 * mean recomputing the match inside {@code onBindViewHolder}, which runs on
 * every scroll.
 *
 * <p>Both lists use this one adapter, and the verdict decides what the second
 * line of the entry says. A recipe you can make is described by what it takes;
 * one you cannot is described by what it needs, in red, because that is the
 * thing you would write down before going to the shop.
 */
public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    public interface OnRecipeClick {
        void onRecipeSelected(Recipe recipe);
    }

    private final List<MatchResult> results = new ArrayList<>();
    private final OnRecipeClick listener;

    public RecipeAdapter(OnRecipeClick listener) {
        this.listener = listener;
    }

    public void replaceAll(List<MatchResult> replacement) {
        results.clear();
        if (replacement != null) {
            results.addAll(replacement);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        holder.bind(results.get(position));
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    class RecipeViewHolder extends RecyclerView.ViewHolder {

        private final TextView nameView;
        private final TextView statusStamp;
        private final TextView noteView;

        RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.text_recipe_name);
            statusStamp = itemView.findViewById(R.id.stamp_status);
            noteView = itemView.findViewById(R.id.text_recipe_note);
        }

        void bind(MatchResult result) {
            final Recipe recipe = result.getRecipe();
            nameView.setText(recipe.getName());

            boolean almost = result.getStatus() == MatchResult.Status.ALMOST;
            statusStamp.setText(almost ? R.string.stamp_short : R.string.stamp_ready);
            statusStamp.setTextColor(ContextCompat.getColor(itemView.getContext(),
                    almost ? R.color.pen : R.color.ink_faint));

            if (almost) {
                noteView.setText(itemView.getContext()
                        .getString(R.string.label_missing, result.getMissingSummary()));
            } else {
                String serves = itemView.getContext()
                        .getString(R.string.label_serves, recipe.getServes());
                String minutes = itemView.getContext()
                        .getString(R.string.label_minutes, recipe.getPrepMinutes());
                noteView.setText(serves + "  ·  " + minutes + "  ·  "
                        + recipe.getIngredients().size() + " ingredients");
            }
            noteView.setTextColor(ContextCompat.getColor(itemView.getContext(),
                    almost ? R.color.pen : R.color.ink_muted));

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onRecipeSelected(recipe);
                }
            });
        }
    }
}
