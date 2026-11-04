Android-RateThisApp - ENHANCED
===================

Android-RateThisApp is a library to show a "Rate this app" dialog.

<img src="Screenshot_20260727-084521.png" alt="Rate dialog of the sample app" width="360"/>

The library monitors

* how many times the app was launched and
* how many days have passed since the app was installed

and shows a dialog that asks users to rate the app.

**Works with all major app markets:** Google Play (including the In-App Review flow), Amazon Appstore,
Samsung Galaxy Store and Huawei AppGallery.

Translated into 100+ languages.

## Getting Started

### Requirements

* minSdk 21
* compileSdk 24 or higher
* Your app's theme must be an AppCompat / MaterialComponents theme and your activities must extend
  `FragmentActivity` (e.g. `AppCompatActivity`).

### Dependency

Add the JitPack repository to your `settings.gradle`:

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

Add the dependency to your app's `build.gradle`:

```groovy
dependencies {
    implementation 'com.github.soenkegissel:Android-RateThisApp:2.1.1'
}
```

## Basic usage

### Application class

Initialize RateThisApp **once** in your Application class. Further calls of `initialize` are ignored,
so use only one of the following variants.

```java
public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Variant 1: default config (7 days OR 10 launches) and a fixed market.
        RateThisApp.initialize(this, Market.GOOGLE);

        // Variant 2: custom config.
        // Config config = new Config(3, 5, Config.Operator.OR);
        // RateThisApp.initialize(this, config, Market.GOOGLE);

        // Variant 3: market from your app's build flavor (see below).
        // RateThisApp.initialize(this, new Config(), Market.fromName(BuildConfig.MARKET));
    }
}
```

Don't forget to register the Application class in your `AndroidManifest.xml`
(`<application android:name=".MainApplication" ...>`).

### One build per market (optional)

To build one app per market, define product flavors with a `MARKET` build config field in your app's
`build.gradle` and pass `Market.fromName(BuildConfig.MARKET)` to `initialize` (variant 3 above).
Use your app's `BuildConfig`, not the one of the library.

```groovy
android {
    ...
    buildFeatures {
        buildConfig true
    }

    flavorDimensions = ["vendor"]
    productFlavors {
        google {
            dimension "vendor"
            buildConfigField "String", "MARKET", "\"GOOGLE\""
        }
        amazon {
            dimension "vendor"
            buildConfigField "String", "MARKET", "\"AMAZON\""
        }
        samsung {
            dimension "vendor"
            buildConfigField "String", "MARKET", "\"SAMSUNG\""
        }
        huawei {
            dimension "vendor"
            buildConfigField "String", "MARKET", "\"HUAWEI\""
        }
    }
}
```

`Market.fromName()` falls back to `Market.GOOGLE` for unknown or empty values.

### Activity class

The simplest setup is to show the dialog when the conditions are met, e.g. in `onCreate()` of your
main activity.

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ...
    RateThisApp.getInstance(this).showRateDialogIfNeeded(false);
}
```

That's all! The "Rate this app" dialog will now appear at an appropriate time. The dialog is shown only
once at a time, even if you call this again (e.g. after rotation).

## Advanced usage

### Show if conditions are met
```java
RateThisApp.getInstance(this).showRateDialogIfNeeded(false);
```

### Force the dialog to show
```java
RateThisApp.getInstance(this).showRateDialogIfNeeded(true);
```

### Custom style
```java
RateThisApp.getInstance(this).showRateDialogIfNeeded(R.style.MyAlertDialogStyle, false);
```

The style should extend a dialog theme, e.g. `Theme.MaterialComponents.DayNight.Dialog.Alert`.

### Custom conditions

By default the dialog is shown when **any of** (`Config.Operator.OR`) the following conditions is met.
With `Config.Operator.AND` **all of** them must be met.

* The app is launched 7 days or more after installation (or after the user pressed "Later").
* The app is launched 10 times or more (process starts, counted since installation or the last "Later").

To use your own conditions, pass a `Config` to `initialize`:

```java
public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Custom condition: 3 days and 5 launches. Both must be met.
        Config config = new Config(3, 5, Config.Operator.AND);
        RateThisApp.initialize(this, config, Market.GOOGLE);
    }
}
```

### Further configuration

```java
Config config = new Config(3, 5, Config.Operator.OR);

// Which ways the user can dismiss the dialog (dismissing counts as "Later"):
// CANCEL_MODE_BACK_KEY_OR_TOUCH_OUTSIDE (default), CANCEL_MODE_BACK_KEY or CANCEL_MODE_NONE
config.setCancelMode(Config.CANCEL_MODE_BACK_KEY);

// Open your own url instead of the market url.
// For Google Play it is used only if the In-App Review flow is not available.
config.setUrl("https://example.com/rate");

// Always use Google's FakeReviewManager (for testing).
config.setUseFakeReviewManager(true);

RateThisApp.initialize(this, config, Market.GOOGLE);
```

### Custom strings

You can override title, message and button labels in your app's `res/values/strings.xml`.

```xml
<string name="rta_dialog_title">My custom title</string>
<string name="rta_dialog_message">My custom message</string>
<string name="rta_dialog_ok">Rate me</string>
<string name="rta_dialog_cancel">Later</string>
<string name="rta_dialog_no">No, thanks</string>
```

The library ships translations for many languages. Strings in `values/` only override the default
(English) texts – to override them for other languages, add the strings to the matching
`values-xx/strings.xml` of your app as well.

### Opt out from your code

To stop showing the rate dialog, call:

```java
RateThisApp.getInstance(this).stopRateDialog();
```

### Callback

You can receive "Rate now", "No, thanks" and "Later" events. "Later" is also reported when the user
dismisses the dialog.

```java
import com.kobakei.ratethisapp.Callback;

RateThisApp.getInstance(this).setCallback(new Callback() {
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

The callback is kept by the RateThisApp singleton. If it references your Activity, remove it in
`onDestroy()` to avoid a memory leak:

```java
@Override
protected void onDestroy() {
    RateThisApp.getInstance(this).setCallback(null);
    super.onDestroy();
}
```

### Status

```java
RateThisApp rateThisApp = RateThisApp.getInstance(this);
rateThisApp.getLaunchCount();      // launches since installation or the last "Later"
rateThisApp.getOptOut();           // true if the user pressed "Rate now" or "No, thanks"
rateThisApp.getDialogShown();      // true if the dialog was shown at least once
```

## Market behavior

| Market  | "Rate now" opens | Fallback if the market app is missing |
|---------|------------------|---------------------------------------|
| GOOGLE  | Google Play In-App Review flow | Play Store app, then Play Store website |
| AMAZON  | Amazon Appstore app | Amazon website |
| SAMSUNG | Galaxy Store app | Galaxy Store website |
| HUAWEI  | AppGallery app | none (AppGallery has no web url by package name) |

### Google Play In-App Review

Google decides whether the review card is actually shown (quota), and the library is not told whether
the user rated. Google's guidelines also discourage asking questions before the review flow – keep that
in mind when customizing the dialog text.

The In-App Review flow only works for apps installed from Google Play (e.g. via an internal test track).
In debuggable builds or with `config.setUseFakeReviewManager(true)` the `FakeReviewManager` is used,
which shows no UI.

## Sample app

The `sample` module shows all features and has one flavor per market.

## Changelog

See [CHANGELOG.md](CHANGELOG.md).

## Contribute to this project

If you want to contribute to this project, please send a pull request.
Translations are especially welcome – if you find a wrong or clumsy translation, please let me know.

## License

```
Copyright 2013-2017 Keisuke Kobayashi
and
Copyright 2019-2026 Sönke Gissel

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
