# GReat-Image-Downloader
An app that connects to a Ricoh GR iii (or iiix) and downloads all images that aren't already on the device.

# Usage
To use the app, just enter the wifi credentials from your camera and the app will scan your `Pictures / Image Sync` folder for images you've already downloaded with the official app, after which it will download them all and finally shut down the camera to save battery.

# Code Structure
This app is built on my own Clean Architecture implementation, in order to make code easily testable and extendable. This means it is split into the following three layers:

## Domain (use cases)
This layer contains the core logic of the app, either business rules (inside the entities) or application rules (inside the use cases) in order to keep this logic as solid as possible when implementation details change. This layer is platform-independent, as it only uses Kotlin/Java code and libraries, and could even be converted to a separate external pure Kotlin module (as opposed to an Android module).

## Presentation (UI) 
This layer includes everything related to the UI of the app, as well as some Android-specific components which require Context or other system-specific resources (e.g. WiFi connection component). It has no logic of its own, beyond the coordination of the UI and the mapping of the use case instructions to it.

## Data (storage)
This layer contains the data storage and network connection code, and its main purpose is retrieving/saving data as well as coordinating data operations in a way that is transparent to the rest of the app, and therefore can easily be changed.

# To Do
While there are many improvements that can be added to this project, here is the current roadmap:
- implement presentation skeleton
- implement permissions handling code/UI
- implement WiFi connection component
- implement data storage for WiFi details
- implement data storage/retrieval for photos
- implement camera API methods
- implement foreground service 

# Extra

Thank you to https://github.com/JohnMaguire/photo_sync and https://github.com/clyang/GRsync for inspiration regarding the Ricoh API and how to handle downloading the photos!
