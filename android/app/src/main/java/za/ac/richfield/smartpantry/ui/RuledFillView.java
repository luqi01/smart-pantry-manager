package za.ac.richfield.smartpantry.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import za.ac.richfield.smartpantry.R;

/**
 * Carries the ruling on to the foot of a page that is not a list.
 *
 * <p>{@link RuledPaperDecoration} rules the pantry all the way down because it
 * draws on a RecyclerView's canvas and can keep going past the last row. The
 * recipes page is a scrolling column rather than one list, so nothing was ruling
 * the space under its last entry, and the two screens read as different
 * surfaces: one a page, the other an app screen with a lot of white space.
 *
 * <p>Dropped in as the last child of a filled viewport with a weight of one, it
 * takes whatever height is left and rules it on the same pitch as everything
 * above. When the content is long enough to scroll it has no height and draws
 * nothing, which is correct - the page is full.
 */
public class RuledFillView extends View {

    private final Paint rulePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float pitch;
    private final float left;

    public RuledFillView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        rulePaint.setColor(ContextCompat.getColor(context, R.color.rule));
        rulePaint.setStrokeWidth(getResources().getDimension(R.dimen.rule_thin));
        pitch = getResources().getDimension(R.dimen.line_pitch);
        left = getResources().getDimension(R.dimen.margin_x)
                + getResources().getDimension(R.dimen.space_sm);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (float y = pitch; y < getHeight(); y += pitch) {
            canvas.drawLine(left, y, getWidth(), y, rulePaint);
        }
    }
}
