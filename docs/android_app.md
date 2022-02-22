# Android App

The Android app is the center of the system. It receives IUEs from the inhaler, collects weather and sensor data, and provides the user access to view and download the data.

To fulfill these responsibilities, the app has two logical flows of information. The flow of collecting and saving an IUE which is initiated by receiving an IUE from the inhaler (Data flow) and the user experience. The database is the means of communication between these two information flows.

## User Story

The user opens the app to find a list of IUEs. The user can see the weather and wearable sensor data when the inhaler was used. The user can annotate individual IUEs and extract the list of IUEs as a CSV file which he can download, upload to Google drive, or share through email.

The transfer of IUEs from the inhaler to the app is automatic and does not involve the user.

## Data Flow

When the phone starts, the app initiates a service which detects and connects to the inhaler whenever the inhaler advertises itself. This allows the app to receive IUEs from the inhaler regardless of whether the app is open or not to ensure the timeliness of receiving IUEs. IUEs must be received as soon as possible because the sensor data is time sensitive.

The data flow is initiated by the inhaler. Once the inhaler is used, it records an IUE and attempts to send it to the app. Once the app receives the IUE, the app requests temperature, humidity, and particulate matter information about the user's immediate vicinity from the wearable sensor. Concurrently to gathering the wearable sensor information, the app collects local temperature, humidity, precipitation intensity, pollen information, and air quality index data from [tomorrow.io](https://tomorrow.io) (a weather, web API). Supplementing the local weather information with the data on the user's immediate vicinity provides the user with a clearer picture of the reason behind using the inhaler.

The IUE and collected data are displayed to the user on the app's homepage. The user can annotate the IUEs with descriptions and can export the IUEs as a CSV file which can be uploaded to google drive or shared through email.
