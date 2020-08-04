<img src="assets/header.png" />

# Grocy Android

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)  [![Release](https://img.shields.io/github/v/release/patzly/grocy-android?label=Release)](https://github.com/patzly/grocy-android/releases)  [![APK Downloads](https://img.shields.io/github/downloads/patzly/grocy-android/total.svg?label=APK%20Downloads)](https://github.com/patzly/grocy-android/releases)

[Grocy](https://grocy.info/) is a self-hosted groceries & household management solution for your home.

Grocy Android uses grocy's official API to provide you a beautiful interface on your smartphone with powerful barcode scanning and intuitive batch processing, all what you need to efficiently manage your groceries.

**This app requires a running self-hosted instance of the [grocy server application](https://grocy.info/).**  
It is a **companion** app, therefore it **cannot run standalone** or manage products itself.  
You can try it using the demo option available on the login screen.

## Downloads

<a href='https://play.google.com/store/apps/details?id=xyz.zedler.patrick.grocy'><img alt='Get it on Google Play' height="80" src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'/></a><a href='https://github.com/patzly/grocy-android/releases'><img alt='Get it on GitHub' height="80" src='assets/badge_github.png'/></a><a href='#'><img alt='Get it on F-Droid' height="80" src='assets/badge_fdroid.png'/></a><a href='https://apt.izzysoft.de/fdroid/index/apk/xyz.zedler.patrick.grocy'><img alt='Get it on IzzyOnDroid' height="80" src='https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png'/></a>

## Screenshots

<a href="#"><img src="assets/screen1.png" width="220px"/></a><a href="#"><img src="assets/screen2.png" width="220px"/></a>

## Features

* Stock overview
* Shopping lists with offline support
* In-store shopping mode (big UI elements)
* Fast barcode scanning
* Open and consume with one swipe
* Consume and purchase with app shortcuts
* OpenFoodFacts implementation
* Master data editing
* Intuitive batch processing
* Dark mode
* No ads, analytics or in-app purchases
* Low data usage
* Tiny app size (~5MB)

## Upcoming

Grocy Android is still under development. In the next weeks we're going to implement some more features!

* Recipes
* Meal plans
* Extended product editing

## Compatibility

Grocy Android requires at least Android 5.0 Lollipop on your device and [grocy](https://github.com/grocy/grocy/releases) `2.7.0` on your server.  
It is also possible to use the grocy Add-on on a [Hass.io](https://www.home-assistant.io/hassio/) server. Our In-app help explains how to do that.

**Self-signed certificates are not supported at the moment** but an implementation is planned.  
Right now, the server certificate has to be signed by a [certificate authority (CA)](https://en.wikipedia.org/wiki/Certificate_authority). This CA has to be public and trusted by Android.  
To meet these requirements, you can use a free certificate from [letsencrypt.org](https://letsencrypt.org/) for proper `https` encryption.

## Contribution

If you run into a bug or miss a feature, feel free to give feedback in the app, [send us an email](mailto:patrick@zedler.xyz) or [open an issue](https://github.com/patzly/grocy-android/issues/new) in this repository.

Like the grocy project, Grocy Android can be translated, too. The main language is English, but we also maintain the German translation because it's our mother tongue.
You can help translating this project at [Transifex](https://www.transifex.com/grocy-android/grocy-android), if your language is incomplete or not available yet.
Translations which reached a completion level of 80% will be included in releases.

Completed translations: German, French, Swedish & Dutch

## Acknowledgement

We'd like to thank the developer of grocy, [Bernd Bestel](https://berrnd.de/), without whose great work this app would never have been possible.

## License

Copyright &copy; 2020 Patrick Zedler & Dominic Zedler. All rights reserved.

[GNU General Public License version 3](https://www.gnu.org/licenses/gpl.txt)

> Grocy Android is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
>
> Grocy Android is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
