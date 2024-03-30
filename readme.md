# Server sending notifications to client

Note: The app is full of bad practices & ugly design. 

It is for personal use with minimal effort to receive notifications from a home server.

## Server

- Holds devices which are defined by name, token & encryption key
- Devices can be registered
- Devices can be deleted
- When an api is hit, all devices receive a push notification

## Client

- Registers itself to the server
- Listens to notifications
- Keeps a list of the notifications as logs
