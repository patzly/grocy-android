# TODO

## Bugs

- fix nav bar divider in landscape after rotation in search mode
- type search -> rotate -> dismiss search -> rotate -> search cannot be set up

## Improvements

- save and restore instance state, migrate to view binding
--- consume                     DONE
--- master location
--- master locations            DONE
--- master product group
--- master product groups       DONE
--- master product
--- master products             DONE
--- master quantity unit
--- master quantity units       DONE
--- master store
--- master stores
--- missing batch items
--- purchase                    DONE
--- shopping list edit
--- shopping list               UNFINISHED -> offline support
--- shopping list item edit
--- stock                       DONE
- product edit with location tracking disabled: space between barcode chips and other field

## Features

- in-store mode for shopping list with big items
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