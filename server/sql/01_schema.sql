-- Smart Pantry Manager - PostgreSQL schema
-- Target: Supabase (PostgreSQL 15). Run this once in the Supabase SQL editor,
-- then run 02_seed_recipes.sql.

DROP TABLE IF EXISTS recipe_ingredient CASCADE;
DROP TABLE IF EXISTS recipe CASCADE;
DROP TABLE IF EXISTS pantry_item CASCADE;

-- What the user currently has at home. This is the only table the user writes to.
CREATE TABLE pantry_item (
    id          BIGSERIAL PRIMARY KEY,
    name        TEXT        NOT NULL,
    quantity    NUMERIC(10,2) NOT NULL CHECK (quantity > 0),
    unit        TEXT        NOT NULL DEFAULT 'piece',
    expiry_date DATE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- One ingredient name should appear once in a pantry. Storing "2 eggs" and
-- "3 eggs" as separate rows would let a recipe match against only one of them,
-- which would break the strict-matching rule in a way that is hard to see.
CREATE UNIQUE INDEX pantry_item_name_key ON pantry_item (lower(trim(name)));

CREATE TABLE recipe (
    id           BIGSERIAL PRIMARY KEY,
    name         TEXT NOT NULL UNIQUE,
    description  TEXT NOT NULL,
    prep_steps   TEXT NOT NULL,
    prep_minutes INTEGER NOT NULL DEFAULT 15 CHECK (prep_minutes > 0),
    serves       INTEGER NOT NULL DEFAULT 2 CHECK (serves > 0)
);

CREATE TABLE recipe_ingredient (
    id        BIGSERIAL PRIMARY KEY,
    recipe_id BIGINT NOT NULL REFERENCES recipe(id) ON DELETE CASCADE,
    name      TEXT   NOT NULL,
    quantity  NUMERIC(10,2) NOT NULL CHECK (quantity > 0),
    unit      TEXT   NOT NULL DEFAULT 'piece'
);

CREATE INDEX recipe_ingredient_recipe_id_idx ON recipe_ingredient (recipe_id);

-- Keep updated_at honest without the API having to remember to set it.
CREATE OR REPLACE FUNCTION touch_updated_at() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER pantry_item_touch
    BEFORE UPDATE ON pantry_item
    FOR EACH ROW EXECUTE FUNCTION touch_updated_at();
