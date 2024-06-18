Android-RateThisApp - ENHANCED
===================

Android-RateThisApp is an library to show "Rate this app" dialog ENHANCED.

![Screen shot](https://raw.github.com/kobakei/Android-RateThisApp/master/screenshot_resized.png)

The library monitors the following status

* How many times is the app launched
* How long days does it take from the app installation
* Works with all app markets (GOOGLE, AMAZON, SAMSUNG, HUAWEI)

and show a dialog to engage users to rate the app in Google Play, Amazon AppStore, Samsung Galaxy and Huawei.

## Getting Started

### Dependency

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
```

```groovy
dependencies {
    implementation 'com.github.soenkegissel:Android-RateThisApp:1.8.1'
}
```

## Basic usage

### Application class

Initialize RateThisApp on Application start. 
```java
public class MainApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        //Use with context, default config (7 days and 10 launches) and a Market (Google, Amazon, Samsung, Huawei).
        RateThisApp.initialize(this, Market.SAMSUNG);
        
        //Use with custom config and Google as Market.
        RateThisApp.initialize(this, new Config(3, 5), Market.GOOGLE);
        
        //Use with Gradle buildConfigField
        RateThisApp.initialize(this, config, Market.valueOf(BuildConfig.MARKET));
        //and add to your build.gradle(:App)
        release {
            buildConfigField "String", "MARKET", "GOOGLE"
        }
        debug {
            buildConfigField "String", "MARKET", "GOOGLE"
        }
    }
}
```

### Activity class

The most simple setup is to show the dialog if conditions match.

```java

@Override
private void ratingMethod() {
    RateThisApp.getInstance(this).showRateDialogIfNeeded(false);
}
```

That's all! You can see "Rate this app" dialog at an appropriate timing.

## Advanced usages

### Show if conditions met
```java
RateThisApp.getInstance(this).showRateDialogIfNeeded(false);
```

### Force dialog to show
```java
RateThisApp.getInstance(this).showRateDialogIfNeeded(true);
```

### Custom style
```java
RateThisApp.getInstance(this).showRateDialogIfNeeded(R.style.MyAlertDialogStyle2, false);
```

### Custom condition

In default, the dialog will be shown when **any off** (usage of Operator.OR) or **all off** (usage of Operator.AND) the following conditions is satisfied.

* App is launched after 7 days than installation.
* App is launched more than 10 times

If you want to use your own condition, call `Config config = new Config(3,5, Config.Operator.AND)` 
in your Application class method.

```java

public class MainApplication extends Application {

// Custom condition: 3 days and 5 launches. Both of which must be satisfied.
    @Override
    public void onCreate() {
        super.onCreate();
        
        Config config = new Config(3, 5, Config.Operator.AND);
        RateThisApp.initialize(this, config, Market.GOOGLE);
    }
}
```

### Custom strings

You can override title, message and button labels in your values.xml.

```java
    <string name="rta_dialog_title">My custom title</string>
    <string name="rta_dialog_message">My custom message</string>
    <string name="rta_dialog_ok">Rate me</string>
    <string name="rta_dialog_cancel">Later</string>
    <string name="rta_dialog_no">No, dude</string>
```

### Opt out from your code

If you want to stop showing the rate dialog, use this method in your code.

```java
RateThisApp.getInstance(this).stopRateDialog();
```

### Callback

You can receive yes/no/cancel button click events.

```java
RateThisApp.getInstance(this).setCallback(new RateThisApp.Callback() {
    @Override
    public void onYesClicked() {
        Toast.makeText(MainActivity.this, "Yes event", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNoClicked() {
        Toast.makeText(MainActivity.this, "No event", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLaterClicked() {
        Toast.makeText(MainActivity.this, "Later event", Toast.LENGTH_SHORT).show();
    }
});
```

## Contribute this project

If you want to contribute this project, please send pull request.
In present, I need contributors who can translate resources from English/German into other languages.

## License

```
Copyright 2013-2017 Keisuke Kobayashi
and
Copyright 2024 Sönke Gissel

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Author

Sönke Gissel
