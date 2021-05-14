#!/usr/bin/python3

# last update of translators list (locales.txt): 05/15/21

import requests
import sys
import argparse


parser = argparse.ArgumentParser(description='Print list of translators from Transifex grocy-android project.')
parser.add_argument('token', metavar='token', type=str,
                    help='a Transifex API token for a user which has the rights to display all translators')
args = parser.parse_args()

r = requests.get(url='https://www.transifex.com/api/2/project/grocy-android/languages/', auth=('api', args.token))
if r.status_code != 200:
    print(f"Could not download list. Error code: {r.status_code}")
    sys.exit(1)

languages: list = r.json()

for lang in languages:
    print(f"language code: {lang['language_code']}")
    translators: str = ""
    for translator in lang["translators"]:
        translators += f"{translator} "
    print(f"translators: {translators}\n")
