import sqlite3
from os import path
from flask import current_app, g
from contextlib import contextmanager

@contextmanager
def get_cursor():
    db = get_db()
    cursor = db.cursor()
    try:
        yield (cursor)
    finally:
        cursor.close()
    db.commit()

default_database_name = "sqlitedb"

def get_db():
    current_app.config.get('DATABASE_PATH')
    if 'db' not in g:
        db_path = current_app.config.get('DATABASE_PATH')
        if (db_path is None):
            db_path = path.join(current_app.instance_path, current_app.config['DATABASE_NAME'])
        g.db = sqlite3.connect(db_path, detect_types=sqlite3.PARSE_DECLTYPES)
        g.db.row_factory = sqlite3.Row

    return g.db

def close_db(e=None):
    db = g.pop('db', None)

    if db is not None:
        db.close()

def init_db(db_path = None, schema_path = None):
    if db_path is None:
        db = get_db()
    else:
        db = sqlite3.connect(db_path, detect_types=sqlite3.PARSE_DECLTYPES)

    if schema_path is None:
        with current_app.open_resource('data/schema.sql') as f:
            script = f.read().decode('UTF-8')
    else:
        with open(schema_path, "r") as f:
            script = f.read()

    db.executescript(script)
    db.commit()
    db.close()

def init_app(app):
	app.teardown_appcontext(close_db)

if __name__ == "__main__":
    db_path = path.join('/home/flask/server/instance/', default_database_name)
    if path.exists(db_path):
        print('Database already exists at {}. Will NOT override, if necessary first delete first then restart initialization!'.format(db_path))
        exit(1)
    schema_path = path.join(path.dirname(__file__), 'schema.sql')
    init_db(db_path = db_path, schema_path = schema_path)
    print('done')
