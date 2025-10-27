package com.pawsome.rescue.features.inventory;

import com.pawsome.rescue.auth.entity.Role;
import com.pawsome.rescue.auth.entity.User;
import com.pawsome.rescue.auth.entity.UserRole;
import com.pawsome.rescue.auth.repository.UserRepository;
import com.pawsome.rescue.auth.repository.UserRoleRepository;
import com.pawsome.rescue.features.inventory.dto.*;
import com.pawsome.rescue.features.inventory.entity.*;
import com.pawsome.rescue.features.inventory.repository.*;
import com.pawsome.rescue.features.notification.NotificationService;
import com.pawsome.rescue.features.notification.NotificationType;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);

    @Autowired private InventoryItemRepository inventoryItemRepository;
    @Autowired private ItemCategoryRepository itemCategoryRepository;
    @Autowired private FirstAidKitRepository firstAidKitRepository;
    @Autowired private FirstAidKitItemRepository firstAidKitItemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RequisitionRepository requisitionRepository;
    @Autowired private InventoryLogRepository inventoryLogRepository;
    @Autowired private ModelMapper modelMapper;
    @Autowired private RequisitionItemRepository requisitionItemRepository;

    @Autowired
    private NotificationService notificationService;
    @Autowired
    private UserRoleRepository userRoleRepository;

    // First-Aid Kit Management
    @Transactional
    public FirstAidKitDto getFirstAidKitByUserId(Long userId) {
        FirstAidKit firstAidKit = firstAidKitRepository.findByUserId(userId)
                .orElseGet(() -> createFirstAidKit(userId));
        return modelMapper.map(firstAidKit, FirstAidKitDto.class);
    }

    @Transactional
    public FirstAidKitItemDto addPersonallyProcuredItem(Long userId, FirstAidKitItemDto itemDto) {
        FirstAidKit firstAidKit = firstAidKitRepository.findByUserId(userId)
                .orElseGet(() -> createFirstAidKit(userId));
        InventoryItem inventoryItem = inventoryItemRepository.findById(itemDto.getInventoryItemId())
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found"));

        FirstAidKitItem existingItem = firstAidKit.getItems().stream()
                .filter(item -> item.getInventoryItem().getId().equals(inventoryItem.getId()) && item.isPersonallyProcured())
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + itemDto.getQuantity());
            firstAidKitItemRepository.save(existingItem);
            createLog(inventoryItem, firstAidKit.getUser(), null, "PERSONALLY_PROCURED_UPDATE", itemDto.getQuantity());
            return modelMapper.map(existingItem, FirstAidKitItemDto.class);
        } else {
            FirstAidKitItem newItem = new FirstAidKitItem();
            newItem.setFirstAidKit(firstAidKit);
            newItem.setInventoryItem(inventoryItem);
            newItem.setQuantity(itemDto.getQuantity());
            newItem.setPersonallyProcured(true);
            firstAidKitItemRepository.save(newItem);
            createLog(inventoryItem, firstAidKit.getUser(), null, "PERSONALLY_PROCURED_ADD", itemDto.getQuantity());
            return modelMapper.map(newItem, FirstAidKitItemDto.class);
        }
    }

    @Transactional
    public void removeItemFromFirstAidKit(Long userId, Long itemId) {
        FirstAidKit firstAidKit = firstAidKitRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("First aid kit not found for user."));

        FirstAidKitItem itemToRemove = firstAidKitItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found in first aid kit."));

        if (!itemToRemove.getFirstAidKit().getId().equals(firstAidKit.getId())) {
            throw new IllegalStateException("Item does not belong to this user's first aid kit.");
        }

        // --- ✨ FIX: Removed the logic that returns items to stock ---
        // if (!itemToRemove.isPersonallyProcured()) {
        //     InventoryItem inventoryItem = itemToRemove.getInventoryItem();
        //     inventoryItem.setQuantity(inventoryItem.getQuantity() + itemToRemove.getQuantity());
        //     inventoryItemRepository.save(inventoryItem);
        //     createLog(inventoryItem, firstAidKit.getUser(), null, "RETURNED_TO_STOCK", itemToRemove.getQuantity());
        // }
        // --- End Fix ---

        // Log that the item was used/removed
        createLog(itemToRemove.getInventoryItem(), firstAidKit.getUser(), null, "USED_FROM_KIT", itemToRemove.getQuantity());

        firstAidKitItemRepository.delete(itemToRemove);
    }


    // Requisition Workflow
    @Transactional
    public RequisitionDto createRequisition(Long userId, List<RequisitionItemDto> itemDtos) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Requisition requisition = new Requisition();
        requisition.setUser(user);

        for (RequisitionItemDto itemDto : itemDtos) {
            InventoryItem inventoryItem = inventoryItemRepository.findById(itemDto.getInventoryItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Inventory item #" + itemDto.getInventoryItemId() + " not found"));

            RequisitionItem requisitionItem = new RequisitionItem();
            requisitionItem.setRequisition(requisition);
            requisitionItem.setInventoryItem(inventoryItem);
            requisitionItem.setQuantity(itemDto.getQuantity());
            requisition.getItems().add(requisitionItem);
        }

        Requisition savedRequisition = requisitionRepository.save(requisition);

        // --- Send Notification to Inventory Managers ---
        try {
            List<Role.RoleName> managerRoles = List.of(Role.RoleName.INVENTORY_MANAGER, Role.RoleName.SUPER_ADMIN);
            List<User> managers = userRoleRepository.findAllByRole_NameIn(managerRoles)
                    .stream()
                    .map(UserRole::getUser)
                    .distinct()
                    .collect(Collectors.toList());

            if (!managers.isEmpty()) {
                String message = String.format("New inventory request (#%d) submitted by %s %s.",
                        savedRequisition.getId(),
                        user.getFirstName(),
                        user.getLastName());

                for (User manager : managers) {
                    if (!manager.getId().equals(userId)) {
                        notificationService.createNotification(
                                manager,
                                NotificationType.INVENTORY,
                                null,
                                message,
                                savedRequisition.getId(),
                                user
                        );
                    }
                }
                logger.info("Sent inventory request notification to {} managers for Requisition ID: {}", managers.size(), savedRequisition.getId());
            } else {
                logger.warn("New inventory request created (ID: {}), but no INVENTORY_MANAGER or SUPER_ADMIN users found to notify.", savedRequisition.getId());
            }
        } catch (Exception e) {
            logger.error("Failed to send inventory request notifications for Requisition ID: {}. Error: {}", savedRequisition.getId(), e.getMessage(), e);
        }

        savedRequisition.getItems().forEach(item ->
                createLog(item.getInventoryItem(), user, null, "REQUESTED", item.getQuantity())
        );

        return modelMapper.map(savedRequisition, RequisitionDto.class);
    }

    @Transactional
    public void approveRequisition(Long requisitionId) {
        Requisition requisition = requisitionRepository.findById(requisitionId)
                .orElseThrow(() -> new IllegalArgumentException("Requisition not found"));

        for (RequisitionItem item : requisition.getItems()) {
            if (item.getInventoryItem().getQuantity() < item.getQuantity()) {
                throw new IllegalStateException("Cannot approve: Not enough stock for '" + item.getInventoryItem().getName() + "'. Required: " + item.getQuantity() + ", Available: " + item.getInventoryItem().getQuantity());
            }
        }

        requisition.setStatus(Requisition.RequestStatus.APPROVED);
        requisition.setApprovedAt(LocalDateTime.now());
        requisitionRepository.save(requisition);

        try {
            String message = String.format("Your inventory request (#%d) has been approved.", requisition.getId());
            notificationService.createNotification(
                    requisition.getUser(), NotificationType.INVENTORY, null, message, requisition.getId(), null
            );
        } catch (Exception e) {
            logger.error("Failed to send approval notification for Requisition ID {}: {}", requisitionId, e.getMessage(), e);
        }

        requisition.getItems().forEach(item ->
                createLog(item.getInventoryItem(), requisition.getUser(), null, "APPROVED", item.getQuantity())
        );
    }

    @Transactional
    public void denyRequisition(Long requisitionId) {
        Requisition requisition = requisitionRepository.findById(requisitionId)
                .orElseThrow(() -> new IllegalArgumentException("Requisition not found"));
        requisition.setStatus(Requisition.RequestStatus.DENIED);
        requisitionRepository.save(requisition);

        try {
            String message = String.format("Your inventory request (#%d) has been denied.", requisition.getId());
            notificationService.createNotification(
                    requisition.getUser(), NotificationType.INVENTORY, null, message, requisition.getId(), null
            );
        } catch (Exception e) {
            logger.error("Failed to send denial notification for Requisition ID {}: {}", requisitionId, e.getMessage(), e);
        }

        requisition.getItems().forEach(item ->
                createLog(item.getInventoryItem(), requisition.getUser(), null, "DENIED", item.getQuantity())
        );
    }

    @Transactional
    public void dispatchRequisition(Long requisitionId) {
        Requisition requisition = requisitionRepository.findById(requisitionId)
                .orElseThrow(() -> new IllegalArgumentException("Requisition not found"));
        if (requisition.getStatus() != Requisition.RequestStatus.APPROVED) {
            throw new IllegalStateException("Only approved requisitions can be dispatched.");
        }
        requisition.setStatus(Requisition.RequestStatus.DISPATCHED);
        requisition.setDispatchedAt(LocalDateTime.now());
        requisitionRepository.save(requisition);

        try {
            String message = String.format("Your inventory request (#%d) has been dispatched and is ready for pickup/acknowledgement.", requisition.getId());
            notificationService.createNotification(
                    requisition.getUser(), NotificationType.INVENTORY, null, message, requisition.getId(), null
            );
        } catch (Exception e) {
            logger.error("Failed to send dispatch notification for Requisition ID {}: {}", requisitionId, e.getMessage(), e);
        }

        requisition.getItems().forEach(item ->
                createLog(item.getInventoryItem(), requisition.getUser(), null, "DISPATCHED", item.getQuantity())
        );
    }

    @Transactional
    public void acknowledgeReceipt(Long requisitionId, Long userId) {
        Requisition requisition = requisitionRepository.findById(requisitionId)
                .orElseThrow(() -> new IllegalArgumentException("Requisition not found"));
        if (!requisition.getUser().getId().equals(userId)) {
            throw new IllegalStateException("You can only acknowledge your own requisitions.");
        }
        if (requisition.getStatus() != Requisition.RequestStatus.DISPATCHED) {
            throw new IllegalStateException("This requisition has not been dispatched yet.");
        }

        FirstAidKit firstAidKit = firstAidKitRepository.findByUserId(userId)
                .orElseGet(() -> createFirstAidKit(userId));

        for (RequisitionItem item : requisition.getItems()) {
            InventoryItem inventoryItem = item.getInventoryItem();
            InventoryItem currentItemState = inventoryItemRepository.findById(inventoryItem.getId())
                    .orElseThrow(() -> new IllegalStateException("Inventory item " + inventoryItem.getName() + " vanished!"));
            if (currentItemState.getQuantity() < item.getQuantity()) {
                throw new IllegalStateException("Not enough stock for " + inventoryItem.getName() + " at time of acknowledgement.");
            }
            currentItemState.setQuantity(currentItemState.getQuantity() - item.getQuantity());
            inventoryItemRepository.save(currentItemState);

            FirstAidKitItem existingItem = firstAidKit.getItems().stream()
                    .filter(kitItem -> kitItem.getInventoryItem().getId().equals(inventoryItem.getId()) && !kitItem.isPersonallyProcured())
                    .findFirst()
                    .orElse(null);

            if (existingItem != null) {
                existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
                firstAidKitItemRepository.save(existingItem);
            } else {
                FirstAidKitItem newItem = new FirstAidKitItem();
                newItem.setFirstAidKit(firstAidKit);
                newItem.setInventoryItem(inventoryItem);
                newItem.setQuantity(item.getQuantity());
                newItem.setPersonallyProcured(false);
                firstAidKitItemRepository.save(newItem);
            }

            createLog(inventoryItem, requisition.getUser(), null, "ACKNOWLEDGED", item.getQuantity());
        }

        requisition.setStatus(Requisition.RequestStatus.ACKNOWLEDGED);
        requisition.setAcknowledgedAt(LocalDateTime.now());
        requisitionRepository.save(requisition);
    }

    @Transactional(readOnly = true)
    public List<RequisitionDto> getOpenRequisitionsForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Requisition.RequestStatus> openStatuses = List.of(
                Requisition.RequestStatus.PENDING,
                Requisition.RequestStatus.APPROVED,
                Requisition.RequestStatus.DISPATCHED
        );

        return requisitionRepository.findByUserAndStatusIn(user, openStatuses).stream()
                .map(req -> modelMapper.map(req, RequisitionDto.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RequisitionDto> getRequisitionsByStatus(String status) {
        Requisition.RequestStatus requestStatus = Requisition.RequestStatus.valueOf(status.toUpperCase());
        return requisitionRepository.findByStatus(requestStatus).stream()
                .map(req -> modelMapper.map(req, RequisitionDto.class))
                .collect(Collectors.toList());
    }

    // Admin Dashboard
    @Transactional(readOnly = true)
    public InventoryDashboardStatsDto getDashboardStats() {
        InventoryDashboardStatsDto stats = new InventoryDashboardStatsDto();
        stats.setPendingRequisitions(requisitionRepository.countByStatus(Requisition.RequestStatus.PENDING));
        stats.setReadyForPickup(requisitionRepository.countByStatus(Requisition.RequestStatus.APPROVED));
        stats.setLowStockItems(inventoryItemRepository.countLowStockItems());
        return stats;
    }

    @Transactional(readOnly = true)
    public ItemInUseDto getItemUsage(Long itemId) {
        List<FirstAidKitItem> itemsInKits = firstAidKitItemRepository.findByInventoryItemId(itemId);
        ItemInUseDto dto = new ItemInUseDto();
        if (itemsInKits.isEmpty()) {
            dto.setInUse(false);
        } else {
            dto.setInUse(true);
            List<String> userNames = itemsInKits.stream()
                    .map(item -> item.getFirstAidKit().getUser().getFirstName() + " " + item.getFirstAidKit().getUser().getLastName())
                    .distinct()
                    .collect(Collectors.toList());
            dto.setUserNames(userNames);
        }
        return dto;
    }

    // CRUD for Inventory Items
    @Transactional(readOnly = true)
    public List<InventoryItemDto> getAllInventoryItems() {
        return inventoryItemRepository.findAll().stream()
                .map(item -> modelMapper.map(item, InventoryItemDto.class))
                .collect(Collectors.toList());
    }

    @Transactional
    public InventoryItemDto createInventoryItem(InventoryItemDto itemDto) {
        ItemCategory category = itemCategoryRepository.findById(itemDto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        InventoryItem item = modelMapper.map(itemDto, InventoryItem.class);
        item.setCategory(category);
        inventoryItemRepository.save(item);

        createLog(item, null, null, "ADDED_TO_STOCK", item.getQuantity());
        return modelMapper.map(item, InventoryItemDto.class);
    }

    @Transactional
    public InventoryItemDto updateInventoryItem(Long id, InventoryItemDto itemDto) {
        InventoryItem item = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));
        ItemCategory category = itemCategoryRepository.findById(itemDto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        item.setName(itemDto.getName());
        item.setCategory(category);
        item.setQuantity(itemDto.getQuantity());
        item.setLowStockThreshold(itemDto.getLowStockThreshold());
        item.setUnit(itemDto.getUnit());

        return modelMapper.map(inventoryItemRepository.save(item), InventoryItemDto.class);
    }

    @Transactional
    public void deleteInventoryItem(Long id) {
        inventoryLogRepository.deleteAllByInventoryItemId(id);
        requisitionItemRepository.deleteAllByInventoryItemId(id);
        List<FirstAidKitItem> itemsInKits = firstAidKitItemRepository.findByInventoryItemId(id);
        firstAidKitItemRepository.deleteAll(itemsInKits);
        inventoryItemRepository.deleteById(id);
    }

    // CRUD for Item Categories
    @Transactional(readOnly = true)
    public List<ItemCategoryDto> getAllItemCategories() {
        return itemCategoryRepository.findAll().stream()
                .map(category -> modelMapper.map(category, ItemCategoryDto.class))
                .collect(Collectors.toList());
    }

    @Transactional
    public ItemCategoryDto createItemCategory(ItemCategoryDto categoryDto) {
        ItemCategory category = modelMapper.map(categoryDto, ItemCategory.class);
        return modelMapper.map(itemCategoryRepository.save(category), ItemCategoryDto.class);
    }

    @Transactional
    public ItemCategoryDto updateItemCategory(Long id, ItemCategoryDto categoryDto) {
        ItemCategory category = itemCategoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        category.setName(categoryDto.getName());
        return modelMapper.map(itemCategoryRepository.save(category), ItemCategoryDto.class);
    }

    @Transactional
    public void deleteItemCategory(Long id) {
        if (inventoryItemRepository.existsByCategoryId(id)) {
            throw new IllegalStateException("Cannot delete category: It is currently in use by inventory items.");
        }
        itemCategoryRepository.deleteById(id);
    }

    // Helper and Private methods
    private FirstAidKit createFirstAidKit(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        FirstAidKit newKit = new FirstAidKit();
        newKit.setUser(user);
        return firstAidKitRepository.save(newKit);
    }

    private void createLog(InventoryItem item, User user, User approvedBy, String action, int quantity) {
        InventoryLog log = new InventoryLog();
        log.setInventoryItem(item);
        log.setUser(user);
        log.setApprovedBy(approvedBy);
        log.setAction(action);
        log.setQuantity(quantity);
        inventoryLogRepository.save(log);
    }

    @Transactional
    public void updateItemInFirstAidKit(Long userId, Long itemId, int newQuantity) {
        FirstAidKit firstAidKit = firstAidKitRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("First aid kit not found for user."));

        FirstAidKitItem itemToUpdate = firstAidKitItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found in first aid kit."));

        if (!itemToUpdate.getFirstAidKit().getId().equals(firstAidKit.getId())) {
            throw new IllegalStateException("Item does not belong to this user's first aid kit.");
        }

        if (newQuantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }

        int oldQuantity = itemToUpdate.getQuantity();

        if (newQuantity == 0) {
            // This now correctly calls the modified removeItemFromFirstAidKit
            removeItemFromFirstAidKit(userId, itemId);
            return;
        }

        int difference = newQuantity - oldQuantity; // Negative if removing

        // --- ✨ FIX: Removed the logic that returns items to stock ---
        // if (!itemToUpdate.isPersonallyProcured() && difference < 0) {
        //     InventoryItem inventoryItem = itemToUpdate.getInventoryItem();
        //     inventoryItem.setQuantity(inventoryItem.getQuantity() - difference); // -difference adds back to stock
        //     inventoryItemRepository.save(inventoryItem);
        //     createLog(inventoryItem, firstAidKit.getUser(), null, "RETURNED_TO_STOCK", -difference);
        // }
        // --- End Fix ---

        // Log the use of the item
        if (difference < 0) {
            createLog(itemToUpdate.getInventoryItem(), firstAidKit.getUser(), null, "USED_FROM_KIT", -difference);
        }

        itemToUpdate.setQuantity(newQuantity);
        firstAidKitItemRepository.save(itemToUpdate);
    }
}