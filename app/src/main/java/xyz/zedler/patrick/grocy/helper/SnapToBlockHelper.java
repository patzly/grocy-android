package xyz.zedler.patrick.grocy.helper;

/*  The number of items in the RecyclerView should be a multiple of block size; otherwise, the
    extra item views will not be positioned on a block boundary when the end of the data is reached.
    Pad out with empty item views if needed.

    Updated to accommodate RTL layouts.
 */

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.Scroller;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

// Source: https://stackoverflow.com/a/47580753

public class SnapToBlockHelper extends SnapHelper {
  private RecyclerView mRecyclerView;

  // Total number of items in a block of view in the RecyclerView
  private int mBlocksize;

  // Maximum number of positions to move on a fling.
  private int mMaxPositionsToMove;

  // Width of a RecyclerView item if orientation is horizonal; height of the item if vertical
  private int mItemDimension;

  // Maxim blocks to move during most vigorous fling.
  private final int mMaxFlingBlocks;

  // Callback interface when blocks are snapped.
  private SnapBlockCallback mSnapBlockCallback;

  // When snapping, used to determine direction of snap.
  private int mPriorFirstPosition = RecyclerView.NO_POSITION;

  // Our private scroller
  private Scroller mScroller;

  // Horizontal/vertical layout helper
  private OrientationHelper mOrientationHelper;

  // LTR/RTL helper
  private LayoutDirectionHelper mLayoutDirectionHelper;

  // Borrowed from ViewPager.java
  private static final Interpolator sInterpolator = new Interpolator() {
    public float getInterpolation(float t) {
      // _o(t) = t * t * ((tension + 1) * t + tension)
      // o(t) = _o(t - 1) + 1
      t -= 1.0f;
      return t * t * t + 1.0f;
    }
  };

  public SnapToBlockHelper(int maxFlingBlocks) {
    super();
    mMaxFlingBlocks = maxFlingBlocks;
  }

  @Override
  public void attachToRecyclerView(@Nullable final RecyclerView recyclerView)
      throws IllegalStateException {

    if (recyclerView != null) {
      mRecyclerView = recyclerView;
      final LinearLayoutManager layoutManager =
          (LinearLayoutManager) recyclerView.getLayoutManager();
      if (layoutManager.canScrollHorizontally()) {
        mOrientationHelper = OrientationHelper.createHorizontalHelper(layoutManager);
        mLayoutDirectionHelper =
            new LayoutDirectionHelper(ViewCompat.getLayoutDirection(mRecyclerView));
      } else if (layoutManager.canScrollVertically()) {
        mOrientationHelper = OrientationHelper.createVerticalHelper(layoutManager);
        // RTL doesn't matter for vertical scrolling for this class.
        mLayoutDirectionHelper = new LayoutDirectionHelper(RecyclerView.LAYOUT_DIRECTION_LTR);
      } else {
        throw new IllegalStateException("RecyclerView must be scrollable");
      }
      mScroller = new Scroller(mRecyclerView.getContext(), sInterpolator);
      initItemDimensionIfNeeded(layoutManager);
    }
    super.attachToRecyclerView(recyclerView);
  }

  // Called when the target view is available and we need to know how much more
  // to scroll to get it lined up with the side of the RecyclerView.
  @NonNull
  @Override
  public int[] calculateDistanceToFinalSnap(@NonNull RecyclerView.LayoutManager layoutManager,
      @NonNull View targetView) {
    int[] out = new int[2];

    if (layoutManager.canScrollHorizontally()) {
      out[0] = mLayoutDirectionHelper.getScrollToAlignView(targetView);
    }
    if (layoutManager.canScrollVertically()) {
      out[1] = mLayoutDirectionHelper.getScrollToAlignView(targetView);
    }
    if (mSnapBlockCallback != null) {
      if (out[0] == 0 && out[1] == 0) {
        mSnapBlockCallback.onBlockSnapped(layoutManager.getPosition(targetView));
      } else {
        mSnapBlockCallback.onBlockSnap(layoutManager.getPosition(targetView));
      }
    }
    return out;
  }

  // We are flinging and need to know where we are heading.
  @Override
  public int findTargetSnapPosition(RecyclerView.LayoutManager layoutManager,
      int velocityX, int velocityY) {
    LinearLayoutManager lm = (LinearLayoutManager) layoutManager;

    initItemDimensionIfNeeded(layoutManager);
    mScroller.fling(0, 0, velocityX, velocityY, Integer.MIN_VALUE, Integer.MAX_VALUE,
        Integer.MIN_VALUE, Integer.MAX_VALUE);

    if (velocityX != 0) {
      return mLayoutDirectionHelper
          .getPositionsToMove(lm, mScroller.getFinalX(), mItemDimension);
    }

    if (velocityY != 0) {
      return mLayoutDirectionHelper
          .getPositionsToMove(lm, mScroller.getFinalY(), mItemDimension);
    }

    return RecyclerView.NO_POSITION;
  }

  // We have scrolled to the neighborhood where we will snap. Determine the snap position.
  @Override
  public View findSnapView(RecyclerView.LayoutManager layoutManager) {
    // Snap to a view that is either 1) toward the bottom of the data and therefore on screen,
    // or, 2) toward the top of the data and may be off-screen.
    int snapPos = calcTargetPosition((LinearLayoutManager) layoutManager);
    View snapView = (snapPos == RecyclerView.NO_POSITION)
        ? null : layoutManager.findViewByPosition(snapPos);

    if (snapView == null) {
      Log.d(TAG, "<<<<findSnapView is returning null!");
    }
    Log.d(TAG, "<<<<findSnapView snapos=" + snapPos);
    return snapView;
  }

  // Does the heavy lifting for findSnapView.
  private int calcTargetPosition(LinearLayoutManager layoutManager) {
    int snapPos;
    int firstVisiblePos = layoutManager.findFirstVisibleItemPosition();

    if (firstVisiblePos == RecyclerView.NO_POSITION) {
      return RecyclerView.NO_POSITION;
    }
    initItemDimensionIfNeeded(layoutManager);
    if (firstVisiblePos >= mPriorFirstPosition) {
      // Scrolling toward bottom of data
      int firstCompletePosition = layoutManager.findFirstCompletelyVisibleItemPosition();
      if (firstCompletePosition != RecyclerView.NO_POSITION
          && firstCompletePosition % mBlocksize == 0) {
        snapPos = firstCompletePosition;
      } else {
        snapPos = roundDownToBlockSize(firstVisiblePos + mBlocksize);
      }
    } else {
      // Scrolling toward top of data
      snapPos = roundDownToBlockSize(firstVisiblePos);
      // Check to see if target view exists. If it doesn't, force a smooth scroll.
      // SnapHelper only snaps to existing views and will not scroll to a non-existant one.
      // If limiting fling to single block, then the following is not needed since the
      // views are likely to be in the RecyclerView pool.
      if (layoutManager.findViewByPosition(snapPos) == null) {
        int[] toScroll = mLayoutDirectionHelper.calculateDistanceToScroll(layoutManager, snapPos);
        mRecyclerView.smoothScrollBy(toScroll[0], toScroll[1], sInterpolator);
      }
    }
    mPriorFirstPosition = firstVisiblePos;

    return snapPos;
  }

  private void initItemDimensionIfNeeded(final RecyclerView.LayoutManager layoutManager) {
    if (mItemDimension != 0) {
      return;
    }

    View child;
    if ((child = layoutManager.getChildAt(0)) == null) {
      return;
    }

    if (layoutManager.canScrollHorizontally()) {
      mItemDimension = child.getWidth();
      mBlocksize = getSpanCount(layoutManager) * (mRecyclerView.getWidth() / mItemDimension);
    } else if (layoutManager.canScrollVertically()) {
      mItemDimension = child.getHeight();
      mBlocksize = getSpanCount(layoutManager) * (mRecyclerView.getHeight() / mItemDimension);
    }
    mMaxPositionsToMove = mBlocksize * mMaxFlingBlocks;
  }

  private int getSpanCount(RecyclerView.LayoutManager layoutManager) {
    return (layoutManager instanceof GridLayoutManager)
        ? ((GridLayoutManager) layoutManager).getSpanCount()
        : 1;
  }

  private int roundDownToBlockSize(int trialPosition) {
    return trialPosition - trialPosition % mBlocksize;
  }

  private int roundUpToBlockSize(int trialPosition) {
    return roundDownToBlockSize(trialPosition + mBlocksize - 1);
  }

  @Nullable
  protected LinearSmoothScroller createScroller(RecyclerView.LayoutManager layoutManager) {
    if (!(layoutManager instanceof RecyclerView.SmoothScroller.ScrollVectorProvider)) {
      return null;
    }
    return new LinearSmoothScroller(mRecyclerView.getContext()) {
      @Override
      protected void onTargetFound(View targetView, RecyclerView.State state, Action action) {
        int[] snapDistances = calculateDistanceToFinalSnap(mRecyclerView.getLayoutManager(),
            targetView);
        final int dx = snapDistances[0];
        final int dy = snapDistances[1];
        final int time = calculateTimeForDeceleration(Math.max(Math.abs(dx), Math.abs(dy)));
        if (time > 0) {
          action.update(dx, dy, time, sInterpolator);
        }
      }

      @Override
      protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
        return MILLISECONDS_PER_INCH / displayMetrics.densityDpi;
      }
    };
  }

  public void setSnapBlockCallback(@Nullable SnapBlockCallback callback) {
    mSnapBlockCallback = callback;
  }

  /*
      Helper class that handles calculations for LTR and RTL layouts.
   */
  private class LayoutDirectionHelper {

    // Is the layout an RTL one?
    private final boolean mIsRTL;

    LayoutDirectionHelper(int direction) {
      mIsRTL = direction == View.LAYOUT_DIRECTION_RTL;
    }

    /*
        Calculate the amount of scroll needed to align the target view with the layout edge.
     */
    int getScrollToAlignView(View targetView) {
      return (mIsRTL)
          ? mOrientationHelper.getDecoratedEnd(targetView) - mRecyclerView.getWidth()
          : mOrientationHelper.getDecoratedStart(targetView);
    }

    /**
     * Calculate the distance to final snap position when the view corresponding to the snap
     * position is not currently available.
     *
     * @param layoutManager LinearLayoutManager or descendent class
     * @param targetPos     - Adapter position to snap to
     * @return int[2] {x-distance in pixels, y-distance in pixels}
     */
    int[] calculateDistanceToScroll(LinearLayoutManager layoutManager, int targetPos) {
      int[] out = new int[2];

      int firstVisiblePos;

      firstVisiblePos = layoutManager.findFirstVisibleItemPosition();
      if (layoutManager.canScrollHorizontally()) {
        if (targetPos <= firstVisiblePos) { // scrolling toward top of data
          if (mIsRTL) {
            View lastView = layoutManager.findViewByPosition(layoutManager.findLastVisibleItemPosition());
            out[0] = mOrientationHelper.getDecoratedEnd(lastView)
                + (firstVisiblePos - targetPos) * mItemDimension;
          } else {
            View firstView = layoutManager.findViewByPosition(firstVisiblePos);
            out[0] = mOrientationHelper.getDecoratedStart(firstView)
                - (firstVisiblePos - targetPos) * mItemDimension;
          }
        }
      }
      if (layoutManager.canScrollVertically()) {
        if (targetPos <= firstVisiblePos) { // scrolling toward top of data
          View firstView = layoutManager.findViewByPosition(firstVisiblePos);
          out[1] = firstView.getTop() - (firstVisiblePos - targetPos) * mItemDimension;
        }
      }

      return out;
    }

    /*
        Calculate the number of positions to move in the RecyclerView given a scroll amount
        and the size of the items to be scrolled. Return integral multiple of mBlockSize not
        equal to zero.
     */
    int getPositionsToMove(LinearLayoutManager llm, int scroll, int itemSize) {
      int positionsToMove;

      positionsToMove = roundUpToBlockSize(Math.abs(scroll) / itemSize);

      if (positionsToMove < mBlocksize) {
        // Must move at least one block
        positionsToMove = mBlocksize;
      } else if (positionsToMove > mMaxPositionsToMove) {
        // Clamp number of positions to move so we don't get wild flinging.
        positionsToMove = mMaxPositionsToMove;
      }

      if (scroll < 0) {
        positionsToMove *= -1;
      }
      if (mIsRTL) {
        positionsToMove *= -1;
      }

      if (mLayoutDirectionHelper.isDirectionToBottom(scroll < 0)) {
        // Scrolling toward the bottom of data.
        return roundDownToBlockSize(llm.findFirstVisibleItemPosition()) + positionsToMove;
      }
      // Scrolling toward the top of the data.
      return roundDownToBlockSize(llm.findLastVisibleItemPosition()) + positionsToMove;
    }

    boolean isDirectionToBottom(boolean velocityNegative) {
      //noinspection SimplifiableConditionalExpression
      return mIsRTL ? velocityNegative : !velocityNegative;
    }
  }

  public interface SnapBlockCallback {
    void onBlockSnap(int snapPosition);

    void onBlockSnapped(int snapPosition);

  }

  private static final float MILLISECONDS_PER_INCH = 100f;
  @SuppressWarnings("unused")
  private static final String TAG = "SnapToBlock";
}