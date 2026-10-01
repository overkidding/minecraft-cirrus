package dev.simplix.cirrus.common.packet;

import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemProfile;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBT;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import dev.simplix.cirrus.item.CirrusBaseItemStack;
import dev.simplix.cirrus.item.CirrusItemType;
import dev.simplix.cirrus.text.CirrusChatElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class PacketItemStackConverter {

    public ItemStack toPacketEventsItemStack(CirrusBaseItemStack cirrusItem, int protocolVersion) {
        CirrusItemType cirrusType = cirrusItem.itemType();
        ClientVersion clientVersion = ClientVersion.getById(protocolVersion);

        String itemIdentifier = cirrusType.identifier();
        int legacyData = 0;

        if (clientVersion.isOlderThan(ClientVersion.V_1_13)) {
            legacyData = LegacyItemMapping.getDataValue(itemIdentifier);
            // Remap to the legacy base item type that PacketEvents has a pre-1.13 ID for.
            // e.g. player_head -> skeleton_skull (ID 397), orange_wool -> white_wool (ID 35)
            String baseType = LegacyItemMapping.getBaseType(itemIdentifier);
            if (baseType != null) {
                itemIdentifier = baseType;
            }
        }

        ItemType itemType = ItemTypes.getByName(itemIdentifier);
        if (itemType == null) {
            log.warn("Unknown item type: {}", cirrusType.identifier());
            return ItemStack.EMPTY;
        }

        ItemStack.Builder builder = ItemStack.builder()
            .type(itemType)
            .amount(cirrusItem.amount())
            .legacyData(legacyData);

        boolean useComponents = !clientVersion.isOlderThan(ClientVersion.V_1_20_5);

        CirrusChatElement displayName = cirrusItem.displayName();
        List<CirrusChatElement> lore = cirrusItem.lore();

        if (useComponents) {
            // 1.20.5+ data components path
            if (displayName != null && !displayName.isEmpty()) {
                Object peComponent = PacketEventsBridge.createPacketEventsComponentFromLegacy(displayName.asLegacyText());
                PacketEventsBridge.setCustomName(builder, peComponent);
            }

            if (lore != null && !lore.isEmpty()) {
                List<Object> peLoreComponents = new ArrayList<>();
                for (CirrusChatElement element : lore) {
                    peLoreComponents.add(PacketEventsBridge.createPacketEventsComponentFromLegacy(element.asLegacyText()));
                }
                PacketEventsBridge.setLore(builder, peLoreComponents);
            }
        } else {
            // Pre-1.20.5: set display name and lore via NBT display tag
            boolean pre113 = clientVersion.isOlderThan(ClientVersion.V_1_13);
            NBTCompound display = new NBTCompound();

            if (displayName != null && !displayName.isEmpty()) {
                if (pre113) {
                    display.setTag("Name", new NBTString(displayName.asLegacyText()));
                } else {
                    display.setTag("Name", new NBTString(PacketEventsBridge.toJsonFromLegacy(displayName.asLegacyText())));
                }
            }

            if (lore != null && !lore.isEmpty()) {
                NBTList<NBTString> loreList = NBTList.createStringList();
                for (CirrusChatElement element : lore) {
                    if (pre113) {
                        loreList.addTag(new NBTString(element.asLegacyText()));
                    } else {
                        loreList.addTag(new NBTString(PacketEventsBridge.toJsonFromLegacy(element.asLegacyText())));
                    }
                }
                display.setTag("Lore", loreList);
            }

            if (!display.getTags().isEmpty()) {
                builder.nbt("display", display);
            }
        }

        NBTCompound nbtData = cirrusItem.nbtData();
        if (nbtData != null && !nbtData.getTags().isEmpty()) {
            if (useComponents) {
                // 1.20.5+: extract SkullOwner into PROFILE component
                // Do NOT mutate nbtData - it's reused across renders
                NBT skullOwnerRaw = nbtData.getTags().get("SkullOwner");
                if (skullOwnerRaw instanceof NBTCompound) {
                    NBTCompound skullOwner = (NBTCompound) skullOwnerRaw;
                    ItemProfile profile = extractProfile(skullOwner);
                    if (profile != null) {
                        builder.component(ComponentTypes.PROFILE, profile);
                    }
                }
                builder.component(ComponentTypes.CUSTOM_DATA, nbtData);
            } else {
                // Pre-1.20.5: merge NBT data directly (SkullOwner, etc.)
                for (Map.Entry<String, NBT> entry : nbtData.getTags().entrySet()) {
                    builder.nbt(entry.getKey(), entry.getValue());
                }
            }
        }

        return builder.build();
    }

    /**
     * Extract an ItemProfile from a SkullOwner NBT compound.
     * Converts the legacy SkullOwner NBT format to PacketEvents' ItemProfile for 1.20.5+ compatibility.
     */
    private ItemProfile extractProfile(NBTCompound skullOwner) {
        try {
            List<ItemProfile.Property> properties = new ArrayList<>();

            NBT propertiesRaw = skullOwner.getTags().get("Properties");
            if (propertiesRaw instanceof NBTCompound) {
                NBTCompound propertiesCompound = (NBTCompound) propertiesRaw;
                NBT texturesRaw = propertiesCompound.getTags().get("textures");
                if (texturesRaw instanceof NBTList<?>) {
                    NBTList<?> texturesList = (NBTList<?>) texturesRaw;
                    for (Object entry : texturesList.getTags()) {
                        if (entry instanceof NBTCompound) {
                            NBTCompound textureEntry = (NBTCompound) entry;
                            NBT valueRaw = textureEntry.getTags().get("Value");
                            String value = valueRaw instanceof NBTString ? ((NBTString) valueRaw).getValue() : null;
                            NBT sigRaw = textureEntry.getTags().get("Signature");
                            String signature = sigRaw instanceof NBTString ? ((NBTString) sigRaw).getValue() : null;
                            if (value != null) {
                                properties.add(new ItemProfile.Property("textures", value, signature));
                            }
                        }
                    }
                }
            }

            if (properties.isEmpty()) {
                return null;
            }

            return new ItemProfile(null, null, properties);
        } catch (Exception e) {
            log.warn("Failed to extract profile from SkullOwner NBT", e);
            return null;
        }
    }
}
