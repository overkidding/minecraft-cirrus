package dev.simplix.cirrus.common.util;

import dev.simplix.cirrus.text.CirrusChatElement;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextDecoration.State;

@UtilityClass
public class ComponentHelper {

    public Component removeItalic(Component component) {
        if (component == null) {
            return null;
        }
        return component.decoration(TextDecoration.ITALIC, State.FALSE);
    }

    public boolean isItalicRequested(CirrusChatElement element) {
        if (element == null) {
            return false;
        }
        Component comp = element.asComponent();
        if (hasItalicTrue(comp)) {
            return true;
        }
        String legacy = element.asLegacyText();
        return legacy != null && (legacy.contains("§o") || legacy.contains("&o"));
    }

    private boolean hasItalicTrue(Component comp) {
        if (comp == null) {
            return false;
        }
        if (comp.decoration(TextDecoration.ITALIC) == State.TRUE) {
            return true;
        }
        for (Component child : comp.children()) {
            if (hasItalicTrue(child)) {
                return true;
            }
        }
        return false;
    }

    public Component removeItalicIfNotRequested(Component component, boolean italicRequested) {
        if (component == null) {
            return null;
        }
        if (!italicRequested && component.decoration(TextDecoration.ITALIC) != State.TRUE) {
            return component.decoration(TextDecoration.ITALIC, State.FALSE);
        }
        return component;
    }

    public String formatLegacy(String legacyText, boolean italicRequested) {
        if (legacyText == null || legacyText.isEmpty()) {
            return "";
        }
        if (!italicRequested && !legacyText.startsWith("§r") && !legacyText.startsWith("&r")) {
            return "§r" + legacyText;
        }
        return legacyText;
    }
}
