from enum import Enum
from enum import IntEnum

class Device:
    def __init__(self, token, encryption_key, name):
        self.token = token
        self.encryption_key = encryption_key
        self.name = name

    def __eq__(self, other): 
        if not isinstance(other, Device):
            return False
        return self.token == other.token \
            and self.encrpytion_key == other.encryption_key \
            and self.name == other.mname 
    
    def __str__(self):
        return 'Device(token={},encryption_key={},name={})'.format(self.token, self.encryption_key, self.name)

    def __repr__(self):
        return self.__str__
        

class DataError(Enum):
  DEVICE_INSERT_ERROR = -1

class ResponseCode(IntEnum):
    EMPTY_DEVICE_TOKEN = 401
    EMPTY_DEVICE_NAME = 402
    EMPTY_DEVICE_ENCRYPTION = 403
    DEVICE_SAVE_FAILURE = 404
    NOTIFICATION_PARAMS_MISSING = 405
