# TODO

## Alpha testing text

**Changelog**
- Fixed: Annoying update info was displayed on every start
- Fixed: Crash on quantity units list refresh (#331)
- Fixed: Wrong product was created if name contained "&" (#333)
- Updated translations

## Improvements

Von den fragments die showMessage methods auf activity.showMessage umstellen, weil da noch die AnchorView gesetzt wird.
Ich habe irgendwann mal showMessage mit einer eigenen Snackbar method gemacht

Alle button Höhen anpassen auf match_parent und formatierung anschauen bei langen Strings
Bei Batch Scanner Best before Date bottom sheet wird "speichern" umgebrochen

Im Product overview bottom sheet wird zB. "5 Packungen (2 geöff..." angezeigt, Umbruch nötig
-> in Produktübersicht erledigt

Replace color on_primary with on_background and delete on_primary (it's always the same as on_background)

# ROLL OUT

- versionCode +1
- version name updated?
- latest libs?
- German translation complete?
- changelog added in CHANGELOG.txt?
- changelog added in fastlane/metadata/android/[language]/changelogs/[versionCode].txt?

# Conventions

## View ID naming

`type_layout_name`

- `type`: e.g. `image` for ImageView, `text_input` for TextInputLayout, `text` for TextView
- `layout`: e.g. `consume` for `fragment_consume`, list_item
- `name`: action or other identification, e.g. `amount` for TextInputLayout of amount input
