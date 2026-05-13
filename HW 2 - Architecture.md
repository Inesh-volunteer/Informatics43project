# Overall Architectural Summary

# Overall Architectural Summary

Hey There is a common-interest friend finder, that utilizes geolocational data along with a tag-based filtering system to find individuals near your area that have similar interests. In order for Hey There to function, it will use a Client-server style, as it’s easily scalable and is reliable at storing data. 

For our components, we will first utilize a large scale backend database known as Firebase, to store user information through JSON formatted data, such as usernames, passwords, interest-tags, recent locational data, and so on. For grabbing locational data, it will use Google Maps’ API. Lastly, a client app will be implemented that will run on the user’s device locally in order to connect to the server and display both the user and locational information provided by the previous two components. 

For interacting with the databases and the API, it will utilize HTTP requests known as REST, in order to track what users request for the database and will process and respond to said requests. For the API, it will simply utilize the API gateway to grab locational information. Finally, to bring it all together the client app will act as the user interaction piece to make these calls to both the database and API to ultimately bring the whole application together.

# Platforms

# Platforms

1. ## iOS

2. ## Android

# Analysis

This app is best developed for mobile devices since it will track the live location of people for others to meet together in real life. iOS and Android are the most popular operating mobile operating systems and picking only either or will exclude a large amount of the population, so the best solution is to develop on both platforms.

Creating an app for both iOS and Android is also most ideal because both platforms can be developed in conjunction using one framework, flutter. Developing the apps under one framework, however, would disallow employing any platform specific features, so we decided to develop each app separately and unite them under one database to store all the information. This method would require more work to complete the project, but enables catering the design of each app towards each platform’s specific user base.

We did not consider other mobile devices such as laptops or tablets because those devices are not often or harder to use while walking around in public, especially when using them to try to find someone else.

# Programming Languages

# Programming Languages

1\. Kotlin (Android Implementation)

### **Benefits**

* **Safety Features:**   
  Kotlin’s null safety is crucial for the Location Manager Entity, preventing the app from crashing if GPS coordinates are momentarily unavailable.  
    
* **Google Maps Integration:**   
  Kotlin is a first-class language for Android, providing seamless integration with the Google Maps API for heatmaps and pin placement.

* **Interoperability:**   
  It works perfectly with existing Java libraries if you need specialized 3rd-party ID verification tools.

### **Trade-offs**

* **Overhead:**   
  While modern, Kotlin can have slightly longer compilation times compared to pure Java.  
    
* **Platform Lock-in:**   
  The code written for the Android version cannot be directly run on iOS, requiring the team to maintain a separate codebase for the Apple ecosystem.

## 2\. Swift (iOS Implementation)

### **Benefits**

* **Performance:**   
  Swift is highly optimized for Apple’s hardware, which is vital for the Location History Sharing feature to minimize significant battery drain.   
    
* **Security & Privacy:**   
  Swift provides easy access to iOS’s Secure Enclave, which is beneficial for ID Verification requirements to mitigate privacy liability.  
    
* **Modern Syntax:**   
  Swift’s expressive syntax makes implementing complex Exceptional Flows (like revoking shared history links) cleaner and easier to debug.

### **Trade-offs**

* **Ecosystem Rigidity:**   
  Swift is strictly limited to the Apple ecosystem; you cannot leverage this code for the Android version.  
    
* **API Differences:**   
  While Swift is powerful, the way it handles background location (for Location Blackouts) differs significantly from Android’s Geofencing API, leading to potential logic discrepancies between platforms.

## **System-Wide Trade-off Analysis**

**Pros:** 

With two versions, each app will feel native to its respective user base (iOS users get iOS-style toggles, Android users get Android-style navigation). You also get full access to the latest GPS and privacy features as soon as Apple or Google releases them

**Cons:** 

Development effort: the team has to implement every feature twice, once in Kotlin and once in Swift. This doubles the work for features such as the Tag Engine and the Meetup Event Entity and requires the team to ensure that a \#hiking tag behaves consistently across both platforms.

# Communication Protocols

Communication Protocol

Our app will use HTTPS requests to allow communication between the mobile apps, calls to the Google API, and the database. The apps also make calls to the database with the API to retrieve user data such as age, bio, interest tags, privacy settings, blackout zones, profile image URLs, and recent location data.

HTTPS \- used to securely send and receive data between components

Ex:

* Client → Firebase: login/signup requests and user profile updates  
* Client → Google Maps API: location and map requests  
* Database → Client: nearby users and matching interests  
* Client → Database: live location updates and privacy setting

# Examples of Component Functions

### **Examples of Component Functions and Connector Communications**

**Feature 1: Multiple Interest Tags**

Basic flow: 

| Type | Function/Call  | Data Passed |
| :---- | :---- | :---- |
| Function | TagSearch.query(text) |  |
| Data | Engine returns matches | \#hiking, \#running |
| Function | UserProfile.addTag(tagId) |  |
| Data | Profile sent to server | userId, tagId |
| Function | MapView.refresh(tags) |  |

Alternative flow:

| Type | Function/Call | Data Passed |
| :---- | :---- | :---- |
| Function | TagSearch.query(text) |  |
| Data | Engine returns empty | \[ \] |
| Function | UI.createTag |   |
| Data | Tag sent to the server | Label: \#underwaterbasketweaving |
| Function | TagEngine.createTag(label) |  |

Exception flow: 

| Type | Function/Call | Data Passed |
| :---- | :---- | :---- |
| Function | TagSearch.query(text) |  |
| Data | Server returns an error | Error |
| Function | CacheStore.getCachedTags(userId) |  |
| Function | UI.showMessage("Unable to load new tags”) |  |

**Feature 2: User-Defined Filtering**

Basic flow:

| Type | Function/Call  | Data Passed |
| :---- | :---- | :---- |
| Function | FilterUI.sliderChange(min, max) |  |
| Data | Filter is saved to the profile | ageMin: 20, ageMax: 25 |
| Function | MapView.refresh(filters) |  |
| Data | Server returns filtered pins | userId, latitude, longitude, age |

Alternative flow:

| Type | Function/Call  | Data Passed |
| :---- | :---- | :---- |
| Function | FilterUI.sliderChange(min, max) |  |
| Data | Server returns no results | Results: \[ \], count: 0 |
| Function | UI.showMessage("No users found. Try expanding your age range”) |  |

Exception flow:

| Type | Function/Call  | Data Passed |
| :---- | :---- | :---- |
| Function | FilterUI.sliderChange(0, 17\) |  |
| Function | UserProfile.validateFilter(max, min) |  |
| Data | Validation rejected | Error |
| Function | UI.showMessage(“Error”) |  |
| Function | Moderation.flagAcc(userId) |  |

**Feature 3: Precise vs. Randomized Location**

Basic flow:

| Type | Function/Call  | Data Passed |
| :---- | :---- | :---- |
| Function | PrivacyUI.toggle(“randomized”) |  |
| Function | LocationManager.setMode(“randomized”, radius \= 1 mi) |  |
| Function | GPS.getCoords() |  |
| Data | GPS returns location | Latitude, longitude |
| Function | LocationManage.applyNoise(coords) |  |
| Data | Noised location broadcast | Latitude: \~offset, longitude: \~offset, mode: “randomized”) |

Alternative flow:

| Type | Function/Call  | Data Passed |
| :---- | :---- | :---- |
| Function | PrivacyUI.toggle(“precise”) |  |
| Function | GPS.getCoords() |  |
| Data | Exact location broadcast | Latitude, longitude, mode: “precise” |
| Function | MapView.updatePin(userId, coords) |  |

Exception flow:

| Type | Function/Call  | Data Passed |
| :---- | :---- | :---- |
| Function | GPS.getCoords() |  |
| Data | GPS returns failure | Error |
| Function | LocationManager.hideUser(userId) |  |
| Function | UI.showMessage(“GPS unavailable”) |  |

**Feature 4: Location Blackouts**

Basic flow:

| Type | Function/Call  | Data Passed |
| :---- | :---- | :---- |
| Function | MapUI.onBlackout(latitude, longitude, radius) |  |
| Data | Blackout zone saved | Latitude, longitude, radius: 500, label: “Home” |
| Function | GeofenceService.registerFence(zone) |  |
| Function | GeofenceService.registerFence(userId) |  |
| Function | Broadcast.hideUser(userId) |  |

Alternative flow:

| Type | Function/Call  | Data Passed |
| :---- | :---- | :---- |
| Function | GeofenceService.exitFence(userId) |  |
| Function | LocationManager.resumeBroadcast(userId) |  |
| Data | Location broadcast resumes | Latitude, longitude, mode: “randomized” |

Exception flow:

| Type | Function/Call  | Data Passed |
| :---- | :---- | :---- |
| Function | GeofenceService.pollStatus(userId) |  |
| Data | GPS signal lost | Error |
| Function | BroadcastService.maintainHidden(userId) |  |
| Function | UI.showMessae(“Poor signal – location hidden”) |  |

**Feature 5: Pre-planning Meet-up Pins**

Basic flow:

| Type | Function/Call  | Data Passed |
| :---- | :---- | :---- |
| Function | MapUI.onLongPress(latitude, longitude) |  |
| Function | MeetupForm.onSubmit(tag, time) |  |
| Data | Meet up saved to the server | (latitude, longitude, tag: “\#skateboarding”, time: “2:00 PM”) |
| Function | MapView.renderPin(meetup |  |

Alternative flow:

| Type | Function/Call  | Data Passed |
| :---- | :---- | :---- |
| Function | MapUI.onPinTap(meetupId) |  |
| Function | MeetupDetail.onRSVP(userId) |  |
| Data | RSVP sent to the server | meetupId, userId |
| Function | NotificationServce.push(creatorId, “Someone is going\!”) |  |

Exception flow:

| Type | Function/Call  | Data Passed |
| :---- | :---- | :---- |
| Function | MapUI.onLongPress(latitude, longitude) |  |
| Data | Maps API returns the location type | placeType: “water”, accessible: false |
| Function | MeetupForm.onValidationFail() |  |
| Function | UI.showMessage(“Can’t place meetup here”) |  |

**Feature 6: ID Verification**

Basic flow:

| Type | Function/Call  | Data Passed |
| :---- | :---- | :---- |
| Function | VerifyUI.onPhotoSubmit(imageData) |  |
| Data | Image sent to provider | image: “\<base64\>” |
| Function | KYCProvider.verify(image) |  |
| Data | Provide returns result (image deleted) | Status: “APPROVED”, imageDeleted: true |
| Function | UserProfile.setVerified(true) |  |

Alternative flow:

| Type | Function/Call  | Data Passed |
| :---- | :---- | :---- |
| Function | KYCProvider.verify(image) |  |
| Data | Provider rejects submission | Status: “REJECTED”, reason: “IMAGE\_BLURRY” |
| Function | VerifyUI.showRetake(message) |  |

Exception flow:

| Type | Function/Call  | Data Passed |
| :---- | :---- | :---- |
| Function | VerificationService.submitProviderpayload) |  |
| Data | Provider times out | Error |
| Function | UserProfile.setVerificationPending() |  |
| Function | UI.showMessage(“Verification temporarily unavailable”) |  |

**Feature 7: Location History Sharing**

Basic flow:

| Type | Function/Call  | Data Passed |
| :---- | :---- | :---- |
| Function | SafetyUI.onShareHistory(friendId) |  |
| Function | HistoryService.createShareToken(userId, friendId) |  |
| Data | Share the link with a friend | mapURL, token, expiresAt |
| Function | NotificationService.push(friendId, link) |  |

Alternative flow:

| Type | Function/Call  | Data Passed |
| :---- | :---- | :---- |
| Function | SafetyUI.onRevokeShare(shareId) |  |
| Data | Token invalidated on the server | token, status: “REVOKED” |
| Function | HistoryService.invalidateToken(token) |  |

Exception flow:

| Type | Function/Call  | Data Passed |
| :---- | :---- | :---- |
| Function | SafetyUI.onShareHistory(friendId) |  |
| Function | HistoryService.fetchHistory(userId, 24 hours) |  |
| Data | Server returned no records | Records: \[ \], reason: “LOCATION\_OFF” |
| Function | UI.showMessage(“Nothing to share – location was off”) |  |

# Prototype Implementation

# Prototype Implementation

Making the prototype for the android app was smooth sailing so far with only once instance of debugging that we had to do. A lot of basic functions excluding multi-user functionality have been implemented without any thoughts of ui. We learned how to restore from git reflog, and the biggest challenge was trying to amend a pushed commit that contained an api key when we accidentally deleted the project. 