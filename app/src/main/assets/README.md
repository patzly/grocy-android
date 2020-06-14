# TODO

## Bugs

## Improvements

- Replace DEBUG with debug from shared prefs
- offline indicator in shopping mode
- turn error layout into custom view ErrorState and add view methods for control
- turn empty state layout into custom view EmptyState and add view methods for control
- missing batch items empty state -> all done/purchased
- replace all getObject (from Id) with global HashMaps and fill them once if they are empty
- login: check server version with demo server, too
- product edit with location tracking disabled: space between barcode chips and other field

## Features

- translation contribution: transifex like grocy
- self-signed client certificates
- extended product editing

# ROLL OUT

- versionCode +1
- version name updated?
- latest libs?
- changelog added?

# Conventions

## View ID naming

`type_layout_name`

- `type`: e.g. `image` for ImageView, `text_input` for TextInputLayout, `text` for TextView
- `layout`: e.g. `consume` for `fragment_consume`, list_item
- `name`: action or other identification, e.g. `amount` for TextInputLayout of amount input