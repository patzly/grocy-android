# TODO

## Bugs

- shopping list edit after rotation showing error when try to save
- restore sort icon

## Improvements

- turn error layout into custom view ErrorState and add view methods for control
- turn empty state layout into custom view EmptyState and add view methods for control
- missing batch items empty state -> all done/purchased
- product edit with location tracking disabled: space between barcode chips and other field

## Features

- translation contribution: transifex like grocy
- self-signed client certificates
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