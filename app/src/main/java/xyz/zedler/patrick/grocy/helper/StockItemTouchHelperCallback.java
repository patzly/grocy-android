package xyz.zedler.patrick.grocy.helper;

import android.graphics.Canvas;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import xyz.zedler.patrick.grocy.adapter.StockItemAdapter;

public class StockItemTouchHelperCallback extends ItemTouchHelperExtension.Callback {

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(0, ItemTouchHelper.END);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) { }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public void onChildDraw(
            Canvas c,
            RecyclerView recyclerView,
            RecyclerView.ViewHolder viewHolder,
            float dX,
            float dY,
            int actionState,
            boolean isCurrentlyActive
    ) {
        /*StockItemAdapter.ViewHolder holder = (StockItemAdapter.ViewHolder) viewHolder;
        holder.linearLayoutItemContainer.setTranslationX(dX);*/

        if (dY != 0 && dX == 0) super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        StockItemAdapter.ViewHolder holder = (StockItemAdapter.ViewHolder) viewHolder;
        if (viewHolder instanceof StockItemAdapter.ItemSwipeWithActionWidthViewHolder) {
            if (dX < -holder.linearLayoutItemBackground.getWidth()) {
                dX = -holder.linearLayoutItemBackground.getWidth();
            }
            holder.linearLayoutItemContainer.setTranslationX(dX);
        }

        /*if (dY != 0 && dX == 0) super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        StockItemAdapter.ViewHolder holder = (StockItemAdapter.ViewHolder) viewHolder;
        if (viewHolder instanceof StockItemAdapter.ItemSwipeWithActionWidthViewHolder) {
            if (dX < -holder.linearLayoutItemBackground.getWidth()) {
                dX = -holder.linearLayoutItemBackground.getWidth();
            }
            holder.linearLayoutItemContainer.setTranslationX(dX);
        }*/
    }
}
