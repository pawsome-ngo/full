// File: pawsome-ngo/full/full-d91a39b5e3886f03789eb932561a5689b5f95888/pawsome-rescue/src/main/java/com/pawsome/rescue/features/notification/NotificationType.java
package com.pawsome.rescue.features.notification;

public enum NotificationType {
    INCIDENT,    // Related to incident status changes, assignments etc.
    INVENTORY,   // Related to inventory requests, approvals, dispatch
    APPROVAL,    // Related to user account approvals
    REWARDS,     // Related to points, achievements (future)
    GENERAL      // For other general messages
}