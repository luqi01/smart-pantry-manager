-- Smart Pantry Manager - recipe seed data (20 recipes).
-- Run after 01_schema.sql. Safe to re-run: it clears the recipe tables first.
-- Generated from server/sql/gen_seed.py - edit there, not here.

TRUNCATE recipe RESTART IDENTITY CASCADE;

INSERT INTO recipe (name, description, prep_steps, prep_minutes, serves) VALUES
  ('Cheese Omelette', 'A fast three-egg omelette folded around melted cheese.', '1. Beat the eggs with the salt until the yolks and whites are fully combined.
2. Melt the butter in a non-stick pan over medium heat until it foams.
3. Pour in the eggs and let them set for about a minute without stirring.
4. Scatter the cheese over one half, fold the omelette closed and slide it onto a plate.', 10, 1);
INSERT INTO recipe_ingredient (recipe_id, name, quantity, unit) VALUES
  ((SELECT id FROM recipe WHERE name = 'Cheese Omelette'), 'eggs', '3', 'piece'),
  ((SELECT id FROM recipe WHERE name = 'Cheese Omelette'), 'cheese', '40', 'g'),
  ((SELECT id FROM recipe WHERE name = 'Cheese Omelette'), 'butter', '10', 'g'),
  ((SELECT id FROM recipe WHERE name = 'Cheese Omelette'), 'salt', '1', 'tsp');

INSERT INTO recipe (name, description, prep_steps, prep_minutes, serves) VALUES
  ('Scrambled Eggs on Toast', 'Soft scrambled eggs served on buttered toast.', '1. Whisk the eggs with the milk and salt.
2. Toast the bread and butter it while it is still hot.
3. Melt the rest of the butter in a pan over low heat and add the eggs.
4. Stir slowly until just set, then spoon onto the toast.', 10, 1);
INSERT INTO recipe_ingredient (recipe_id, name, quantity, unit) VALUES
  ((SELECT id FROM recipe WHERE name = 'Scrambled Eggs on Toast'), 'eggs', '2', 'piece'),
  ((SELECT id FROM recipe WHERE name = 'Scrambled Eggs on Toast'), 'bread', '2', 'slice'),
  ((SELECT id FROM recipe WHERE name = 'Scrambled Eggs on Toast'), 'butter', '15', 'g'),
  ((SELECT id FROM recipe WHERE name = 'Scrambled Eggs on Toast'), 'milk', '30', 'ml'),
  ((SELECT id FROM recipe WHERE name = 'Scrambled Eggs on Toast'), 'salt', '1', 'tsp');

INSERT INTO recipe (name, description, prep_steps, prep_minutes, serves) VALUES
  ('French Toast', 'Bread soaked in sweetened egg custard and fried until golden.', '1. Whisk the eggs, milk, sugar and cinnamon together in a shallow bowl.
2. Soak each slice of bread for about ten seconds a side.
3. Fry in butter over medium heat until golden on both sides.
4. Serve immediately while the outside is still crisp.', 15, 2);
INSERT INTO recipe_ingredient (recipe_id, name, quantity, unit) VALUES
  ((SELECT id FROM recipe WHERE name = 'French Toast'), 'bread', '4', 'slice'),
  ((SELECT id FROM recipe WHERE name = 'French Toast'), 'eggs', '2', 'piece'),
  ((SELECT id FROM recipe WHERE name = 'French Toast'), 'milk', '120', 'ml'),
  ((SELECT id FROM recipe WHERE name = 'French Toast'), 'sugar', '2', 'tsp'),
  ((SELECT id FROM recipe WHERE name = 'French Toast'), 'butter', '20', 'g'),
  ((SELECT id FROM recipe WHERE name = 'French Toast'), 'cinnamon', '1', 'tsp');

INSERT INTO recipe (name, description, prep_steps, prep_minutes, serves) VALUES
  ('Flapjacks', 'Thick pancakes for breakfast or a quick snack.', '1. Sift the flour and baking powder together and stir in the sugar.
2. Beat the eggs into the milk, then whisk the wet mixture into the dry.
3. Rest the batter for ten minutes so the flour hydrates.
4. Fry spoonfuls in butter until bubbles appear, then flip and cook through.', 20, 4);
INSERT INTO recipe_ingredient (recipe_id, name, quantity, unit) VALUES
  ((SELECT id FROM recipe WHERE name = 'Flapjacks'), 'flour', '250', 'g'),
  ((SELECT id FROM recipe WHERE name = 'Flapjacks'), 'milk', '300', 'ml'),
  ((SELECT id FROM recipe WHERE name = 'Flapjacks'), 'eggs', '2', 'piece'),
  ((SELECT id FROM recipe WHERE name = 'Flapjacks'), 'sugar', '30', 'g'),
  ((SELECT id FROM recipe WHERE name = 'Flapjacks'), 'baking powder', '2', 'tsp'),
  ((SELECT id FROM recipe WHERE name = 'Flapjacks'), 'butter', '20', 'g');

INSERT INTO recipe (name, description, prep_steps, prep_minutes, serves) VALUES
  ('Peanut Butter Oats', 'Oats cooked in milk and finished with peanut butter and banana.', '1. Bring the milk to a simmer and stir in the oats.
2. Cook for five minutes, stirring, until the oats soften and thicken.
3. Take off the heat and stir through the peanut butter and sugar.
4. Top with sliced banana.', 10, 1);
INSERT INTO recipe_ingredient (recipe_id, name, quantity, unit) VALUES
  ((SELECT id FROM recipe WHERE name = 'Peanut Butter Oats'), 'oats', '80', 'g'),
  ((SELECT id FROM recipe WHERE name = 'Peanut Butter Oats'), 'milk', '250', 'ml'),
  ((SELECT id FROM recipe WHERE name = 'Peanut Butter Oats'), 'peanut butter', '30', 'g'),
  ((SELECT id FROM recipe WHERE name = 'Peanut Butter Oats'), 'banana', '1', 'piece'),
  ((SELECT id FROM recipe WHERE name = 'Peanut Butter Oats'), 'sugar', '1', 'tsp');

INSERT INTO recipe (name, description, prep_steps, prep_minutes, serves) VALUES
  ('Banana Smoothie', 'A blended banana and milk smoothie with peanut butter.', '1. Peel the bananas and break them into chunks.
2. Blend the banana, milk, peanut butter and sugar until smooth.
3. Pour into a tall glass and drink straight away.', 5, 1);
INSERT INTO recipe_ingredient (recipe_id, name, quantity, unit) VALUES
  ((SELECT id FROM recipe WHERE name = 'Banana Smoothie'), 'banana', '2', 'piece'),
  ((SELECT id FROM recipe WHERE name = 'Banana Smoothie'), 'milk', '250', 'ml'),
  ((SELECT id FROM recipe WHERE name = 'Banana Smoothie'), 'peanut butter', '20', 'g'),
  ((SELECT id FROM recipe WHERE name = 'Banana Smoothie'), 'sugar', '1', 'tsp');

INSERT INTO recipe (name, description, prep_steps, prep_minutes, serves) VALUES
  ('Tuna Mayo Sandwich', 'Tinned tuna bound with mayonnaise and finely chopped onion.', '1. Drain the tuna well and flake it into a bowl.
2. Finely chop the onion and stir it into the tuna with the mayonnaise and salt.
3. Spread over two slices of bread and close the sandwiches.
4. Cut in half and serve.', 10, 2);
INSERT INTO recipe_ingredient (recipe_id, name, quantity, unit) VALUES
  ((SELECT id FROM recipe WHERE name = 'Tuna Mayo Sandwich'), 'bread', '4', 'slice'),
  ((SELECT id FROM recipe WHERE name = 'Tuna Mayo Sandwich'), 'tuna', '1', 'can'),
  ((SELECT id FROM recipe WHERE name = 'Tuna Mayo Sandwich'), 'mayonnaise', '40', 'g'),
  ((SELECT id FROM recipe WHERE name = 'Tuna Mayo Sandwich'), 'onion', '1', 'piece'),
  ((SELECT id FROM recipe WHERE name = 'Tuna Mayo Sandwich'), 'salt', '1', 'tsp');

INSERT INTO recipe (name, description, prep_steps, prep_minutes, serves) VALUES
  ('Garlic Butter Pasta', 'Pasta tossed in garlic butter and finished with cheese.', '1. Boil the pasta in salted water until just tender, then drain and keep a cup of the water.
2. Melt the butter over low heat and cook the sliced garlic until fragrant but not coloured.
3. Toss the pasta through the garlic butter, loosening it with the reserved water.
4. Season with black pepper and stir through the grated cheese.', 20, 2);
INSERT INTO recipe_ingredient (recipe_id, name, quantity, unit) VALUES
  ((SELECT id FROM recipe WHERE name = 'Garlic Butter Pasta'), 'pasta', '200', 'g'),
  ((SELECT id FROM recipe WHERE name = 'Garlic Butter Pasta'), 'butter', '40', 'g'),
  ((SELECT id FROM recipe WHERE name = 'Garlic Butter Pasta'), 'garlic', '3', 'clove'),
  ((SELECT id FROM recipe WHERE name = 'Garlic Butter Pasta'), 'cheese', '30', 'g'),
  ((SELECT id FROM recipe WHERE name = 'Garlic Butter Pasta'), 'salt', '1', 'tsp'),
  ((SELECT id FROM recipe WHERE name = 'Garlic Butter Pasta'), 'black pepper', '1', 'tsp');

INSERT INTO recipe (name, description, prep_steps, prep_minutes, serves) VALUES
  ('Macaroni and Cheese', 'Baked pasta in a cheese sauce built on a simple roux.', '1. Boil the pasta until just short of tender and drain it.
2. Melt the butter, stir in the flour and cook the roux for a minute.
3. Whisk in the milk a little at a time until the sauce thickens, then melt in most of the cheese.
4. Fold the pasta through the sauce, top with the last of the cheese and bake until browned.', 35, 4);
INSERT INTO recipe_ingredient (recipe_id, name, quantity, unit) VALUES
  ((SELECT id FROM recipe WHERE name = 'Macaroni and Cheese'), 'pasta', '250', 'g'),
  ((SELECT id FROM recipe WHERE name = 'Macaroni and Cheese'), 'cheese', '150', 'g'),
  ((SELECT id FROM recipe WHERE name = 'Macaroni and Cheese'), 'milk', '300', 'ml'),
  ((SELECT id FROM recipe WHERE name = 'Macaroni and Cheese'), 'butter', '30', 'g'),
  ((SELECT id FROM recipe WHERE name = 'Macaroni and Cheese'), 'flour', '25', 'g'),
  ((SELECT id FROM recipe WHERE name = 'Macaroni and Cheese'), 'salt', '1', 'tsp');

INSERT INTO recipe (name, description, prep_steps, prep_minutes, serves) VALUES
  ('Spaghetti Bolognese', 'Beef mince simmered with tomato and onion, served over pasta.', '1. Fry the chopped onion and garlic in the oil until soft.
2. Add the mince and brown it well, breaking up any lumps.
3. Stir in the chopped tomatoes and salt, then simmer for at least half an hour.
4. Boil the pasta and serve the sauce over it.', 45, 4);
INSERT INTO recipe_ingredient (recipe_id, name, quantity, unit) VALUES
  ((SELECT id FROM recipe WHERE name = 'Spaghetti Bolognese'), 'pasta', '250', 'g'),
  ((SELECT id FROM recipe WHERE name = 'Spaghetti Bolognese'), 'beef mince', '400', 'g'),
  ((SELECT id FROM recipe WHERE name = 'Spaghetti Bolognese'), 'tomato', '3', 'piece'),
  ((SELECT id FROM recipe WHERE name = 'Spaghetti Bolognese'), 'onion', '1', 'piece'),
  ((SELECT id FROM recipe WHERE name = 'Spaghetti Bolognese'), 'garlic', '2', 'clove'),
  ((SELECT id FROM recipe WHERE name = 'Spaghetti Bolognese'), 'oil', '15', 'ml'),
  ((SELECT id FROM recipe WHERE name = 'Spaghetti Bolognese'), 'salt', '1', 'tsp');

INSERT INTO recipe (name, description, prep_steps, prep_minutes, serves) VALUES
  ('Tomato and Onion Smoor', 'A South African braised tomato and onion relish.', '1. Slice the onions and fry them in the oil until translucent.
2. Add the chopped tomatoes, salt and sugar.
3. Simmer uncovered for twenty minutes until the sauce thickens and darkens.
4. Serve with pap, rice or bread.', 25, 4);
INSERT INTO recipe_ingredient (recipe_id, name, quantity, unit) VALUES
  ((SELECT id FROM recipe WHERE name = 'Tomato and Onion Smoor'), 'tomato', '4', 'piece'),
  ((SELECT id FROM recipe WHERE name = 'Tomato and Onion Smoor'), 'onion', '2', 'piece'),
  ((SELECT id FROM recipe WHERE name = 'Tomato and Onion Smoor'), 'oil', '20', 'ml'),
  ((SELECT id FROM recipe WHERE name = 'Tomato and Onion Smoor'), 'salt', '1', 'tsp'),
  ((SELECT id FROM recipe WHERE name = 'Tomato and Onion Smoor'), 'sugar', '1', 'tsp');

INSERT INTO recipe (name, description, prep_steps, prep_minutes, serves) VALUES
  ('Chakalaka', 'A spiced vegetable and bean relish eaten across South Africa.', '1. Fry the onion and garlic in the oil until soft.
2. Grate the carrots in and add the curry powder, cooking until it smells toasted.
3. Add the chopped tomatoes and cook down for ten minutes.
4. Stir in the baked beans and warm through without breaking them up.', 30, 4);
INSERT INTO recipe_ingredient (recipe_id, name, quantity, unit) VALUES
  ((SELECT id FROM recipe WHERE name = 'Chakalaka'), 'onion', '1', 'piece'),
  ((SELECT id FROM recipe WHERE name = 'Chakalaka'), 'carrot', '2', 'piece'),
  ((SELECT id FROM recipe WHERE name = 'Chakalaka'), 'tomato', '2', 'piece'),
  ((SELECT id FROM recipe WHERE name = 'Chakalaka'), 'baked beans', '1', 'can'),
  ((SELECT id FROM recipe WHERE name = 'Chakalaka'), 'curry powder', '2', 'tsp'),
  ((SELECT id FROM recipe WHERE name = 'Chakalaka'), 'garlic', '2', 'clove'),
  ((SELECT id FROM recipe WHERE name = 'Chakalaka'), 'oil', '20', 'ml');

INSERT INTO recipe (name, description, prep_steps, prep_minutes, serves) VALUES
  ('Mielie Pap', 'Stiff maize porridge, the staple starch of a South African plate.', '1. Bring salted water to a rolling boil.
2. Rain the maize meal in slowly while stirring so it does not form lumps.
3. Cover and steam on the lowest heat for twenty-five minutes.
4. Stir hard with a wooden spoon before serving.', 30, 4);
INSERT INTO recipe_ingredient (recipe_id, name, quantity, unit) VALUES
  ((SELECT id FROM recipe WHERE name = 'Mielie Pap'), 'maize meal', '250', 'g'),
  ((SELECT id FROM recipe WHERE name = 'Mielie Pap'), 'salt', '1', 'tsp');

INSERT INTO recipe (name, description, prep_steps, prep_minutes, serves) VALUES
  ('Chicken Curry', 'Chicken and potato simmered in a tomato and curry base.', '1. Fry the onion and garlic in the oil until golden.
2. Add the curry powder and cook it for a minute to take off the raw edge.
3. Add the chicken and brown it, then stir in the chopped tomatoes and salt.
4. Add the cubed potato, cover and simmer for half an hour until everything is tender.', 50, 4);
INSERT INTO recipe_ingredient (recipe_id, name, quantity, unit) VALUES
  ((SELECT id FROM recipe WHERE name = 'Chicken Curry'), 'chicken', '500', 'g'),
  ((SELECT id FROM recipe WHERE name = 'Chicken Curry'), 'onion', '2', 'piece'),
  ((SELECT id FROM recipe WHERE name = 'Chicken Curry'), 'tomato', '3', 'piece'),
  ((SELECT id FROM recipe WHERE name = 'Chicken Curry'), 'potato', '2', 'piece'),
  ((SELECT id FROM recipe WHERE name = 'Chicken Curry'), 'garlic', '3', 'clove'),
  ((SELECT id FROM recipe WHERE name = 'Chicken Curry'), 'curry powder', '15', 'g'),
  ((SELECT id FROM recipe WHERE name = 'Chicken Curry'), 'oil', '30', 'ml'),
  ((SELECT id FROM recipe WHERE name = 'Chicken Curry'), 'salt', '1', 'tsp');

INSERT INTO recipe (name, description, prep_steps, prep_minutes, serves) VALUES
  ('Vegetable Stir-Fry', 'Mixed vegetables cooked fast and finished with soy sauce.', '1. Heat the oil in a wok or wide pan until it shimmers.
2. Fry the sliced onion and garlic for thirty seconds.
3. Add the vegetables and keep them moving over high heat for four minutes.
4. Add the soy sauce at the end so it glazes rather than steams.', 15, 2);
INSERT INTO recipe_ingredient (recipe_id, name, quantity, unit) VALUES
  ((SELECT id FROM recipe WHERE name = 'Vegetable Stir-Fry'), 'mixed vegetables', '400', 'g'),
  ((SELECT id FROM recipe WHERE name = 'Vegetable Stir-Fry'), 'onion', '1', 'piece'),
  ((SELECT id FROM recipe WHERE name = 'Vegetable Stir-Fry'), 'garlic', '2', 'clove'),
  ((SELECT id FROM recipe WHERE name = 'Vegetable Stir-Fry'), 'oil', '20', 'ml'),
  ((SELECT id FROM recipe WHERE name = 'Vegetable Stir-Fry'), 'soy sauce', '30', 'ml');

INSERT INTO recipe (name, description, prep_steps, prep_minutes, serves) VALUES
  ('Egg Fried Rice', 'Leftover rice fried with egg and vegetables.', '1. Use cold cooked rice so the grains stay separate.
2. Scramble the eggs in half the oil and set them aside.
3. Fry the onion and vegetables in the rest of the oil until hot through.
4. Add the rice and soy sauce, then fold the egg back in.', 20, 2);
INSERT INTO recipe_ingredient (recipe_id, name, quantity, unit) VALUES
  ((SELECT id FROM recipe WHERE name = 'Egg Fried Rice'), 'rice', '300', 'g'),
  ((SELECT id FROM recipe WHERE name = 'Egg Fried Rice'), 'eggs', '2', 'piece'),
  ((SELECT id FROM recipe WHERE name = 'Egg Fried Rice'), 'mixed vegetables', '200', 'g'),
  ((SELECT id FROM recipe WHERE name = 'Egg Fried Rice'), 'onion', '1', 'piece'),
  ((SELECT id FROM recipe WHERE name = 'Egg Fried Rice'), 'oil', '20', 'ml'),
  ((SELECT id FROM recipe WHERE name = 'Egg Fried Rice'), 'soy sauce', '30', 'ml');

INSERT INTO recipe (name, description, prep_steps, prep_minutes, serves) VALUES
  ('Lentil Soup', 'A thick brown lentil soup built on softened vegetables.', '1. Soften the chopped onion, carrot and garlic in the oil.
2. Rinse the lentils and add them to the pot.
3. Cover with water, bring to the boil and simmer for thirty-five minutes.
4. Season with salt and blend briefly if a smoother soup is wanted.', 45, 4);
INSERT INTO recipe_ingredient (recipe_id, name, quantity, unit) VALUES
  ((SELECT id FROM recipe WHERE name = 'Lentil Soup'), 'lentils', '250', 'g'),
  ((SELECT id FROM recipe WHERE name = 'Lentil Soup'), 'onion', '1', 'piece'),
  ((SELECT id FROM recipe WHERE name = 'Lentil Soup'), 'carrot', '2', 'piece'),
  ((SELECT id FROM recipe WHERE name = 'Lentil Soup'), 'garlic', '2', 'clove'),
  ((SELECT id FROM recipe WHERE name = 'Lentil Soup'), 'oil', '15', 'ml'),
  ((SELECT id FROM recipe WHERE name = 'Lentil Soup'), 'salt', '1', 'tsp');

INSERT INTO recipe (name, description, prep_steps, prep_minutes, serves) VALUES
  ('Butternut Soup', 'Roasted butternut blended with milk into a smooth soup.', '1. Peel and cube the butternut and roast it with the oil until the edges caramelise.
2. Soften the onion and garlic in a pot.
3. Add the butternut, cover with water and simmer for fifteen minutes.
4. Blend until smooth, stir in the milk and season with salt.', 50, 4);
INSERT INTO recipe_ingredient (recipe_id, name, quantity, unit) VALUES
  ((SELECT id FROM recipe WHERE name = 'Butternut Soup'), 'butternut', '800', 'g'),
  ((SELECT id FROM recipe WHERE name = 'Butternut Soup'), 'onion', '1', 'piece'),
  ((SELECT id FROM recipe WHERE name = 'Butternut Soup'), 'garlic', '2', 'clove'),
  ((SELECT id FROM recipe WHERE name = 'Butternut Soup'), 'milk', '150', 'ml'),
  ((SELECT id FROM recipe WHERE name = 'Butternut Soup'), 'oil', '15', 'ml'),
  ((SELECT id FROM recipe WHERE name = 'Butternut Soup'), 'salt', '1', 'tsp');

INSERT INTO recipe (name, description, prep_steps, prep_minutes, serves) VALUES
  ('Potato Salad', 'Boiled potato and egg in a mayonnaise dressing.', '1. Boil the potatoes whole until a knife slides in easily, then cool and cube them.
2. Hard-boil the eggs, cool them under running water and chop them.
3. Finely chop the onion.
4. Fold everything through the mayonnaise with the salt and chill before serving.', 35, 4);
INSERT INTO recipe_ingredient (recipe_id, name, quantity, unit) VALUES
  ((SELECT id FROM recipe WHERE name = 'Potato Salad'), 'potato', '6', 'piece'),
  ((SELECT id FROM recipe WHERE name = 'Potato Salad'), 'eggs', '3', 'piece'),
  ((SELECT id FROM recipe WHERE name = 'Potato Salad'), 'mayonnaise', '80', 'g'),
  ((SELECT id FROM recipe WHERE name = 'Potato Salad'), 'onion', '1', 'piece'),
  ((SELECT id FROM recipe WHERE name = 'Potato Salad'), 'salt', '1', 'tsp');

INSERT INTO recipe (name, description, prep_steps, prep_minutes, serves) VALUES
  ('Baked Bean Toastie', 'A toasted sandwich of baked beans and melted cheese.', '1. Butter the bread on the outside of each slice.
2. Spoon the drained beans onto two slices and cover with the grated cheese.
3. Close the sandwiches and toast in a pan, pressing down, until both sides are golden.
4. Rest for a minute before cutting so the filling stays put.', 12, 2);
INSERT INTO recipe_ingredient (recipe_id, name, quantity, unit) VALUES
  ((SELECT id FROM recipe WHERE name = 'Baked Bean Toastie'), 'bread', '4', 'slice'),
  ((SELECT id FROM recipe WHERE name = 'Baked Bean Toastie'), 'baked beans', '1', 'can'),
  ((SELECT id FROM recipe WHERE name = 'Baked Bean Toastie'), 'cheese', '60', 'g'),
  ((SELECT id FROM recipe WHERE name = 'Baked Bean Toastie'), 'butter', '20', 'g');

-- Sanity check: every recipe should list the ingredient count shown here.
SELECT r.name, count(ri.id) AS ingredients
  FROM recipe r JOIN recipe_ingredient ri ON ri.recipe_id = r.id
  GROUP BY r.name ORDER BY ingredients, r.name;
