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

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import com.android.volley.VolleyError;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterEquipmentBinding;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Equipment;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import androidx.core.content.FileProvider;
import androidx.core.text.HtmlCompat;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class MasterEquipmentFragment extends BaseFragment {

  private static final String TAG = MasterEquipmentFragment.class.getSimpleName();
  private static final String DIALOG_DELETE = "dialog_delete";

  private MainActivity activity;
  private Gson gson;
  private GrocyApi grocyApi;
  private DownloadHelper dlHelper;
  private FragmentMasterEquipmentBinding binding;

  private ArrayList<Equipment> equipmentList;
  private ArrayList<String> equipmentNames;
  private Equipment editEquipment;
  private AlertDialog dialogDelete;

  private boolean isRefresh;
  private boolean debug;
  private boolean pendingDeleteManual;
  private byte[] pendingManualBytes;
  private String pendingManualExtension;
  private Uri pendingManualUri;

  private ActivityResultLauncher<String> manualPickerLauncher;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentMasterEquipmentBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if (dialogDelete != null) {
      dialogDelete.dismiss();
    }
    binding = null;
    dlHelper.destroy();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    if (isHidden()) return;

    activity = (MainActivity) requireActivity();

    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    dlHelper = new DownloadHelper(activity, TAG);
    grocyApi = activity.getGrocyApi();
    gson = new Gson();

    equipmentList = new ArrayList<>();
    equipmentNames = new ArrayList<>();
    editEquipment = null;
    pendingDeleteManual = false;
    pendingManualBytes = null;
    pendingManualUri = null;

    manualPickerLauncher = registerForActivityResult(
        new ActivityResultContracts.GetContent(),
        uri -> {
          if (uri == null) return;
          pendingManualUri = uri;
          pendingManualExtension = resolveExtension(uri);
          readBytesAsync(uri);
        }
    );

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setContainer(binding.swipeMasterEquipment);
    systemBarBehavior.setScroll(binding.scrollMasterEquipment, binding.constraint);
    systemBarBehavior.setUp();
    activity.setSystemBarBehavior(systemBarBehavior);

    binding.toolbar.setNavigationOnClickListener(v -> activity.performOnBackPressed());
    binding.swipeMasterEquipment.setOnRefreshListener(this::refresh);

    binding.editTextMasterEquipmentName.setOnFocusChangeListener((v, hasFocus) -> {
      if (hasFocus) ViewUtil.startIcon(binding.imageMasterEquipmentName);
    });
    binding.editTextMasterEquipmentDescription.setOnFocusChangeListener((v, hasFocus) -> {
      if (hasFocus) ViewUtil.startIcon(binding.imageMasterEquipmentDescription);
    });

    binding.buttonPickManual.setOnClickListener(v -> {
      if (hasManual()) {
        openManual();
      } else {
        pickManual();
      }
    });
    binding.btnDeleteManual.setOnClickListener(v -> {
      pendingDeleteManual = true;
      pendingManualBytes = null;
      pendingManualUri = null;
      binding.textManualFilename.setText(R.string.subtitle_none_selected);
      binding.buttonDeleteManual.setVisibility(View.GONE);
    });
    binding.btnReplaceManual.setOnClickListener(v -> pickManual());

    MasterEquipmentFragmentArgs args = MasterEquipmentFragmentArgs.fromBundle(requireArguments());
    editEquipment = args.getEquipment();
    if (editEquipment != null && savedInstanceState == null) {
      fillWithEditReferences();
    } else if (savedInstanceState == null) {
      activity.showKeyboard(binding.editTextMasterEquipmentName);
    }

    if (savedInstanceState == null) {
      load();
    } else {
      restoreSavedInstanceState(savedInstanceState);
    }

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(true);
    activity.getScrollBehavior().setUpScroll(
        binding.appBar, false, binding.scrollMasterEquipment, true
    );
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(
        true,
        editEquipment != null ? R.menu.menu_master_item_edit : R.menu.menu_empty,
        getBottomMenuClickListener()
    );
    activity.updateFab(
        R.drawable.ic_round_backup,
        R.string.action_save,
        Constants.FAB.TAG.SAVE,
        (getArguments() == null
            || getArguments().getBoolean(Constants.ARGUMENT.ANIMATED, true))
            && savedInstanceState == null,
        this::saveEquipment
    );

    if (savedInstanceState != null && savedInstanceState.getBoolean(DIALOG_DELETE)) {
      new Handler(Looper.getMainLooper()).postDelayed(this::showDeleteConfirmationDialog, 1);
    }
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    if (isHidden()) return;
    boolean isShowing = dialogDelete != null && dialogDelete.isShowing();
    outState.putBoolean(DIALOG_DELETE, isShowing);
    outState.putParcelableArrayList("equipmentList", equipmentList);
    outState.putStringArrayList("equipmentNames", equipmentNames);
    outState.putParcelable("editEquipment", editEquipment);
    outState.putBoolean("isRefresh", isRefresh);
  }

  private void restoreSavedInstanceState(@NonNull Bundle savedInstanceState) {
    if (isHidden()) return;
    equipmentList = savedInstanceState.getParcelableArrayList("equipmentList");
    equipmentNames = savedInstanceState.getStringArrayList("equipmentNames");
    editEquipment = savedInstanceState.getParcelable("editEquipment");
    isRefresh = savedInstanceState.getBoolean("isRefresh");
    binding.swipeMasterEquipment.setRefreshing(false);
  }

  @Override
  public void onHiddenChanged(boolean hidden) {
    if (!hidden && getView() != null) onViewCreated(getView(), null);
  }

  private void load() {
    if (activity.isOnline()) download();
  }

  @SuppressLint("ShowToast")
  private void refresh() {
    isRefresh = true;
    if (activity.isOnline()) {
      download();
    } else {
      binding.swipeMasterEquipment.setRefreshing(false);
      activity.showSnackbar(
          activity.getSnackbar(R.string.msg_no_connection, false)
              .setAction(R.string.action_retry, v1 -> refresh())
      );
    }
  }

  private void download() {
    binding.swipeMasterEquipment.setRefreshing(true);
    dlHelper.get(
        grocyApi.getObjects(GrocyApi.ENTITY.EQUIPMENT),
        response -> {
          equipmentList = gson.fromJson(
              response,
              new TypeToken<ArrayList<Equipment>>() {}.getType()
          );
          equipmentNames = getEquipmentNames();
          binding.swipeMasterEquipment.setRefreshing(false);
          updateEditReferences();
          if (isRefresh && editEquipment != null) fillWithEditReferences();
        },
        error -> {
          binding.swipeMasterEquipment.setRefreshing(false);
          activity.showSnackbar(
              activity.getSnackbar(getErrorMessage(error), false)
                  .setAction(R.string.action_retry, v1 -> download())
          );
        }
    );
  }

  private void updateEditReferences() {
    if (editEquipment != null) {
      Equipment fresh = Equipment.getEquipmentFromId(equipmentList, editEquipment.getId());
      if (fresh != null) editEquipment = fresh;
    }
  }

  private ArrayList<String> getEquipmentNames() {
    ArrayList<String> names = new ArrayList<>();
    for (Equipment equipment : equipmentList) {
      if (editEquipment == null || equipment.getId() != editEquipment.getId()) {
        names.add(equipment.getName().trim());
      }
    }
    return names;
  }

  private void fillWithEditReferences() {
    clearInputFocusAndErrors();
    if (editEquipment == null) return;
    binding.editTextMasterEquipmentName.setText(editEquipment.getName());
    String rawDesc = editEquipment.getDescription();
    if (rawDesc != null && rawDesc.contains("<")) {
      rawDesc = HtmlCompat.fromHtml(rawDesc, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim();
    }
    binding.editTextMasterEquipmentDescription.setText(rawDesc);
    String manualFile = editEquipment.getInstructionManualFileName();
    if (manualFile != null && !manualFile.isBlank()) {
      binding.textManualFilename.setText(manualFile);
      binding.buttonDeleteManual.setVisibility(View.VISIBLE);
    }
  }

  private void clearInputFocusAndErrors() {
    activity.hideKeyboard();
    binding.textInputMasterEquipmentName.clearFocus();
    binding.textInputMasterEquipmentName.setErrorEnabled(false);
    binding.textInputMasterEquipmentDescription.clearFocus();
    binding.textInputMasterEquipmentDescription.setErrorEnabled(false);
  }

  public void saveEquipment() {
    if (isFormInvalid()) return;

    String name = String.valueOf(binding.editTextMasterEquipmentName.getText()).trim();
    Editable descEditable = binding.editTextMasterEquipmentDescription.getText();
    String description = descEditable != null ? descEditable.toString().trim() : "";

    if (pendingManualBytes != null) {
      String filename = UUID.randomUUID().toString().replace("-", "") + pendingManualExtension;
      String replaced = editEquipment != null
          ? editEquipment.getInstructionManualFileName() : null;
      dlHelper.putFile(
          grocyApi.getEquipmentManual(filename),
          pendingManualBytes,
          () -> persistEquipment(name, description, filename, () -> deleteManualFile(replaced)),
          error -> showErrorMessage(error)
      );
    } else {
      String manualFilename = null;
      if (editEquipment != null) {
        manualFilename = pendingDeleteManual ? null : editEquipment.getInstructionManualFileName();
      }
      String removed = pendingDeleteManual && editEquipment != null
          ? editEquipment.getInstructionManualFileName() : null;
      persistEquipment(name, description, manualFilename, () -> deleteManualFile(removed));
    }
  }

  /**
   * Deletes a stored manual on the server. Called only once the equipment object no longer
   * references it, so a failure here leaves an orphaned file rather than a broken reference.
   */
  private void deleteManualFile(@Nullable String filename) {
    if (filename == null || filename.isBlank()) {
      return;
    }
    dlHelper.delete(
        grocyApi.getEquipmentManual(filename),
        response -> {},
        error -> Log.w(TAG, "deleteManualFile: could not delete " + filename + ": " + error)
    );
  }

  private void persistEquipment(
      String name, String description, String manualFilename, Runnable onPersisted
  ) {
    JSONObject json = new JSONObject();
    try {
      json.put("name", name);
      json.put("description", description);
      if (manualFilename != null) {
        json.put("instruction_manual_file_name", manualFilename);
      } else {
        json.put("instruction_manual_file_name", JSONObject.NULL);
      }
    } catch (JSONException e) {
      if (debug) Log.e(TAG, "persistEquipment: " + e);
    }

    if (editEquipment != null) {
      dlHelper.put(
          grocyApi.getObject(GrocyApi.ENTITY.EQUIPMENT, editEquipment.getId()),
          json,
          response -> {
            onPersisted.run();
            activity.navUtil.navigateUp();
          },
          error -> showErrorMessage(error)
      );
    } else {
      dlHelper.post(
          grocyApi.getObjects(GrocyApi.ENTITY.EQUIPMENT),
          json,
          response -> {
            onPersisted.run();
            activity.navUtil.navigateUp();
          },
          error -> showErrorMessage(error)
      );
    }
  }

  private boolean isFormInvalid() {
    clearInputFocusAndErrors();
    String name = String.valueOf(binding.editTextMasterEquipmentName.getText()).trim();
    if (name.isEmpty()) {
      binding.textInputMasterEquipmentName.setError(activity.getString(R.string.error_empty));
      return true;
    }
    if (!equipmentNames.isEmpty() && equipmentNames.contains(name)) {
      binding.textInputMasterEquipmentName.setError(activity.getString(R.string.error_duplicate));
      return true;
    }
    return false;
  }

  private void readBytesAsync(Uri uri) {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    executor.execute(() -> {
      byte[] bytes = readBytes(uri);
      new Handler(Looper.getMainLooper()).post(() -> {
        if (binding == null) return;
        if (bytes != null) {
          pendingManualBytes = bytes;
          pendingDeleteManual = false;
          String display = uri.getLastPathSegment();
          binding.textManualFilename.setText(display != null ? display : getString(R.string.property_instruction_manual));
          binding.buttonDeleteManual.setVisibility(View.VISIBLE);
        } else {
          activity.showSnackbar(R.string.error_undefined, false);
        }
        executor.shutdown();
      });
    });
  }

  @Nullable
  private String resolveExtension(Uri uri) {
    String mime = requireContext().getContentResolver().getType(uri);
    String fromMime = mime != null
        ? MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) : null;
    if (fromMime != null && !fromMime.isEmpty()) {
      return "." + fromMime;
    }
    String path = uri.getLastPathSegment();
    if (path != null) {
      int dot = path.lastIndexOf('.');
      if (dot > -1 && dot < path.length() - 1) {
        return path.substring(dot).toLowerCase(Locale.ROOT);
      }
    }
    return ".bin";
  }

  private boolean hasManual() {
    if (pendingManualUri != null) {
      return true;
    }
    if (pendingDeleteManual || editEquipment == null) {
      return false;
    }
    String stored = editEquipment.getInstructionManualFileName();
    return stored != null && !stored.isBlank();
  }

  private void pickManual() {
    manualPickerLauncher.launch("*/*");
  }

  private void openManual() {
    if (pendingManualUri != null) {
      String mime = requireContext().getContentResolver().getType(pendingManualUri);
      launchViewer(pendingManualUri, mime != null ? mime : "*/*");
      return;
    }
    if (editEquipment == null) return;
    String filename = editEquipment.getInstructionManualFileName();
    if (filename == null || filename.isBlank()) return;

    dlHelper.getFile(
        grocyApi.getEquipmentManual(filename),
        bytes -> {
          if (binding == null || bytes == null) return;
          File cached = writeToCache(filename, bytes);
          if (cached == null) {
            activity.showSnackbar(R.string.error_undefined, false);
            return;
          }
          Uri uri = FileProvider.getUriForFile(
              requireContext(), requireContext().getPackageName() + ".fileprovider", cached
          );
          launchViewer(uri, resolveMimeForViewing(filename, bytes));
        },
        this::showErrorMessage
    );
  }

  private File writeToCache(String filename, byte[] bytes) {
    File dir = new File(requireContext().getExternalFilesDir(null), "Manuals");
    if (!dir.exists() && !dir.mkdirs()) return null;
    File target = new File(dir, filename.replaceAll("[^A-Za-z0-9._-]", "_"));
    try (FileOutputStream out = new FileOutputStream(target)) {
      out.write(bytes);
      return target;
    } catch (IOException e) {
      Log.e(TAG, "writeToCache: ", e);
      return null;
    }
  }

  /**
   * Resolves the type from the file extension. Manuals uploaded by older versions of this
   * screen are all stored as ".bin", so fall back to sniffing the magic bytes for those.
   */
  private String resolveMimeForViewing(String filename, byte[] bytes) {
    int dot = filename.lastIndexOf('.');
    if (dot > -1 && dot < filename.length() - 1) {
      String ext = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
      String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
      if (mime != null) return mime;
    }
    return sniffMime(bytes);
  }

  private String sniffMime(byte[] b) {
    if (b.length >= 4 && b[0] == 0x25 && b[1] == 0x50 && b[2] == 0x44 && b[3] == 0x46) {
      return "application/pdf";
    }
    if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
      return "image/jpeg";
    }
    if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47) {
      return "image/png";
    }
    return "*/*";
  }

  private void launchViewer(Uri uri, String mimeType) {
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setDataAndType(uri, mimeType);
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    try {
      startActivity(intent);
    } catch (ActivityNotFoundException e) {
      activity.showSnackbar(R.string.error_open_manual, false);
    }
  }

  private byte[] readBytes(Uri uri) {
    try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      if (in == null) return null;
      byte[] buf = new byte[4096];
      int n;
      while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
      return out.toByteArray();
    } catch (IOException e) {
      return null;
    }
  }

  @Override
  public void deleteObject(int equipmentId) {
    dlHelper.delete(
        grocyApi.getObject(GrocyApi.ENTITY.EQUIPMENT, equipmentId),
        response -> activity.navUtil.navigateUp(),
        this::showErrorMessage
    );
  }

  private void showErrorMessage(VolleyError volleyError) {
    activity.showSnackbar(getErrorMessage(volleyError), false);
  }

  private void showDeleteConfirmationDialog() {
    dialogDelete = new MaterialAlertDialogBuilder(
        activity, R.style.ThemeOverlay_Grocy_AlertDialog_Caution
    ).setTitle(R.string.title_confirmation)
        .setMessage(
            activity.getString(
                R.string.msg_master_delete,
                getString(R.string.property_equipment_singular),
                editEquipment.getName()
            )
        ).setPositiveButton(R.string.action_delete, (dialog, which) -> {
          performHapticClick();
          if (editEquipment != null) deleteObject(editEquipment.getId());
        }).setNegativeButton(R.string.action_cancel, (dialog, which) -> performHapticClick())
        .setOnCancelListener(dialog -> performHapticClick())
        .create();
    dialogDelete.show();
  }

  public Toolbar.OnMenuItemClickListener getBottomMenuClickListener() {
    return item -> {
      if (item.getItemId() == R.id.action_delete) {
        ViewUtil.startIcon(item);
        showDeleteConfirmationDialog();
        return true;
      }
      return false;
    };
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
