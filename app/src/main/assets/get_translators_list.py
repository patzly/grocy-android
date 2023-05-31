#!/usr/bin/python3

# last update of plural rules in PluralUtils: 05/11/23  (MM/DD/YY)
# don't forget to add case "en" to "de", "es", ... Else "en" would be an unsupported language

# last update of translators list (res/raw/locales.txt): 05/11/23  (MM/DD/YY)

# https://developers.transifex.com/reference/get_projects-project-id-languages
# https://developers.transifex.com/reference/get_team-memberships

import requests
import sys
import argparse


parser = argparse.ArgumentParser(description='Print list of translators from Transifex grocy-android project.')
parser.add_argument('token', metavar='token', type=str,
                    help='a Transifex API token for a user which has the rights to display all translators')
args = parser.parse_args()

headers = {
    "accept": "application/vnd.api+json",
    "authorization": f"Bearer {args.token}"
}
r = requests.get(url='https://rest.api.transifex.com/projects/o%3Apatzly%3Ap%3Agrocy-android/languages', headers=headers)
if r.status_code != 200:
    print(f"Could not download language list. Error code: {r.status_code}")
    sys.exit(1)

languages: list = r.json()["data"]
languages.sort(key = lambda lang : lang['id'])

lang_code_short_last = None

for lang in languages:
    lang_code_short = lang['id'].split(':')[1].split('_')[0]
    if lang_code_short_last is not None \
            and lang_code_short == lang_code_short_last:
        continue
    plural_equation = lang['attributes']['plural_equation']
    if plural_equation == "(n != 1)":
        plural_equation = "(n != 1) ? 1 : 0"
    if plural_equation == "(n > 1)":
        plural_equation = "(n > 1) ? 1 : 0"
    print(f"""
    case "{lang['id'].split(':')[1].split('_')[0]}":
        pluralDetails = new LangPluralDetails(
            {len(lang['attributes']['plural_rules'])},
            n -> {plural_equation}
        );
        break;""")
    lang_code_short_last = lang_code_short

print("\n\n")

r = requests.get(url='https://rest.api.transifex.com/team_memberships?filter[organization]=o%3Apatzly&filter[team]=o%3Apatzly%3At%3Agrocy-android-team&filter[role]=translator', headers=headers)
if r.status_code != 200:
    print(f"Could not download team membership list. Error code: {r.status_code}")
    sys.exit(1)

memberships: list = r.json()["data"]

while r.json()["links"].get("next"):
    r = requests.get(url=r.json()["links"].get("next"), headers=headers)
    if r.status_code != 200:
        print(f"Could not download team membership list with pagination. Error code: {r.status_code}")
        sys.exit(1)
    memberships.extend(r.json()["data"])

translators = {}

for membership in memberships:
    language_code = membership["relationships"]["language"]["data"]["id"].split(":")[1]
    if not translators.get(language_code):
        translators[language_code] = list()
    translators[language_code].append(membership["relationships"]["user"]["data"]["id"].split(":")[1])


for lang, users in translators.items():
    translators_str: str = ""
    for translator in users:
        translators_str += f"{translator}"
        if translator != users[-1]:
            translators_str += ", "
    print(f"{lang}")
    print(f"{translators_str}\n")
