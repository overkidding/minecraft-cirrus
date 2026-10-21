package dev.simplix.cirrus.common.util;

import dev.simplix.cirrus.text.CirrusChatElement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextDecoration.State;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComponentHelperTest {

    @Test
    void testIsItalicRequested() {
        assertFalse(ComponentHelper.isItalicRequested(null));
        assertFalse(ComponentHelper.isItalicRequested(CirrusChatElement.empty()));
        assertFalse(ComponentHelper.isItalicRequested(CirrusChatElement.ofLegacyText("§aSumo")));
        assertFalse(ComponentHelper.isItalicRequested(CirrusChatElement.ofLegacyText("&aSumo")));
        assertFalse(ComponentHelper.isItalicRequested(CirrusChatElement.of(
            Component.text("Sumo", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, State.FALSE)
        )));

        assertTrue(ComponentHelper.isItalicRequested(CirrusChatElement.ofLegacyText("§a§oSumo")));
        assertTrue(ComponentHelper.isItalicRequested(CirrusChatElement.ofLegacyText("&a&oSumo")));
        assertTrue(ComponentHelper.isItalicRequested(CirrusChatElement.of(
            Component.text("Sumo", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, State.TRUE)
        )));
        assertTrue(ComponentHelper.isItalicRequested(CirrusChatElement.of(
            Component.text("Parent").append(
                Component.text("Child").decoration(TextDecoration.ITALIC, State.TRUE)
            )
        )));
    }

    @Test
    void testRemoveItalicIfNotRequested() {
        assertNull(ComponentHelper.removeItalicIfNotRequested(null, false));

        Component comp = Component.text("Sumo");
        Component result = ComponentHelper.removeItalicIfNotRequested(comp, false);
        assertEquals(State.FALSE, result.decoration(TextDecoration.ITALIC));

        Component compWithItalic = Component.text("Sumo").decoration(TextDecoration.ITALIC, State.TRUE);
        Component kept = ComponentHelper.removeItalicIfNotRequested(compWithItalic, true);
        assertEquals(State.TRUE, kept.decoration(TextDecoration.ITALIC));
    }

    @Test
    void testFormatLegacy() {
        assertEquals("", ComponentHelper.formatLegacy(null, false));
        assertEquals("", ComponentHelper.formatLegacy("", false));
        assertEquals("§r§aSumo", ComponentHelper.formatLegacy("§aSumo", false));
        assertEquals("§r§7Click", ComponentHelper.formatLegacy("§7Click", false));

        // Already starts with §r or &r
        assertEquals("§r§aSumo", ComponentHelper.formatLegacy("§r§aSumo", false));
        assertEquals("&r&aSumo", ComponentHelper.formatLegacy("&r&aSumo", false));

        // Italic requested retains original
        assertEquals("§a§oSumo", ComponentHelper.formatLegacy("§a§oSumo", true));
        assertEquals("§oSumo", ComponentHelper.formatLegacy("§oSumo", true));
    }
}
