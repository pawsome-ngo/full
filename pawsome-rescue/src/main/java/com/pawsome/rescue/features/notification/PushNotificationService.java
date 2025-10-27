// File: src/main/java/com/pawsome/rescue/features/notification/PushNotificationService.java
package com.pawsome.rescue.features.notification;

import com.pawsome.rescue.auth.entity.User;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.List;

@Service
public class PushNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(PushNotificationService.class);

    @Value("${vapid.public-key}")
    private String vapidPublicKey;
    @Value("${vapid.private-key}")
    private String vapidPrivateKey;

    private PushService pushService;

    @Autowired
    private PushSubscriptionRepository subscriptionRepository;

    @PostConstruct
    private void init() throws GeneralSecurityException {
        Security.addProvider(new BouncyCastleProvider());
        pushService = new PushService(vapidPublicKey, vapidPrivateKey, "mailto:admin@pawsome.buzz"); // Use your email
    }

    /**
     * Sends a push notification to all registered devices for a specific user.
     * @param user The user to notify.
     * @param payload The notification message (e.g., "New incident reported").
     */
    public void sendNotificationToUser(User user, String payload) {
        List<PushSubscription> subscriptions = subscriptionRepository.findByUser(user);
        if (subscriptions.isEmpty()) {
            logger.info("No push subscriptions found for user ID {}", user.getId());
            return;
        }

        logger.info("Sending push notification to {} device(s) for user ID {}", subscriptions.size(), user.getId());

        for (PushSubscription sub : subscriptions) {
            try {
                // Create the Subscription object for the web-push library
                Subscription pushLibSubscription = new Subscription(sub.getEndpoint(), new Subscription.Keys(sub.getP256dh(), sub.getAuth()));

                // Send the notification
                // We'll use a simple text payload for now
                pushService.send(new Notification(pushLibSubscription, payload));

            } catch (JoseException | GeneralSecurityException | IOException e) {
                logger.error("Failed to send push notification to endpoint {}: {}", sub.getEndpoint(), e.getMessage());
                // TODO: Add logic here to delete expired/invalid subscriptions
                if (e.getMessage().contains("410") || e.getMessage().contains("404")) {
                    logger.info("Deleting expired subscription: {}", sub.getEndpoint());
                    subscriptionRepository.deleteByEndpoint(sub.getEndpoint());
                }
            } catch (Exception e) {
                logger.error("Unexpected error sending push notification: {}", e.getMessage(), e);
            }
        }
    }
}