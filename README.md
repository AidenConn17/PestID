# PestID

PestID is an Android application that identifies insects from photos taken with the device camera or selected from the gallery. The app sends the image to the insect.id API by Kindwise and displays the top identification results (name, confidence, danger, roles, and an image) to the user.

## Features
- CameraX integration for taking photos directly in the app
- Gallery picker for selecting existing images
- Asynchronous API calls for insect identification
- Displays results including:
  - Common name and scientific name
  - Confidence score
  - Danger information
  - Ecological roles
  - Representative image
- Uses Glide to efficiently load and display result images from URLs

## Technology Stack
- Language: Kotlin/Java (Android)
- Camera: AndroidX CameraX
- Networking: OkHttp
- Image Loading: Glide
- JSON: org.json

## How It Works
1. Capture or select an image.
2. The image is converted to Base64 and sent to the insect.id API by Kindwise.
3. The response is parsed for confident suggestions (> 20% probability).
4. The results are shown in a dedicated screen. Images in the results are loaded with Glide.

## External Services and Libraries
- insect.id API by Kindwise: Used to identify insects and retrieve details such as names, roles, danger, and an image reference for each suggestion.
- Glide: Used to load and cache images returned in the identification results.

## Project Structure
- app/src/main/java/com/example/pestid/
  - MainActivity: Captures/chooses images, sends them for identification, and navigates to results.
  - Identification: Handles the network request to the insect.id API and parses confident suggestions.
  - ResponseActivity: Displays top identification results and renders images via Glide.
  - ThreadPerTaskExecutor: Simple executor for running tasks on background threads.

## Setup
1. Open the project in Android Studio.
2. Ensure you have the Android SDK and required components installed.
3. Add your API key:
   - Create a class or constant named `APIKey` with a public static field `API_KEY` containing your insect.id API key. The code references `APIKey.API_KEY`.
4. Sync and build the project.

## Permissions
- Camera permission is required to take photos within the app.

## Notes
- Glide is used to load URLs in the results screen for reliable, performant image handling.
- The insect.id API by Kindwise is called using OkHttp with a simple JSON body that embeds the Base64 image.

## License
This project is provided as-is for educational and demonstration purposes.
