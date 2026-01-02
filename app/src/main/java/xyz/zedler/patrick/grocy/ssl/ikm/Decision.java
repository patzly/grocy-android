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

package xyz.zedler.patrick.grocy.ssl.ikm;

/**
 * Source: github.com/stephanritscher/InteractiveKeyManager
 */
public class Decision {

  public final static int DECISION_INVALID = 0;
  public final static int DECISION_ABORT = 1;
  public final static int DECISION_KEYCHAIN = 2;
  public final static int DECISION_FILE = 3;

  public int state = DECISION_INVALID;
  public String param;
  public String hostname;
  public Integer port;
}