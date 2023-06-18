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

package xyz.zedler.patrick.grocy;

import androidx.appcompat.app.AppCompatDelegate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class Constants {

  public final static class PREF {

    public final static String SERVER_URL = "server_url";
    public final static String API_KEY = "api_key";
    public final static String HOME_ASSISTANT_SERVER_URL = "home_assistant_server_url";
    public final static String HOME_ASSISTANT_LONG_LIVED_TOKEN = "home_assistant_long_lived_token";
    public final static String HOME_ASSISTANT_INGRESS_SESSION_KEY = "home_assistant_ingress_session_key";
    public final static String HOME_ASSISTANT_INGRESS_SESSION_KEY_TIME = "home_assistant_ingress_session_key_time";
    public final static String CURRENCY = "currency";
    public final static String ENERGY_UNIT = "energy_unit";
    public final static String ENERGY_UNIT_DEFAULT = "kcal";
    public final static String GROCY_VERSION = "grocy_version";
    public final static String CREDENTIALS = "credentials";
    public final static String CURRENT_USER_ID = "current_user_id";

    public final static String FEATURE_STOCK = "feature_stock";
    public final static String FEATURE_SHOPPING_LIST = "feature_shopping_list";
    public final static String FEATURE_STOCK_PRICE_TRACKING = "feature_stock_price_tracking";
    public final static String FEATURE_STOCK_LOCATION_TRACKING = "feature_stock_location_tracking";
    public final static String FEATURE_STOCK_BBD_TRACKING = "feature_stock_bbd_tracking";
    public final static String FEATURE_STOCK_FREEZING_TRACKING = "feature_stock_freezing_tracking";
    public final static String FEATURE_STOCK_OPENED_TRACKING = "feature_stock_opened_tracking";
    public final static String FEATURE_MULTIPLE_SHOPPING_LISTS = "feature_multiple_shopping_lists";
    public final static String FEATURE_RECIPES = "feature_recipes";
    public final static String FEATURE_TASKS = "feature_tasks";
    public final static String FEATURE_CHORES = "feature_chores";
    public final static String FEATURE_CHORES_ASSIGNMENTS = "feature_chores_assignments";
    public final static String FEATURE_LABEL_PRINTER = "feature_label_printer";

    public final static String STOCK_SORT_MODE = "stock_sort_mode";
    public final static String STOCK_SORT_ASCENDING = "stock_sort_ascending";
    public final static String STOCK_GROUPING_MODE = "stock_grouping_mode";
    public final static String STOCK_FIELDS = "stock_fields";
    public final static String STOCK_ENTRIES_SORT_MODE = "stock_entries_sort_mode";
    public final static String STOCK_ENTRIES_SORT_ASCENDING = "stock_entries_sort_ascending";
    public final static String STOCK_ENTRIES_GROUPING_MODE = "stock_entries_grouping_mode";
    public final static String SHOPPING_LIST_GROUPING_MODE = "shopping_list_grouping_mode";
    public final static String SHOPPING_LIST_FIELDS = "shopping_list_fields";
    public final static String SHOPPING_LIST_LAST_ID = "shopping_list_last_id";
    public final static String SHOPPING_MODE_GROUPING_MODE = "shopping_mode_grouping_mode";
    public final static String SHOPPING_MODE_FIELDS = "shopping_mode_fields";
    public final static String TASKS_SORT_MODE = "tasks_sort_mode";
    public final static String TASKS_SORT_ASCENDING = "tasks_sort_ascending";
    public final static String TASKS_SHOW_DONE = "tasks_show_done";
    public final static String RECIPES_SORT_MODE = "recipes_sort_mode";
    public final static String RECIPES_SORT_ASCENDING = "recipes_sort_ascending";
    public final static String RECIPES_FIELDS = "recipes_fields";
    public final static String RECIPES_LIST_LAYOUT = "recipes_list_layout";

    public final static String DB_LAST_TIME_STOCK_ITEMS = "db_last_time_stock_items";
    public final static String DB_LAST_TIME_STOCK_ENTRIES = "db_last_time_stock_entries";
    public final static String DB_LAST_TIME_STORES = "db_last_time_stores";
    public final static String DB_LAST_TIME_LOCATIONS = "db_last_time_locations";
    public final static String DB_LAST_TIME_STOCK_LOCATIONS = "db_last_time_stock_locations";
    public final static String DB_LAST_TIME_SHOPPING_LIST_ITEMS = "db_last_time_shopping_list_items";
    public final static String DB_LAST_TIME_SHOPPING_LISTS = "db_last_time_shopping_lists";
    public final static String DB_LAST_TIME_PRODUCT_GROUPS = "db_last_time_product_groups";
    public final static String DB_LAST_TIME_QUANTITY_UNITS = "db_last_time_quantity_units";
    public final static String DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS = "db_last_time_quantity_unit_conversions";
    public final static String DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS_RESOLVED = "db_last_time_quantity_unit_conversions_resolved";
    public final static String DB_LAST_TIME_PRODUCTS = "db_last_time_products";
    public final static String DB_LAST_TIME_PRODUCTS_LAST_PURCHASED = "db_last_time_products_last_purchased";
    public final static String DB_LAST_TIME_PRODUCTS_AVERAGE_PRICE = "db_last_time_products_average_price";
    public final static String DB_LAST_TIME_PRODUCT_BARCODES = "db_last_time_product_barcodes";
    public final static String DB_LAST_TIME_VOLATILE = "db_last_time_volatile";
    public final static String DB_LAST_TIME_VOLATILE_MISSING = "db_last_time_volatile_missing";
    public final static String DB_LAST_TIME_TASKS = "db_last_time_tasks";
    public final static String DB_LAST_TIME_TASK_CATEGORIES = "db_last_time_task_categories";
    public final static String DB_LAST_TIME_CHORES = "db_last_time_chores";
    public final static String DB_LAST_TIME_CHORE_ENTRIES = "db_last_time_chore_entries";
    public final static String DB_LAST_TIME_USERS = "db_last_time_users";
    public final static String DB_LAST_TIME_RECIPES = "db_last_time_recipes";
    public final static String DB_LAST_TIME_RECIPE_FULFILLMENTS = "db_last_time_recipe_fulfillments";
    public final static String DB_LAST_TIME_RECIPE_POSITIONS = "db_last_time_recipe_positions";

    public final static String QUICK_MODE_ACTIVE_CONSUME = "quick_mode_active_consume";
    public final static String QUICK_MODE_ACTIVE_PURCHASE = "quick_mode_active_purchase";
    public final static String QUICK_MODE_ACTIVE_SHOPPING_ITEM = "quick_mode_active_shopping_item";
    public final static String QUICK_MODE_ACTIVE_TRANSFER = "quick_mode_active_transfer";
    public final static String QUICK_MODE_ACTIVE_INVENTORY = "quick_mode_active_inventory";
    public final static String CAMERA_SCANNER_VISIBLE_CONSUME = "camera_scanner_visible_consume";
    public final static String CAMERA_SCANNER_VISIBLE_PURCHASE = "camera_scanner_visible_purchase";
    public final static String CAMERA_SCANNER_VISIBLE_SHOPPING_ITEM = "camera_scanner_visible_shopping_item";
    public final static String CAMERA_SCANNER_VISIBLE_TRANSFER = "camera_scanner_visible_transfer";
    public final static String CAMERA_SCANNER_VISIBLE_INVENTORY = "camera_scanner_visible_inventory";
    public final static String CAMERA_SCANNER_VISIBLE_RECIPE = "camera_scanner_visible_recipe";

    public final static String INTRO_SHOWN = "intro_shown";
    public final static String VERSION_COMPATIBILITY_IGNORED = "version_ignored_compatibility";
    public final static String UPDATE_INFO_READ = "update_info_read";
    public final static String LAST_VERSION = "last_version";
    public final static String LAST_VERSION_DATABASE = "last_version_database";
    public final static String ZXING_PROMPT = "zxing_prompt";
    public final static String OVERVIEW_FAB_INFO_SHOWN = "overview_fab_info_shown";
  }

  public final static class SETTINGS {

    public final static class SERVER {

    }

    public final static class APPEARANCE {

      public final static String THEME = "app_theme"; // theme identifier is already used
      public final static String DARK_MODE = "dark_mode";
      public static final String USE_SLIDING = "use_sliding_transition";
    }

    public final static class NETWORK {

      public final static String LOADING_CIRCLE = "loading_circle";
      public final static String LOADING_TIMEOUT = "loading_timeout";
      public final static String TOR = "tor";
      public final static String PROXY = "proxy";
      public final static String PROXY_HOST = "proxy_host";
      public final static String PROXY_PORT = "proxy_port";
    }

    public final static class BEHAVIOR {

      public final static String BEGINNER_MODE = "beginner_mode";
      public final static String HAPTIC = "haptic";
      public final static String FOOD_FACTS = "food_facts";
      public final static String SHOW_MAIN_MENU_BUTTON = "show_main_menu_button";
      public final static String EXPAND_BOTTOM_SHEETS = "expand_bottom_sheets";
      public final static String SPEED_UP_START = "speed_up_start";
      public final static String TURN_ON_QUICK_MODE = "turn_on_quick_mode";
      public final static String DATE_KEYBOARD_INPUT = "date_keyboard_input";
      public final static String DATE_KEYBOARD_REVERSE = "date_keyboard_reverse";
      public final static String MESSAGE_DURATION = "message_duration";
      public final static String COPY_BARCODE_NOTE = "copy_barcode_note";
    }

    public final static class SCANNER {

      public final static String USE_ML_KIT = "use_ml_kit";
      public final static String FRONT_CAM = "front_cam";
      public final static String SCANNER_FORMAT_2D = "scanner_format_2d";
      public final static String BARCODE_FORMATS = "barcode_formats";
      public final static String EXTERNAL_SCANNER = "external_scanner";
    }

    public final static class STOCK {

      public final static String LOCATION = "product_presets_location_id"; // used for pref sync, DO NOT EDIT VALUE
      public final static String PRODUCT_GROUP = "product_presets_product_group_id"; // used for pref sync, DO NOT EDIT VALUE
      public final static String QUANTITY_UNIT = "product_presets_qu_id"; // used for pref sync, DO NOT EDIT VALUE
      public final static String DEFAULT_DUE_DAYS = "product_presets_default_due_days"; // used for pref sync, DO NOT EDIT VALUE
      public final static String TREAT_OPENED_OUT_OF_STOCK = "product_presets_treat_opened_as_out_of_stock"; // used for pref sync, DO NOT EDIT VALUE
      public final static String DISPLAY_DOTS_IN_STOCK = "show_icon_on_stock_overview_page_when_product_is_on_shopping_list"; // used for pref sync, DO NOT EDIT VALUE
      public final static String DUE_SOON_DAYS = "stock_due_soon_days"; // used for pref sync, DO NOT EDIT VALUE
      public final static String SHOW_PURCHASED_DATE = "show_purchased_date_on_purchase"; // used for pref sync, DO NOT EDIT VALUE
      public final static String DEFAULT_PURCHASE_AMOUNT = "stock_default_purchase_amount"; // used for pref sync, DO NOT EDIT VALUE
      public final static String DEFAULT_CONSUME_AMOUNT = "stock_default_consume_amount"; // used for pref sync, DO NOT EDIT VALUE
      public final static String USE_QUICK_CONSUME_AMOUNT = "stock_default_consume_amount_use_quick_consume_amount"; // used for pref sync, DO NOT EDIT VALUE
      public final static String DECIMAL_PLACES_AMOUNT = "stock_decimal_places_amounts"; // used for pref sync, DO NOT EDIT VALUE
      public final static String DECIMAL_PLACES_PRICES_INPUT = "stock_decimal_places_prices_input"; // used for pref sync, DO NOT EDIT VALUE
      public final static String DECIMAL_PLACES_PRICES_DISPLAY = "stock_decimal_places_prices_display"; // used for pref sync, DO NOT EDIT VALUE
      public final static String AUTO_DECIMAL_SEPARATOR_PRICES = "stock_auto_decimal_separator_prices"; // used for pref sync, DO NOT EDIT VALUE
    }

    public final static class SHOPPING_LIST {

      public final static String AUTO_ADD = "shopping_list_auto_add_below_min_stock_amount"; // used for pref sync, DO NOT EDIT VALUE
      public final static String AUTO_ADD_LIST_ID = "shopping_list_auto_add_below_min_stock_amount_list_id"; // used for pref sync, DO NOT EDIT VALUE
    }

    public final static class CHORES {

      public final static String DUE_SOON_DAYS = "chores_due_soon_days"; // used for pref sync, DO NOT EDIT VALUE
    }

    public final static class SHOPPING_MODE {

      public final static String UPDATE_INTERVAL = "shopping_mode_update_interval";
      public final static String KEEP_SCREEN_ON = "shopping_keep_screen_on";
      public final static String SHOW_DONE_ITEMS = "show_done_items";
      public final static String USE_SMALLER_FONT = "use_smaller_font";
    }

    public final static class RECIPES {

      public final static String KEEP_SCREEN_ON = "recipes_keep_screen_on";
    }

    public final static class DEBUGGING {

      public final static String ENABLE_DEBUGGING = "enable_debugging";
    }

    public final static class NOTIFICATIONS {

      public final static String DUE_SOON_ENABLE = "notification_due_soon_enable";
      public final static String DUE_SOON_TIME = "notification_due_soon_time";
      public final static String DUE_SOON_CHANNEL = "xyz.zedler.patrick.grocy.due_soon";
      public final static int DUE_SOON_ID = 1;

      public final static String CHORES_ENABLE = "notification_chores_enable";
      public final static String CHORES_TIME = "notification_chores_time";
      public final static String CHORES_CHANNEL = "xyz.zedler.patrick.grocy.chores";
      public final static int CHORES_ID = 2;
    }
  }

  public final static class SETTINGS_DEFAULT {

    public final static class APPEARANCE {

      public final static String THEME = "";
      public final static int DARK_MODE = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
      public static final boolean USE_SLIDING = false;
    }

    public final static class NETWORK {

      public final static int LOADING_TIMEOUT = 30;
      public final static boolean LOADING_CIRCLE = false;
      public final static boolean TOR = false;
      public final static boolean PROXY = false;
      public final static String PROXY_HOST = "127.0.0.1";
      public final static int PROXY_PORT = 8118;
    }

    public final static class BEHAVIOR {

      public final static boolean BEGINNER_MODE = true;
      public final static boolean HAPTIC = true;
      public final static boolean FOOD_FACTS = false;
      public final static boolean SHOW_MAIN_MENU_BUTTON = true;
      public final static boolean EXPAND_BOTTOM_SHEETS = false;
      public final static boolean SPEED_UP_START = false;
      public final static boolean TURN_ON_QUICK_MODE = true;
      public final static boolean DATE_KEYBOARD_INPUT = false;
      public final static boolean DATE_KEYBOARD_REVERSE = false;
      public final static int MESSAGE_DURATION = 10;
      public final static boolean COPY_BARCODE_NOTE = false;
    }

    public final static class SCANNER {

      public final static boolean USE_ML_KIT = false;
      public final static boolean FRONT_CAM = false;
      public final static boolean SCANNER_FORMAT_2D = false;
      public final static Set<String> BARCODE_FORMATS = new HashSet<>(Arrays.asList(
          BarcodeFormats.BARCODE_FORMAT_CODE128,
          BarcodeFormats.BARCODE_FORMAT_CODE39,
          BarcodeFormats.BARCODE_FORMAT_CODE93,
          BarcodeFormats.BARCODE_FORMAT_CODABAR,
          BarcodeFormats.BARCODE_FORMAT_EAN13,
          BarcodeFormats.BARCODE_FORMAT_EAN8,
          BarcodeFormats.BARCODE_FORMAT_ITF,
          BarcodeFormats.BARCODE_FORMAT_UPCA,
          BarcodeFormats.BARCODE_FORMAT_UPCE,
          BarcodeFormats.BARCODE_FORMAT_QR,
          BarcodeFormats.BARCODE_FORMAT_PDF417,
          BarcodeFormats.BARCODE_FORMAT_AZTEC,
          BarcodeFormats.BARCODE_FORMAT_MATRIX,
          BarcodeFormats.BARCODE_FORMAT_RSS14,
          BarcodeFormats.BARCODE_FORMAT_RSSE
      ));
      public final static boolean EXTERNAL_SCANNER = false;
    }

    public final static class STOCK {

      public final static int LOCATION = -1;
      public final static int PRODUCT_GROUP = -1;
      public final static int QUANTITY_UNIT = -1;
      public final static int DEFAULT_DUE_DAYS = 0;
      public final static boolean DISPLAY_DOTS_IN_STOCK = true;
      public final static String DUE_SOON_DAYS = "5";
      public final static boolean SHOW_PURCHASED_DATE = false;
      public final static String DEFAULT_PURCHASE_AMOUNT = "1";
      public final static String DEFAULT_CONSUME_AMOUNT = "1";
      public final static boolean USE_QUICK_CONSUME_AMOUNT = false;
      public final static boolean TREAT_OPENED_OUT_OF_STOCK = true;
      public final static int DECIMAL_PLACES_AMOUNT = 2;
      public final static int DECIMAL_PLACES_PRICES_INPUT = 2;
      public final static int DECIMAL_PLACES_PRICES_DISPLAY = 2;
      public final static boolean AUTO_DECIMAL_SEPARATOR_PRICES = false;
    }

    public final static class SHOPPING_LIST {

      public final static boolean AUTO_ADD = false;
      public final static int AUTO_ADD_LIST_ID = 1;
    }

    public final static class CHORES {

      public final static int DUE_SOON_DAYS = 5;
    }

    public final static class SHOPPING_MODE {

      public final static int UPDATE_INTERVAL = 10;
      public final static boolean KEEP_SCREEN_ON = true;
      public final static boolean SHOW_DONE_ITEMS = true;
      public final static boolean USE_SMALLER_FONT = false;
    }

    public final static class RECIPES {

      public final static boolean KEEP_SCREEN_ON = true;
    }

    public final static class DEBUGGING {

      public final static boolean ENABLE_DEBUGGING = false;
    }

    public final static class NOTIFICATIONS {
      public final static boolean DUE_SOON_ENABLE = false;
      public final static String DUE_SOON_TIME = "12:00";

      public final static boolean CHORES_ENABLE = false;
      public final static String CHORES_TIME = "12:00";
    }
  }

  public final static class BarcodeFormats {
    public final static String BARCODE_FORMAT_CODE128 = "barcode_format_code128";
    public final static String BARCODE_FORMAT_CODE39 = "barcode_format_code39";
    public final static String BARCODE_FORMAT_CODE93 = "barcode_format_code93";
    public final static String BARCODE_FORMAT_CODABAR = "barcode_format_codabar";  // only ML Kit
    public final static String BARCODE_FORMAT_EAN13 = "barcode_format_ean13";
    public final static String BARCODE_FORMAT_EAN8 = "barcode_format_ean8";
    public final static String BARCODE_FORMAT_ITF = "barcode_format_itf";
    public final static String BARCODE_FORMAT_UPCA = "barcode_format_upca";
    public final static String BARCODE_FORMAT_UPCE = "barcode_format_upce";
    public final static String BARCODE_FORMAT_QR = "barcode_format_qr";
    public final static String BARCODE_FORMAT_PDF417 = "barcode_format_pdf417";
    public final static String BARCODE_FORMAT_AZTEC = "barcode_format_aztec";  // only ML Kit
    public final static String BARCODE_FORMAT_MATRIX = "barcode_format_matrix";
    public final static String BARCODE_FORMAT_RSS14 = "barcode_format_rss14";  // only ZXing
    public final static String BARCODE_FORMAT_RSSE = "barcode_format_rsse";  // only ZXing
  }

  public final static class DATE {

    public final static String NEVER_OVERDUE = "2999-12-31";
    public final static String NEVER_OVERDUE_WITH_TIME = "2999-12-31 23:59:59";
  }

  public final static class ACTION {

    public final static String CONSUME = "action_consume";
    public final static String OPEN = "action_open";
    public final static String CONSUME_ALL = "action_consume_all";
    public final static String CONSUME_SPOILED = "action_consume_spoiled";
    public final static String PURCHASE = "action_purchase";
    public final static String CREATE = "action_create";
    public final static String EDIT = "action_edit";
    public final static String SAVE = "action_save";
    public final static String SAVE_CLOSE = "action_save_close";
    public final static String SAVE_NOT_CLOSE = "action_save_not_close";
    public final static String DELETE = "action_delete";
    public final static String COMPLETE = "action_complete";
    public final static String UNDO = "action_undo";
  }

  public final static class ARGUMENT {

    public final static String PREFERENCE = "option";
    public final static String ANIMATED = "animated";
    public final static String QUANTITY_UNIT = "quantity_unit";
    public final static String QUANTITY_UNITS = "quantity_units";
    public final static String QUANTITY_UNIT_PURCHASE = "quantity_unit_purchase";
    public final static String QUANTITY_UNIT_STOCK = "quantity_unit_stock";
    public final static String QUANTITY_UNIT_CONVERSIONS = "quantity_unit_conversions";
    public final static String AMOUNT = "amount";
    public final static String LOCATION = "location";
    public final static String LOCATIONS = "locations";
    public final static String STORE = "store";
    public final static String STORES = "stores";
    public final static String CHORE = "chore";
    public final static String TASK = "task";
    public final static String TASK_CATEGORY = "task_category";
    public final static String TASK_CATEGORIES = "task_categories";
    public final static String SHOPPING_LIST = "shopping_list";
    public final static String PRODUCT_GROUP = "product_group";
    public final static String PRODUCT_GROUPS = "product_groups";
    public final static String STOCK_LOCATIONS = "stock_locations";
    public final static String STOCK_ENTRIES = "product_entries";
    public final static String STOCK_ENTRY = "product_entry";
    public final static String PRODUCT_DETAILS = "product_details";
    public final static String PRODUCT = "product";
    public final static String PRODUCTS = "products";
    public final static String PRODUCT_NAME = "product_name";
    public final static String PRODUCT_INPUT = "product_input";
    public final static String OBJECT_NAME = "object_name";
    public final static String OBJECT_ID = "object_id";
    public final static String DEMO_CHOSEN = "demo_chosen";
    public final static String POSITION = "position";
    public final static String TYPE = "type";
    public final static String ENTITY = "entity";
    public final static String SELECTED_ID = "selected_id";
    public final static String PRODUCT_ID = "product_id";
    public final static String DESCRIPTION = "description";
    public final static String SELECTED_DATE = "selected_date";
    public final static String SHOPPING_LIST_ITEM = "shopping_list_item";
    public final static String ACTION = "action";
    public final static String BUNDLE = "bundle";
    public final static String DEFAULT_DAYS_FROM_NOW = "default_best_before_days";
    public final static String NUMBER = "number";
    public final static String TEXT = "text";
    public final static String TEXT_ALTERNATIVE = "text_alt";
    public final static String FILE = "file";
    public final static String LINK = "link";
    public final static String HTML = "html";
    public final static String TITLE = "title";
    public final static String HIGHLIGHTS = "highlights";
    public final static String HINT = "hint";
    public final static String CURRENCY = "currency";
    public final static String SUPPORTED_VERSIONS = "supported_versions";
    public final static String VERSION = "version";
    public final static String SERVER = "server";
    public final static String KEY = "key";
    public final static String SHOW_OFFLINE = "show_offline";
    public final static String BARCODE = "barcode";
    public final static String DISPLAY_EMPTY_OPTION = "display_empty_option";
    public final static String DISPLAY_NEW_OPTION = "display_new_option";
    public final static String DISPLAY_PIN_BUTTONS = "display_pin_buttons";
    public final static String CURRENT_PIN_ID = "current_pin_id";
    public final static String NONE_SELECTABLE = "none_selectable";
    public final static String PENDING_PRODUCT_ID = "pending_product_id";
    public final static String BACK_FROM_CHOOSE_PRODUCT_PAGE = "back_from_choose_product_page";
    public final static String USER = "user";
    public final static String USERS = "users";
    public final static String SHOW_OPTION_NEVER_EXPIRES = "show_option_never_expires";
    public final static String RECIPE = "recipe";
    public final static String RECIPE_ID = "recipe_id";
    public final static String RECIPE_FULFILLMENT = "recipe_fulfillment";
    public final static String RECIPE_POSITION = "recipe_position";
    public final static String RECIPE_POSITIONS = "recipe_positions";
    public final static String RUN_AS_SUPER_CLASS = "run_as_super_class";
    public final static String INSTANCE_STATE = "instance_state";
    public final static String SCROLL_POSITION = "scroll_position";
  }

  public final static class FAB {

    public final static class TAG {

      public final static String ADD = "add";
      public final static String SCAN = "scan";
      public final static String CONSUME = "consume";
      public final static String PURCHASE = "purchase";
      public final static String TRANSFER = "transfer";
      public final static String INVENTORY = "inventory";
      public final static String SAVE = "save";
      public final static String SAVE_NOT_CLOSE = "save_not_close";
      public final static String DONE = "done";
      public final static String PREPARATION = "preparation";
    }
  }

  public static final class THEME {

    public static final String DYNAMIC = "dynamic";
    public static final String RED = "red";
    public static final String YELLOW = "yellow";
    public static final String LIME = "lime";
    public static final String GREEN = "green";
    public static final String TURQUOISE = "turquoise";
    public static final String TEAL = "teal";
    public static final String BLUE = "blue";
    public static final String PURPLE = "purple";
  }
}
