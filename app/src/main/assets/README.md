# TODO

## Features

- open food facts implementation for product naming with barcode
- help page with note that all buttons can be long pressed to get a tip
- translation contribution
- Batch consume: icon (like flash toggle) for opening product, default is off for each scan
- create and edit shopping lists
- extended product edit
- Master product group sheet: go to master products with this product filter applied
- app setting for shopping list indicator with server integration

## Improvements

- use server settings and configs
- shorter tags & ids for master edit
- Shopping list: make bottom notes clickable
- Always show bottom app bar on new fragment or on back press

## Bugs

- crash on batch activity after press back from "product edit" or "add to shopping list"

## Illustrations

- offline
- search not found
- unknown error
- empty

## View ID naming convention

`type_layout_name`

- `type`: e.g. `image` for ImageView, `text_input` for TextInputLayout, `text` for TextView
- `layout`: e.g. `consume` for `fragment_consume`, list_item
- `name`: action or other identification, e.g. `amount` for TextInputLayout of amount input