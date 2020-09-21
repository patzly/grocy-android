# TODO

## Bugs

## Improvements

- Bei Intents keine großen Objektelisten wie z.B. Products übertragen, nur productNames
- turn error layout into custom view ErrorState and add view methods for control
- turn empty state layout into custom view EmptyState and add view methods for control
- missing batch items empty state -> all done/purchased
- replace all getObject (from Id) with global HashMaps and fill them once if they are empty
- product edit with location tracking disabled: space between barcode chips and other field

Von den fragments die showMessage methods auf activity.showMessage umstellen, weil da noch die AnchorView gesetzt wird.
Ich habe irgendwann mal showMessage mit einer eigenen Snackbar method gemacht

Alle button Höhen anpassen auf match_parent und formatierung anschauen bei langen Strings
Bei Batch Scanner Best before Date bottom sheet wird "speichern" umgebrochen

Im Product overview bottom sheet wird zB. "5 Packungen (2 geöff..." angezeigt, Umbruch nötig
-> in Produktübersicht erledigt

## Features

- self-signed client certificates
- extended product editing

# ROLL OUT

- versionCode +1
- version name updated?
- latest libs?
- German translation complete?
- changelog added in CHANGELOG.txt?
- changelog added in fastlane/metadata/android/[language]/changelogs/[versionCode].txt

# Conventions

## View ID naming

`type_layout_name`

- `type`: e.g. `image` for ImageView, `text_input` for TextInputLayout, `text` for TextView
- `layout`: e.g. `consume` for `fragment_consume`, list_item
- `name`: action or other identification, e.g. `amount` for TextInputLayout of amount input
