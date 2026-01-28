package ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload DTO for the pickupItem command.
 */
public record PickupItemPayload(
        @JsonProperty("itemEntityId") Long itemEntityId,
        @JsonProperty("pickerEntityId") Long pickerEntityId,
        @JsonProperty("slotIndex") Float slotIndex
) {
    public long getItemEntityId() {
        return itemEntityId != null ? itemEntityId : 0L;
    }

    public long getPickerEntityId() {
        return pickerEntityId != null ? pickerEntityId : 0L;
    }

    public int getSlotIndex() {
        return slotIndex != null ? slotIndex.intValue() : 0;
    }

    public boolean hasItemEntityId() {
        return itemEntityId != null;
    }

    public boolean hasPickerEntityId() {
        return pickerEntityId != null;
    }
}
