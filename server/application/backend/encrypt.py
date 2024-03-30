from Crypto.PublicKey import RSA
from Crypto.Random import get_random_bytes
from Crypto.Cipher import AES, PKCS1_OAEP
from Crypto.Util.Padding import pad
import base64

def encrypt(message: str, encryption_key: str):
    base64_decoded_key = base64.b64decode(encryption_key)
    recipient_key = RSA.import_key(base64_decoded_key)
    cipher_rsa = PKCS1_OAEP.new(recipient_key)
    session_key = get_random_bytes(16)
    enc_session_key = cipher_rsa.encrypt(session_key)
    iv_key = get_random_bytes(16)
    enc_iv_key = cipher_rsa.encrypt(iv_key)
    cipher_aes = AES.new(session_key, AES.MODE_CBC, iv_key)
    ciphertext = cipher_aes.encrypt(pad(message.encode("utf-8"), AES.block_size))
    return {
      "session_key": _encodeBytes(enc_session_key),
      "iv": _encodeBytes(enc_iv_key),
      "message": _encodeBytes(ciphertext),
    }

def _encodeBytes(bytes: bytes) :
    return base64.encodebytes(bytes).decode('ASCII').replace('\n','')