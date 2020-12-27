package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

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

import android.app.Dialog;
import android.graphics.drawable.Animatable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;

import java.lang.ref.WeakReference;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.ShoppingListAdapter;
import xyz.zedler.patrick.grocy.fragment.ShoppingListFragmentDirections;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.repository.ShoppingListRepository;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.view.ActionButton;

public class ShoppingListsBottomSheet extends CustomBottomSheet
        implements ShoppingListAdapter.ShoppingListAdapterListener {

    private final static String TAG = ShoppingListsBottomSheet.class.getSimpleName();

    private MainActivity activity;

    private ProgressBar progressConfirmation;
    private ConfirmationProgressTask confirmationProgressTask;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new BottomSheetDialog(requireContext(), R.style.Theme_Grocy_BottomSheetDialog);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(
                R.layout.fragment_bottomsheet_list_selection, container, false
        );

        activity = (MainActivity) getActivity();
        Bundle bundle = getArguments();
        assert activity != null && bundle != null;

        MutableLiveData<Integer> selectedIdLive = activity.getCurrentFragment()
                .getSelectedShoppingListIdLive();
        if(selectedIdLive == null) {
            dismiss();
            return view;
        }

        ShoppingListRepository repository = new ShoppingListRepository(activity.getApplication());

        TextView textViewTitle = view.findViewById(R.id.text_list_selection_title);
        textViewTitle.setText(activity.getString(R.string.property_shopping_lists));

        RecyclerView recyclerView = view.findViewById(R.id.recycler_list_selection);
        recyclerView.setLayoutManager(
                new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        );
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        repository.getShoppingListsLive().observe(getViewLifecycleOwner(), shoppingLists -> {
            if(shoppingLists == null) return;
            if(recyclerView.getAdapter() == null) {
                recyclerView.setAdapter(new ShoppingListAdapter(
                        shoppingLists,
                        selectedIdLive.getValue(),
                        this
                ));
            } else {
                ((ShoppingListAdapter) recyclerView.getAdapter()).updateData(
                        shoppingLists,
                        selectedIdLive.getValue()
                );
            }
        });

        selectedIdLive.observe(getViewLifecycleOwner(), selectedId -> {
            if(recyclerView.getAdapter() == null) return;
            ((ShoppingListAdapter) recyclerView.getAdapter()).updateSelectedId(
                    selectedIdLive.getValue()
            );
        });

        ActionButton buttonNew = view.findViewById(R.id.button_list_selection_new);
        if(!bundle.getBoolean(Constants.ARGUMENT.SHOW_OFFLINE)) {
            buttonNew.setVisibility(View.VISIBLE);
            buttonNew.setOnClickListener(v -> {
                dismiss();
                navigate(ShoppingListFragmentDirections
                        .actionShoppingListFragmentToShoppingListEditFragment());
            });
        }

        progressConfirmation = view.findViewById(R.id.progress_confirmation);

        return view;
    }

    @Override
    public void onDestroyView() {
        if(confirmationProgressTask != null) {
            confirmationProgressTask.cancel(true);
            confirmationProgressTask = null;
        }
        super.onDestroyView();
    }

    @Override
    public void onItemRowClicked(ShoppingList shoppingList) {
        activity.getCurrentFragment().selectShoppingList(shoppingList.getId());
        dismiss();
    }

    @Override
    public void onClickEdit(ShoppingList shoppingList) {
        dismiss();
        navigate(ShoppingListFragmentDirections
                .actionShoppingListFragmentToShoppingListEditFragment()
                .setShoppingList(shoppingList));
    }

    @Override
    public void onTouchDelete(View view, MotionEvent event, ShoppingList shoppingList) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            showAndStartProgress(view, shoppingList);
        } else if(event.getAction() == MotionEvent.ACTION_UP
                || event.getAction() == MotionEvent.ACTION_CANCEL) {
            hideAndStopProgress();
        }
    }

    private void showAndStartProgress(View buttonView, ShoppingList shoppingList) {
        TransitionManager.beginDelayedTransition((ViewGroup) getView());
        progressConfirmation.setVisibility(View.VISIBLE);
        if(confirmationProgressTask != null) {
            confirmationProgressTask.cancel(true);
            confirmationProgressTask = null;
        }
        confirmationProgressTask = new ConfirmationProgressTask(
                (ViewGroup) getView(),
                progressConfirmation,
                () -> {
                    ImageView buttonImage = buttonView.findViewById(R.id.image_action_button);
                    ((Animatable) buttonImage.getDrawable()).start();
                    activity.getCurrentFragment().deleteShoppingList(shoppingList);
                }
        );
        confirmationProgressTask.execute();
    }

    private void hideAndStopProgress() {
        if(confirmationProgressTask != null) confirmationProgressTask.setDirectionForward(false);

        if(progressConfirmation.getProgress() != 100) {
            Snackbar.make(getView(), "Keep pressing to confirm", Snackbar.LENGTH_SHORT).show(); // TODO: String
        }
    }

    public static class ConfirmationProgressTask extends AsyncTask<Void, Integer, Void> {
        private final WeakReference<ViewGroup> container;
        private final WeakReference<ProgressBar> progressBar;
        private final OnFinishedListener onFinishedListener;
        private boolean directionForward;
        private int progress;

        public ConfirmationProgressTask(
                ViewGroup viewGroup,
                ProgressBar progressBar,
                OnFinishedListener onFinishedListener
        ) {
            this.container = new WeakReference<>(viewGroup);
            this.progressBar = new WeakReference<>(progressBar);
            this.onFinishedListener = onFinishedListener;
            this.directionForward = true;
            this.progress = 0;
        }

        @Override
        protected void onPreExecute() {
            if(progressBar.get() != null) progressBar.get().setProgress(0);
        }

        @Override
        protected Void doInBackground(Void... aVoid) {
            while (0 <= progress && progress <= 100) {
                if(directionForward) {
                    progress++;
                } else {
                    progress = progress - 5;
                }
                publishProgress(progress);
                if(!wait10Millis()) break;
            }

            return null;
        }

        public void setDirectionForward(boolean forward) {
            this.directionForward = forward;
        }

        private boolean wait10Millis() {
            try {
                Thread.sleep(10);
                return true;
            } catch (InterruptedException e) {
                return false;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... integers) {
            int progress = integers[0];
            if(progressBar.get() != null) progressBar.get().setProgress(progress);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if(container.get() == null || progressBar.get() == null) return;
            TransitionManager.beginDelayedTransition(container.get());
            progressBar.get().setVisibility(View.GONE);
            if(progress >= 100) onFinishedListener.finished();
        }

        public interface OnFinishedListener {
            void finished();
        }
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
