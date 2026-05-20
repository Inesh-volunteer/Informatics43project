const { onSchedule } = require("firebase-functions/v2/scheduler");
const admin = require("firebase-admin");
admin.initializeApp();

exports.purgeStaleLocations = onSchedule("every 10 minutes", async (event) => {
    const db = admin.firestore();
    
    // Calculate the timestamp from 10 minutes ago
    const tenMinutesAgo = Date.now() - (10 * 60 * 1000);
    
    // Query users who have a timestamp older than 10 minutes and aren't already zeroed out
    const staleUsersQuery = db.collection("users")
        .where("locationData.lastUpdatedTimestamp", "<=", tenMinutesAgo)
        .where("locationData.publicLatitude", "!=", 0.0);

    const snapshot = await staleUsersQuery.get();

    if (snapshot.empty) {
        console.log("No stale locations found.");
        return null;
    }

    const batch = db.batch();
    
    snapshot.docs.forEach((doc) => {
        const userRef = db.collection("users").doc(doc.id);
        
        batch.update(userRef, {
            "locationData.publicLatitude": 0.0,
            "locationData.publicLongitude": 0.0,
        });
    });

    await batch.commit();
    console.log(`Successfully purged ${snapshot.size} stale locations.`);
    
    return null;
});
