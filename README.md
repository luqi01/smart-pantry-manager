# Smart Pantry Manager

An Android application, written in Java, that tracks the ingredients you already
have at home and suggests only the recipes you can cook with them right now.

Most recipe apps work the other way round: you pick a dish, then go shopping.
Smart Pantry Manager starts from what is in the cupboard. A recipe appears in
your suggestions only when **every** ingredient it needs is already in your
pantry, in at least the quantity the recipe calls for. Nothing is suggested that
would send you to the shop, which is the point — the food you already own is the
food most likely to be thrown away.

Built for **Mobile App Development 700**, Richfield Graduate Institute of
Technology.

---

## Contents

- [What it does](#what-it-does)
- [The strict-matching rule](#the-strict-matching-rule)
- [Database choice](#database-choice-and-why)
- [Architecture](#architecture)
- [Setup](#setup)
- [Running the tests](#running-the-tests)
- [Project layout](#project-layout)
- [Troubleshooting](#troubleshooting)

---

## What it does

- **Pantry management.** Add, edit and delete ingredients, each with a quantity,
  a unit and an optional expiry date. Items expiring within three days are
  highlighted.
- **Suggested recipes.** Twenty seeded recipes are tested against your pantry.
  Only the ones you can make appear.
- **One short.** A separate list, clearly divided from the suggestions, for
  recipes you are exactly one ingredient short of.
- **Recipe detail.** The full ingredient list and method, with a tick against
  each ingredient you already hold.
- **Settings.** The API address, expiry warnings, whether the One short list is
  shown, and the default unit for new items.

## The strict-matching rule

A recipe is suggested only when every ingredient it requires is present in at
least the required quantity. Four ingredients out of five is not a match.

The naive version of this — comparing ingredient names with `equals` — breaks
immediately on real input. A pantry holding "Tomatoes" would not satisfy a recipe
asking for "tomato", and the user would be told they cannot make something while
looking straight at the ingredients. Three things prevent that:

**Name normalisation** (`logic/IngredientNormaliser.java`) lower-cases, drops
bracketed notes and punctuation, removes preparation words such as *chopped*,
*fresh* and *large*, strips regular plurals, and applies a synonym table so
*mielie meal* and *maize meal*, or *spaghetti* and *pasta*, resolve to one key.

**Unit conversion** (`logic/UnitConverter.java`) converts both sides to a base
unit — grams, millilitres or item count — so 1 kg of cheese satisfies a recipe
asking for 40 g. Where a count meets a mass, a table of average item weights
bridges the two, so two whole onions satisfy a recipe wanting 150 g of onion.

**The rule itself** (`logic/RecipeMatcher.java`) walks every ingredient and
collects the ones that are missing or short. No misses means suggested, exactly
one puts it under One short, two or more keeps it out altogether.

Where a comparison cannot be made honestly — an unknown unit, or a mass against a
volume with no conversion — the ingredient counts as **not** satisfied. Guessing
in the user's favour would quietly break the rule the app exists to enforce.

The matching runs on the device rather than in SQL, because it re-runs on every
pantry edit and the normalisation rules are far easier to read, test and explain
as Java than as a query.

## Database choice, and why

**PostgreSQL 16**, reached through a REST API that ships with this project.
The database runs locally during development and deploys to a managed Supabase
instance by changing one connection string.

The brief allowed SQLite, Firebase or PostgreSQL. PostgreSQL was chosen for three
reasons:

1. **The data is genuinely relational.** A recipe has many ingredients, and
   `recipe_ingredient` is a textbook foreign-key relationship with a cascade
   delete. A document store would have meant nesting ingredients inside recipes
   and losing the ability to query across them.
2. **Constraints belong in the database.** `CHECK (quantity > 0)`, `NOT NULL`,
   and a unique index on the lower-cased ingredient name mean bad data cannot be
   stored even if a bug gets past the app's validation. Two rows for "Eggs" and
   "eggs" would silently break the matching rule, and the index makes that
   impossible rather than unlikely.
3. **It forces a clean separation.** The app cannot hold the database password —
   anyone can unzip an APK and read it. Putting a REST API in between means
   credentials stay on the server and the app talks to a small, purposeful
   interface instead of to tables.

The cost is honest: the app needs a network connection and a running API, where
SQLite would have needed neither. For a pantry that syncs across devices that is
the right trade; for a purely offline app it would not be.

## Architecture

```
Android app  ──HTTP/JSON──>  Flask REST API  ──psycopg2──>  PostgreSQL 16
   (Java)                       (Python)                    (local / Supabase)
```

The app never speaks SQL. It calls the API, which owns every query.

| Layer | Class | Responsibility |
| --- | --- | --- |
| Screens | `MainActivity`, fragments, two activities | Display and user input |
| Adapters | `PantryAdapter`, `RecipeAdapter` | Bind data to RecyclerView rows |
| Logic | `RecipeMatcher`, `IngredientNormaliser`, `UnitConverter` | The strict rule |
| Data | `PantryRepository`, `RecipeRepository` | CRUD in the app's own terms |
| Transport | `ApiClient` | HTTP, JSON, threading, error messages |

Screens never touch `ApiClient` directly. Swapping PostgreSQL for an on-device
database would mean rewriting the data layer and nothing above it.

### Screens

| Screen | Class | Reached by |
| --- | --- | --- |
| Pantry list | `ui/PantryFragment` | Bottom navigation |
| Suggested recipes | `ui/SuggestionsFragment` | Bottom navigation |
| Settings | `ui/SettingsFragment` | Bottom navigation |
| Add / edit ingredient | `AddEditIngredientActivity` | Intent from pantry list |
| Recipe detail | `RecipeDetailActivity` | Intent from suggestions |

The three tabs are fragments because they share one navigation bar. The two that
are separate tasks are activities, launched with explicit Intents carrying data.

## Setup

You need Android Studio (Hedgehog or newer) and Python 3.9+.

### 1. Create the database

1. Sign up at [supabase.com](https://supabase.com) and create a project. Note the
   database password you choose — it cannot be recovered later, only reset.
2. Open **SQL Editor** and run `server/sql/01_schema.sql`.
3. Run `server/sql/02_seed_recipes.sql`. The final query should list twenty
   recipes with their ingredient counts.

### 2. Run the API

```bash
cd server
python -m pip install -r requirements.txt
cp .env.example .env
```

Edit `.env` and paste your connection string. In Supabase it is under
**Project Settings → Database → Connection string → URI**; use the Session pooler
URI and replace `[YOUR-PASSWORD]` with your database password.

```bash
python app.py
```

It listens on port 3000. Check it:

```bash
curl http://localhost:3000/api/health
# {"status":"ok","recipes":20}
```

Leave this running while you use the app.

### 3. Run the app

Open the `android/` folder in Android Studio, let Gradle sync, and run on an
emulator.

The app defaults to `http://10.0.2.2:3000`, which is how the Android emulator
reaches your computer. `localhost` would point the app at the emulator itself.

On a physical device, put your computer's network address in **Settings → API
base URL** (for example `http://192.168.1.50:3000`), with both on the same
Wi-Fi. Note that the app permits plain HTTP only for the development hosts listed
in `res/xml/network_security_config.xml`; anything else must be HTTPS.

**Settings → Test connection** confirms the app can reach the API before you go
looking for problems elsewhere.

### API endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/health` | Connection check and recipe count |
| GET | `/api/pantry` | **Read** all pantry items |
| POST | `/api/pantry` | **Create** an item |
| PUT | `/api/pantry/{id}` | **Update** an item |
| DELETE | `/api/pantry/{id}` | **Delete** an item |
| GET | `/api/recipes` | All recipes with ingredients nested |
| GET | `/api/recipes/{id}` | One recipe |

## Running the tests

Twenty-nine JUnit tests run on the JVM, with no emulator and no database:

```bash
cd android
./gradlew test
```

Fifteen cover the strict-matching rule: the brief's own example (four
ingredients out of five must not be suggested), exact-quantity boundaries,
plural and synonym handling, unit conversion across dimensions, and the refusal
to compare incomparable units. Seven cover what cooking a recipe takes off the
pantry, including the two cases where it must do nothing: an ingredient it
cannot find, and one it cannot honestly convert. The last seven cover how a
quantity is written on a line, where counting units take a plural and measuring
units do not.

## Project layout

```
SmartPantryManager/
├── android/                     Android Studio project (Java)
│   └── app/src/
│       ├── main/java/za/ac/richfield/smartpantry/
│       │   ├── MainActivity.java              host + bottom navigation
│       │   ├── AddEditIngredientActivity.java create / update / delete
│       │   ├── RecipeDetailActivity.java      ingredients + method
│       │   ├── ui/                            the three tab fragments
│       │   ├── adapter/                       RecyclerView adapters
│       │   ├── logic/                         the strict-matching rule
│       │   ├── data/                          repositories + HTTP
│       │   ├── model/                         data classes
│       │   └── util/Prefs.java                SharedPreferences
│       ├── main/res/                          layouts, strings, icons
│       └── test/java/                         JUnit tests
├── server/
│   ├── app.py                   Flask REST API
│   ├── requirements.txt
│   ├── .env.example
│   └── sql/
│       ├── 01_schema.sql        tables, constraints, trigger
│       ├── 02_seed_recipes.sql  20 recipes (generated)
│       └── gen_seed.py          generates the seed above
└── docs/                        report figures and screenshots
```

## Troubleshooting

**"Cannot reach the pantry API"** — the server is not running, or the URL is
wrong. Check `python app.py` is still going, and that Settings shows
`http://10.0.2.2:3000` on an emulator.

**The API will not start, "DATABASE_URL is not set"** — `server/.env` is missing
or empty. Copy `.env.example` and paste your connection string.

**The API starts but every request fails** — the connection string is wrong or
the password contains characters that need URL-encoding. Test it with `psql` or
the Supabase SQL editor first.

**No recipes anywhere** — `02_seed_recipes.sql` has not been run. Check with
`curl http://localhost:3000/api/health`, which reports the recipe count.

**A recipe will not appear even though the ingredients look present** — open the
recipe from One short; the unticked ingredient is the one falling short. The
usual cause is a unit that cannot be compared, such as salt held in grams against
a recipe asking for a teaspoon.

---

Luqmaan Haffejee · 402306683 · BSc in Information Systems and Technology
