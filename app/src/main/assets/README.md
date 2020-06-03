# TODO (sorted by priority)

## Bugs

- don't refresh on device rotation (but replace layout, so no manifest declaration)

## Improvements

- server settings for default values in settings
- product edit with location tracking disabled: space between barcode chips and other field

## Features

- batch consume: icon (like flash toggle) for opening product, default is off for each scan
- illustrations: offline, empty
- master product group sheet: go to master products with current product filter applied
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