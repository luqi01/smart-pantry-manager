"""Smart Pantry Manager - REST API over a Supabase PostgreSQL database.

The Android app never talks to PostgreSQL directly. It calls this service, which
holds the database credentials and owns the SQL. Two reasons for that split:

  1. A database password shipped inside an APK is readable by anyone who
     downloads it, so the credentials have to live on a server.
  2. Keeping SQL in one place means the app can be changed without touching the
     schema, and the schema without rebuilding the app.

Run it with:
    python app.py

Configuration comes from server/.env - copy .env.example and fill in the
Supabase connection string.
"""

import os
import re
from decimal import Decimal

import psycopg2
import psycopg2.extras
from flask import Flask, jsonify, request
from psycopg2 import pool

# --------------------------------------------------------------------------
# configuration
# --------------------------------------------------------------------------


def load_env(path=".env"):
    """Minimal .env reader.

    python-dotenv would do this, but one fewer dependency is one fewer thing to
    install before the app will run on a marker's machine.
    """
    here = os.path.join(os.path.dirname(os.path.abspath(__file__)), path)
    if not os.path.exists(here):
        return
    with open(here, "r", encoding="utf-8") as handle:
        for line in handle:
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            os.environ.setdefault(key.strip(), value.strip().strip('"').strip("'"))


load_env()

DATABASE_URL = os.environ.get("DATABASE_URL", "")
PORT = int(os.environ.get("PORT", "3000"))

if not DATABASE_URL:
    raise SystemExit(
        "DATABASE_URL is not set.\n"
        "Copy server/.env.example to server/.env and paste your Supabase\n"
        "connection string into it, then run this script again."
    )

app = Flask(__name__)

# A small pool rather than a connection per request. Supabase's free tier caps
# concurrent connections, and opening one per request exhausts it quickly.
CONNECTIONS = pool.SimpleConnectionPool(1, 5, DATABASE_URL)


class ApiError(Exception):
    def __init__(self, message, status=400):
        super().__init__(message)
        self.message = message
        self.status = status


@app.errorhandler(ApiError)
def handle_api_error(error):
    return jsonify({"error": error.message}), error.status


@app.errorhandler(Exception)
def handle_unexpected(error):
    app.logger.exception("unhandled error")
    return jsonify({"error": "Server error: %s" % error}), 500


def query(sql, args=(), fetch="all"):
    """Run one statement on a pooled connection and always give it back.

    Without the finally block a failed query leaks its connection, and after
    five failures the pool is empty and every later request hangs.
    """
    connection = CONNECTIONS.getconn()
    try:
        with connection.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cursor:
            cursor.execute(sql, args)
            result = None
            if fetch == "all":
                result = cursor.fetchall()
            elif fetch == "one":
                result = cursor.fetchone()
        connection.commit()
        return result
    except Exception:
        connection.rollback()
        raise
    finally:
        CONNECTIONS.putconn(connection)


def serialise(row):
    """Decimal and date are not JSON types, so convert them on the way out."""
    if row is None:
        return None
    out = {}
    for key, value in row.items():
        if isinstance(value, Decimal):
            out[key] = float(value)
        elif hasattr(value, "isoformat"):
            out[key] = value.isoformat()
        else:
            out[key] = value
    return out


# --------------------------------------------------------------------------
# validation
# --------------------------------------------------------------------------

VALID_UNITS = {
    "g", "kg", "mg", "ml", "l", "tsp", "tbsp", "cup",
    "piece", "slice", "clove", "can", "packet",
}


def clean_payload(body, require_all=True):
    """Validate an incoming pantry item.

    The Android form validates too, but a server that trusts its client is one
    curl command away from bad rows, and a NUMERIC column will reject a string
    with an error the user cannot act on.
    """
    if not isinstance(body, dict):
        raise ApiError("Request body must be a JSON object.")

    fields = {}

    if "name" in body or require_all:
        name = str(body.get("name", "")).strip()
        if not name:
            raise ApiError("Ingredient name is required.")
        if len(name) > 80:
            raise ApiError("Ingredient name must be 80 characters or fewer.")
        if not re.match(r"^[A-Za-z0-9 ,'()\-/.]+$", name):
            raise ApiError("Ingredient name contains unsupported characters.")
        fields["name"] = name

    if "quantity" in body or require_all:
        try:
            quantity = float(body.get("quantity"))
        except (TypeError, ValueError):
            raise ApiError("Quantity must be a number.")
        if quantity <= 0:
            raise ApiError("Quantity must be greater than zero.")
        if quantity > 100000:
            raise ApiError("Quantity is unrealistically large.")
        fields["quantity"] = round(quantity, 2)

    if "unit" in body or require_all:
        unit = str(body.get("unit", "piece")).strip().lower() or "piece"
        if unit not in VALID_UNITS:
            raise ApiError("Unit must be one of: %s" % ", ".join(sorted(VALID_UNITS)))
        fields["unit"] = unit

    if "expiry_date" in body:
        expiry = body.get("expiry_date")
        if expiry in (None, "", "null"):
            fields["expiry_date"] = None
        else:
            expiry = str(expiry).strip()
            if not re.match(r"^\d{4}-\d{2}-\d{2}$", expiry):
                raise ApiError("Expiry date must be in YYYY-MM-DD format.")
            fields["expiry_date"] = expiry

    if not fields:
        raise ApiError("Nothing to update.")
    return fields


# --------------------------------------------------------------------------
# routes - pantry (full CRUD)
# --------------------------------------------------------------------------


@app.get("/api/health")
def health():
    row = query("SELECT count(*) AS recipes FROM recipe", fetch="one")
    return jsonify({"status": "ok", "recipes": row["recipes"]})


@app.get("/api/pantry")
def list_pantry():
    rows = query(
        "SELECT id, name, quantity, unit, expiry_date, updated_at"
        "  FROM pantry_item ORDER BY lower(name)"
    )
    return jsonify([serialise(r) for r in rows])


@app.post("/api/pantry")
def create_pantry_item():
    fields = clean_payload(request.get_json(silent=True) or {})
    existing = query(
        "SELECT id FROM pantry_item WHERE lower(trim(name)) = lower(trim(%s))",
        (fields["name"],), fetch="one",
    )
    if existing:
        raise ApiError("'%s' is already in your pantry. Edit that item instead."
                       % fields["name"], 409)
    row = query(
        "INSERT INTO pantry_item (name, quantity, unit, expiry_date)"
        " VALUES (%s, %s, %s, %s)"
        " RETURNING id, name, quantity, unit, expiry_date, updated_at",
        (fields["name"], fields["quantity"], fields["unit"], fields.get("expiry_date")),
        fetch="one",
    )
    return jsonify(serialise(row)), 201


@app.put("/api/pantry/<int:item_id>")
def update_pantry_item(item_id):
    fields = clean_payload(request.get_json(silent=True) or {}, require_all=False)
    assignments = ", ".join("%s = %%s" % column for column in fields)
    args = list(fields.values()) + [item_id]
    row = query(
        "UPDATE pantry_item SET %s WHERE id = %%s"
        " RETURNING id, name, quantity, unit, expiry_date, updated_at" % assignments,
        args, fetch="one",
    )
    if row is None:
        raise ApiError("No pantry item with id %d." % item_id, 404)
    return jsonify(serialise(row))


@app.delete("/api/pantry/<int:item_id>")
def delete_pantry_item(item_id):
    row = query("DELETE FROM pantry_item WHERE id = %s RETURNING id", (item_id,), fetch="one")
    if row is None:
        raise ApiError("No pantry item with id %d." % item_id, 404)
    return jsonify({"deleted": item_id})


# --------------------------------------------------------------------------
# routes - recipes (read only; the seed owns this data)
# --------------------------------------------------------------------------


@app.get("/api/recipes")
def list_recipes():
    """Return every recipe with its ingredients nested.

    The app needs all of them anyway to run strict matching, and one request
    that returns twenty recipes beats twenty-one round trips over mobile data.
    """
    recipes = query(
        "SELECT id, name, description, prep_steps, prep_minutes, serves"
        "  FROM recipe ORDER BY name"
    )
    ingredients = query(
        "SELECT recipe_id, name, quantity, unit FROM recipe_ingredient ORDER BY id"
    )

    by_recipe = {}
    for ingredient in ingredients:
        by_recipe.setdefault(ingredient["recipe_id"], []).append({
            "name": ingredient["name"],
            "quantity": float(ingredient["quantity"]),
            "unit": ingredient["unit"],
        })

    payload = []
    for recipe in recipes:
        item = serialise(recipe)
        item["ingredients"] = by_recipe.get(recipe["id"], [])
        payload.append(item)
    return jsonify(payload)


@app.get("/api/recipes/<int:recipe_id>")
def get_recipe(recipe_id):
    recipe = query(
        "SELECT id, name, description, prep_steps, prep_minutes, serves"
        "  FROM recipe WHERE id = %s", (recipe_id,), fetch="one",
    )
    if recipe is None:
        raise ApiError("No recipe with id %d." % recipe_id, 404)
    payload = serialise(recipe)
    payload["ingredients"] = [
        {"name": r["name"], "quantity": float(r["quantity"]), "unit": r["unit"]}
        for r in query(
            "SELECT name, quantity, unit FROM recipe_ingredient"
            "  WHERE recipe_id = %s ORDER BY id", (recipe_id,)
        )
    ]
    return jsonify(payload)


if __name__ == "__main__":
    print("Smart Pantry Manager API")
    print("  listening on http://0.0.0.0:%d" % PORT)
    print("  Android emulator reaches this as http://10.0.2.2:%d" % PORT)
    # host 0.0.0.0 so the emulator and a phone on the same Wi-Fi can both reach it
    app.run(host="0.0.0.0", port=PORT, debug=False)
