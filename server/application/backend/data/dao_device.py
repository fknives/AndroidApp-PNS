from .data_models import Device
from .data_models import DataError
from .db import get_cursor
from sqlite3 import IntegrityError

def _device_from_row(row):
    return Device(
        name = row['device_name'],
        token = row['device_token'],
        encryption_key = row['encryption_key'],
    )


def get_devices():
    with get_cursor() as db_cursor:
        db_cursor.execute("SELECT * FROM device")
        rows = db_cursor.fetchall()

    return map(_device_from_row, rows)

_INSERT_DEVICE_SQL = "INSERT INTO device(device_name, device_token, encryption_key)"\
"VALUES(:device_name, :device_token, :encryption_key)"
def insert_device(device: Device):
    params = {
        "device_name": device.name,
        "device_token": device.token,
        "encryption_key": device.encryption_key,
    }
    with get_cursor() as db_cursor:
        try:
            db_cursor.execute(_INSERT_DEVICE_SQL, params)
        except IntegrityError as e:
            return DataError.DEVICE_INSERT_ERROR
    
    return db_cursor.lastrowid

def delete_device_by_name(name: str):
    with get_cursor() as db_cursor:
        db_cursor.execute('DELETE FROM device WHERE device_name=:name',{'name':name})

