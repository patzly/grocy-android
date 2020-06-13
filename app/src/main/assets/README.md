# TODO

## Bugs

## Improvements

- turn error layout into custom view ErrorState and add view methods for control
- turn empty state layout into custom view EmptyState and add view methods for control
- Maybe a simpler solution for fragment navigation: https://github.com/Zhuinden/simple-stack/
  A medium article about it:
  https://medium.com/@Zhuinden/simplified-fragment-navigation-using-a-custom-backstack-552e06961257
- completed fragments
--- stock [x]
--- shopping list item edit [x]
--- shopping list [x]
--- consume [x]
--- location [x]
--- locations [x]
--- missing batch items [x]
--- product group [x]
- save and restore instance state, migrate to view binding
--- shopping list edit
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