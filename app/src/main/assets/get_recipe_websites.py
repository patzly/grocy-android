#!/usr/bin/env python

# last update of translators list (res/raw/recipe_websites.txt):
# 05/02/23  (MM/DD/YY)

import requests
import sys

r = requests.get(url='https://raw.githubusercontent.com/hhursev/recipe-scrapers/main/README.rst')
if r.status_code != 200:
    print(f"Could not download data. Error code: {r.status_code}")
    sys.exit(1)

websites = (r.text
            .split("Scrapers available for:")[1]
            .split("(*) offline saved files only")[0]
            .split("-----------------------")[1]
            .strip().split("\n"))

for website in websites:
    print("> " + website
          .removeprefix("- `")
          .removesuffix(" (*)")
          .removesuffix(">`_")
          .split(" <")[1] + "\n")
