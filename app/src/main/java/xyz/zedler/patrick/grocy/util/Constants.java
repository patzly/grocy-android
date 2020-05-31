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

        public final static String STOCK_SORT_MODE = "stock_sort_mode";
        public final static String STOCK_SORT_ASCENDING = "stock_sort_ascending";

        public final static String STOCK_EXPIRING_SOON_DAYS = "stock_expring_soon_days";
        public final static String STOCK_DEFAULT_PURCHASE_AMOUNT = "stock_default_purchase_amount";
        public final static String STOCK_DEFAULT_CONSUME_AMOUNT = "stock_default_consume_amount";
        public final static String PRODUCT_PRESETS_LOCATION_ID = "product_presets_location_id";
        public final static String PRODUCT_PRESETS_PRODUCT_GROUP_ID = "product_presets_product_group_id";
        public final static String PRODUCT_PRESETS_QU_ID = "product_presets_qu_id";
        public final static String SHOW_SHOPPING_LIST_ICON_IN_STOCK = "show_icon_on_stock_overview_page_when_product_is_on_shopping_list";
        public final static String RECIPE_INGREDIENTS_GROUP_BY_PRODUCT_GROUP = "recipe_ingredients_group_by_product_group";

        public final static String BATCH_CONFIG_BBD = "batch_config_bbd";
        public final static String BATCH_CONFIG_PRICE = "batch_config_price";
        public final static String BATCH_CONFIG_STORE = "batch_config_store";
        public final static String BATCH_CONFIG_LOCATION = "batch_config_location";
        public final static String BATCH_CONFIG_STOCK_LOCATION = "batch_config_stock_location";
        public final static String BATCH_CONFIG_SPECIFIC = "batch_config_specific";

        public final static String DARK_MODE = "force_dark_mode";
        public final static String FOOD_FACTS = "use_open_food_facts";

        public final static String INTRO_SHOWN = "intro_shown";
    }

    public final static class DATE {
        public final static String NEVER_EXPIRES = "2999-12-31";
    }

    public final static class UI {
        public final static String STOCK = "stock";
        public final static String STOCK_DEFAULT = "stock_default";
        public final static String STOCK_SEARCH	= "stock_search";
        public final static String SHOPPING_LIST = "shopping_list";
        public final static String SHOPPING_LIST_DEFAULT = "shopping_list_default";
        public final static String SHOPPING_LIST_SEARCH = "shopping_list_search";
        public final static String SHOPPING_LIST_ITEM_EDIT = "shopping_list_item_edit";
        public final static String CONSUME = "consume";
        public final static String PURCHASE = "purchase";
        public final static String MASTER = "master";
        public final static String MASTER_PRODUCTS = "master_products";
        public final static String MASTER_PRODUCTS_DEFAULT = "master_products_default";
        public final static String MASTER_PRODUCTS_SEARCH = "master_products_search";
        public final static String MASTER_PRODUCT_EXTENDED = "master_product_extended";
        public final static String MASTER_PRODUCT_SIMPLE = "master_product_simple";
        public final static String MASTER_LOCATIONS = "master_locations";
        public final static String MASTER_LOCATIONS_DEFAULT = "master_locations_default";
        public final static String MASTER_LOCATIONS_SEARCH = "master_locations_search";
        public final static String MASTER_LOCATION = "master_location";
        public final static String MASTER_STORES = "master_stores";
        public final static String MASTER_STORES_DEFAULT = "master_stores_default";
        public final static String MASTER_STORES_SEARCH = "master_stores_search";
        public final static String MASTER_STORE = "master_store";
        public final static String MASTER_QUANTITY_UNITS = "master_quantity_units";
        public final static String MASTER_QUANTITY_UNITS_DEFAULT = "master_quantity_units_default";
        public final static String MASTER_QUANTITY_UNITS_SEARCH = "master_quantity_units_search";
        public final static String MASTER_QUANTITY_UNIT = "master_quantity_unit";
        public final static String MASTER_PRODUCT_GROUPS = "master_product_groups";
        public final static String MASTER_PRODUCT_GROUPS_DEFAULT = "master_product_groups_default";
        public final static String MASTER_PRODUCT_GROUPS_SEARCH = "master_product_groups_search";
        public final static String MASTER_PRODUCT_GROUP = "master_product_group";
        public final static String BATCH_SCAN = "batch_scan";
        public final static String MISSING_BATCH_ITEMS = "missing_batch_products";
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

    public final static class SHOPPING_LIST {
        public final static class FILTER {
            public final static String UNDONE = "undone";
            public final static String MISSING = "missing";
            public final static String ALL = "all";
        }
    }

    public final static class EXTRA {
        public final static String SCAN_RESULT = "scan_result";
        public final static String FLASH_VIEW_ID = "flash_view_id";
        public final static String AFTER_FEATURES_ACTIVITY = "after_features_activity";
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
        public final static String CREATE_FROM_STOCK = "action_create_from_stock";
        public final static String CREATE_THEN_PURCHASE = "create_then_purchase";
        public final static String CREATE_THEN_PURCHASE_BATCH = "create_then_purchase_batch";
        public final static String CREATE_THEN_SHOPPING_LIST_ITEM = "create_then_shopping_list_item";
        public final static String EDIT_THEN_PURCHASE_BATCH = "edit_then_purchase_batch";
        public final static String DELETE_THEN_PURCHASE_BATCH = "delete_then_purchase_batch";
        public final static String PURCHASE_THEN_SHOPPING_LIST = "purchase_then_shopping_list";
        public final static String PURCHASE_THEN_STOCK = "purchase_then_stock";
        public final static String CONSUME_THEN_STOCK = "consume_then_stock";
    }

    public final static class SHORTCUT_ACTION {
        public final static String CONSUME = "xyz.zedler.patrick.grocy.action.consume";
        public final static String PURCHASE = "xyz.zedler.patrick.grocy.action.purchase";
        public final static String SHOPPING_LIST = "xyz.zedler.patrick.grocy.action.shoppingList";
        public final static String ADD_ENTRY = "xyz.zedler.patrick.grocy.action.shoppingListEntry";
    }

    public final static class ARGUMENT {
        public final static String UI_MODE = "ui_mode";
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
        public final static String SHOPPING_LISTS = "shopping_lists";
        public final static String PRODUCT_GROUP = "product_group";
        public final static String PRODUCT_GROUPS = "product_groups";
        public final static String STOCK_LOCATIONS = "stock_locations";
        public final static String STOCK_ENTRIES = "product_entries";
        public final static String PRODUCT_DETAILS = "product_details";
        public final static String PRODUCT = "product";
        public final static String PRODUCTS = "products";
        public final static String PRODUCT_NAME = "product_name";
        public final static String POSITION = "position";
        public final static String PRODUCT_NAMES = "product_names";
        public final static String TYPE = "type";
        public final static String SET_UP_WITH_PRODUCT_DETAILS = "set_up_with_product_details";
        public final static String SHOW_ACTIONS = "show_actions";
        public final static String SELECTED_ID = "selected_id";
        public final static String PRODUCT_ID = "product_id";
        public final static String SELECTED_DATE = "selected_date";
        public final static String CURRENT_FRAGMENT = "current_fragment";
        public final static String BARCODE = "barcode";
        public final static String SHOPPING_LIST_ITEM = "shopping_list_item";
        public final static String BARCODES = "barcodes";
        public final static String CREATE_PRODUCT_OBJECT = "create_product_object";
        public final static String BATCH_ITEMS = "batch_items";
        public final static String ACTION = "action";
        public final static String BUNDLE = "bundle";
        public final static String DEFAULT_BEST_BEFORE_DAYS = "default_best_before_days";
        public final static String PRICE = "price";
        public final static String AMOUNT = "amount";
        public final static String NOTE = "note";
        public final static String TEXT = "text";
        public final static String FILE = "file";
        public final static String LINK = "link";
        public final static String HTML = "html";
        public final static String TITLE = "title";
        public final static String HINT = "hint";
        public final static String STATUS = "status";
        public final static String CURRENCY = "currency";
        public final static String SUPPORTED_VERSIONS = "supported_versions";
        public final static String VERSION = "version";
        public final static String SERVER = "server";
        public final static String KEY = "key";
        public final static String QU_PURCHASE = "quantity_unit_purchase";
        public final static String QU_STOCK = "quantity_unit_stock";
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
    }
}
