# JDoc4droid
[<img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' width='100px'/>](https://play.google.com/store/apps/details?id=io.github.danthe1st.jdoc4droid&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1)

A Javadoc Viewer for Android

## Installation
JDoc4droid is available on Google Play [here](https://play.google.com/store/apps/details?id=io.github.danthe1st.jdoc4droid) but you can also download an APK of any commit from [GitHub Actions](https://github.com/danthe1st/JDoc4droid/actions).

## Setup with AndroidStudio
* Install AndroidStudio
* Clone the project
* Open the project in AndroidStudio
* Connect an android device (emulator or real device) via adb
* Run it

## Setup without IDE
* Clone the project
* If you are using Windows, run `gradlew build` in the project directory
* If you are using a UNIX-like operating system, run `./gradlew build` in the project directory
* After the command finishes, you should find a file named `app-debug.apk` in the directory `app/build/outputs/apk/debug`
  and a file named `app-release-unsigned.apk` in the directory `app/build/outputs/apk/release`
* You can install the apk file on an android device (emulator or real device) and use it
