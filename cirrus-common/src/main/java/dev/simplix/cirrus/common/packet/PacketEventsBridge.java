package dev.simplix.cirrus.common.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import dev.simplix.cirrus.menu.CirrusInventoryType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class PacketEventsBridge {

    private static ClassLoader getPacketEventsClassLoader() {
        return PacketEvents.class.getClassLoader();
    }

    public static Object createPacketEventsComponentFromLegacy(String legacyText) {
        if (legacyText == null) {
            legacyText = "";
        }
        try {
            Class<?> serializerClass = Class.forName(
                "com.github.retrooper.packetevents.util.adventure.AdventureSerializer",
                true,
                getPacketEventsClassLoader()
            );
            Method fromLegacyFormat = serializerClass.getMethod("fromLegacyFormat", String.class);
            return fromLegacyFormat.invoke(null, legacyText);
        } catch (Exception e) {
            log.error("Failed to create PacketEvents component from legacy text: {}", legacyText, e);
            return null;
        }
    }

    public static String toJsonFromPacketEventsComponent(Object peComponent) {
        if (peComponent == null) {
            return "{\"text\":\"\"}";
        }
        try {
            Class<?> serializerClass = Class.forName(
                "com.github.retrooper.packetevents.util.adventure.AdventureSerializer",
                true,
                getPacketEventsClassLoader()
            );
            Method fromLegacyFormat = serializerClass.getMethod("fromLegacyFormat", String.class);
            Method toJson = serializerClass.getMethod("toJson", fromLegacyFormat.getReturnType());
            return (String) toJson.invoke(null, peComponent);
        } catch (Exception e) {
            log.error("Failed to convert PacketEvents component to json", e);
            return "{\"text\":\"\"}";
        }
    }

    public static String toJsonFromLegacy(String legacyText) {
        Object peComp = createPacketEventsComponentFromLegacy(legacyText);
        return toJsonFromPacketEventsComponent(peComp);
    }

    public static PacketWrapper<?> createOpenWindowPacket(
        int windowId,
        CirrusInventoryType type,
        String titleLegacyText,
        ClientVersion clientVersion
    ) {
        try {
            Object peTitle = createPacketEventsComponentFromLegacy(titleLegacyText != null ? titleLegacyText : "");
            Class<?> wrapperClass = Class.forName(
                "com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow",
                true,
                getPacketEventsClassLoader()
            );
            Class<?> peComponentClass = Class.forName("net.kyori.adventure.text.Component", true, getPacketEventsClassLoader());

            if (clientVersion.isOlderThan(ClientVersion.V_1_14)) {
                Constructor<?> ctor = wrapperClass.getConstructor(int.class, String.class, peComponentClass, int.class, int.class);
                return (PacketWrapper<?>) ctor.newInstance(windowId, type.toLegacyType(), peTitle, type.size(), -1);
            } else {
                Constructor<?> ctor = wrapperClass.getConstructor(int.class, int.class, peComponentClass);
                return (PacketWrapper<?>) ctor.newInstance(windowId, type.toPacketEventsTypeId(), peTitle);
            }
        } catch (Exception e) {
            log.error("Failed to create WrapperPlayServerOpenWindow packet", e);
            return null;
        }
    }

    public static void setCustomName(ItemStack.Builder builder, Object peComponent) {
        if (peComponent == null) return;
        try {
            Class<?> componentTypeClass = Class.forName(
                "com.github.retrooper.packetevents.protocol.component.ComponentType",
                true,
                getPacketEventsClassLoader()
            );
            Method componentMethod = builder.getClass().getMethod("component", componentTypeClass, Object.class);
            componentMethod.invoke(builder, ComponentTypes.CUSTOM_NAME, peComponent);
        } catch (Exception e) {
            log.error("Failed to set CUSTOM_NAME component on ItemStack", e);
        }
    }

    public static void setLore(ItemStack.Builder builder, List<Object> peComponents) {
        if (peComponents == null || peComponents.isEmpty()) return;
        try {
            Class<?> itemLoreClass = Class.forName(
                "com.github.retrooper.packetevents.protocol.component.builtin.item.ItemLore",
                true,
                getPacketEventsClassLoader()
            );
            Constructor<?> loreCtor = itemLoreClass.getConstructor(List.class);
            Object itemLore = loreCtor.newInstance(peComponents);

            Class<?> componentTypeClass = Class.forName(
                "com.github.retrooper.packetevents.protocol.component.ComponentType",
                true,
                getPacketEventsClassLoader()
            );
            Method componentMethod = builder.getClass().getMethod("component", componentTypeClass, Object.class);
            componentMethod.invoke(builder, ComponentTypes.LORE, itemLore);
        } catch (Exception e) {
            log.error("Failed to set LORE component on ItemStack", e);
        }
    }
}
