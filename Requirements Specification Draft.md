# Tab 1

REQUIREMENTS SPECIFICATION  
(Inesh Agarwal, Sinjon Dearborn, Jayden Lee, Annika Liu, Justin Tran)

# Tab 2

**APP TITLE:**

*Hey There\!*

Inesh Agarwal(Inesha1@uci.edu)  
Sinjon Dearborn(sdearbor@uci.edu),   
Jayden Lee(jaydenl9@uci.edu),   
Justin Tran(jgtran1@uci.edu), and  
 Annika Liu(annikl5@uci.edu)

# Tab 3

**EXECUTIVE SUMMARY:**

Hey There\! is an IRL (In Real Life) common-interest friend finder that seeks to address social isolation among individuals through location tagging.

Target Audience: Adults (primarily in the United States) interested in meeting new people who share their hobbies or interests.

Objective: To create a platform where people can be brought together through their interest in something, keeping user privacy and security in terms of location information as the most important priority.

Key Features: Subscription-based interest tags, heatmap-style mapping based on interests, real-time instant messaging capability, and detailed location information management (such as turning off location information, adding random noise locations, and blocking locations).

Assumptions: The person using the application is a modern smartphone user (either iOS or Android). They have working GPS and internet access, and are prepared to at least give generalized location data to enable location-based matches.

# Tab 4

**APPLICATION CONTEXT/ENVIRONMENTAL CONSTRAINTS**

* Environment (Physical/World): The application will run primarily on mobile devices carried by users in real-world environments (outdoors, public spaces, events).

* Hardware/Platform: Mobile smartphones running the latest versions of iOS or Android.

* Dependencies: There is heavy reliance on the Google Maps API for map rendering, pin placement, and heat maps of interests. Also, there is the use of the native GPS of the device and the internet to sync locations and send messages.

# Tab 5

**FUNCTIONAL REQUIREMENTS**

The system consists of several basic entities:

User Profile Entity: Contains states (verified/unverified, active/inactive), attributes (age, gender, description), and relationships (subscriptions to tags, friendships).

Tag Engine Entity: Controls the tag database containing freeform and developer-created tags.

Location Manager Entity: Uses the Google Maps API. Keeps track of states (precise, randomized, hidden), attributes (longitude/latitude coordinates, blackout area radii).

Meetup Event Entity: Consists of pins, with attributes such as time and geographical location, as well as related tags.

**FUNCTIONAL REQUIREMENTS ANALYSES:**

| Feature | Pros for Users | Cons for Users | Potential Ethical Concerns |
| :---- | :---- | :---- | :---- |
| **1\. Multiple Interest Tags** | Allows users to express diverse hobbies; increases chances of finding a match. | Can clutter profiles; users might spam tags just to get noticed. | If highly sensitive tags (e.g., political/medical) are allowed, it could subject users to targeted harassment. |
| **2\. User-Defined Filtering (Age/Gender)** | Users can control who they interact with, increasing comfort and relevance. | May create echo chambers or drastically reduce the overall pool of potential friends. | **Critical:** Limiting age ranges is necessary to prevent adults from tracking minors. |
| **3\. Precise vs. Randomized Location** | Gives users control over their safety; allows them to be "seen" without exposing their exact address. | Randomized noise might make it slightly harder to actually find someone in a crowded park or venue. | If the "noise" radius is too small, a bad actor could still triangulate a user's exact location. |
| **4\. Location Blackouts (Home/Work)** | Prevents stalking at sensitive, frequently visited locations. | Requires the user to actively set them up; forgetting to do so might expose their home. | Storing a user's "Home" or "Work" coordinates on servers creates a high-value target for data breaches. |
| **5\. Pre-planning Meet-up Pins** | Facilitates actual IRL interaction rather than just digital chatting; creates a clear time/place to meet. | If public, pins might attract unwanted attendees or crowds. | Bad actors could drop a fake meetup pin in an isolated area to lure users. |
| **6\. ID Verification (Own Idea 1\)** | Drastically increases trust on the platform; reduces catfishing and bot accounts. | Creates friction during sign-up; users may abandon the app if they don't want to upload their ID. | Storing government IDs is a massive privacy liability and ethical risk if the database is hacked. |
| **7\. Location History Sharing (Own Idea 2\)** | Enhances safety by letting a trusted friend or partner track where the user went during a meetup. | Significant battery drain; high storage requirements on the backend. | Severe privacy implications if a user is coerced into sharing their history by an abusive partner. |

**USE CASES:**

1\. Feature: Multiple Interest Tags

* Basic Flow: User navigates to profile, searches for "\#hiking", selects it, and saves. The map updates to show other users with the \#hiking tag.  
* Alternative Flow: User types a tag that doesn't exist yet ("\#underwaterbasketweaving"). The system prompts them to create it as a new open-ended tag.  
* Exceptional Flow: The database fails to fetch tags. The system displays a cached version of the user's tags and an error message: "Unable to load new tags."

**2\. Feature: User-Defined Filtering (Age Limit)**

* **Basic Flow:** User accesses settings, adjusts the age slider to "20-25", and applies filters. The map refreshes to only show users in that age bracket.  
* **Alternative Flow:** User sets a filter so restrictive that zero matches exist. The app displays a prompt: "No users found. Try expanding your age range."  
* **Exceptional Flow:** A user attempts to bypass the 18+ age restriction entirely. The system blocks the action and flags the account for review.

**Feature: Precise vs. Randomized Location**

* **Basic Flow:** User toggles the setting to "Randomized". The Location Manager applies a 1-mile offset to their coordinates before broadcasting to other users.  
* **Alternative Flow:** User decides to meet up and toggles back to "Precise". The system immediately updates their pin to their exact GPS coordinates.  
* **Exceptional Flow:** The device's GPS signal is lost. The system defaults to hiding the user's location entirely rather than broadcasting a stale or inaccurate precise location.

**4\. Feature: Location Blackouts**

* **Basic Flow:** User drops a pin on their apartment and sets a 500ft blackout radius. When the user enters this radius, their location disappears from the map.  
* **Alternative Flow:** User leaves the blackout radius. The app automatically resumes broadcasting their location based on their current privacy settings.  
* **Exceptional Flow:** The user's device fails to register, leaving the geofence due to poor cell service. Location sharing remains off by default to privacy.

**5\. Feature: Pre-planning Meet-up Pins**

* **Basic Flow:** User taps the map, selects "Create Meetup", inputs the \#skateboarding tag, sets the time for 2:00 PM, and publishes it.  
* **Alternative Flow:** Another user taps the pin and selects "I'm Going". The creator receives a notification.  
* **Exceptional Flow:** User tries to schedule a pin in a location the Google Maps API identifies as a body of water or as inaccessible. The app rejects the pin placement.

**6\. Feature: ID Verification**

* **Basic Flow:** User uploads a photo of their driver's license. The system verifies it, deletes the photo, and adds a "Verified" badge to their profile.  
* **Alternative Flow:** The ID photo is blurry. The system rejects it and prompts the user to retake the photo.  
* **Exceptional Flow:** The 3rd-party verification API is down. The system alerts the user that verification is temporarily unavailable and pauses the process.

**7\. Feature: Location History Sharing**

* **Basic Flow:** User selects a verified friend in the app and clicks "Share Location History (Last 24h)". The friend receives a secure link to view the path on a map.  
* **Alternative Flow:** User realizes they shared it with the wrong person. They click "Revoke Access," and the link instantly expires for the recipient.  
* **Exceptional Flow:** A user tries to share history, but they have had location tracking turned off for the last 24 hours. The app notifies them that there is no data to share.

