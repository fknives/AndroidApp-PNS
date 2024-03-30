const addDeviceDialog = document.getElementById("add_device_dialog");
const openAddDeviceCTA = document.getElementById("add_device");

// close dialog on backdrop click
addDeviceDialog.addEventListener('click', function(event) {
  var rect = addDeviceDialog.getBoundingClientRect();
  var isInDialog = (rect.top <= event.clientY && event.clientY <= rect.top + rect.height &&
    rect.left <= event.clientX && event.clientX <= rect.left + rect.width);
  if (!isInDialog) {
    addDeviceDialog.close();
  }
});

openAddDeviceCTA.addEventListener("click", () => {
  addDeviceDialog.showModal();
});