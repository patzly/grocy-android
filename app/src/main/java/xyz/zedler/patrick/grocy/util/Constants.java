package xyz.zedler.patrick.grocy.util;

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

public final class Constants {

    public final static class PREF {
        public final static String SERVER_URL = "server_url";
        public final static String API_KEY = "api_key";
        public final static String CURRENCY = "currency";
        public final static String GROCY_VERSION = "grocy_version";
        public final static String CREDENTIALS = "credentials";

        public final static String FEATURE_SHOPPING_LIST = "feature_shopping_list";
        public final static String FEATURE_STOCK_PRICE_TRACKING = "feature_stock_price_tracking";
        public final static String FEATURE_STOCK_LOCATION_TRACKING = "feature_stock_location_tracking";
        public final static String FEATURE_STOCK_BBD_TRACKING = "feature_stock_bbd_tracking";
        public final static String FEATURE_STOCK_OPENED_TRACKING = "feature_stock_opened_tracking";
        public final static String FEATURE_MULTIPLE_SHOPPING_LISTS = "feature_multiple_shopping_lists";

        public final static String STOCK_SORT_MODE = "stock_sort_mode";
        public final static String STOCK_SORT_ASCENDING = "stock_sort_ascending";

        // DO NOT EDIT THE FOLLOWING STRINGS, THEY ARE FOR SERVER SYNC STUFF
        // (but you can edit the variable names)
        public final static String STOCK_EXPIRING_SOON_DAYS = "stock_expring_soon_days";
        public final static String STOCK_DEFAULT_PURCHASE_AMOUNT = "stock_default_purchase_amount";
        public final static String STOCK_DEFAULT_CONSUME_AMOUNT = "stock_default_consume_amount";
        public final static String PRODUCT_PRESETS_LOCATION_ID = "product_presets_location_id";
        public final static String PRODUCT_PRESETS_PRODUCT_GROUP_ID = "product_presets_product_group_id";
        public final static String PRODUCT_PRESETS_QU_ID = "product_presets_qu_id";
        public final static String SHOW_SHOPPING_LIST_ICON_IN_STOCK = "show_icon_on_stock_overview_page_when_product_is_on_shopping_list";
        public final static String RECIPE_INGREDIENTS_GROUP_BY_PRODUCT_GROUP = "recipe_ingredients_group_by_product_group";
        // end of server sync stuff

        public final static String SHOPPING_MODE_UPDATE_INTERVAL = "shopping_mode_update_interval";
        public final static String SHOPPING_LIST_LAST_ID = "shopping_list_last_id";
        public final static String KEEP_SHOPPING_SCREEN_ON = "shopping_keep_screen_on";

        public final static String BATCH_CONFIG_BBD = "batch_config_bbd";
        public final static String BATCH_CONFIG_PRICE = "batch_config_price";
        public final static String BATCH_CONFIG_STORE = "batch_config_store";
        public final static String BATCH_CONFIG_LOCATION = "batch_config_location";
        public final static String BATCH_CONFIG_STOCK_LOCATION = "batch_config_stock_location";
        public final static String BATCH_CONFIG_SPECIFIC = "batch_config_specific";
        public final static String BATCH_CONFIG_CONSUME_ALL = "batch_config_consume_all";

        public final static String DB_LAST_TIME_STORES = "db_last_time_stores";
        public final static String DB_LAST_TIME_LOCATIONS = "db_last_time_locations";
        public final static String DB_LAST_TIME_SHOPPING_LIST_ITEMS = "db_last_time_shopping_list_items";
        public final static String DB_LAST_TIME_SHOPPING_LISTS = "db_last_time_shopping_lists";
        public final static String DB_LAST_TIME_PRODUCT_GROUPS = "db_last_time_product_groups";
        public final static String DB_LAST_TIME_QUANTITY_UNITS = "db_last_time_quantity_units";
        public final static String DB_LAST_TIME_PRODUCTS = "db_last_time_products";
        public final static String DB_LAST_TIME_VOLATILE_MISSING = "db_last_time_volatile_missing";

        public final static String DARK_MODE = "force_dark_mode";
        public final static String FOOD_FACTS = "use_open_food_facts";
        public final static String USE_FRONT_CAM = "use_front_camera";
        public final static String DEBUG = "debug";
        public final static String SHOW_INFO_LOGS = "info_logs";

        public final static String INTRO_SHOWN = "intro_shown";
        public final static String VERSION_COMPATIBILITY_IGNORED = "version_ignored_compatibility";
        public final static String UPDATE_INFO_READ = "update_info_read";
    }

    public final static class SETTINGS {
        public final static class SERVER {
            public final static String GROCY_URL = "grocy_url";
            public final static String GROCY_VERSION = "grocy_version";
            public final static String RELOAD_CONFIG = "reload_config";
            public final static String LOGOUT = "logout";
        }
        public final static class APPEARANCE {
            public final static String DARK_MODE = "dark_mode";
        }
        public final static class NETWORK {
            public final static String ORBOT = "orbot";
            public final static String SOCKS_PROXY = "socks_proxy";
            public final static String LOADING_CIRCLE = "loading_circle";
            public final static String LOADING_TIMEOUT = "loading_timeout";
        }
        public final static class BEHAVIOR {
            public final static String START_DESTINATION = "start_destination";
            public final static String SHORTCUTS = "shortcuts";
        }
        public final static class SCANNER {
            public final static String FOOD_FACTS = "food_facts";
            public final static String FRONT_CAM = "front_cam";
        }
        public final static class STOCK {
            public final static String DISPLAY_DOTS_IN_STOCK = "show_icon_on_stock_overview_page_when_product_is_on_shopping_list"; // used for pref sync, DO NOT EDIT VALUE
            public final static String EXPIRING_SOON_DAYS = "stock_expring_soon_days"; // used for pref sync, DO NOT EDIT VALUE
        }
        public final static class SHOPPING_MODE {
            public final static String UPDATE_INTERVAL = "shopping_mode_update_interval";
            public final static String KEEP_SCREEN_ON = "shopping_keep_screen_on";
            public final static String SHOW_DONE_ITEMS = "show_done_items";
        }
        public final static class PURCHASE_CONSUME {

        }
        public final static class PRESETS {
            public final static String LOCATION = "product_presets_location_id"; // used for pref sync, DO NOT EDIT VALUE
            public final static String PRODUCT_GROUP = "product_presets_product_group_id"; // used for pref sync, DO NOT EDIT VALUE
            public final static String QUANTITY_UNIT = "product_presets_qu_id"; // used for pref sync, DO NOT EDIT VALUE
        }
        public final static class DEBUGGING {
            public final static String ENABLE_DEBUGGING = "enable_debugging";
            public final static String ENABLE_INFO_LOGS = "enable_info_logs";
            public final static String SHOW_LOGS = "show_logs";
        }
    }

    public final static class SETTINGS_DEFAULT {
        public final static class SERVER {
        }
        public final static class APPEARANCE {
            public final static boolean DARK_MODE_DEFAULT = false;
        }
        public final static class SCANNER {
            public final static boolean FOOD_FACTS = false;
            public final static boolean FRONT_CAM = false;
        }
        public final static class STOCK {
            public final static boolean DISPLAY_DOTS_IN_STOCK = true;
            public final static String EXPIRING_SOON_DAYS = "5";
        }
        public final static class SHOPPING_MODE {
            public final static int UPDATE_INTERVAL = 10;
            public final static boolean KEEP_SCREEN_ON = true;
            public final static boolean SHOW_DONE_ITEMS = true;
        }
        public final static class PURCHASE_CONSUME {
        }
        public final static class PRESETS {
            public final static int LOCATION = -1;
            public final static int PRODUCT_GROUP = -1;
            public final static int QUANTITY_UNIT = -1;
        }
        public final static class DEBUGGING {
            public final static boolean ENABLE_DEBUGGING = false;
            public final static boolean ENABLE_INFO_LOGS = false;
        }
    }

    public final static class URL {
        public final static String FAQ = "https://github.com/patzly/grocy-android/blob/master/FAQ.md";
        public final static String HELP = "https://github.com/patzly/grocy-android/blob/master/FAQ.md#user-content-pagetop";
    }

    public final static class DATE {
        public final static String NEVER_EXPIRES = "2999-12-31";
    }

    public final static class STOCK {
        public final static class FILTER {
            public final static String ALL = "all";
            public final static class VOLATILE {
                public final static String EXPIRING = "expiring";
                public final static String EXPIRED = "expired";
                public final static String MISSING = "missing";
            }
        }
        public final static class SORT {
            public final static String NAME = "name";
            public final static String BBD = "best_before";
        }
    }

    public final static class STATE {
        public final static String ERROR = "error";
        public final static String OFFLINE = "offline";
        public final static String NONE = "none";
    }

    public final static class ERROR {  // warning: create no unclear error states, that's bad UX
        public final static String NONE = "none"; // no error
        public final static String COMMUNICATION = "communication"; // example: server code 400
        public final static String NETWORK = "network"; // example: any volley error other than 400
        public final static String OFFLINE = "offline"; // example: device is offline
    }

    public final static class SHOPPING_LIST {
        public final static class FILTER {
            public final static String UNDONE = "undone";
            public final static String MISSING = "missing";
            public final static String ALL = "all";
        }
    }

    public final static class EXTRA {
        public final static String SCAN_RESULT = "scan_result";
    }

    public final static class ACTION {
        public final static String CONSUME = "action_consume";
        public final static String OPEN = "action_open";
        public final static String CONSUME_ALL = "action_consume_all";
        public final static String CONSUME_SPOILED = "action_consume_spoiled";
        public final static String PURCHASE = "action_purchase";
        public final static String CREATE = "action_create";
        public final static String EDIT = "action_edit";
        public final static String LINK = "action_link";
        public final static String CREATE_THEN_PURCHASE = "create_then_purchase";
        public final static String CREATE_THEN_PURCHASE_BATCH = "create_then_purchase_batch";
        public final static String CREATE_THEN_SHOPPING_LIST_ITEM_EDIT = "create_then_shopping_list_item_edit";
        public final static String EDIT_THEN_PURCHASE_BATCH = "edit_then_purchase_batch";
        public final static String DELETE_THEN_PURCHASE_BATCH = "delete_then_purchase_batch";
    }

    public final static class SHORTCUT_ACTION {
        public final static String CONSUME = "xyz.zedler.patrick.grocy.action.consume";
        public final static String PURCHASE = "xyz.zedler.patrick.grocy.action.purchase";
        public final static String SHOPPING_LIST = "xyz.zedler.patrick.grocy.action.shoppingList";
        public final static String ADD_ENTRY = "xyz.zedler.patrick.grocy.action.shoppingListEntry";
        public final static String SHOPPING_MODE = "xyz.zedler.patrick.grocy.action.shoppingMode";
    }

    public final static class ARGUMENT {
        public final static String PREFERENCE = "option";
        public final static String ANIMATED = "animated";
        public final static String STOCK_ITEM = "stock_item";
        public final static String QUANTITY_UNIT = "quantity_unit";
        public final static String QUANTITY_UNITS = "quantity_units";
        public final static String QUANTITY_UNIT_PURCHASE = "quantity_unit_purchase";
        public final static String QUANTITY_UNIT_STOCK = "quantity_unit_stock";
        public final static String LOCATION = "location";
        public final static String LOCATIONS = "locations";
        public final static String STORE = "store";
        public final static String STORES = "stores";
        public final static String SHOPPING_LIST = "shopping_list";
        public final static String SHOPPING_LIST_ID = "shopping_list_id";
        public final static String SHOPPING_LIST_NAME = "shopping_list_name";
        public final static String SHOPPING_LISTS = "shopping_lists";
        public final static String PRODUCT_GROUP = "product_group";
        public final static String PRODUCT_GROUPS = "product_groups";
        public final static String STOCK_LOCATIONS = "stock_locations";
        public final static String STOCK_ENTRIES = "product_entries";
        public final static String PRODUCT_DETAILS = "product_details";
        public final static String PRODUCT = "product";
        public final static String PRODUCTS = "products";
        public final static String PRODUCT_NAME = "product_name";
        public final static String OBJECT_NAME = "object_name";
        public final static String OBJECT_ID = "object_id";
        public final static String DEMO_CHOSEN = "demo_chosen";
        public final static String POSITION = "position";
        public final static String PRODUCT_NAMES = "product_names";
        public final static String TYPE = "type";
        public final static String ENTITY_TEXT = "entity_text";
        public final static String SHOW_ACTIONS = "show_actions";
        public final static String SELECTED_ID = "selected_id";
        public final static String PRODUCT_ID = "product_id";
        public final static String SELECTED_DATE = "selected_date";
        public final static String CURRENT_FRAGMENT = "current_fragment";
        public final static String BARCODE = "barcode";
        public final static String SHOPPING_LIST_ITEM = "shopping_list_item";
        public final static String SHOPPING_LIST_ITEMS = "shopping_list_items";
        public final static String BARCODES = "barcodes";
        public final static String CREATE_PRODUCT_OBJECT = "create_product_object";
        public final static String BATCH_ITEMS = "batch_items";
        public final static String ACTION = "action";
        public final static String BUNDLE = "bundle";
        public final static String DEFAULT_BEST_BEFORE_DAYS = "default_best_before_days";
        public final static String PRICE = "price";
        public final static String FACTOR = "factor";
        public final static String AMOUNT = "amount";
        public final static String TEXT = "text";
        public final static String FILE = "file";
        public final static String LINK = "link";
        public final static String HTML = "html";
        public final static String TITLE = "title";
        public final static String HINT = "hint";
        public final static String CURRENCY = "currency";
        public final static String SUPPORTED_VERSIONS = "supported_versions";
        public final static String VERSION = "version";
        public final static String SERVER = "server";
        public final static String KEY = "key";
        public final static String QU_PURCHASE = "quantity_unit_purchase";
        public final static String QU_STOCK = "quantity_unit_stock";
        public final static String SHOW_OFFLINE = "show_offline";
    }

    public final static class FAB {
        public final static class POSITION {
            public final static int GONE = 0;
            public final static int CENTER = 1;
            public final static int END = 2;
        }
        public final static class TAG {
            public final static String ADD = "add";
            public final static String SCAN = "scan";
            public final static String CONSUME = "consume";
            public final static String PURCHASE = "purchase";
            public final static String SAVE = "save";
        }
    }

    public final static class REQUEST {
        public final static int LOGIN = 1;
        public final static int FEATURES = 2;
        public final static int SCAN = 3;
        public final static int SCAN_BATCH = 4;
        public final static int SCAN_PARENT_PRODUCT = 5;
        public final static int SHOPPING_MODE = 6;
    }
}
