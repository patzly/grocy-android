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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.HashMap;
import java.util.List;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.RowLanguageBinding;
import xyz.zedler.patrick.grocy.model.Language;
import xyz.zedler.patrick.grocy.util.LocaleUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.ViewHolder> {

  private final static String TAG = LanguageAdapter.class.getSimpleName();

  private final List<Language> languages;
  private final String selectedCode;
  private final LanguageAdapterListener listener;
  private final HashMap<String, Language> languageHashMap;

  public LanguageAdapter(
      List<Language> languages, String selectedCode, LanguageAdapterListener listener
  ) {
    this.languages = languages;
    this.selectedCode = selectedCode;
    this.listener = listener;
    this.languageHashMap = new HashMap<>();
    for (Language language : languages) {
      languageHashMap.put(language.getCode(), language);
    }
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    private final RowLanguageBinding binding;

    public ViewHolder(RowLanguageBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  @NonNull
  @Override
  public LanguageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new ViewHolder(
        RowLanguageBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false
        )
    );
  }

  @Override
  public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
    Context context = holder.binding.getRoot().getContext();
    holder.binding.linearLanguageContainer.setBackground(
        ViewUtil.getRippleBgListItemSurface(context)
    );

    if (position == 0) {
      holder.binding.textLanguageName.setText(R.string.setting_language_system);
      holder.binding.textLanguageTranslators.setText(R.string.setting_language_not_available);

      setSelected(holder, selectedCode == null);
      holder.binding.linearLanguageContainer.setOnClickListener(
          view -> listener.onItemRowClicked(null)
      );
      return;
    }

    Language language = languages.get(holder.getAbsoluteAdapterPosition() - 1);
    holder.binding.textLanguageName.setText(language.getName());
    holder.binding.textLanguageTranslators.setText(language.getTranslators());

    boolean isSelected = language.getCode().equals(selectedCode);
    if (selectedCode != null && !isSelected && !languageHashMap.containsKey(selectedCode)) {
      String lang = LocaleUtil.getLangFromLanguageCode(selectedCode);
      if (languageHashMap.containsKey(lang)) {
        isSelected = language.getCode().equals(lang);
      }
    }
    setSelected(holder, isSelected);

    // CONTAINER

    holder.binding.linearLanguageContainer.setOnClickListener(
        view -> listener.onItemRowClicked(language)
    );
  }

  private void setSelected(ViewHolder holder, boolean selected) {
    Context context = holder.binding.getRoot().getContext();
    int colorSelected = ResUtil.getColor(context, R.attr.colorOnSecondaryContainer);
    holder.binding.imageLanguageSelected.setColorFilter(colorSelected);
    holder.binding.imageLanguageSelected.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
    if (selected) {
      holder.binding.linearLanguageContainer.setBackground(ViewUtil.getBgListItemSelected(context));
    } else {
      holder.binding.linearLanguageContainer.setBackground(
          ViewUtil.getRippleBgListItemSurface(context)
      );
    }
    holder.binding.textLanguageName.setTextColor(
        selected ? colorSelected : ResUtil.getColor(context, R.attr.colorOnSurface)
    );
    holder.binding.textLanguageTranslators.setTextColor(
        selected ? colorSelected : ResUtil.getColor(context, R.attr.colorOnSurfaceVariant)
    );
    holder.binding.linearLanguageContainer.setOnClickListener(
        view -> listener.onItemRowClicked(null)
    );
  }

  @Override
  public int getItemCount() {
    return languages.size() + 1;
  }

  public interface LanguageAdapterListener {

    void onItemRowClicked(Language language);
  }
}
