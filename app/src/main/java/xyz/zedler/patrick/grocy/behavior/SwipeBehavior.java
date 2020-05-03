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
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.BitmapUtil;
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
    public int getSwipeDirs(
            @NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder
    ) {
        List<UnderlayButton> buffer = new ArrayList<>();
        instantiateUnderlayButton(viewHolder, buffer);
        if(buffer.isEmpty()) return 0;
        return super.getSwipeDirs(recyclerView, viewHolder);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        int pos = viewHolder.getAdapterPosition();

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

    public void recoverLatestSwipedItem() {
        if(swipedPos != -1 && recoverQueue.isEmpty() && recyclerView.getAdapter() != null) {
            recyclerView.getAdapter().notifyItemChanged(swipedPos);
            swipedPos = -1;
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
        float cornerRadius = UnitUtil.getDp(context, 8);
        float buttonWidth = dX / buttons.size() - cornerRadius / buttons.size();
        float buttonWidthMax = BUTTON_WIDTH - cornerRadius / buttons.size();

        Paint paint = new Paint();
        paint.setColor(ContextCompat.getColor(context, R.color.on_background_variant));

        // draw background

        canvas.drawRect(left, itemView.getTop(), dX, itemView.getBottom(), paint);

        // draw actions

        float buttonLeft = left;
        for (int i = 0; i < buttons.size(); i++) {
            float right = buttonLeft + buttonWidth;
            buttons.get(i).onDraw(
                    recyclerView.getContext(),
                    canvas,
                    new RectF(buttonLeft, itemView.getTop(), right, itemView.getBottom()),
                    buttonWidthMax,
                    i,
                    buttons.size(),
                    pos
            );
            buttonLeft = right;
        }

        // draw right edge (possibly over icons)

        float radius = Math.min(dX, cornerRadius);
        float[] corners = new float[]{
                radius, radius, // tl
                0, 0, // tr
                0, 0, // br
                radius, radius, // bl
        };
        paint.setColor(ContextCompat.getColor(context, R.color.background));
        final Path rightEdge = new Path();
        float leftOfRightEdge = left;
        if(dX > cornerRadius) {
            leftOfRightEdge = dX - cornerRadius;
        }
        rightEdge.addRoundRect(
                new RectF(
                        leftOfRightEdge,
                        itemView.getTop(),
                        dX + cornerRadius,
                        itemView.getBottom()
                ), corners,
                Path.Direction.CW
        );
        canvas.drawPath(rightEdge, paint);
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

        void onDraw(
                Context context,
                Canvas canvas,
                RectF rect,
                float width,
                int nr,
                int count,
                int pos
        ) {
            float animFriction = rect.width() / width;

            // push actions towards each other if there are two
            float offsetX = count == 2
                    ? nr == 0 ? UnitUtil.getDp(context, 4) : - UnitUtil.getDp(context, 4)
                    : 0;

            // DRAW ROUND BACKGROUND

            Paint paintBg = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintBg.setColor(ContextCompat.getColor(context, R.color.on_background));
            paintBg.setAlpha((int) (12 * (animFriction <= 1 ? animFriction : 1)));
            canvas.drawCircle(
                    rect.centerX() + offsetX,
                    rect.centerY(),
                    UnitUtil.getDp(context, 20 * (animFriction <= 1 ? animFriction : 1)),
                    paintBg
            );

            // DRAW ICON

            Paint paintIcon = new Paint();
            paintIcon.setColorFilter(
                    new PorterDuffColorFilter(
                            ContextCompat.getColor(context, R.color.icon),
                            PorterDuff.Mode.SRC_IN
                    )
            );
            paintIcon.setAlpha((int) (255 * animFriction));
            Bitmap icon = BitmapUtil.scale(
                    BitmapUtil.getFromDrawable(context, resId), animFriction
            );
            if(icon != null) {
                canvas.drawBitmap(
                        icon,
                        rect.centerX() - icon.getWidth() / 2f + offsetX,
                        rect.top + (rect.bottom - rect.top - icon.getHeight()) / 2,
                        paintIcon
                );
            }

            clickRegion = rect;
            this.pos = pos;
        }


    }
}
