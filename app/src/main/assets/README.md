# TODO

## Bugs

- empty state should show empty filter if location or product filter is active
- style doesn't change on rotation! Other method is required (save instance state)
- on emulator at startup the scrollview is not at top?!

## Improvements

- replace all `if(DEBUG) Log.e()` with log.e
- replace all `printToStackTrace` with `log.e`
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