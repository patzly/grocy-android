/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2023 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.adapter;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.ColorRoles;
import com.google.android.material.divider.MaterialDivider;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.databinding.RowRecipeEntryBinding;
import xyz.zedler.patrick.grocy.databinding.RowRecipeEntryGridBinding;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataRecipesExtraField;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.RecipeFulfillment;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;
import xyz.zedler.patrick.grocy.web.RequestHeaders;

public class RecipeEntryAdapter extends
    RecyclerView.Adapter<RecipeEntryAdapter.ViewHolder> {

  private final static String TAG = RecipeEntryAdapter.class.getSimpleName();
  private final static boolean DEBUG = false;

  private Context context;
  private final LayoutManager layoutManager;
  private final ArrayList<Recipe> recipes;
  private final ArrayList<RecipeFulfillment> recipeFulfillments;
  private final RecipesItemAdapterListener listener;
  private final GrocyApi grocyApi;
  private final LazyHeaders grocyAuthHeaders;
  private String sortMode;
  private boolean sortAscending;
  private String extraField;
  private final int maxDecimalPlacesAmount;
  private boolean containsPictures;

  public RecipeEntryAdapter(
      Context context,
      LayoutManager layoutManager,
      ArrayList<Recipe> recipes,
      ArrayList<RecipeFulfillment> recipeFulfillments,
      RecipesItemAdapterListener listener,
      String sortMode,
      boolean sortAscending,
      String extraField
  ) {
    this.context = context;
    this.layoutManager = layoutManager;
    this.recipes = new ArrayList<>(recipes);
    this.recipeFulfillments = new ArrayList<>(recipeFulfillments);
    this.listener = listener;
    this.grocyApi = new GrocyApi((Application) context.getApplicationContext());
    this.grocyAuthHeaders = RequestHeaders.getGlideGrocyAuthHeaders(context);
    this.sortMode = sortMode;
    this.sortAscending = sortAscending;
    this.extraField = extraField;
    maxDecimalPlacesAmount = PreferenceManager.getDefaultSharedPreferences(context).getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );

    containsPictures = false;
    for (Recipe recipe : recipes) {
      String pictureFileName = recipe.getPictureFileName();
      if (pictureFileName != null && !pictureFileName.isEmpty()) {
        containsPictures = true;
        break;
      }
    }
  }

  @Override
  public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
    super.onDetachedFromRecyclerView(recyclerView);
    this.context = null;
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    public ViewHolder(View view) {
      super(view);
    }
  }

  public static class RecipeViewHolder extends ViewHolder {

    private final RowRecipeEntryBinding binding;

    public RecipeViewHolder(RowRecipeEntryBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  public static class RecipeGridViewHolder extends ViewHolder {

    private final RowRecipeEntryGridBinding binding;
    StaggeredGridLayoutManager.LayoutParams layoutParams;

    public RecipeGridViewHolder(RowRecipeEntryGridBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
      layoutParams = (StaggeredGridLayoutManager.LayoutParams) itemView.getLayoutParams();
    }
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (layoutManager instanceof LinearLayoutManager) {
      return new RecipeViewHolder(RowRecipeEntryBinding.inflate(
          LayoutInflater.from(parent.getContext()),
          parent,
          false
      ));
    } else {
      return new RecipeGridViewHolder(RowRecipeEntryGridBinding.inflate(
          LayoutInflater.from(parent.getContext()),
          parent,
          false
      ));
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int positionDoNotUse) {

    int position = viewHolder.getAdapterPosition();

    Recipe recipe = recipes.get(position);
    RecipeFulfillment recipeFulfillment = RecipeFulfillment.getRecipeFulfillmentFromRecipeId(
        recipeFulfillments, recipe.getId()
    );

    LinearLayout container;
    TextView title;
    TextView dueScore;
    TextView fulfilled;
    ImageView imageFulfillment;
    TextView missing;
    TextView extraFieldTitle;
    TextView extraFieldSubtitle;
    LinearLayout extraFieldContainer;
    ImageView picture;
    MaterialCardView picturePlaceholder;
    MaterialDivider divider;

    if (viewHolder instanceof RecipeViewHolder) {
      container = ((RecipeViewHolder) viewHolder).binding.container;
      title = ((RecipeViewHolder) viewHolder).binding.title;
      dueScore = ((RecipeViewHolder) viewHolder).binding.dueScore;
      fulfilled = ((RecipeViewHolder) viewHolder).binding.fulfilled;
      imageFulfillment = ((RecipeViewHolder) viewHolder).binding.imageFulfillment;
      missing = ((RecipeViewHolder) viewHolder).binding.missing;
      extraFieldTitle = ((RecipeViewHolder) viewHolder).binding.extraField;
      extraFieldSubtitle = ((RecipeViewHolder) viewHolder).binding.extraFieldSubtitle;
      extraFieldContainer = ((RecipeViewHolder) viewHolder).binding.extraFieldContainer;
      picture = ((RecipeViewHolder) viewHolder).binding.picture;
      picturePlaceholder = ((RecipeViewHolder) viewHolder).binding.picturePlaceholder;
      divider = ((RecipeViewHolder) viewHolder).binding.divider;
    } else {
      container = ((RecipeGridViewHolder) viewHolder).binding.container;
      title = ((RecipeGridViewHolder) viewHolder).binding.title;
      dueScore = ((RecipeGridViewHolder) viewHolder).binding.dueScore;
      fulfilled = ((RecipeGridViewHolder) viewHolder).binding.fulfilled;
      imageFulfillment = ((RecipeGridViewHolder) viewHolder).binding.imageFulfillment;
      missing = ((RecipeGridViewHolder) viewHolder).binding.missing;
      extraFieldTitle = ((RecipeGridViewHolder) viewHolder).binding.extraField;
      extraFieldSubtitle = ((RecipeGridViewHolder) viewHolder).binding.extraFieldSubtitle;
      extraFieldContainer = ((RecipeGridViewHolder) viewHolder).binding.extraFieldContainer;
      picture = ((RecipeGridViewHolder) viewHolder).binding.picture;
      picturePlaceholder = ((RecipeGridViewHolder) viewHolder).binding.picturePlaceholder;
      divider = ((RecipeGridViewHolder) viewHolder).binding.divider;
    }

    // NAME

    title.setText(recipe.getName());

    if (recipeFulfillment != null) {
      ColorRoles colorGreen = ResUtil.getHarmonizedRoles(context, R.color.green);
      ColorRoles colorYellow = ResUtil.getHarmonizedRoles(context, R.color.yellow);
      ColorRoles colorRed = ResUtil.getHarmonizedRoles(context, R.color.red);

      // DUE SCORE
      int due_score = recipeFulfillment.getDueScore();
      @ColorInt int color;

      if (due_score == 0) {
        color = colorGreen.getAccent();
      }
      else if (due_score <= 10) {
        color = colorYellow.getAccent();
      }
      else {
        color = colorRed.getAccent();
      }
      dueScore.setTextColor(color);

      dueScore.setText(
              context.getString(
                      R.string.subtitle_recipe_due_score,
                      String.valueOf(recipeFulfillment.getDueScore())
              )
      );

      // REQUIREMENTS FULFILLED
      if (recipeFulfillment.isNeedFulfilled()) {
        fulfilled.setText(R.string.msg_recipes_enough_in_stock);
        imageFulfillment.setImageResource(R.drawable.ic_round_check_circle_outline);
        imageFulfillment.setImageTintList(
            ColorStateList.valueOf(colorGreen.getAccent())
        );
        missing.setVisibility(View.GONE);
      } else if (recipeFulfillment.isNeedFulfilledWithShoppingList()) {
        fulfilled.setText(R.string.msg_recipes_not_enough);
        imageFulfillment.setImageResource(R.drawable.ic_round_error_outline);
        imageFulfillment.setImageTintList(
            ColorStateList.valueOf(colorYellow.getAccent())
        );
        missing.setText(
            context.getResources()
                .getQuantityString(R.plurals.msg_recipes_ingredients_missing_but_on_shopping_list,
                    recipeFulfillment.getMissingProductsCount(),
                    recipeFulfillment.getMissingProductsCount())
        );
        missing.setVisibility(View.VISIBLE);
      } else {
        fulfilled.setText(R.string.msg_recipes_not_enough);
        imageFulfillment.setImageResource(R.drawable.ic_round_highlight_off);
        imageFulfillment.setImageTintList(
            ColorStateList.valueOf(colorRed.getAccent())
        );
        missing.setText(
            context.getResources()
                .getQuantityString(R.plurals.msg_recipes_ingredients_missing,
                    recipeFulfillment.getMissingProductsCount(),
                    recipeFulfillment.getMissingProductsCount())
        );
        missing.setVisibility(View.VISIBLE);
      }
    }

    String extraFieldText = null;
    String extraFieldSubtitleText = null;
    switch (extraField) {
      case FilterChipLiveDataRecipesExtraField.EXTRA_FIELD_CALORIES:
        if (recipeFulfillment != null) {
          extraFieldText = NumUtil.trimAmount(
              recipeFulfillment.getCalories(), maxDecimalPlacesAmount
          );
          extraFieldSubtitleText = "kcal";
        }
        break;
    }
    if (extraFieldText != null) {
      extraFieldTitle.setText(extraFieldText);
      extraFieldContainer.setVisibility(View.VISIBLE);
    } else {
      extraFieldContainer.setVisibility(View.GONE);
    }
    if (extraFieldSubtitleText != null) {
      extraFieldSubtitle.setText(extraFieldSubtitleText);
      extraFieldSubtitle.setVisibility(View.VISIBLE);
    } else {
      extraFieldSubtitle.setVisibility(View.GONE);
    }

    String pictureFileName = recipe.getPictureFileName();
    if (pictureFileName != null && !pictureFileName.isEmpty()) {
      picture.layout(0, 0, 0, 0);

      RequestBuilder<Drawable> requestBuilder = Glide.with(context).load(new GlideUrl(grocyApi.getRecipePicture(pictureFileName), grocyAuthHeaders));
      requestBuilder = requestBuilder
          .transform(new CenterCrop(), new RoundedCorners(UiUtil.dpToPx(context, 12)))
          .transition(DrawableTransitionOptions.withCrossFade());
      if (viewHolder instanceof RecipeGridViewHolder) {
        requestBuilder = requestBuilder.override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
      }
      requestBuilder.listener(new RequestListener<>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                Target<Drawable> target, boolean isFirstResource) {
              picture.setVisibility(View.GONE);
              picturePlaceholder.setVisibility(View.VISIBLE);
              return false;
            }
            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target,
                DataSource dataSource, boolean isFirstResource) {
              picture.setVisibility(View.VISIBLE);
              picturePlaceholder.setVisibility(View.GONE);
              return false;
            }
          }).into(picture);
    } else if (containsPictures && viewHolder instanceof RecipeViewHolder) {
      picture.setVisibility(View.GONE);
      picturePlaceholder.setVisibility(View.VISIBLE);
    } else {
      picture.setVisibility(View.GONE);
      picturePlaceholder.setVisibility(View.GONE);
    }

    // CONTAINER

    container.setOnClickListener(
        view -> listener.onItemRowClicked(recipe)
    );

    // DIVIDER

    if (layoutManager instanceof StaggeredGridLayoutManager
        && viewHolder instanceof RecipeGridViewHolder) {
      int spanCount = ((StaggeredGridLayoutManager) layoutManager).getSpanCount();
      int spanIndex = ((RecipeGridViewHolder) viewHolder).layoutParams.getSpanIndex();
      int itemCount = getItemCount();
      if ((position + spanCount - spanIndex) >= itemCount) {
        // last item in column
        divider.setVisibility(View.GONE);
      } else {
        divider.setVisibility(View.VISIBLE);
      }
    }
  }

  @Override
  public int getItemCount() {
    return recipes.size();
  }

  public interface RecipesItemAdapterListener {

    void onItemRowClicked(Recipe recipe);
  }

  public void updateData(
      ArrayList<Recipe> newList,
      ArrayList<RecipeFulfillment> newRecipeFulfillments,
      String sortMode,
      boolean sortAscending,
      String extraField
  ) {

    RecipeEntryAdapter.DiffCallback diffCallback = new RecipeEntryAdapter.DiffCallback(
        this.recipes,
        newList,
        this.recipeFulfillments,
        newRecipeFulfillments,
        this.sortMode,
        sortMode,
        this.sortAscending,
        sortAscending,
        this.extraField,
        extraField
    );
    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
    this.recipes.clear();
    this.recipes.addAll(newList);
    this.recipeFulfillments.clear();
    this.recipeFulfillments.addAll(newRecipeFulfillments);
    this.sortMode = sortMode;
    this.sortAscending = sortAscending;
    this.extraField = extraField;
    if (layoutManager instanceof LinearLayoutManager) {
      diffResult.dispatchUpdatesTo(
          new AdapterListUpdateCallback(this, (LinearLayoutManager) layoutManager)
      );
    } else {
      diffResult.dispatchUpdatesTo(this);
    }
  }

  static class DiffCallback extends DiffUtil.Callback {

    ArrayList<Recipe> oldItems;
    ArrayList<Recipe> newItems;
    ArrayList<RecipeFulfillment> oldRecipeFulfillments;
    ArrayList<RecipeFulfillment> newRecipeFulfillments;
    String sortModeOld;
    String sortModeNew;
    boolean sortAscendingOld;
    boolean sortAscendingNew;
    String extraFieldOld;
    String extraFieldNew;

    public DiffCallback(
        ArrayList<Recipe> oldItems,
        ArrayList<Recipe> newItems,
        ArrayList<RecipeFulfillment> oldRecipeFulfillments,
        ArrayList<RecipeFulfillment> newRecipeFulfillments,
        String sortModeOld,
        String sortModeNew,
        boolean sortAscendingOld,
        boolean sortAscendingNew,
        String extraFieldOld,
        String extraFieldNew
    ) {
      this.oldItems = oldItems;
      this.newItems = newItems;
      this.oldRecipeFulfillments = oldRecipeFulfillments;
      this.newRecipeFulfillments = newRecipeFulfillments;
      this.sortModeOld = sortModeOld;
      this.sortModeNew = sortModeNew;
      this.sortAscendingOld = sortAscendingOld;
      this.sortAscendingNew = sortAscendingNew;
      this.extraFieldOld = extraFieldOld;
      this.extraFieldNew = extraFieldNew;
    }

    @Override
    public int getOldListSize() {
      return oldItems.size();
    }

    @Override
    public int getNewListSize() {
      return newItems.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
      return compare(oldItemPosition, newItemPosition, false);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
      return compare(oldItemPosition, newItemPosition, true);
    }

    private boolean compare(int oldItemPos, int newItemPos, boolean compareContent) {
      Recipe newItem = newItems.get(newItemPos);
      Recipe oldItem = oldItems.get(oldItemPos);

      if (!sortModeOld.equals(sortModeNew)) {
        return false;
      }
      if (sortAscendingOld != sortAscendingNew) {
        return false;
      }
      if (!extraFieldOld.equals(extraFieldNew)) {
        return false;
      }

      RecipeFulfillment recipeFulfillmentOld = RecipeFulfillment.getRecipeFulfillmentFromRecipeId(oldRecipeFulfillments, oldItem.getId());
      RecipeFulfillment recipeFulfillmentNew = RecipeFulfillment.getRecipeFulfillmentFromRecipeId(newRecipeFulfillments, newItem.getId());
      if (recipeFulfillmentOld == null && recipeFulfillmentNew != null
          || recipeFulfillmentOld != null && recipeFulfillmentNew == null
          || recipeFulfillmentOld != null && !recipeFulfillmentOld.equals(recipeFulfillmentNew)) {
        return false;
      }

      if (!compareContent) {
        return newItem.getId() == oldItem.getId();
      }

      return newItem.equals(oldItem);
    }
  }

  /**
   * Custom ListUpdateCallback that prevents RecyclerView from scrolling down if top item is moved.
   */
  public static final class AdapterListUpdateCallback implements ListUpdateCallback {

    @NonNull
    private final RecipeEntryAdapter mAdapter;
    private final LinearLayoutManager linearLayoutManager;

    public AdapterListUpdateCallback(
        @NonNull RecipeEntryAdapter adapter,
        LinearLayoutManager linearLayoutManager
    ) {
      this.mAdapter = adapter;
      this.linearLayoutManager = linearLayoutManager;
    }

    @Override
    public void onInserted(int position, int count) {
      mAdapter.notifyItemRangeInserted(position, count);
    }

    @Override
    public void onRemoved(int position, int count) {
      mAdapter.notifyItemRangeRemoved(position, count);
    }

    @Override
    public void onMoved(int fromPosition, int toPosition) {
      // workaround for https://github.com/patzly/grocy-android/issues/439
      // figure out the position of the first visible item
      int firstPos = linearLayoutManager.findFirstCompletelyVisibleItemPosition();
      int offsetTop = 0;
      if(firstPos >= 0) {
        View firstView = linearLayoutManager.findViewByPosition(firstPos);
        if (firstView != null) {
          offsetTop = linearLayoutManager.getDecoratedTop(firstView)
              - linearLayoutManager.getTopDecorationHeight(firstView);
        }
      }

      mAdapter.notifyItemMoved(fromPosition, toPosition);

      // reapply the saved position
      if(firstPos >= 0) {
        linearLayoutManager.scrollToPositionWithOffset(firstPos, offsetTop);
      }
    }

    @Override
    public void onChanged(int position, int count, Object payload) {
      mAdapter.notifyItemRangeChanged(position, count, payload);
    }
  }
}
