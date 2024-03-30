from os import path
from flask import render_template
from .data import db as db
from .data.data_models import ResponseCode
from .data.data_models import DataError
from .data.data_models import Device
from .data import dao_device
from .encrypt import encrypt

import firebase_admin
from firebase_admin import credentials, messaging

import json
from flask import request, jsonify, redirect
from flask import Flask

def create_app(test_config=None):
    app = Flask(__name__)
    if (test_config == None):
        app.config.from_file('config.json', silent=True, load=json.load)
    else:
        app.config.from_mapping(test_config)
    db.init_app(app)

    firebase_cred = credentials.Certificate(app.config.get('FIREBASE_JSON'))
    firebase_app = firebase_admin.initialize_app(firebase_cred)

    @app.route('/', methods=['GET'])
    def home():
      devices = dao_device.get_devices()
      token_limit = app.config.get('TOKEN_SHOW_LIMIT')
      if (token_limit is None):
        token_limit = 2
      device_name_and_token = map(lambda device: format_device(device=device, token_limit=token_limit), devices)

      return render_template('index.html', devices=device_name_and_token)
  
    def format_device(device: Device, token_limit: int):
      device_token_first = device.token[:token_limit]
      device_token_last = device.token[-token_limit:]
      device_token = device_token_first + ' ... ' + device_token_last
      return (device.name, device_token)

    @app.route('/delete', methods=['POST'])
    def registerDelete():
      device_name = request.form.get('device_name')
      if device_name is None:
        errorResponse = jsonify({'message':'DeviceName cannot be empty!','code':ResponseCode.EMPTY_DEVICE_NAME})
        return errorResponse, 400
      dao_device.delete_device_by_name(name=device_name)
      return redirect("/")

    @app.route("/register", methods=['POST'])
    def register():
      device_token = request.form.get('device_token')
      device_name = request.form.get('device_name')
      encryption_key = request.form.get('encryption_key')
      if device_token is None:
        errorResponse = jsonify({'message':'DeviceToken cannot be empty!','code':ResponseCode.EMPTY_DEVICE_TOKEN, 'request': request.form})
        return errorResponse, 400
      if device_name is None:
        errorResponse = jsonify({'message':'DeviceName cannot be empty!','code':ResponseCode.EMPTY_DEVICE_NAME})
        return errorResponse, 400
      if encryption_key is None:
        errorResponse = jsonify({'message':'DeviceEncryption cannot be empty!','code':ResponseCode.EMPTY_DEVICE_ENCRYPTION})
        return errorResponse, 400

      device = Device(name=device_name, token=device_token, encryption_key=encryption_key)
      dao_device.delete_device_by_name(name=device.name)
      result = dao_device.insert_device(device)
      if result == DataError.DEVICE_INSERT_ERROR:
        errorResponse = jsonify({'message':'Couldn\'t save device!','code':ResponseCode.DEVICE_SAVE_FAILURE})
        return errorResponse, 400

      return redirect("/")

    @app.route("/notify", methods=['POST'])
    def notify():
      service = request.form.get('service') # name of the service
      priority = request.form.get('priority') # Low, Medium, High
      log = request.form.get('log') # log message
      
      # could use batching but there shouldn't be that many devices so ¯\_(ツ)_/¯
      devices = dao_device.get_devices()
      if service and priority and log:
        for device in devices:
          dataWithEncryptedLog = encrypt(message=log, encryption_key=device.encryption_key)
          dataWithEncryptedLog['priority'] = priority
          dataWithEncryptedLog['service'] = service
          message = messaging.Message(
            data = dataWithEncryptedLog,
            token = device.token
          )
          messaging.send(message)
        return redirect("/")
      else:
        errorResponse = jsonify({'message':'service, priority & log cannot be empty!','code':ResponseCode.NOTIFICATION_PARAMS_MISSING})
        return errorResponse, 400
    
    return app

if __name__ == "__main__":
    app = create_app()
    app.run(host='0.0.0.0')


           
        
        