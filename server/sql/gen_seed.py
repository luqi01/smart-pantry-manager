# -*- coding: utf-8 -*-
"""Generate 02_seed_recipes.sql.

Written as Python data rather than hand-typed SQL so the ingredient overlap can
be checked before it becomes SQL. Overlap matters: the strict-matching demo in
the video needs small recipes that share ingredients with larger ones.
"""
import collections
import sys

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except (AttributeError, ValueError):
    pass

R = []


def recipe(name, desc, mins, serves, steps, ings):
    R.append(dict(name=name, desc=desc, mins=mins, serves=serves, steps=steps, ings=ings))


recipe("Cheese Omelette", "A fast three-egg omelette folded around melted cheese.", 10, 1,
       ["Beat the eggs with the salt until the yolks and whites are fully combined.",
        "Melt the butter in a non-stick pan over medium heat until it foams.",
        "Pour in the eggs and let them set for about a minute without stirring.",
        "Scatter the cheese over one half, fold the omelette closed and slide it onto a plate."],
       [("eggs", 3, "piece"), ("cheese", 40, "g"), ("butter", 10, "g"), ("salt", 1, "tsp")])

recipe("Scrambled Eggs on Toast", "Soft scrambled eggs served on buttered toast.", 10, 1,
       ["Whisk the eggs with the milk and salt.",
        "Toast the bread and butter it while it is still hot.",
        "Melt the rest of the butter in a pan over low heat and add the eggs.",
        "Stir slowly until just set, then spoon onto the toast."],
       [("eggs", 2, "piece"), ("bread", 2, "slice"), ("butter", 15, "g"), ("milk", 30, "ml"),
        ("salt", 1, "tsp")])

recipe("French Toast", "Bread soaked in sweetened egg custard and fried until golden.", 15, 2,
       ["Whisk the eggs, milk, sugar and cinnamon together in a shallow bowl.",
        "Soak each slice of bread for about ten seconds a side.",
        "Fry in butter over medium heat until golden on both sides.",
        "Serve immediately while the outside is still crisp."],
       [("bread", 4, "slice"), ("eggs", 2, "piece"), ("milk", 120, "ml"), ("sugar", 2, "tsp"),
        ("butter", 20, "g"), ("cinnamon", 1, "tsp")])

recipe("Flapjacks", "Thick pancakes for breakfast or a quick snack.", 20, 4,
       ["Sift the flour and baking powder together and stir in the sugar.",
        "Beat the eggs into the milk, then whisk the wet mixture into the dry.",
        "Rest the batter for ten minutes so the flour hydrates.",
        "Fry spoonfuls in butter until bubbles appear, then flip and cook through."],
       [("flour", 250, "g"), ("milk", 300, "ml"), ("eggs", 2, "piece"), ("sugar", 30, "g"),
        ("baking powder", 2, "tsp"), ("butter", 20, "g")])

recipe("Peanut Butter Oats", "Oats cooked in milk and finished with peanut butter and banana.", 10, 1,
       ["Bring the milk to a simmer and stir in the oats.",
        "Cook for five minutes, stirring, until the oats soften and thicken.",
        "Take off the heat and stir through the peanut butter and sugar.",
        "Top with sliced banana."],
       [("oats", 80, "g"), ("milk", 250, "ml"), ("peanut butter", 30, "g"), ("banana", 1, "piece"),
        ("sugar", 1, "tsp")])

recipe("Banana Smoothie", "A blended banana and milk smoothie with peanut butter.", 5, 1,
       ["Peel the bananas and break them into chunks.",
        "Blend the banana, milk, peanut butter and sugar until smooth.",
        "Pour into a tall glass and drink straight away."],
       [("banana", 2, "piece"), ("milk", 250, "ml"), ("peanut butter", 20, "g"), ("sugar", 1, "tsp")])

recipe("Tuna Mayo Sandwich", "Tinned tuna bound with mayonnaise and finely chopped onion.", 10, 2,
       ["Drain the tuna well and flake it into a bowl.",
        "Finely chop the onion and stir it into the tuna with the mayonnaise and salt.",
        "Spread over two slices of bread and close the sandwiches.",
        "Cut in half and serve."],
       [("bread", 4, "slice"), ("tuna", 1, "can"), ("mayonnaise", 40, "g"), ("onion", 1, "piece"),
        ("salt", 1, "tsp")])

recipe("Garlic Butter Pasta", "Pasta tossed in garlic butter and finished with cheese.", 20, 2,
       ["Boil the pasta in salted water until just tender, then drain and keep a cup of the water.",
        "Melt the butter over low heat and cook the sliced garlic until fragrant but not coloured.",
        "Toss the pasta through the garlic butter, loosening it with the reserved water.",
        "Season with black pepper and stir through the grated cheese."],
       [("pasta", 200, "g"), ("butter", 40, "g"), ("garlic", 3, "clove"), ("cheese", 30, "g"),
        ("salt", 1, "tsp"), ("black pepper", 1, "tsp")])

recipe("Macaroni and Cheese", "Baked pasta in a cheese sauce built on a simple roux.", 35, 4,
       ["Boil the pasta until just short of tender and drain it.",
        "Melt the butter, stir in the flour and cook the roux for a minute.",
        "Whisk in the milk a little at a time until the sauce thickens, then melt in most of the cheese.",
        "Fold the pasta through the sauce, top with the last of the cheese and bake until browned."],
       [("pasta", 250, "g"), ("cheese", 150, "g"), ("milk", 300, "ml"), ("butter", 30, "g"),
        ("flour", 25, "g"), ("salt", 1, "tsp")])

recipe("Spaghetti Bolognese", "Beef mince simmered with tomato and onion, served over pasta.", 45, 4,
       ["Fry the chopped onion and garlic in the oil until soft.",
        "Add the mince and brown it well, breaking up any lumps.",
        "Stir in the chopped tomatoes and salt, then simmer for at least half an hour.",
        "Boil the pasta and serve the sauce over it."],
       [("pasta", 250, "g"), ("beef mince", 400, "g"), ("tomato", 3, "piece"), ("onion", 1, "piece"),
        ("garlic", 2, "clove"), ("oil", 15, "ml"), ("salt", 1, "tsp")])

recipe("Tomato and Onion Smoor", "A South African braised tomato and onion relish.", 25, 4,
       ["Slice the onions and fry them in the oil until translucent.",
        "Add the chopped tomatoes, salt and sugar.",
        "Simmer uncovered for twenty minutes until the sauce thickens and darkens.",
        "Serve with pap, rice or bread."],
       [("tomato", 4, "piece"), ("onion", 2, "piece"), ("oil", 20, "ml"), ("salt", 1, "tsp"),
        ("sugar", 1, "tsp")])

recipe("Chakalaka", "A spiced vegetable and bean relish eaten across South Africa.", 30, 4,
       ["Fry the onion and garlic in the oil until soft.",
        "Grate the carrots in and add the curry powder, cooking until it smells toasted.",
        "Add the chopped tomatoes and cook down for ten minutes.",
        "Stir in the baked beans and warm through without breaking them up."],
       [("onion", 1, "piece"), ("carrot", 2, "piece"), ("tomato", 2, "piece"),
        ("baked beans", 1, "can"), ("curry powder", 2, "tsp"), ("garlic", 2, "clove"),
        ("oil", 20, "ml")])

recipe("Mielie Pap", "Stiff maize porridge, the staple starch of a South African plate.", 30, 4,
       ["Bring salted water to a rolling boil.",
        "Rain the maize meal in slowly while stirring so it does not form lumps.",
        "Cover and steam on the lowest heat for twenty-five minutes.",
        "Stir hard with a wooden spoon before serving."],
       [("maize meal", 250, "g"), ("salt", 1, "tsp")])

recipe("Chicken Curry", "Chicken and potato simmered in a tomato and curry base.", 50, 4,
       ["Fry the onion and garlic in the oil until golden.",
        "Add the curry powder and cook it for a minute to take off the raw edge.",
        "Add the chicken and brown it, then stir in the chopped tomatoes and salt.",
        "Add the cubed potato, cover and simmer for half an hour until everything is tender."],
       [("chicken", 500, "g"), ("onion", 2, "piece"), ("tomato", 3, "piece"), ("potato", 2, "piece"),
        ("garlic", 3, "clove"), ("curry powder", 15, "g"), ("oil", 30, "ml"), ("salt", 1, "tsp")])

recipe("Vegetable Stir-Fry", "Mixed vegetables cooked fast and finished with soy sauce.", 15, 2,
       ["Heat the oil in a wok or wide pan until it shimmers.",
        "Fry the sliced onion and garlic for thirty seconds.",
        "Add the vegetables and keep them moving over high heat for four minutes.",
        "Add the soy sauce at the end so it glazes rather than steams."],
       [("mixed vegetables", 400, "g"), ("onion", 1, "piece"), ("garlic", 2, "clove"),
        ("oil", 20, "ml"), ("soy sauce", 30, "ml")])

recipe("Egg Fried Rice", "Leftover rice fried with egg and vegetables.", 20, 2,
       ["Use cold cooked rice so the grains stay separate.",
        "Scramble the eggs in half the oil and set them aside.",
        "Fry the onion and vegetables in the rest of the oil until hot through.",
        "Add the rice and soy sauce, then fold the egg back in."],
       [("rice", 300, "g"), ("eggs", 2, "piece"), ("mixed vegetables", 200, "g"),
        ("onion", 1, "piece"), ("oil", 20, "ml"), ("soy sauce", 30, "ml")])

recipe("Lentil Soup", "A thick brown lentil soup built on softened vegetables.", 45, 4,
       ["Soften the chopped onion, carrot and garlic in the oil.",
        "Rinse the lentils and add them to the pot.",
        "Cover with water, bring to the boil and simmer for thirty-five minutes.",
        "Season with salt and blend briefly if a smoother soup is wanted."],
       [("lentils", 250, "g"), ("onion", 1, "piece"), ("carrot", 2, "piece"), ("garlic", 2, "clove"),
        ("oil", 15, "ml"), ("salt", 1, "tsp")])

recipe("Butternut Soup", "Roasted butternut blended with milk into a smooth soup.", 50, 4,
       ["Peel and cube the butternut and roast it with the oil until the edges caramelise.",
        "Soften the onion and garlic in a pot.",
        "Add the butternut, cover with water and simmer for fifteen minutes.",
        "Blend until smooth, stir in the milk and season with salt."],
       [("butternut", 800, "g"), ("onion", 1, "piece"), ("garlic", 2, "clove"), ("milk", 150, "ml"),
        ("oil", 15, "ml"), ("salt", 1, "tsp")])

recipe("Potato Salad", "Boiled potato and egg in a mayonnaise dressing.", 35, 4,
       ["Boil the potatoes whole until a knife slides in easily, then cool and cube them.",
        "Hard-boil the eggs, cool them under running water and chop them.",
        "Finely chop the onion.",
        "Fold everything through the mayonnaise with the salt and chill before serving."],
       [("potato", 6, "piece"), ("eggs", 3, "piece"), ("mayonnaise", 80, "g"), ("onion", 1, "piece"),
        ("salt", 1, "tsp")])

recipe("Baked Bean Toastie", "A toasted sandwich of baked beans and melted cheese.", 12, 2,
       ["Butter the bread on the outside of each slice.",
        "Spoon the drained beans onto two slices and cover with the grated cheese.",
        "Close the sandwiches and toast in a pan, pressing down, until both sides are golden.",
        "Rest for a minute before cutting so the filling stays put."],
       [("bread", 4, "slice"), ("baked beans", 1, "can"), ("cheese", 60, "g"), ("butter", 20, "g")])


# ---------------------------------------------------------------- validate
assert len(R) == 20, len(R)
assert len({r["name"] for r in R}) == 20, "duplicate recipe name"
pool = collections.Counter()
for r in R:
    names = [i[0] for i in r["ings"]]
    assert len(names) == len(set(names)), "duplicate ingredient in " + r["name"]
    for name, qty, unit in r["ings"]:
        assert qty > 0 and unit, r["name"]
        pool[name] += 1

print("recipes: %d | distinct ingredients: %d" % (len(R), len(pool)))
print("ingredient-count spread:", sorted(collections.Counter(len(r["ings"]) for r in R).items()))
print("\nmost shared ingredients:")
for name, count in pool.most_common(10):
    print("  %-18s %2d recipes" % (name, count))
print("\nsmallest recipes (best for the strict-matching demo):")
for r in sorted(R, key=lambda x: len(x["ings"]))[:4]:
    print("  %-24s %d: %s" % (r["name"], len(r["ings"]), ", ".join(i[0] for i in r["ings"])))


# -------------------------------------------------------------- emit SQL
def quote(text):
    return "'" + str(text).replace("'", "''") + "'"


out = [
    "-- Smart Pantry Manager - recipe seed data (20 recipes).",
    "-- Run after 01_schema.sql. Safe to re-run: it clears the recipe tables first.",
    "-- Generated from server/sql/gen_seed.py - edit there, not here.",
    "",
    "TRUNCATE recipe RESTART IDENTITY CASCADE;",
    "",
]
for r in R:
    steps = "\n".join("%d. %s" % (i + 1, s) for i, s in enumerate(r["steps"]))
    out.append("INSERT INTO recipe (name, description, prep_steps, prep_minutes, serves) VALUES")
    out.append("  (%s, %s, %s, %d, %d);" % (quote(r["name"]), quote(r["desc"]), quote(steps),
                                            r["mins"], r["serves"]))
    out.append("INSERT INTO recipe_ingredient (recipe_id, name, quantity, unit) VALUES")
    rows = ["  ((SELECT id FROM recipe WHERE name = %s), %s, %s, %s)"
            % (quote(r["name"]), quote(n), quote(q), quote(u)) for n, q, u in r["ings"]]
    out.append(",\n".join(rows) + ";")
    out.append("")

out += [
    "-- Sanity check: every recipe should list the ingredient count shown here.",
    "SELECT r.name, count(ri.id) AS ingredients",
    "  FROM recipe r JOIN recipe_ingredient ri ON ri.recipe_id = r.id",
    "  GROUP BY r.name ORDER BY ingredients, r.name;",
]

PATH = (r"C:\Users\luqma\OneDrive\Documents\Assignments\Assignments(2026)"
        r"\Mobile App dev\SmartPantryManager\server\sql\02_seed_recipes.sql")
with open(PATH, "w", encoding="utf-8") as fh:
    fh.write("\n".join(out) + "\n")
print("\nwrote %s (%d lines)" % (PATH, len(out)))
