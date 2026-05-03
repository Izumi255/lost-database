package com.lost.database.entity;

/** Сутність предмета інвентарю. */
public class InventoryItem {

    private Long id;
    private Long playerId;
    private String itemType;
    private String itemName;
    private int quantity = 1;
    private int itemValue;

    public InventoryItem() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getItemValue() {
        return itemValue;
    }

    public void setItemValue(int itemValue) {
        this.itemValue = itemValue;
    }

    @Override
    public String toString() {
        return "InventoryItem{id=" + id + ", itemName='" + itemName + "', qty=" + quantity + "}";
    }
}
