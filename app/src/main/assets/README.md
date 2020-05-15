# TODO

## Features
- add and edit shopping lists and shopping list entries
- scanning: indicate type (consume or purchase) and maybe allow switching
- app shortcut: new shopping list entry
- maybe open food facts implementation for product naming with barcode
- HELP page with note that all buttons can be long pressed to get a tip
- translations

## Improvements
- api: inner classes for categories
- hide `<br>` in product description
- use server settings and configs
- shorter tags & ids for master edit
- id naming convention: `frame_back_...` to `frame_..._back`, same with close and `linear_app_bar_..._default` to `linear_..._app_bar_default`
- shopping list doesn't show items without name & description, but it shows items with only description?
- average shelf life is empty in product overview when expired
- fix color contrast of retro colors with text: [contrast checker](https://webaim.org/resources/contrastchecker/)
- thanks to grocy author in ABOUT

## Illustrations
- offline
- search not found
- unknown error
- empty

## View id naming conventions
TYPE_LAYOUT_NAME
- type: e.g. image for ImageView, text_input for TextInputLayout, text for TextView
- layout: e.g. consume for fragment_consume, list_item
- name: action for buttons or other identification, e.g. amount for TextInputLayout of amount input