package xyz.zedler.patrick.grocy.model;

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

import xyz.zedler.patrick.grocy.R;

public class ErrorFullscreen {

    public static final int OFFLINE = 2;
    public static final int NETWORK = 4;

    private int errorPicture = -1;
    private int errorTitle = -1;
    private int errorSubtitle = -1;
    private String errorExact;

    public ErrorFullscreen(int errorType, String errorExact) {
        switch (errorType) {
            case OFFLINE:
                errorPicture = R.drawable.illustration_broccoli;
                errorTitle = R.string.error_offline;
                errorSubtitle = R.string.error_offline_subtitle;
                break;
            case NETWORK:
                errorPicture = R.drawable.illustration_broccoli;
                errorTitle = R.string.error_network;
                errorSubtitle = R.string.error_network_subtitle;
                break;
        }
        this.errorExact = errorExact;
    }

    public ErrorFullscreen(int errorType) {
        this(errorType, null);
    }

    public int getErrorPicture() {
        return errorPicture;
    }

    public int getErrorTitle() {
        return errorTitle;
    }

    public int getErrorSubtitle() {
        return errorSubtitle;
    }

    public String getErrorExact() {
        return errorExact;
    }
}
