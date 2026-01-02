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
 * Copyright (c) 2020-2024 by Patrick Zedler and Dominic Zedler
 * Copyright (c) 2024-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.adapter;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.load.model.LazyHeaders;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.databinding.RowMasterItemBinding;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.util.ObjectUtil;
import xyz.zedler.patrick.grocy.util.PictureUtil;
import xyz.zedler.patrick.grocy.web.RequestHeaders;

public class MasterObjectListAdapter extends
    RecyclerView.Adapter<MasterObjectListAdapter.ViewHolder> {

  private final static String TAG = MasterObjectListAdapter.class.getSimpleName();

  private final ArrayList<Object> objects;
  private final MasterObjectListAdapterListener listener;
  private final String entity;
  private final GrocyApi grocyApi;
  private final LazyHeaders grocyAuthHeaders;
  private boolean containsPictures;

  public MasterObjectListAdapter(
      Context context,
      String entity,
      MasterObjectListAdapterListener listener
  ) {
    this.objects = new ArrayList<>();
    this.listener = listener;
    this.entity = entity;
    this.grocyApi = new GrocyApi((Application) context.getApplicationContext());
    this.grocyAuthHeaders = RequestHeaders.getGlideGrocyAuthHeaders(context);
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    private final RowMasterItemBinding binding;

    public ViewHolder(RowMasterItemBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new ViewHolder(RowMasterItemBinding.inflate(
        LayoutInflater.from(parent.getContext()),
        parent,
        false
    ));
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
    Object object = objects.get(holder.getAdapterPosition());

    // NAME
    holder.binding.textMasterItemName.setText(ObjectUtil.getObjectName(object, entity));

    // PICTURE
    String pictureFileName = entity.equals(GrocyApi.ENTITY.PRODUCTS)
        ? ((Product) object).getPictureFileName()
        : null;
    if (pictureFileName != null && !pictureFileName.isEmpty()) {
      holder.binding.picture.layout(0, 0, 0, 0);

      PictureUtil.loadPicture(
          holder.binding.picture,
          null,
          holder.binding.picturePlaceholder,
          grocyApi.getProductPictureServeSmall(pictureFileName),
          grocyAuthHeaders,
          false
      );
    } else if (containsPictures) {
      holder.binding.picture.setVisibility(View.GONE);
      holder.binding.picturePlaceholder.setVisibility(View.VISIBLE);
    } else {
      holder.binding.picture.setVisibility(View.GONE);
      holder.binding.picturePlaceholder.setVisibility(View.GONE);
    }

    // CONTAINER
    holder.binding.linearMasterItemContainer.setOnClickListener(
        view -> listener.onItemRowClicked(object)
    );
  }

  public void updateData(ArrayList<Object> newObjects, Runnable onListFilled) {
    DiffCallback diffCallback = new DiffCallback(
        newObjects,
        this.objects,
        entity
    );

    containsPictures = false;
    for (Object object : newObjects) {
      if (!(object instanceof Product)) continue;
      String pictureFileName = ((Product) object).getPictureFileName();
      if (pictureFileName != null && !pictureFileName.isEmpty()) {
        containsPictures = true;
        break;
      }
    }

    if (onListFilled != null && !newObjects.isEmpty() && objects.isEmpty()) {
      onListFilled.run();
    }

    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
    this.objects.clear();
    this.objects.addAll(newObjects);
    diffResult.dispatchUpdatesTo(this);
  }

  static class DiffCallback extends DiffUtil.Callback {

    ArrayList<Object> oldItems;
    ArrayList<Object> newItems;
    String entity;

    public DiffCallback(
        ArrayList<Object> newItems,
        ArrayList<Object> oldItems,
        String entity
    ) {
      this.newItems = newItems;
      this.oldItems = oldItems;
      this.entity = entity;
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
      Object newItem = newItems.get(newItemPos);
      Object oldItem = oldItems.get(oldItemPos);
      return compareContent ? newItem.equals(oldItem)
          : ObjectUtil.getObjectId(newItem, entity)
              == ObjectUtil.getObjectId(oldItem, entity);
    }
  }

  @Override
  public int getItemCount() {
    return objects.size();
  }

  public interface MasterObjectListAdapterListener {

    void onItemRowClicked(Object object);
  }
}
