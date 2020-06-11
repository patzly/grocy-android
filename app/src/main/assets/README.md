# TODO

## Bugs

- fix nav bar divider in landscape after rotation in search mode
- type search -> rotate -> dismiss search -> rotate -> search cannot be set up

## Improvements

- link to grocy.info on login screen
- replace self-signed certificates description
- save and restore instance state, migrate to view binding
--- master product group
--- master quantity unit
--- missing batch items
--- shopping list edit
--- shopping list               UNFINISHED -> offline support
--- shopping list item edit
- product edit with location tracking disabled: space between barcode chips and other field

## Features

- translation contribution
- self-signed certificates
- help page with note that buttons can be long pressed to get a tip
- extended product editing

# ROLL OUT

- versionCode +1
- version name updated?
- latest libs?
- changelog added?
- DEBUG off for unneeded classes?

# Conventions

## View ID naming

`type_layout_name`

- `type`: e.g. `image` for ImageView, `text_input` for TextInputLayout, `text` for TextView
- `layout`: e.g. `consume` for `fragment_consume`, list_item
- `name`: action or other identification, e.g. `amount` for TextInputLayout of amount input