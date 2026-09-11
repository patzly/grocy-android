<a name="pagetop"></a>
# Grocy Android Support

If you have a question, please check the frequently asked questions below first.
At the bottom, you can find out how to ask other questions, request features, and report bugs.

Credits for the layout and some text of this FAQ go to [M66B](https://github.com/M66B) and his [FairEmail](https://github.com/M66B/FairEmail) repo.

## Index

* [How to](#how-to)
* [Planned features](#planned-features)
* [Frequently Asked Questions](#frequently-asked-questions)
* [Deeplinks](#deeplinks)
* [Support](#support)

<a name="howto"></a>

## How to

<a name="howto-consume"></a>
**How to consume a product?**

Every time you consume something, you consume it in Grocy as well.
Ways to consume a product:

- Stock overview: search for the product, swipe right on it and tap the CONSUME icon.
- Stock overview: search for the product, tap on it to open the product overview, then tap the CONSUME icon.
- Tap the green button in stock overview to go into scan mode, make sure you are in CONSUME mode, then scan your product.
- Tap on the CONSUME shortcut of Grocy in your app launcher (since Android 7.1) and scan your product.
- Open the CONSUME page in the bottom drawer and fill out the form, then tap the green button.

<br />

<a name="howto-open"></a>
**How to mark a product as opened?**

Every time you open or broach something, you mark one of it in Grocy as open.
Ways to mark a product as opened:

- Stock overview: search for the product, swipe right on it and tap the OPEN icon.
- Stock overview: search for the product, tap on it to open the product overview, then tap the OPEN icon.
- Tap the green button in stock overview to go into scan mode, tap the CONSUME label to switch to MARK AS OPENED, then scan your product.
- Tap on the CONSUME shortcut of Grocy in your app launcher (since Android 7.1) and continue as described previously.
- Open the CONSUME page in the bottom drawer and fill out the form, then tap the OPEN icon in the bottom bar.

<br />

## Planned features

* Recipes
* Mealplans

Anything on this list is in random order and *might* be added in the near future.

## Frequently Asked Questions

* [(1) Which permissions are needed and why?](#user-content-faq1)
* [(2) How do I know what something does?](#user-content-faq2)
* [(3) My API key is invalid. What can I do?](#user-content-faq3)
* [(4) How can I use this app with Hass.io?](#user-content-faq4)
* [(5) Why are my barcodes unreadable if I use the selfie camera?](#user-content-faq5)
* [(6) What is ML Kit barcode scanner?](#user-content-faq6)
* [(7) How can I connect to my instance through Tor Hidden Service or Proxy?](#user-content-faq7)
* [(8) How can I use my USB or Bluetooth™ barcode scanner device?](#user-content-faq8)
+ [(9) How can I use an NFC tag to quickly open a page in Grocy?](#user-content-faq9)
[I have another question.](#support)

<br />

<a name="faq1"></a>
**(1) Which permissions are needed and why?**

The following Android permissions are needed:

* *have full network access* (INTERNET): to access your grocy server
* *view network connections* (ACCESS_NETWORK_STATE): to monitor internet connectivity changes
* *have camera access* (CAMERA): to scan barcodes
* *have access to vibration* (VIBRATE): to vibrate after a barcode was detected

<br />

<a name="faq2"></a>
**(2) How do I know what something does?**

Starting with Android 8 you can long press almost every clickable element without a label to see what it will do.

<br />

<a name="faq3"></a>
**(3) My API key is invalid. What can I do?**

Tap CREATE KEY to open the key management page of your Grocy instance.
Create a new key and copy it to your clipboard, then paste it in the key field of Grocy Android and try again.

If you use Hass.io with the Grocy Add-on, read [this](#user-content-faq4) section.

<br />

<a name="faq4"></a>
**(4) How can I use this app with the grocy Home Assistant add-on?**

After tapping "Own server" on the login page, you will be prompted to scan the QR code from an API key of your grocy server. Once you do that, the app will detect that you are using Home Assistant and prompt you to scan the QR code of a long-lived access token from Home Assistant. You can get this in your Home Assistant user profile in the bottom section. Once you scan the QR code, you will be directed to a form where you will need to fill in any missing data. If you have followed the steps up to this point, all that should be left is the server URL of Home Assistant (i.e. the URL where you can reach your Home Assistant instance). Be aware that you may add the port if your server uses another port than 443 or 80 (e.g. the local port of Home Assistant server is 8123).

You can also enter the URL of your Home Assistant Cloud (Nabu Casa), if you use this service. The URL must then be in the form `https://xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.ui.nabu.casa/`. Since typing can cause typos, we recommend copying the URL from somewhere else via Android's clipboard and pasting it into the field.

<br />

<a name="faq5"></a>
**(5) Why are my barcodes unreadable if I use the selfie camera?**

Most devices have a front camera with a fixed focus for the normal distance between your face and the device.
With this distance, the picture resolution is sometimes too low for the scanning algorithm.

<br />

<a name="faq6"></a>
**(6) What is ML Kit barcode scanner?**

ML Kit is an alternative AI-based barcode scanner which is implemented in the Google Play and GitHub version of our app since v2.0.0, F-Droid doesn't allow closed-source app components and thus we can't offer the same version there. ML Kit is from Google and doesn't make recognition mistakes compared to ZXing (the primary scanner option in our app). Additionaly, you don't have to align the barcode horizontally in your camera because it recognizes barcodes with any orientation. Go to `app settings -> Scanner -> Barcode scanner` to use ML Kit. You can either purchase the required unlock app in Google Play Store (which would make us happy) or you can download the unlock APK [here](https://github.com/patzly/grocy-android-unlock).

<br />

<a name="faq7"></a>
**(7) How can I connect to my instance through Tor Hidden Service or Proxy?**

Go to `app settings -> Network` and enable Tor support or HTTP-Proxy. Orbot is required for Tor support.

<br />

<a name="faq8"></a>
**(8) How can I use my USB or Bluetooth™ barcode scanner device?**

Go to `app settings -> Scanner` and enable support for external scanners. Then you can connect a scanner for example using USB or Bluetooth™ and use it on the purchase/consume/transfer/inventory pages (the product field must be focused). Your scanner should have TAB or Enter as suffix, otherwise the workflow won't start after a scan.

<a name="faq9"></a>
**(9) How can I use an NFC tag to quickly open a page in Grocy?**

See [deeplinks](#deeplinks)

## Deeplinks

You can open a desired page of the app using an URI. This could be written e.g. into a NFC tag for
easy navigation to the consume page or other pages in the app using physical interaction.

#### URIs:

- grocy://stockOverviewFragment
- grocy://stockOverviewFragment?statusFilterId={statusFilterId}
- grocy://shoppingListFragment
- grocy://shoppingListItemEditFragment?action=create
- grocy://shoppingModeFragment
- grocy://purchaseFragment
- grocy://consumeFragment
- grocy://transferFragment
- grocy://inventoryFragment
- grocy://settingsFragment
- grocy://tasksFragment
- grocy://taskEntryEditFragment?action=create
- grocy://choresFragment
- grocy://choresFragment?statusFilterId={statusFilterId}
- grocy://recipesFragment
- grocy://masterDataOverviewFragment
- grocy://stockEntriesFragment

The value of parameter statusFilterId for the stockOverviewFragment URI can be 0 for all items,
1 for due soon, 2 for overdue, 3 for expired, 4 for below min. stock amount, 5 for in stock, 6 for
opened and 7 for "not fresh" combination. The value of parameter statusFilterId for the
choresFragment URI can be 0 for all items, 1 for due today, 2 for due soon, 3 for overdue
and 4 for due.

## Support

Please go to our [project page](https://github.com/patzly/grocy-android) on GitHub and open a new issue with your question.
We will try to help you as fast as possible.
