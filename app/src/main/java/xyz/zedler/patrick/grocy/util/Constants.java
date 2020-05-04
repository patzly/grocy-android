package xyz.zedler.patrick.grocy.util;

public final class Constants {

    public final static class PREF {
        public final static String SERVER_URL = "server_url";
        public final static String API_KEY = "api_key";
        public final static String CURRENCY = "currency";
        public final static String GROCY_VERSION = "grocy_version";

        public final static String STOCK_SORT_MODE = "stock_sort_mode";
        public final static String STOCK_SORT_ASCENDING = "stock_sort_ascending";

        public final static String PRODUCT_PRESETS_LOCATION_ID = "product_presets_location_id";
        public final static String PRODUCT_PRESETS_PRODUCT_GROUP_ID = "product_presets_product_group_id";
        public final static String PRODUCT_PRESETS_QU_ID = "product_presets_qu_id";
        public final static String STOCK_EXPIRING_SOON_DAYS = "stock_expring_soon_days";
        public final static String STOCK_DEFAULT_PURCHASE_AMOUNT = "stock_default_purchase_amount";
        public final static String STOCK_DEFAULT_CONSUME_AMOUNT = "stock_default_consume_amount";
        public final static String SHOW_SHOPPING_LIST_ICON_IN_STOCK = "show_icon_on_stock_overview_page_when_product_is_on_shopping_list";
        public final static String RECIPE_INGREDIENTS_GROUP_BY_PRODUCT_GROUP = "recipe_ingredients_group_by_product_group";

        public final static String DARK_MODE = "force_dark_mode";
        public final static String ANIM_UI_UPDATE = "animated_ui_update";

        public final static String WEAR_POP_UP_COUNT = "wear_pop_up_count";
        public final static String SHOW_HELP = "show_help";
        public final static String SHOW_GUIDELINE = "show_guideline";
        public final static String EXTERNAL_BROWSER = "external_browser";
        public final static String GUIDELINE_PAGE = "guideline_page";
        public final static String USE_KEYBOARD = "use_keyboard";
        public final static String SHOW_ARROWS = "show_arrows";
        public final static String INTRO_SHOWN = "intro_shown";
    }

    public final static class DATE {
        public final static String NEVER_EXPIRES = "2999-12-31";
    }

    public final static class BOTTOM_SHEET_TEXT {
        public final static String TITLE = "title";
        public final static String FILE = "file";
        public final static String BIG_TEXT = "big";
        public final static String LINK = "link";
    }

    public final static class UI {
        public final static String STOCK = "stock";
        public final static String STOCK_DEFAULT = "stock_default";
        public final static String STOCK_SEARCH	= "stock_search";
        public final static String CONSUME = "consume";
        public final static String PURCHASE = "purchase";
        public final static String MASTER = "master";
        public final static String MASTER_PRODUCTS = "master_products";
        public final static String MASTER_PRODUCTS_DEFAULT = "master_products_default";
        public final static String MASTER_PRODUCTS_SEARCH = "master_products_search";
        public final static String MASTER_PRODUCT_EDIT_EXTENDED = "master_product_edit_extended";
        public final static String MASTER_PRODUCT_EDIT_SIMPLE = "master_product_edit_simple";
        public final static String MASTER_LOCATIONS = "master_locations";
        public final static String MASTER_LOCATIONS_DEFAULT = "master_locations_default";
        public final static String MASTER_LOCATIONS_SEARCH = "master_locations_search";
        public final static String MASTER_LOCATION_EDIT = "master_location";
        public final static String MASTER_STORES = "master_stores";
        public final static String MASTER_STORES_DEFAULT = "master_stores_default";
        public final static String MASTER_STORES_SEARCH = "master_stores_search";
        public final static String MASTER_STORE_EDIT = "master_store";
        public final static String MASTER_QUANTITY_UNITS = "master_quantity_units";
        public final static String MASTER_QUANTITY_UNITS_DEFAULT = "master_quantity_units_default";
        public final static String MASTER_QUANTITY_UNITS_SEARCH = "master_quantity_units_search";
        public final static String MASTER_QUANTITY_UNIT_EDIT = "master_quantity_unit";
        public final static String MASTER_PRODUCT_GROUPS = "master_product_groups";
        public final static String MASTER_PRODUCT_GROUPS_DEFAULT = "master_product_groups_default";
        public final static String MASTER_PRODUCT_GROUPS_SEARCH = "master_product_groups_search";
        public final static String MASTER_PRODUCT_GROUP_EDIT = "master_product_group";
        public final static String BATCH_SCAN = "batch_scan";
        public final static String BATCH_SCAN_BOTTOM_SHEET = "batch_scan_bottom_sheet";
        public final static String MISSING_BATCH_PRODUCTS = "missing_batch_products";
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
            public final static String LAST_PURCHASED = "last_purchased";
        }
    }

    public final static class EXTRA {
        public final static String SCAN_RESULT = "scan_result";
        public final static String FLASH_VIEW_ID = "flash_view_id";
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
    }

    public final static class ARGUMENT {
        public final static String UI_MODE = "ui_mode";
        public final static String STOCK_ITEM = "stock_item";
        public final static String QUANTITY_UNIT = "quantity_unit";
        public final static String QUANTITY_UNITS = "quantity_units";
        public final static String QUANTITY_UNIT_PURCHASE = "quantity_unit_purchase";
        public final static String QUANTITY_UNIT_STOCK = "quantity_unit_stock";
        public final static String LOCATION = "location";
        public final static String LOCATIONS = "locations";
        public final static String STORE = "store";
        public final static String STORES = "stores";
        public final static String PRODUCT_GROUP = "product_group";
        public final static String PRODUCT_GROUPS = "product_groups";
        public final static String STOCK_LOCATIONS = "stock_locations";
        public final static String STOCK_ENTRIES = "product_entries";
        public final static String PRODUCT_DETAILS = "product_details";
        public final static String PRODUCT = "product";
        public final static String PRODUCTS = "products";
        public final static String PRODUCT_NAME = "product_name";
        public final static String PRODUCT_NAMES = "product_names";
        public final static String TYPE = "type";
        public final static String PARENT_PRODUCT = "parent_product";
        public final static String SET_UP_WITH_PRODUCT_DETAILS = "set_up_with_product_details";
        public final static String SHOW_ACTIONS = "show_actions";
        public final static String SELECTED_ID = "selected_id";
        public final static String SELECTED_DATE = "selected_date";
        public final static String CURRENT_FRAGMENT = "current_fragment";
        public final static String BARCODE = "barcode";
        public final static String BARCODES = "barcodes";
        public final static String BATCH_ITEMS = "batch_items";
        public final static String ACTION = "action";
        public final static String BUNDLE = "bundle";
        public final static String DEFAULT_BEST_BEFORE_DAYS = "default_best_before_days";
        public final static String AMOUNT = "amount";
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
            public final static String CREATE_PURCHASE = "create_and_purchase";
            public final static String SAVE = "save";
        }
    }

    public final static class REQUEST {
        public final static int LOGIN = 1;
        public final static int SCAN = 2;
        public final static int SCAN_CONSUME = 3;
        public final static int SCAN_PURCHASE = 4;
        public final static int SCAN_PARENT_PRODUCT = 5;
    }
}
