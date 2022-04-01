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
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import xyz.zedler.patrick.grocy.fragment.LoginApiFormFragmentArgs;
import xyz.zedler.patrick.grocy.model.FormDataLoginApiForm;

public class LoginApiFormViewModel extends BaseViewModel {

  private static final String TAG = LoginApiFormViewModel.class.getSimpleName();

  private final FormDataLoginApiForm formData;
  private boolean autoProceedDoneWasDone = false;

  public LoginApiFormViewModel(@NonNull Application application, LoginApiFormFragmentArgs args) {
    super(application);

    formData = new FormDataLoginApiForm(application, args);
  }

  public FormDataLoginApiForm getFormData() {
    return formData;
  }

  public boolean isAutoProceedDoneWasDone() {
    return autoProceedDoneWasDone;
  }

  public void setAutoProceedDoneWasDone(boolean autoProceedDoneWasDone) {
    this.autoProceedDoneWasDone = autoProceedDoneWasDone;
  }

  public static class LoginApiFormViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;
    private final LoginApiFormFragmentArgs args;

    public LoginApiFormViewModelFactory(Application application, LoginApiFormFragmentArgs args) {
      this.application = application;
      this.args = args;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new LoginApiFormViewModel(application, args);
    }
  }
}
