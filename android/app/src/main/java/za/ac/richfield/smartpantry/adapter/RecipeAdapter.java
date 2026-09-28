package za.ac.richfield.smartpantry.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import za.ac.richfield.smartpantry.R;
import za.ac.richfield.smartpantry.model.MatchResult;
import za.ac.richfield.smartpantry.model.Recipe;

/**
 * Binds match results to rows on the Suggested Recipes screen.
 *
 * <p>Takes {@link MatchResult} rather than {@link Recipe} because the row needs
 * the verdict as well as the recipe: a suggested row shows a green tick, an
 * "Almost There" row shows what is missing. Passing the recipe alone would mean
 * recomputing the match inside {@code onBindViewHolder}, which runs on every
 * scroll.
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
        private final TextView descriptionView;
        private final TextView metaView;
        private final TextView missingView;

        RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.text_recipe_name);
            descriptionView = itemView.findViewById(R.id.text_recipe_description);
            metaView = itemView.findViewById(R.id.text_recipe_meta);
            missingView = itemView.findViewById(R.id.text_recipe_missing);
        }

        void bind(MatchResult result) {
            final Recipe recipe = result.getRecipe();
            nameView.setText(recipe.getName());
            descriptionView.setText(recipe.getDescription());

            String serves = itemView.getContext().getString(R.string.label_serves, recipe.getServes());
            String minutes = itemView.getContext().getString(R.string.label_minutes, recipe.getPrepMinutes());
            metaView.setText(serves + "  ·  " + minutes + "  ·  "
                    + recipe.getIngredients().size() + " ingredients");

            if (result.getStatus() == MatchResult.Status.ALMOST) {
                missingView.setVisibility(View.VISIBLE);
                missingView.setText(itemView.getContext()
                        .getString(R.string.label_missing, result.getMissingSummary()));
            } else {
                missingView.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onRecipeSelected(recipe);
                }
            });
        }
    }
}
