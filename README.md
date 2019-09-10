Android-RateThisApp - ENHANCED
===================

Android-RateThisApp is an library to show "Rate this app" dialog ENHANCED.

![Screen shot](https://raw.github.com/kobakei/Android-RateThisApp/master/screenshot_resized.png)

The library monitors the following status

* How many times is the app launched
* How long days does it take from the app installation

and show a dialog to engage users to rate the app in Google Play.

This project implements a DialogFragment instead of a AlertDialog.

## Getting Started

### Dependency

```groovy
dependencies {
    implementation 'com.github.soenkegissel:Android-RateThisApp:1.2.5'
}
```

### Basic usage

```java
private RateThisApp rateThisApp;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    //The criteria needs to match the operator. Need to be 1 day AND 4 launches.
        Config config = new Config(1,4, Config.Operator.AND);

        rateThisApp = new RateThisApp(this, config);

        // Set callback (optional)
        rateThisApp.setCallback(new RateThisApp.Callback() {
            @Override
            public void onYesClicked() {
                Toast.makeText(MainActivity.this, "Yes event", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNoClicked() {
                Toast.makeText(MainActivity.this, "No event", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelClicked() {
                Toast.makeText(MainActivity.this, "Cancel event", Toast.LENGTH_SHORT).show();
            }
        });
}
```

That's all! You can see "Rate this app" dialog at an appropriate timing.

## Advanced usages

### Custom condition

In default, the dialog will be shown when **any off** (usage of Operator.OR) or **all off** (usage of Operator.AND) the following conditions is satisfied.

* App is launched more than 10 times
* App is launched more than 7 days later than installation.

If you want to use your own condition, please call `Config config = new Config(3,5, Config.Operator.AND)` in your Application or launcher activity onCreate method.

```java
// Custom condition: 3 days and 5 launches. Both of which must be satisfied.
Config config = new Config(3,5, Config.Operator.AND);
rateThisApp = new RateThisApp(this, config);
```

### Custom strings

You can override title, message and button labels.

```java
Config config = new Config(); //Here again with default constructor and 7 days and 10 launches with OR operator.
config.setTitle(R.string.my_own_title);
config.setMessage(R.string.my_own_message);
config.setYesButtonText(R.string.my_own_rate);
config.setNoButtonText(R.string.my_own_thanks);
config.setCancelButtonText(R.string.my_own_cancel);
rateThisApp = new RateThisApp(this, config);
```

### Custom url

In default, rate button navigates to the application page on Google Play. You can override this url as below.

```java
Config config = new Config();
config.setUrl("http://www.example.com");
rateThisApp = new RateThisApp(this, config);
```

### Opt out from your code

If you want to stop showing the rate dialog, use this method in your code.

```java
rateThisApp.stopRateDialog();
```

### Callback

You can receive yes/no/cancel button click events.

```java
rateThisApp.setCallback(new RateThisApp.Callback() {
    @Override
    public void onYesClicked() {
        Toast.makeText(MainActivity.this, "Yes event", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNoClicked() {
        Toast.makeText(MainActivity.this, "No event", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCancelClicked() {
        Toast.makeText(MainActivity.this, "Cancel event", Toast.LENGTH_SHORT).show();
    }
});
```

## Contribute this project

If you want to contribute this project, please send pull request.
In present, I need contributors who can translate resources from English/Japanese into other languages.

## License

```
Copyright 2013-2017 Keisuke Kobayashi
and
Copyright 2019 Sönke Gissel

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
