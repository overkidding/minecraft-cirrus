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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextDecoration.State;

@Slf4j
@UtilityClass
public class PacketEventsBridge {

    private static ClassLoader getPacketEventsClassLoader() {
        return PacketEvents.class.getClassLoader();
    }

    public static Object createPacketEventsComponentFromLegacy(String legacyText) {
        return createPacketEventsComponentFromLegacy(legacyText, false);
    }

    public static Object createPacketEventsComponentFromLegacy(String legacyText, boolean removeItalic) {
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
            Object peComponent = fromLegacyFormat.invoke(null, legacyText);
            if (removeItalic) {
                peComponent = applyItalicFalse(peComponent);
            }
            return peComponent;
        } catch (Exception e) {
            log.error("Failed to create PacketEvents component from legacy text: {}", legacyText, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Object applyItalicFalse(Object peComponent) {
        if (peComponent == null) {
            return null;
        }
        try {
            if (peComponent instanceof Component) {
                Component comp = (Component) peComponent;
                if (comp.decoration(TextDecoration.ITALIC) != State.TRUE) {
                    return comp.decoration(TextDecoration.ITALIC, State.FALSE);
                }
                return comp;
            }
            Class<?> textDecorationClass = Class.forName(
                "net.kyori.adventure.text.format.TextDecoration",
                true,
                getPacketEventsClassLoader()
            );
            Class<?> stateClass = Class.forName(
                "net.kyori.adventure.text.format.TextDecoration$State",
                true,
                getPacketEventsClassLoader()
            );
            Method decorationGetter = peComponent.getClass().getMethod("decoration", textDecorationClass);
            Object italicDec = Enum.valueOf((Class<Enum>) textDecorationClass, "ITALIC");
            Object currentDec = decorationGetter.invoke(peComponent, italicDec);
            Object trueState = Enum.valueOf((Class<Enum>) stateClass, "TRUE");
            if (!trueState.equals(currentDec)) {
                Object falseState = Enum.valueOf((Class<Enum>) stateClass, "FALSE");
                Method decorationSetter = peComponent.getClass().getMethod("decoration", textDecorationClass, stateClass);
                return decorationSetter.invoke(peComponent, italicDec, falseState);
            }
            return peComponent;
        } catch (Exception e) {
            log.warn("Failed to apply italic=false to PacketEvents component", e);
            return peComponent;
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
        return toJsonFromLegacy(legacyText, false);
    }

    public static String toJsonFromLegacy(String legacyText, boolean removeItalic) {
        Object peComp = createPacketEventsComponentFromLegacy(legacyText, removeItalic);
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
