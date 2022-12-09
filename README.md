
# Mappy Android SDK Data XY

DataXY is a framework to collect relevant information about user's location to better fit its needs.


## Requirements

* Android SDK >= 21 (Android 5.0+)

* DataXY granted with `INTERNET`

* DataXY needs to be granted with `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION`

* An authentication ID (contact support if you don't have one)


## Versions

See [CHANGELOG](CHANGELOG.md)

## Import Data XY library

To access DTS Repository add the following configuration in your project build.gradle

``` 
  repositories {
      maven {
          credentials {
              username '${YOUR_USER_NAME}'
              password '${YOUR_PASSWORD}'
          }
          url "http://sdkandroid.mappy.net/"
      }
  }
```

In your gradle file dependencies section add : 

```
  dependencies {
      implementation 'com.mappy:data-xy:+'
  }
```


## Init framework

You must provide the authentication ID at startup (contact support if you don't have one)

```
  DataXY.initialize(context, <your_locationId>);
```

or

```
  DataXY.initialize(context, <your_client>, <your_locationId>);
```

Add the following piece of code `DataXY.onRequestPermissionsResult(context)` in order to init the framework as soon as the permission is granted

```
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
      DataXY.onRequestPermissionsResult(this)) 
  }
```

Set Application Version Name send to the server

```
  DataXY.setApplicationVersion(version);
```

By default, it's the version of project.

Refer to samples in `Data XY samples` to learn how to use the data XY SDK.


## GDPR

With GDPR (General Data Protection Regulation) it is important to be able to enable or disable the data XY SDK. To do so, on user choice change, just call


### Add Server

Enable send location for your server

```
  DataXY.enableServerTier(context, enable);
```

By default, the server is disabled.

And set the url of your server, the url must be in https

```
  DataXY.setUrlServerTier(context, url);
```

You can have a second server if you want, by using 

```
  DataXY.enableServerTier2(context, enable);
```

By default, the server 2 is disabled.

and set the url of your server, the url must be in https

```
  DataXY.setUrlServerTier2(context, url);
```


## Profiling and Studying

If you enable Profiling or Studying, the locations will be send to the server


Enable profiling

```
  DataXY.enableProfiling(context, enable);
```

By default, it's disabled.

Enable studying

```
  DataXY.enableAnonymousStudiesLog(context, enable);
```

By default, it's disabled.


## Custom consent

You can add custom consent in url, this consent is a pair of key / boolean

```
  DataXY.addConsent("myconsent", true);
```

If you want remove a custom consent added just call

```
  DataXY.removeConsent("myconsent");
```

To clear all custom consent 

```
  DataXY.clearConsents();
```


## Configuration

```
  DataXY.configure(context, configuration);
```

The parameter configuration is an object

```
        new Configuration.Builder()
             .enable(true) // true or false
             .setInterval(time) // interval time in milliseconds
             .setMinimumPoints(100) // minimum number of points needed to send to the server
             .setMaximumIntervalConfigure(1000) // interval in milliseconds before forcing sending
             .build()); // build object
```


### Contact
* **Technical support:** support.sdk.android@adhslx.com
* **Commercial support:** vosdonnees@adhslx.com
