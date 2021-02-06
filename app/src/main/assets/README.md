# TODO

## Alpha testing text
Thank you, that so many of you want to test the new version while it is in alpha/beta stage!
Here is a changelog for this first alpha:

**Compatibility**
- Compatible with grocy version 3.0.x
- Not compatible anymore with grocy version 2.7.x

**General improvements**
- Improved page loading speed by use of database
- Less data usage by use of database (only fetch new data if necessary)
- Offline usage (no transactions but viewing lists)
- List difference animations are used in every list
- Smooth & consistent navigation between pages
- Product autocomplete lists now use fuzzy finder (idea from grocy/grocy#1275)
- Server requests are now more secure with API key in header
- Added many help texts which are also in the web interface (e.g. on master product pages)
- No more buggy bottom app bar animations
- Fixed layout for text on features pages

**New languages (Thanks for your efforts!)**
- Russian
- Ukrainian

**New pages**
- Start overview
- Product create/edit
- External scanner

**Removed pages**
- Batch mode (consume & purchase forms now behave like web interface and have a scan mode instead)
- Missing batch items (unnecessary without batch mode)

**Rewritten & strongly edited pages**
- Purchase
- Consume
- Settings (with lots of new options)
- Login (New login flow for QR codes from grocy server)

**:warning:Not fully working yet (coming in later test versions):warning:**
Please don't report issues yet for these points!
- Purchase action from shopping list not implemented yet
- Stock page
- Product page (barcodes and unit conversions functionality not implemented yet)
- Settings page (not all options implemented yet)
- Tor and SOCKS support (not implemented yet)
- **Deactivated features: if Due Date Tracking or Location Tracking feature is disabled, there can be misbehavior!**
- Online->offline & offline->online changes a bit buggy

**Testing**
I you want to test the first alpha version (v2.0.0_alpha1), you can download the attached APK file now and install it on your device. You may have to enable an option in your device's settings with a name like "Allow from unknown sources" or if you have a newer Android version there might be a popup with a toggle "Allow from this source".
You can install the APK over your current installation, because it has the same signature.
After update, some or all app preferences may have the default status because their identifiers changed internally and I won't write migrations for them.

Please report any bug or misbehavior in a [new issue](https://github.com/patzly/grocy-android/issues) **if there isn't already an open one**.
New or rewritten pages may also contain issues which were fixed in previous releases (so they may have a closed issue here) – please report them too in a new issue.

If you just want to say thanks for my work and this new version or motivate us, you can write a comment in the new issue #276.
Thanks!

**Translating**
There are lots of new string to translate, so you can start translating them on [Transifex](https://www.transifex.com/grocy-android/grocy-android). :-)

---
Sorry to the guys whose comments I marked as off-topic or spam: It was not against you, but I did it because else maybe more people would answer the same and spam email notifications to everyone like @towo wrote.

And sorry, my English is not *brilliant* yet... If your mother tongue is English and you find language mistakes in the app, please report them also in the issue tracker.

And finally, sorry for the long waiting time – I'm studying computer science since a few months now and hence I don't have that much time anymore to develop this app.

Best regards
Dominic & Patrick

## Improvements

Von den fragments die showMessage methods auf activity.showMessage umstellen, weil da noch die AnchorView gesetzt wird.
Ich habe irgendwann mal showMessage mit einer eigenen Snackbar method gemacht

Alle button Höhen anpassen auf match_parent und formatierung anschauen bei langen Strings
Bei Batch Scanner Best before Date bottom sheet wird "speichern" umgebrochen

Im Product overview bottom sheet wird zB. "5 Packungen (2 geöff..." angezeigt, Umbruch nötig
-> in Produktübersicht erledigt

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
