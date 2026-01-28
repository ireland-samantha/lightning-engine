package ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload DTO for the createItemType command.
 */
public record CreateItemTypePayload(
        @JsonProperty("matchId") Long matchId,
        @JsonProperty("name") String name,
        @JsonProperty("maxStack") Float maxStack,
        @JsonProperty("rarity") Float rarity,
        @JsonProperty("value") Float value,
        @JsonProperty("weight") Float weight,
        @JsonProperty("healAmount") Float healAmount,
        @JsonProperty("damageBonus") Float damageBonus,
        @JsonProperty("armorValue") Float armorValue
) {
    public long getMatchId() {
        return matchId != null ? matchId : 0L;
    }

    public String getName() {
        return name != null ? name : "Unknown";
    }

    public int getMaxStack() {
        return maxStack != null ? maxStack.intValue() : 1;
    }

    public int getRarity() {
        return rarity != null ? rarity.intValue() : 0;
    }

    public float getValue() {
        return value != null ? value : 0f;
    }

    public float getWeight() {
        return weight != null ? weight : 0f;
    }

    public float getHealAmount() {
        return healAmount != null ? healAmount : 0f;
    }

    public float getDamageBonus() {
        return damageBonus != null ? damageBonus : 0f;
    }

    public float getArmorValue() {
        return armorValue != null ? armorValue : 0f;
    }
}
