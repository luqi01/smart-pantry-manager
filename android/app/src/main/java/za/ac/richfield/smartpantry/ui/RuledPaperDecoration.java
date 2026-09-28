package za.ac.richfield.smartpantry.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import za.ac.richfield.smartpantry.R;

/**
 * Draws the ruled page the pantry is written on.
 *
 * <p>An {@link RecyclerView.ItemDecoration} rather than a background, because a
 * background would not scroll. A drawable behind the list stays put while the
 * rows move over it, so within one flick the writing no longer sits on the
 * lines. Drawing here puts the ruling in the same canvas as the rows, so the
 * two move together.
 *
 * <p>It also lets the ruling carry on past the last row to the foot of the
 * screen, which is the thing that makes a page read as a page. A rule drawn by
 * each row would stop where the list stops and leave the bottom of the screen
 * blank, which is what a list looks like and not what a page looks like.
 *
 * <p>The margin rule is not drawn here. It runs the whole height of the page,
 * across the heading as well as the list, so it belongs to the activity rather
 * than to this one scrolling region.
 */
public class RuledPaperDecoration extends RecyclerView.ItemDecoration {

    private final Paint rulePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float pitch;
    private final float marginX;
    private final float inset;

    public RuledPaperDecoration(Context context) {
        rulePaint.setColor(ContextCompat.getColor(context, R.color.rule));
        rulePaint.setStrokeWidth(context.getResources().getDimension(R.dimen.rule_thin));
        pitch = context.getResources().getDimension(R.dimen.line_pitch);
        marginX = context.getResources().getDimension(R.dimen.margin_x);
        // The ruling starts a little right of the margin, as printed ruling does.
        inset = context.getResources().getDimension(R.dimen.space_sm);
    }

    @Override
    public void onDraw(@NonNull Canvas canvas, @NonNull RecyclerView parent,
                       @NonNull RecyclerView.State state) {
        final float left = marginX + inset;
        final float right = parent.getWidth();

        float lastBottom = parent.getPaddingTop();
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            lastBottom = child.getBottom() + child.getTranslationY();
            canvas.drawLine(left, lastBottom, right, lastBottom, rulePaint);
        }

        // Carry the ruling on below the last row, on the same pitch, so the page
        // is ruled all the way down whether anything is written there or not.
        for (float y = lastBottom + pitch; y < parent.getHeight(); y += pitch) {
            canvas.drawLine(left, y, right, y, rulePaint);
        }
    }
}
