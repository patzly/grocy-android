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

package xyz.zedler.patrick.grocy.fragment;

import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import info.appdev.charting.animation.Easing;
import info.appdev.charting.charts.PieChart;
import info.appdev.charting.data.PieData;
import info.appdev.charting.data.PieDataSet;
import info.appdev.charting.data.EntryFloat;
import info.appdev.charting.data.PieEntryFloat;
import info.appdev.charting.formatter.IValueFormatter;
import info.appdev.charting.interfaces.datasets.IPieDataSet;
import info.appdev.charting.renderer.PieChartRenderer;
import info.appdev.charting.utils.ViewPortHandler;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.SpendingItemAdapter;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentStockReportSpendingsBinding;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.viewmodel.StockReportSpendingsViewModel;
import xyz.zedler.patrick.grocy.viewmodel.StockReportSpendingsViewModel.SpendingItem;

public class StockReportSpendingsFragment extends BaseFragment {

  private static final String TAG = StockReportSpendingsFragment.class.getSimpleName();
  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  // Material 3 compatible palette — works on both light and dark backgrounds
  private static final int[] CHART_COLORS = {
      0xFF6750A4, // M3 primary purple
      0xFF4CAF50, // green
      0xFF2196F3, // blue
      0xFFF44336, // red
      0xFFFF9800, // orange
      0xFF00BCD4, // cyan
      0xFFE91E63, // pink
      0xFF9C27B0, // deep purple
      0xFF8BC34A, // light green
      0xFF03A9F4, // light blue
      0xFFFFEB3B, // yellow
      0xFF795548, // brown
  };

  /** Below this share of the total, a slice is too narrow for the full-size label. */
  private static final float SMALL_SLICE_PERCENT = 8f;
  private static final float VALUE_TEXT_SIZE = 11f;
  private static final float VALUE_TEXT_SIZE_SMALL = 8f;

  private MainActivity activity;
  private FragmentStockReportSpendingsBinding binding;
  private StockReportSpendingsViewModel viewModel;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentStockReportSpendingsBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    viewModel = new ViewModelProvider(this).get(StockReportSpendingsViewModel.class);

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setScroll(binding.scroll, binding.constraint);
    systemBarBehavior.setUp();
    activity.setSystemBarBehavior(systemBarBehavior);

    binding.toolbar.setNavigationOnClickListener(v -> activity.navUtil.navigateUp());

    binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));

    binding.swipe.setOnRefreshListener(this::loadWithCurrentSelection);

    viewModel.getIsLoadingLive().observe(getViewLifecycleOwner(),
        loading -> binding.swipe.setRefreshing(loading));

    viewModel.getSpendingItemsLive().observe(getViewLifecycleOwner(), this::updateChart);

    viewModel.getTotalSpendLive().observe(getViewLifecycleOwner(),
        total -> binding.textTotal.setText(total));

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(
            ((SnackbarMessage) event).getSnackbar(activity.binding.coordinatorMain));
      }
    });

    binding.chipGroupDate.setOnCheckedStateChangeListener(
        (group, checkedIds) -> loadWithCurrentSelection());

    binding.chipGroupBy.setOnCheckedStateChangeListener((group, checkedIds) -> {
      if (checkedIds.isEmpty()) return;
      int id = checkedIds.get(0);
      if (id == R.id.chip_group_product) {
        viewModel.getGroupByLive().setValue(StockReportSpendingsViewModel.GROUP_BY_PRODUCT);
      } else if (id == R.id.chip_group_product_group) {
        viewModel.getGroupByLive().setValue(StockReportSpendingsViewModel.GROUP_BY_PRODUCT_GROUP);
      } else if (id == R.id.chip_group_store) {
        viewModel.getGroupByLive().setValue(StockReportSpendingsViewModel.GROUP_BY_STORE);
      }
      viewModel.refreshWithGroupBy();
    });

    setupChart();

    if (savedInstanceState == null) {
      loadWithCurrentSelection();
    }

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(true);
    activity.getScrollBehavior().setUpScroll(binding.appBar, false, binding.scroll, false);
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(false, R.menu.menu_empty);
  }

  private void setupChart() {
    int colorOnSurface = ResUtil.getColor(requireContext(), R.attr.colorOnSurface);
    int colorSurface = ResUtil.getColor(requireContext(), R.attr.colorSurface);

    binding.pieChart.setUsePercentValues(true);
    binding.pieChart.getDescription().setEnabled(false);
    binding.pieChart.setDrawHole(true);
    binding.pieChart.setHoleColor(colorSurface);
    binding.pieChart.setHoleRadius(52f);
    binding.pieChart.setTransparentCircleRadius(57f);
    binding.pieChart.setTransparentCircleColor(colorSurface);
    binding.pieChart.setTransparentCircleAlpha(110);
    binding.pieChart.setDrawCenterText(false);
    binding.pieChart.setRotationEnabled(true);
    binding.pieChart.setHighlightPerTap(true);
    binding.pieChart.setDrawEntryLabels(false);

    // Legend
    info.appdev.charting.components.Legend legend = binding.pieChart.getLegend();
    legend.setEnabled(true);
    legend.setTextColor(colorOnSurface);
    legend.setTextSize(12f);
    legend.setForm(info.appdev.charting.components.Legend.LegendForm.CIRCLE);
    legend.setFormSize(10f);
    legend.setWordWrapEnabled(true);
    legend.setHorizontalAlignment(
        info.appdev.charting.components.Legend.LegendHorizontalAlignment.CENTER);
    legend.setVerticalAlignment(
        info.appdev.charting.components.Legend.LegendVerticalAlignment.BOTTOM);
    legend.setOrientation(
        info.appdev.charting.components.Legend.LegendOrientation.HORIZONTAL);
    legend.setDrawInside(false);

    binding.pieChart.setNoDataText(getString(R.string.error_empty_stock));
    binding.pieChart.setNoDataTextColor(colorOnSurface);
  }

  private void updateChart(List<SpendingItem> items) {
    if (items == null || items.isEmpty()) {
      binding.pieChart.setVisibility(View.GONE);
      binding.linearEmpty.setVisibility(View.VISIBLE);
      binding.recycler.setAdapter(null);
      return;
    }
    binding.pieChart.setVisibility(View.VISIBLE);
    binding.linearEmpty.setVisibility(View.GONE);

    // Show only top N slices, group the rest as "Other"
    int maxSlices = 8;
    List<SpendingItem> chartItems = items.size() > maxSlices
        ? items.subList(0, maxSlices) : items;
    double otherTotal = 0;
    if (items.size() > maxSlices) {
      for (int i = maxSlices; i < items.size(); i++) {
        otherTotal += items.get(i).total;
      }
    }

    List<PieEntryFloat> entries = new ArrayList<>();
    for (SpendingItem item : chartItems) {
      entries.add(new PieEntryFloat((float) item.total, item.name));
    }
    if (otherTotal > 0) {
      entries.add(new PieEntryFloat((float) otherTotal, getString(R.string.subtitle_others)));
    }

    PieDataSet dataSet = new PieDataSet(entries, "");
    dataSet.setColors(CHART_COLORS);
    dataSet.setSliceSpace(2f);
    dataSet.setSelectionShift(6f);
    dataSet.setValueLinePart1OffsetPercentage(80f);
    dataSet.setValueLinePart1Length(0.3f);
    dataSet.setValueLinePart2Length(0.4f);
    dataSet.setValueTextColor(Color.WHITE);
    dataSet.setValueTextSize(VALUE_TEXT_SIZE);

    PieData pieData = new PieData(dataSet);
    PercentSliceFormatter formatter = new PercentSliceFormatter();
    pieData.setValueFormatter(formatter);
    pieData.setValueTextSize(VALUE_TEXT_SIZE);
    binding.pieChart.setRenderer(new DualSizeValueRenderer(binding.pieChart, formatter));
    pieData.setValueTextColor(Color.WHITE);

    binding.pieChart.setData(pieData);
    binding.pieChart.animateY(600, Easing.INSTANCE.getEaseInOutQuad());
    binding.pieChart.invalidate();

    binding.recycler.setAdapter(
        new SpendingItemAdapter(items, amount -> viewModel.formatCurrency(amount))
    );
  }

  private void loadWithCurrentSelection() {
    int checkedId = binding.chipGroupDate.getCheckedChipId();
    String[] range = getDateRange(checkedId);
    viewModel.loadData(range[0], range[1]);
  }

  private String[] getDateRange(int chipId) {
    LocalDate today = LocalDate.now();
    if (chipId == R.id.chip_last_month) {
      YearMonth lastMonth = YearMonth.now().minusMonths(1);
      return new String[]{
          lastMonth.atDay(1).format(DATE_FMT),
          lastMonth.atEndOfMonth().format(DATE_FMT)
      };
    } else if (chipId == R.id.chip_this_year) {
      return new String[]{
          LocalDate.of(today.getYear(), 1, 1).format(DATE_FMT),
          LocalDate.of(today.getYear(), 12, 31).format(DATE_FMT)
      };
    } else {
      YearMonth thisMonth = YearMonth.now();
      return new String[]{
          thisMonth.atDay(1).format(DATE_FMT),
          thisMonth.atEndOfMonth().format(DATE_FMT)
      };
    }
  }

  @Override
  public void updateConnectivity(boolean online) {
    if (!online == viewModel.isOffline()) return;
    loadWithCurrentSelection();
  }

  /**
   * Formats the percentage of a slice, and can restrict itself to either the wide or the narrow
   * slices so that {@link DualSizeValueRenderer} can draw the two groups at different text sizes.
   */
  private static class PercentSliceFormatter implements IValueFormatter {

    static final int ALL = 0;
    static final int WIDE_ONLY = 1;
    static final int NARROW_ONLY = 2;

    private final DecimalFormat format =
        new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.getDefault()));
    private int mode = ALL;

    void setMode(int mode) {
      this.mode = mode;
    }

    @Override
    public String getFormattedValue(
        float value, EntryFloat entry, int dataSetIndex, ViewPortHandler viewPortHandler
    ) {
      // The chart uses percent values, so value already is the share of the total.
      boolean narrow = value < SMALL_SLICE_PERCENT;
      if ((mode == WIDE_ONLY && narrow) || (mode == NARROW_ONLY && !narrow)) {
        return "";
      }
      return format.format(value) + " %";
    }
  }

  /**
   * Draws the labels of narrow slices smaller so that they still fit into their segment.
   *
   * <p>The library only supports one value text size per data set, so this renders the values
   * twice: once for the wide slices at the regular size and once for the narrow ones at the
   * reduced size. The formatter blanks out whichever group is not part of the current pass, so
   * no label is drawn twice and the slice geometry never has to be recomputed here.
   */
  private static class DualSizeValueRenderer extends PieChartRenderer {

    private final PieChart chart;
    private final PercentSliceFormatter formatter;

    DualSizeValueRenderer(PieChart chart, PercentSliceFormatter formatter) {
      super(chart, chart.getAnimator(), chart.getViewPortHandler());
      this.chart = chart;
      this.formatter = formatter;
    }

    @Override
    public void drawValues(Canvas c) {
      PieData data = chart.getData();
      if (data == null) {
        return;
      }
      IPieDataSet dataSet = data.getDataSet();
      if (dataSet == null) {
        return;
      }
      drawPass(c, dataSet, PercentSliceFormatter.WIDE_ONLY, VALUE_TEXT_SIZE);
      drawPass(c, dataSet, PercentSliceFormatter.NARROW_ONLY, VALUE_TEXT_SIZE_SMALL);
      formatter.setMode(PercentSliceFormatter.ALL);
      dataSet.setValueTextSize(VALUE_TEXT_SIZE);
    }

    private void drawPass(Canvas c, IPieDataSet dataSet, int mode, float textSize) {
      formatter.setMode(mode);
      dataSet.setValueTextSize(textSize);
      super.drawValues(c);
    }
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
