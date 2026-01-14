// ✅ תיקון: הוספת onDocumentDeleted לאימפורט
const { onDocumentCreated, onDocumentUpdated, onDocumentDeleted } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

// --- פונקציה 1: התראה על הודעת צ'אט ---
exports.sendchatnotification = onDocumentCreated("chats/{chatId}/messages/{messageId}", async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;

    const messageData = snapshot.data();
    const senderId = messageData.senderId;
    const text = messageData.message || messageData.text || messageData.content || "שלח תמונה";
    const chatId = event.params.chatId;

    console.log(`New message from ${senderId} in chat ${chatId}`);

    const chatDoc = await admin.firestore().collection("chats").doc(chatId).get();
    if (!chatDoc.exists) return;
    const chatData = chatDoc.data();

    const participants = chatData.userIds;
    if (!participants || !Array.isArray(participants)) return;

    const recipientId = participants.find(uid => uid !== senderId);
    if (!recipientId) return;

    const senderDoc = await admin.firestore().collection("users").doc(senderId).get();
    const senderName = senderDoc.exists ? (senderDoc.data().name || "הודעה חדשה") : "הודעה חדשה";

    try {
        await admin.firestore().collection("users").doc(recipientId).collection("notifications").add({
            title: senderName,
            message: text,
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            isRead: false,
            type: "CHAT",
            chatId: chatId
        });
    } catch (dbError) {
        console.log("Failed to save notification history:", dbError);
    }

    const userDoc = await admin.firestore().collection("users").doc(recipientId).get();
    if (!userDoc.exists) return;

    const fcmToken = userDoc.data().fcmToken;
    if (!fcmToken) return;

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

// --- ✅ פונקציה 2: התראה לשותף (יוזר 2) כשיוזר 1 שולח בקשה ---
// זו הפונקציה ששאלת עליה - היא כבר כתובה נכון!
exports.sendPartnerRequestNotification = onDocumentCreated("match_requests/{requestId}", async (event) => {

    const snapshot = event.data;
    if (!snapshot) return;

    const requestData = snapshot.data();
    // שליפת שם השולח (יוזר 1)
    const fromUserName = requestData.fromUserName || "משתמש";
    // זיהוי המקבל (יוזר 2)
    const toUserId = requestData.toUserId;
    const requestId = event.params.requestId;

    console.log(`New partner request created from ${requestData.fromUserId} to ${toUserId}`);

    const title = "בקשת שותפות חדשה 🤝";
    const body = `${fromUserName} רוצה לחלוק איתך הובלה! לחץ לפרטים.`;

    // שמירה בהיסטוריה (כדי שיופיע בפרגמנט ההתראות)
    try {
        await admin.firestore().collection("users").doc(toUserId).collection("notifications").add({
            title: title,
            message: body,
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            isRead: false,
            type: "PARTNER_REQUEST",
            requestId: requestId,
            actionId: requestId
        });
        console.log("Partner request notification saved to Firestore history");
    } catch (dbError) {
        console.log("Failed to save notification history:", dbError);
    }

    // שליחת הפוש לטלפון
    const userDoc = await admin.firestore().collection("users").doc(toUserId).get();
    if (!userDoc.exists) return;

    const fcmToken = userDoc.data().fcmToken;
    if (!fcmToken) {
        console.log("No FCM token for user, skipping push.");
        return;
    }

    const message = {
        token: fcmToken,
        notification: {
            title: title,
            body: body
        },
        data: {
            type: "partner_request",
            requestId: requestId
        }
    };

    return admin.messaging().send(message);
});

// --- פונקציה 3: התראה למוביל כשהשותף מאשר ---
exports.sendMoverPartnerApprovalNotification = onDocumentUpdated("match_requests/{requestId}", async (event) => {

    const beforeData = event.data.before.data();
    const afterData = event.data.after.data();

    if (beforeData.status === afterData.status || afterData.status !== "waiting_for_mover") {
        return;
    }

    const requestId = event.params.requestId;
    const moveId = afterData.moveId;

    console.log(`MatchRequest ${requestId} approved by partner. Notifying mover.`);

    const moveDoc = await admin.firestore().collection("moves").doc(moveId).get();
    if (!moveDoc.exists) return;

    const moverId = moveDoc.data().moverId;
    if (!moverId) return;

    const title = "בקשת שותף ממתינה לאישור ⏳";
    const body = "השותף אישר את ההצטרפות. כנס לפרטי ההובלה כדי לאשר סופית.";

    try {
        await admin.firestore().collection("users").doc(moverId).collection("notifications").add({
            title: title,
            message: body,
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            isRead: false,
            type: "PARTNER_APPROVAL_NEEDED",
            requestId: requestId,
            moveId: moveId
        });
    } catch (e) {
        console.log("Failed to save history", e);
    }

    const moverUserDoc = await admin.firestore().collection("users").doc(moverId).get();
    if (!moverUserDoc.exists) return;

    const fcmToken = moverUserDoc.data().fcmToken;
    if (fcmToken) {
        const message = {
            token: fcmToken,
            notification: {
                title: title,
                body: body
            },
            data: {
                type: "mover_partner_approval",
                moveId: moveId
            }
        };
        return admin.messaging().send(message);
    }
});

// --- פונקציה 4: התראה על סירוב (משתמשת ב-onDocumentDeleted) ---
exports.sendRejectionNotification = onDocumentDeleted("match_requests/{requestId}", async (event) => {

    const data = event.data.before.data();
    if (!data) return;

    const status = data.status;
    const fromUserId = data.fromUserId;
    const toUserId = data.toUserId;
    const requestId = event.params.requestId;

    console.log(`Match request ${requestId} deleted. Status was: ${status}`);

    if (status === "pending") {
        const user2Doc = await admin.firestore().collection("users").doc(toUserId).get();
        const user2Name = user2Doc.exists ? (user2Doc.data().name || "השותף") : "השותף";

        await sendPushAndSaveHistory(
            fromUserId,
            "השותפות לא אושרה ❌",
            `${user2Name} לא אישר/ה את בקשת השותפות שלך.`,
            "PARTNER_REJECTED"
        );
    }
    else if (status === "waiting_for_mover") {
        const title = "המוביל דחה את השותפות 🛑";
        const body = "המוביל לא אישר את בקשת הצירוף להובלה.";

        await sendPushAndSaveHistory(fromUserId, title, body, "MOVER_REJECTED");
        await sendPushAndSaveHistory(toUserId, title, body, "MOVER_REJECTED");
    }
});

async function sendPushAndSaveHistory(userId, title, body, type) {
    if (!userId) return;

    try {
        await admin.firestore().collection("users").doc(userId).collection("notifications").add({
            title: title,
            message: body,
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            isRead: false,
            type: type
        });
    } catch (e) {
        console.error("Failed to save notification history", e);
    }

    const userDoc = await admin.firestore().collection("users").doc(userId).get();
    if (!userDoc.exists) return;

    const fcmToken = userDoc.data().fcmToken;
    if (fcmToken) {
        try {
            await admin.messaging().send({
                token: fcmToken,
                notification: { title: title, body: body },
                data: { type: "system_message" }
            });
        } catch (e) {
            console.error("Failed to send push", e);
        }
    }
}

// --- פונקציה 5: התראה על הובלה שאושרה ---
exports.sendBookingNotification = onDocumentUpdated("moves/{requestId}", async (event) => {
    const beforeData = event.data.before.data();
    const afterData = event.data.after.data();

    const statusChanged = beforeData.status !== afterData.status;
    const isNowAccepted = afterData.status === "CONFIRMED";

    if (!statusChanged || !isNowAccepted) return;

    const customerId = afterData.customerId;
    const moverId = afterData.moverId;

    const moverDoc = await admin.firestore().collection("users").doc(moverId).get();
    const moverName = moverDoc.exists ? (moverDoc.data().name || "מוביל") : "מוביל";

    const customerDoc = await admin.firestore().collection("users").doc(customerId).get();
    if (!customerDoc.exists) return;

    const fcmToken = customerDoc.data().fcmToken;

    const title = "ההובלה אושרה! 🚚";
    const body = `${moverName} אישר את בקשת ההובלה שלך.`;

    try {
        await admin.firestore().collection("users").doc(customerId).collection("notifications").add({
            title: title,
            message: body,
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            isRead: false,
            type: "BOOKING_ACCEPTED",
            requestId: event.params.requestId
        });
    } catch (e) {
        console.log("Failed to save history", e);
    }

    if (fcmToken) {
        const payload = {
            token: fcmToken,
            notification: { title: title, body: body },
            data: {
                requestId: event.params.requestId,
                type: "order_update"
            }
        };
        return admin.messaging().send(payload);
    }
});