# App List

[![Android CI](https://github.com/keeganwitt/android-app-list/actions/workflows/android.yml/badge.svg)](https://github.com/keeganwitt/android-app-list/actions/workflows/android.yml)
[![codecov](https://codecov.io/github/keeganwitt/android-app-list/graph/badge.svg?token=O4K7X7MEQG)](https://codecov.io/github/keeganwitt/android-app-list)

This app lists a selected piece of application info across all apps, to see that information at a glance.
Some of the useful things that can be displayed are

* The package manager that installed the app
* The target SDK
* The number of requested/granted permissions
* Whether the app is enabled

<img src="images/screenshot-1.png" alt="Screenshot 1" width="300"/>
<img src="images/screenshot-2.png" alt="Screenshot 2" width="300"/>

## Setup

This project uses Firebase. For security reasons, the `google-services.json` file is not included in the repository.

To build the project:
1. The build script will automatically create a `app/google-services.json` file from `app/google-services.json.template` if it's missing. This allows the project to build with dummy values.
2. To use Firebase features, you must replace `app/google-services.json` with your own valid configuration file from the Firebase Console.
