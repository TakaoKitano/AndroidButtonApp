# AndroidButtonApp
Android application that controls Sesame lock with a cheap remote shutter device

![Bluetooth Remote Shutter][https://github.com/TakaoKitano/AndroidButtonApp/blob/master/button.jpg](https://github.com/TakaoKitano/AndroidButtonApp/blob/master/button.jpg)

# Requirements

* CANDY HOUSE sesame smartlock with WifiAP
* sesame API auth token (retrieved at CANDY HOUSE dashboard) and device ID
* ABShutter3 
* Android handset API level26 (Ver 8.0 Oreo)

# Setup

```
cd app/src/main/assets
cp buttonapp.properties.sample buttonapp.properties
put your sesame API auth token (apikey) and sasame device ID(s) into buttonapp.properties
```

Build apk as an android application (Build Variant: sesame_realmodeDebug)

# Notes

If your handset has advanced power management settings, exclude this application from the power management. 
As this application keeps a fake 'media session' to detect volume control key event, on some handsets, advanced power management functionality prevents it to work.


