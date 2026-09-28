package za.ac.richfield.smartpantry.logic;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reduces an ingredient name to a form that can be compared reliably.
 *
 * <p>This class exists because the obvious implementation of the strict-matching
 * rule - comparing the two names with {@code equals} - fails on differences a
 * user would never think twice about. A recipe asking for "tomato" would not
 * match a pantry holding "Tomatoes", and the app would tell someone standing in
 * front of four tomatoes that they cannot make the dish.
 *
 * <p>Four passes, in this order:
 *
 * <ol>
 *   <li>Clean: lower-case, drop bracketed notes, drop digits and punctuation.</li>
 *   <li>Whole-string synonyms, applied first so that "ground beef" becomes
 *       "mince" before "ground" is stripped as a descriptor and leaves "beef".</li>
 *   <li>Drop descriptor words, which say how an ingredient was prepared rather
 *       than what it is.</li>
 *   <li>Singularise each remaining word, then check synonyms once more.</li>
 * </ol>
 *
 * <p>This is not natural language processing, and it is not meant to be. It
 * handles the cases a South African kitchen actually produces and leaves
 * anything stranger to fail visibly rather than match by accident.
 */
public final class IngredientNormaliser {

    private static final Map<String, String> SYNONYMS = buildSynonyms();
    private static final Set<String> DESCRIPTORS = buildDescriptors();

    private IngredientNormaliser() {
        // Utility class; never instantiated.
    }

    private static Map<String, String> buildSynonyms() {
        Map<String, String> map = new HashMap<>();

        map.put("beef mince", "mince");
        map.put("ground beef", "mince");
        map.put("minced beef", "mince");
        map.put("mince meat", "mince");
        map.put("beef", "mince");

        map.put("aubergine", "eggplant");
        map.put("brinjal", "eggplant");
        map.put("courgette", "zucchini");
        map.put("mielie meal", "maize meal");
        map.put("mealie meal", "maize meal");
        map.put("spring onion", "green onion");

        map.put("chicken breast", "chicken");
        map.put("chicken fillet", "chicken");
        map.put("chicken thigh", "chicken");

        map.put("cooking oil", "oil");
        map.put("sunflower oil", "oil");
        map.put("olive oil", "oil");
        map.put("vegetable oil", "oil");

        map.put("table salt", "salt");
        map.put("fine salt", "salt");
        map.put("sea salt", "salt");

        map.put("castor sugar", "sugar");
        map.put("white sugar", "sugar");
        map.put("brown sugar", "sugar");

        map.put("full cream milk", "milk");
        map.put("low fat milk", "milk");
        map.put("fresh milk", "milk");

        map.put("cheddar", "cheese");
        map.put("cheddar cheese", "cheese");
        map.put("gouda", "cheese");

        map.put("mayo", "mayonnaise");
        map.put("peanutbutter", "peanut butter");
        map.put("soya sauce", "soy sauce");
        map.put("butternut squash", "butternut");

        map.put("macaroni", "pasta");
        map.put("spaghetti", "pasta");
        map.put("penne", "pasta");
        map.put("noodles", "pasta");

        map.put("mixed veg", "mixed vegetables");
        map.put("frozen veg", "mixed vegetables");
        map.put("bicarbonate of soda", "baking powder");

        return Collections.unmodifiableMap(map);
    }

    private static Set<String> buildDescriptors() {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                "fresh", "dried", "chopped", "diced", "sliced", "minced", "grated",
                "ground", "large", "small", "medium", "ripe", "raw", "cooked",
                "frozen", "tinned", "canned", "whole", "plain", "fine", "coarse",
                "extra", "free", "range", "organic", "unsalted", "salted", "peeled")));
    }

    /**
     * Strips a regular English plural.
     *
     * <p>Deliberately crude. A full stemmer would bring a dependency and a lot of
     * behaviour nobody can predict from reading the code, and the input here is a
     * short list of food words rather than arbitrary prose.
     */
    static String singularise(String word) {
        if (word.length() > 4 && word.endsWith("ies")) {
            return word.substring(0, word.length() - 3) + "y";
        }
        for (String ending : new String[]{"ches", "shes", "sses", "xes", "zes"}) {
            if (word.length() > ending.length() && word.endsWith(ending)) {
                return word.substring(0, word.length() - 2);
            }
        }
        if (word.length() > 4 && word.endsWith("oes")) {
            return word.substring(0, word.length() - 2);
        }
        if (word.length() > 3 && word.endsWith("s") && !word.endsWith("ss")) {
            return word.substring(0, word.length() - 1);
        }
        return word;
    }

    /**
     * Returns the comparison key for an ingredient name. Two names that describe
     * the same thing return the same key.
     *
     * @param raw the name as typed by the user or as stored on a recipe
     * @return the normalised key, or an empty string when there is nothing usable
     */
    public static String normalise(String raw) {
        if (raw == null) {
            return "";
        }

        String text = raw.toLowerCase(Locale.ROOT).trim();
        text = text.replaceAll("\\(.*?\\)", " ");   // "milk (full cream)" -> "milk"
        text = text.replaceAll("[^a-z ]+", " ");     // drop digits and punctuation
        text = text.replaceAll("\\s+", " ").trim();

        if (text.isEmpty()) {
            return "";
        }

        String whole = SYNONYMS.get(text);
        if (whole != null) {
            text = whole;
        }

        String[] words = text.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty() || DESCRIPTORS.contains(word)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(singularise(word));
        }

        // Someone who types only descriptors ("fresh") would otherwise be left
        // with an empty key that matches every other empty key.
        if (builder.length() == 0) {
            for (String word : words) {
                if (word.isEmpty()) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(singularise(word));
            }
        }

        String stemmed = builder.toString().trim();
        String second = SYNONYMS.get(stemmed);
        return second != null ? second : stemmed;
    }
}
