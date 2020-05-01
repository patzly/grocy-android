package xyz.zedler.patrick.grocy.behavior;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.adapter.StockItemAdapter;
import xyz.zedler.patrick.grocy.util.UnitUtil;

public abstract class SwipeBehavior extends ItemTouchHelper.SimpleCallback {

    private static final String TAG = SwipeBehavior.class.getSimpleName();

    private static final int BUTTON_WIDTH = 200;
    private Context context;
    private RecyclerView recyclerView;
    private List<UnderlayButton> buttons;
    private GestureDetector gestureDetector;
    private int swipedPos = -1;
    private float swipeThreshold = 0.5f;
    private Map<Integer, List<UnderlayButton>> buttonsBuffer;
    private Queue<Integer> recoverQueue;

    private View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent e) {
            view.performClick();

            if (swipedPos < 0) return false;
            Point point = new Point((int) e.getRawX(), (int) e.getRawY());

            RecyclerView.ViewHolder swipedViewHolder
                    = recyclerView.findViewHolderForAdapterPosition(swipedPos);
            if(swipedViewHolder == null) return false;
            View swipedItem = swipedViewHolder.itemView;
            Rect rect = new Rect();
            swipedItem.getGlobalVisibleRect(rect);

            if (e.getAction() == MotionEvent.ACTION_DOWN
                    || e.getAction() == MotionEvent.ACTION_UP
                    || e.getAction() == MotionEvent.ACTION_MOVE
            ) {
                if (rect.top < point.y && rect.bottom > point.y) {

                    gestureDetector.onTouchEvent(e);
                } else {

                    StockItemAdapter.ViewHolder viewHolderTop = (StockItemAdapter.ViewHolder)
                            recyclerView.findViewHolderForLayoutPosition(swipedPos - 1);
                    if(viewHolderTop != null) {
                        rect = new Rect();
                        viewHolderTop.itemView.getGlobalVisibleRect(rect);
                        if (rect.top < point.y && rect.bottom > point.y) {
                            viewHolderTop.resetBottomCornerRadiusNow();
                        } else {
                            viewHolderTop.resetBottomCornerRadius();
                        }
                        viewHolderTop.setBottomCornerPolicy(false);
                    }
                    StockItemAdapter.ViewHolder viewHolderBottom = (StockItemAdapter.ViewHolder)
                            recyclerView.findViewHolderForLayoutPosition(swipedPos + 1);
                    if(viewHolderBottom != null) {
                        rect = new Rect();
                        viewHolderBottom.itemView.getGlobalVisibleRect(rect);
                        if (rect.top < point.y && rect.bottom > point.y) {
                            viewHolderBottom.resetTopCornerRadiusNow();
                        } else {
                            viewHolderBottom.resetTopCornerRadius();
                        }
                        viewHolderBottom.setTopCornerPolicy(false);
                    }

                    recoverQueue.add(swipedPos);
                    swipedPos = -1;
                    recoverSwipedItem();
                }
            }
            return false;
        }
    };

    protected SwipeBehavior(Context context) {
        super(0, ItemTouchHelper.RIGHT);
        this.context = context;
        this.buttons = new ArrayList<>();
        GestureDetector.SimpleOnGestureListener gestureListener
                = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                for (UnderlayButton button : buttons) {
                    if (button.onClick(e.getX(), e.getY()))
                        break;
                }
                return true;
            }
        };
        this.gestureDetector = new GestureDetector(context, gestureListener);
        buttonsBuffer = new HashMap<>();
        recoverQueue = new LinkedList<Integer>() {
            @Override
            public boolean add(Integer o) {
                if (contains(o))
                    return false;
                else
                    return super.add(o);
            }
        };
    }

    @Override
    public boolean onMove(
            @NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder,
            @NonNull RecyclerView.ViewHolder target
    ) {
        return false;
    }

    @Override
    public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        List<UnderlayButton> buffer = new ArrayList<>();
        instantiateUnderlayButton(viewHolder, buffer);
        if(buffer.isEmpty()) return 0;
        return super.getSwipeDirs(recyclerView, viewHolder);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        int pos = viewHolder.getAdapterPosition();
        Log.i(TAG, "onSwiped: " + true);

        if (swipedPos != pos)
            recoverQueue.add(swipedPos);

        swipedPos = pos;

        if (buttonsBuffer.containsKey(swipedPos)) {
            buttons = buttonsBuffer.get(swipedPos);
        } else {
            buttons.clear();
        }

        buttonsBuffer.clear();
        assert buttons != null;
        swipeThreshold = 0.5f * buttons.size() * BUTTON_WIDTH;
        recoverSwipedItem();
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return swipeThreshold;
    }

    @Override
    public float getSwipeEscapeVelocity(float defaultValue) {
        return 0.1f * defaultValue;
    }

    @Override
    public float getSwipeVelocityThreshold(float defaultValue) {
        return 5.0f * defaultValue;
    }

    @Override
    public void onChildDraw(
            @NonNull Canvas c,
            @NonNull RecyclerView recyclerView,
            RecyclerView.ViewHolder viewHolder,
            float dX,
            float dY,
            int actionState,
            boolean isCurrentlyActive
    ) {
        int pos = viewHolder.getAdapterPosition();
        float translationX = dX;
        View itemView = viewHolder.itemView;

        if (pos < 0) {
            swipedPos = pos;
            return;
        }

        // remove elevation
        itemView.setElevation(0);
        itemView.setTranslationZ(0);

        int radiusMax = UnitUtil.getDp(context, 8);
        int radius = dX / 4 <= radiusMax ? (int) (dX / 4) : radiusMax;
        StockItemAdapter.ViewHolder stockItemViewHolderTop
                = (StockItemAdapter.ViewHolder) recyclerView.findViewHolderForLayoutPosition(
                pos + 1
        );
        if(stockItemViewHolderTop != null && stockItemViewHolderTop.getTopCornerPolicy()) {
            stockItemViewHolderTop.setTopCornerRadius(radius);
        }
        // TODO: Why 1000? Other values are working too - but not all
        if(stockItemViewHolderTop != null && !stockItemViewHolderTop.getTopCornerPolicy() && dX < 1000) {
            stockItemViewHolderTop.setTopCornerPolicy(true);
        }
        StockItemAdapter.ViewHolder stockItemViewHolderBottom
                = (StockItemAdapter.ViewHolder) recyclerView.findViewHolderForLayoutPosition(
                pos - 1
        );
        if(stockItemViewHolderBottom != null && stockItemViewHolderBottom.getBottomCornerPolicy()) {
            stockItemViewHolderBottom.setBottomCornerRadius(radius);
        }
        // TODO: Why 1000? Other values are working too - but not all
        if(stockItemViewHolderBottom != null && !stockItemViewHolderBottom.getBottomCornerPolicy() && dX < 1000) {
            stockItemViewHolderBottom.setBottomCornerPolicy(true);
        }

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            if (dX > 0) {
                List<UnderlayButton> buffer = new ArrayList<>();

                if (!buttonsBuffer.containsKey(pos)) {
                    instantiateUnderlayButton(viewHolder, buffer);
                    buttonsBuffer.put(pos, buffer);
                } else {
                    buffer = buttonsBuffer.get(pos);
                }

                assert buffer != null;
                translationX = dX * buffer.size() * BUTTON_WIDTH / itemView.getWidth();
                drawButtons(c, itemView, buffer, pos, translationX);
            } else if(swipedPos == pos && dX == 0) {
                swipedPos = -1;
            }
        }

        super.onChildDraw(
                c,
                recyclerView,
                viewHolder,
                translationX,
                dY,
                actionState,
                isCurrentlyActive
        );
    }

    private synchronized void recoverSwipedItem() {
        while (!recoverQueue.isEmpty()) {
            int pos = recoverQueue.poll();
            if (pos > -1) {
                if(recyclerView.getAdapter() != null) {
                    recyclerView.getAdapter().notifyItemChanged(pos);
                }
            }

        }
    }

    public void resetCornersAtPosition(int position) {
        StockItemAdapter.ViewHolder viewHolderTop = (StockItemAdapter.ViewHolder)
                recyclerView.findViewHolderForLayoutPosition(position - 1);
        if(viewHolderTop != null) {
            viewHolderTop.resetBottomCornerRadius();
            viewHolderTop.setBottomCornerPolicy(false);
        }
        StockItemAdapter.ViewHolder viewHolderBottom = (StockItemAdapter.ViewHolder)
                recyclerView.findViewHolderForLayoutPosition(position + 1);
        if(viewHolderBottom != null) {
            viewHolderBottom.resetTopCornerRadius();
            viewHolderBottom.setTopCornerPolicy(false);
        }
        if(swipedPos == position) {
            swipedPos = -1;
        }
    }

    public void recoverLatestSwipedItem() {
        if(swipedPos != -1 && recoverQueue.isEmpty() && recyclerView.getAdapter() != null) {
            recyclerView.getAdapter().notifyItemChanged(swipedPos);
            resetCornersAtPosition(swipedPos);
        }
    }

    private void drawButtons(
            Canvas canvas,
            View itemView,
            List<UnderlayButton> buttons,
            int pos,
            float dX
    ) {
        float left = itemView.getLeft();
        float dButtonWidth = dX / buttons.size();

        // draw background

        int radiusMax = UnitUtil.getDp(context, 8);
        int radius = dX / 2 <= radiusMax ? (int) (dX / 2) : radiusMax;

        float[] corners = new float[]{
                0, 0,        // Top left radius in px
                radius, radius,        // Top right radius in px
                radius, radius,          // Bottom right radius in px
                0, 0           // Bottom left radius in px
        };
        Paint paint = new Paint();
        paint.setColor(ContextCompat.getColor(context, R.color.secondary));
        final Path path = new Path();
        path.addRoundRect(new RectF(left, itemView.getTop(), dX, itemView.getBottom()), corners, Path.Direction.CW);
        canvas.drawPath(path, paint);

        for (UnderlayButton button : buttons) {
            float right = left + dButtonWidth;
            button.onDraw(
                    recyclerView.getContext(),
                    canvas,
                    new RectF(left, itemView.getTop(), right, itemView.getBottom()),
                    pos
            );
            left = right;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public void attachToRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        this.recyclerView.setOnTouchListener(onTouchListener);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(this);
        itemTouchHelper.attachToRecyclerView(this.recyclerView);
    }

    public abstract void instantiateUnderlayButton(
            RecyclerView.ViewHolder viewHolder,
            List<UnderlayButton> underlayButtons
    );

    public interface UnderlayButtonClickListener {
        void onClick(int position);
    }

    public static class UnderlayButton {
        private int resId;
        private int pos;
        private RectF clickRegion;
        private UnderlayButtonClickListener clickListener;

        public UnderlayButton(
                @DrawableRes int resId,
                UnderlayButtonClickListener clickListener
        ) {
            this.resId = resId;
            this.clickListener = clickListener;
        }

        boolean onClick(float x, float y) {
            if (clickRegion != null && clickRegion.contains(x, y)) {
                clickListener.onClick(pos);
                return true;
            }

            return false;
        }

        void onDraw(Context context, Canvas canvas, RectF rect, int pos) {
            Paint paint = new Paint();
            paint.setColorFilter(
                    new PorterDuffColorFilter(
                            ContextCompat.getColor(context, R.color.on_secondary),
                            PorterDuff.Mode.SRC_IN
                    )
            );
            paint.setAlpha((int) (255 * rect.width() / BUTTON_WIDTH));

            Drawable drawable = ResourcesCompat.getDrawable(
                    context.getResources(), resId, null
            );

            Bitmap icon = drawableToBitmap(drawable);
            icon = getScaledBitmap(icon, rect.width() / BUTTON_WIDTH);

            canvas.drawBitmap(
                    icon,
                    rect.centerX() - icon.getWidth() / 2f,
                    rect.top + (rect.bottom - rect.top - icon.getHeight()) / 2,
                    paint
            );

            clickRegion = rect;
            this.pos = pos;
        }

        private static Bitmap getScaledBitmap(Bitmap bm, float scale) {
            if(scale > 1) return bm;
            int width = bm.getWidth();
            int height = bm.getHeight();
            int scaleWidth = (int) (width * scale);
            if(scaleWidth <= 0) scaleWidth = 1;
            int scaleHeight = (int) (height * scale);
            if(scaleHeight <= 0) scaleHeight = 1;
            return Bitmap.createScaledBitmap(
                    bm, scaleWidth, scaleHeight, false
            );
        }

        private static Bitmap drawableToBitmap (Drawable drawable) {
            if (drawable instanceof BitmapDrawable) {
                return ((BitmapDrawable)drawable).getBitmap();
            }
            Bitmap bitmap = Bitmap.createBitmap(
                    drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(),
                    Bitmap.Config.ARGB_8888
            );
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        }
    }
}
