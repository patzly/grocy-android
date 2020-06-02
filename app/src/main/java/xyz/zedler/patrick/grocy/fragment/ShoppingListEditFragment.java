package xyz.zedler.patrick.grocy.fragment;

/*
    This file is part of Grocy Android.

    Grocy Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Grocy Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2020 by Patrick Zedler & Dominic Zedler
*/

import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.VolleyError;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.databinding.FragmentShoppingListEditBinding;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class ShoppingListEditFragment extends Fragment {

    private final static String TAG = Constants.UI.SHOPPING_LIST_ITEM_EDIT;
    private final static boolean DEBUG = true;

    private MainActivity activity;
    private Gson gson = new Gson();
    private GrocyApi grocyApi;
    private WebRequest request;
    private Bundle startupBundle;

    private ArrayList<ShoppingList> shoppingLists = new ArrayList<>();
    private ArrayList<String> shoppingListNames = new ArrayList<>();

    private FragmentShoppingListEditBinding binding;
    private TextInputLayout textInputName;
    private EditText editTextName;
    private String action;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentShoppingListEditBinding.inflate(
                inflater,
                container,
                false
        );
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (MainActivity) getActivity();
        assert activity != null;

        startupBundle = getArguments();
        if(startupBundle != null) {
            action = startupBundle.getString(Constants.ARGUMENT.TYPE);
            if(action == null) action = Constants.ACTION.CREATE;
        }

        // WEB REQUESTS

        request = new WebRequest(activity.getRequestQueue());
        grocyApi = activity.getGrocy();

        // INITIALIZE VIEWS

        binding.frameShoppingListEditBack.setOnClickListener(v -> activity.onBackPressed());

        // title

        if(action.equals(Constants.ACTION.EDIT)) {
            binding.textShoppingListEditTitle.setText(
                    activity.getString(R.string.title_shopping_list_edit)
            );
        } else {
            binding.textShoppingListEditTitle.setText(
                    activity.getString(R.string.title_shopping_list_new)
            );
        }

        // swipe refresh

        binding.swipeShoppingListEdit.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        binding.swipeShoppingListEdit.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        binding.swipeShoppingListEdit.setOnRefreshListener(this::refresh);

        // name

        textInputName = binding.textInputShoppingListEditName;
        editTextName = textInputName.getEditText();
        assert editTextName != null;
        editTextName.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) {
                IconUtil.start(binding.imageShoppingListEditName);
            }
        });
        editTextName.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                String input = s.toString().trim();
                if(!input.isEmpty() && shoppingListNames.contains(input)) {
                    textInputName.setError(activity.getString(R.string.error_name_exists));
                } else if(textInputName.isErrorEnabled()) {
                    textInputName.setErrorEnabled(false);
                }
            }
        });

        // START

        refresh();

        // UPDATE UI

        activity.updateUI(
                Constants.UI.SHOPPING_LIST_EDIT,
                getArguments() == null || getArguments().getBoolean(
                        Constants.ARGUMENT.ANIMATED, true
                ),
                TAG
        );
        if(getArguments() != null) {
            getArguments().putBoolean(Constants.ARGUMENT.ANIMATED, true);
        }
    }

    private void refresh() {
        if(activity.isOnline()) {
            download();
        } else {
            binding.swipeShoppingListEdit.setRefreshing(false);
            activity.showMessage(
                    Snackbar.make(
                            activity.findViewById(R.id.linear_container_main),
                            activity.getString(R.string.msg_no_connection),
                            Snackbar.LENGTH_SHORT
                    ).setActionTextColor(
                            ContextCompat.getColor(activity, R.color.secondary)
                    ).setAction(
                            activity.getString(R.string.action_retry),
                            v1 -> refresh()
                    )
            );
        }

        clearAll();
    }

    private void download() {
        binding.swipeShoppingListEdit.setRefreshing(true);
        downloadShoppingLists();
    }

    private void downloadShoppingLists() {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.SHOPPING_LISTS),
                TAG,
                response -> {
                    binding.swipeShoppingListEdit.setRefreshing(false);
                    shoppingLists = gson.fromJson(
                            response,
                            new TypeToken<List<ShoppingList>>(){}.getType()
                    );
                    shoppingListNames = getShoppingListNames();
                    if(DEBUG) Log.i(
                            TAG,
                            "downloadShoppingLists: shoppingLists = " + shoppingLists
                    );
                    if(action.equals(Constants.ACTION.EDIT)) {
                        ShoppingList shoppingList = startupBundle.getParcelable(
                                Constants.ARGUMENT.SHOPPING_LIST
                        );
                        assert shoppingList != null;

                        shoppingListNames.remove(shoppingList.getName());
                        fillWithEditDetails(shoppingList);
                    }
                },
                this::onError
        );
    }

    private void onError(VolleyError error) {
        Log.e(TAG, "onError: VolleyError: " + error);
        request.cancelAll(TAG);
        binding.swipeShoppingListEdit.setRefreshing(false);
        activity.showMessage(
                Snackbar.make(
                        activity.findViewById(R.id.linear_container_main),
                        activity.getString(R.string.msg_error),
                        Snackbar.LENGTH_SHORT
                ).setActionTextColor(
                        ContextCompat.getColor(activity, R.color.secondary)
                ).setAction(
                        activity.getString(R.string.action_retry),
                        v1 -> download()
                )
        );
    }

    private void fillWithEditDetails(ShoppingList shoppingList) {
        clearInputFocus();

        // NAME
        editTextName.setText(shoppingList.getName());
        textInputName.setErrorEnabled(false);
    }

    private void clearInputFocus() {
        activity.hideKeyboard();
        textInputName.clearFocus();
    }

    private void clearErrors() {
        textInputName.setErrorEnabled(false);
    }

    public void saveItem() {
        if(isFormIncomplete()) return;

        String name = editTextName.getText().toString().trim();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", name);
        } catch (JSONException e) {
            if(DEBUG) Log.e(TAG, "saveShoppingList: " + e);
        }
        Bundle bundle = new Bundle();
        bundle.putString(Constants.ARGUMENT.SHOPPING_LIST_NAME, name);

        if(action.equals(Constants.ACTION.EDIT)) {
            ShoppingList shoppingList = startupBundle.getParcelable(
                    Constants.ARGUMENT.SHOPPING_LIST
            );
            assert shoppingList != null;
            request.put(
                    grocyApi.getObject(GrocyApi.ENTITY.SHOPPING_LISTS, shoppingList.getId()),
                    jsonObject,
                    response -> activity.dismissFragment(bundle),
                    error -> {
                        showErrorMessage();
                        Log.e(TAG, "saveShoppingListItem: " + error);
                    }
            );
        } else {
            request.post(
                    grocyApi.getObjects(GrocyApi.ENTITY.SHOPPING_LISTS),
                    jsonObject,
                    response -> activity.dismissFragment(bundle),
                    error -> {
                        showErrorMessage();
                        Log.e(TAG, "saveShoppingListItem: " + error);
                    }
            );
        }
    }

    private boolean isFormIncomplete() {
        clearInputFocus();
        clearErrors();

        if(shoppingListNames.contains(editTextName.getText().toString().trim())) {
            textInputName.setError(activity.getString(R.string.error_name_exists));
            return true;
        }
        return false;
    }

    private ArrayList<String> getShoppingListNames() {
        ArrayList<String> names = new ArrayList<>();
        if(shoppingLists != null) {
            for(ShoppingList shoppingList : shoppingLists) {
                names.add(shoppingList.getName());
            }
        }
        return names;
    }

    public void setUpBottomMenu() {
        MenuItem menuItemDelete;
        menuItemDelete = activity.getBottomMenu().findItem(R.id.action_delete);
        if(menuItemDelete != null) {
            menuItemDelete.setVisible(action.equals(Constants.ACTION.EDIT));
            if(action.equals(Constants.ACTION.CREATE)) return;

            menuItemDelete.setOnMenuItemClickListener(item -> {
                ((Animatable) menuItemDelete.getIcon()).start();
                ShoppingList shoppingList = startupBundle.getParcelable(
                        Constants.ARGUMENT.SHOPPING_LIST
                );
                assert shoppingList != null;
                request.delete(
                        grocyApi.getObject(GrocyApi.ENTITY.SHOPPING_LISTS, shoppingList.getId()),
                        response -> activity.dismissFragment(),
                        error -> {
                            showErrorMessage();
                            if(DEBUG) Log.i(TAG, "setUpMenu: deleteItem: " + error);
                        }
                );
                return true;
            });
        }
    }

    public void clearAll() {
        textInputName.setErrorEnabled(false);
        editTextName.setText(null);
    }

    private void showErrorMessage() {
        activity.showMessage(
                Snackbar.make(
                        activity.findViewById(R.id.linear_container_main),
                        activity.getString(R.string.msg_error),
                        Snackbar.LENGTH_SHORT
                )
        );
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
