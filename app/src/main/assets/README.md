# TODO

## upcoming changelog lines:

## Android 12 and Material You

1. Implement edge to edge behavior (including for BottomAppBar)
2. Replace LinearLayouts in app bars with full-width MaterialToolbar
3. Replace MaterialComponents themes with Material3 and implement attribute themes
4. Replace all color references with attributes
5. Replace all text appearances with Material3 text styles
6. Implement different color themes
7. Optimize splashscreen for Android 12

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
- changelog added in changelog.txt?
- changelog added in fastlane/metadata/android/[language]/changelogs/[versionCode].txt?

# Conventions

## View ID naming

`type_layout_name`

- `type`: e.g. `image` for ImageView, `text_input` for TextInputLayout, `text` for TextView
- `layout`: e.g. `consume` for `fragment_consume`, list_item
- `name`: action or other identification, e.g. `amount` for TextInputLayout of amount input

## Transifex config

filters:
  - filter_type: file
    file_format: ANDROID
    source_language: en
    source_file: app/src/main/res/values/strings.xml
    translation_files_expression: 'app/src/main/res/values-<lang>/strings.xml'
settings:
  language_mapping:
    ca_ES: ca-rES
    el_GR: el-rGR
    en_BE: en-rBE
    es_DO: es-rDO
    he: iw
    it_IT: it-rIT
    ko_KR: ko-rKR
    nl_BE: nl-rBE
    nl_NL: nl-rNL
    pl_PL: pl-rPL
    pt_BR: pt-rBR
    pt_PT: pt-rPT
    ru_RU: ru-rRU
    ru_UA: ru-rUA
    zh_CN: zh-rCN
    zh_TW: zh-rTW