const deviceNameInput = document.getElementById("add_device_name");
const deviceTokenInput = document.getElementById("add_device_token");
const encryptionKeyInput = document.getElementById("add_encryption_key");

if (typeof Android !== 'undefined') {
  encryptionKeyInput.value = Android.publicKey()
  deviceTokenInput.value = Android.messagingToken()
  deviceNameInput.value = Android.deviceName()
}