# android-userinterface

This library is designed to provide hooks to log in and register devices via the [php-userhandler](https://github.com/tigerhawkvok/php-userhandler) endpoint.

## Instancing

You can instance the userhandler object like so:

```java
Userhandler u = new Userhander(getApplicationContext(),ENDPOINT_URI_STRING);
```

OR not declare the endpint until later

```java
Userhandler u = new Userhandler(getApplicationContext());
u.setEndpoint(ENDPOINT_URI_STRING);
```


## Creating a new user

Creating a new user also registers this device to the server.

```java
u.registerNewUser(FIRST_NAME,LAST_NAME,USER_HANDLE,USER_PASSWORD,INT_USER_PHONE);
```

## Registering device with an existing user

```java
u.registerNewDevice();
```

## Verifying a user's phone

The registered phone may be sent a text message during setup to confirm it. Calling

```java
u.verifyPhone();
```

Will overlay a `popupWindow` and handle the processing
