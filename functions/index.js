const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendchatnotification = onDocumentCreated("chats/{chatId}/messages/{messageId}", async (event) => {
    
    const snapshot = event.data;
    if (!snapshot) return;

    const messageData = snapshot.data();
    const senderId = messageData.senderId;
    // Look for various text fields
    const text = messageData.message || messageData.text || messageData.content || "שלח תמונה";
    const chatId = event.params.chatId;

    console.log(`New message from ${senderId} in chat ${chatId}`);

    // Get Chat Metadata
    const chatDoc = await admin.firestore().collection("chats").doc(chatId).get();
    if (!chatDoc.exists) return;
    const chatData = chatDoc.data();

    // Get Participants
    const participants = chatData.userIds; 
    if (!participants || !Array.isArray(participants)) return;

    // Find Recipient
    const recipientId = participants.find(uid => uid !== senderId);
    if (!recipientId) return;

    // Get Sender Name
    const senderDoc = await admin.firestore().collection("users").doc(senderId).get();
    const senderName = senderDoc.exists ? (senderDoc.data().name || "הודעה חדשה") : "הודעה חדשה";

    // --- 7. SAVE TO FIRESTORE HISTORY (Populates Notification Center) ---
    try {
        await admin.firestore().collection("users").doc(recipientId).collection("notifications").add({
            title: senderName,
            message: text,
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            isRead: false,
            type: "CHAT",
            chatId: chatId
        });
        console.log("Notification saved to history for Notification Center");
    } catch (dbError) {
        console.log("Failed to save notification history:", dbError);
    }

    // 8. Send Push Notification (If token exists)
    const userDoc = await admin.firestore().collection("users").doc(recipientId).get();
    if (!userDoc.exists) return;
    
    const fcmToken = userDoc.data().fcmToken;
    if (!fcmToken) {
        console.log("No FCM token, skipping push.");
        return;
    }

    const message = {
        token: fcmToken,
        notification: {
            title: senderName,
            body: text
        },
        data: { chatId: chatId }
    };

    return admin.messaging().send(message);
});

exports.sendBookingNotification = onDocumentUpdated("requests/{requestId}", async (event) => {
    
    // 1. Get Old and New Data
    const beforeData = event.data.before.data();
    const afterData = event.data.after.data();

    // 2. Check if Status Changed to "accepted"
    // (Adjust "accepted" to match your exact string in the database)
    const statusChanged = beforeData.status !== afterData.status;
    const isNowAccepted = afterData.status === "accepted";

    if (!statusChanged || !isNowAccepted) {
        return; // Exit if it's not the change we care about
    }

    console.log(`Request ${event.params.requestId} was accepted!`);

    const customerId = afterData.customerId; // Who ordered the move?
    const moverId = afterData.moverId;       // Who accepted it?

    // 3. Get Mover's Name (To show "David accepted your request")
    const moverDoc = await admin.firestore().collection("users").doc(moverId).get();
    const moverName = moverDoc.exists ? (moverDoc.data().name || "מוביל") : "מוביל";

    // 4. Get Customer's Token (To send the push)
    const customerDoc = await admin.firestore().collection("users").doc(customerId).get();
    if (!customerDoc.exists) return;
    
    const fcmToken = customerDoc.data().fcmToken;

    // 5. Create the Message
    const title = "ההובלה אושרה! 🚚";
    const body = `${moverName} אישר את בקשת ההובלה שלך.`;

    // --- 6. SAVE TO NOTIFICATION CENTER HISTORY ---
    // (This ensures it shows up in the list we just built)
    try {
        await admin.firestore().collection("users").doc(customerId).collection("notifications").add({
            title: title,
            message: body,
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            isRead: false,
            type: "BOOKING_ACCEPTED", // Useful for icon logic later
            requestId: event.params.requestId
        });
    } catch (e) {
        console.log("Failed to save history", e);
    }

    // 7. Send Push (If user has notifications enabled)
    if (fcmToken) {
        const payload = {
            token: fcmToken,
            notification: {
                title: title,
                body: body,
            },
            data: {
                requestId: event.params.requestId,
                type: "order_update" // Check this in MainActivity to redirect to Order Details
            }
        };
        return admin.messaging().send(payload);
    }
});