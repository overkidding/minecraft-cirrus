package dev.simplix.cirrus.common.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.injector.ChannelInjector;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.manager.protocol.ProtocolManager;
import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.NettyManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PacketEventsBridgeTest {

    @BeforeEach
    void setUp() {
        PacketEvents.setAPI(new DummyPacketEventsApi());
    }

    @AfterEach
    void tearDown() {
        PacketEvents.setAPI(null);
    }

    @Test
    void testToJsonFromLegacyWithRemoveItalic() {
        String jsonWithout = PacketEventsBridge.toJsonFromLegacy("§aSumo", false);
        assertNotNull(jsonWithout);
        assertFalse(jsonWithout.contains("\"italic\":false"), "JSON should not force italic:false: " + jsonWithout);

        String json = PacketEventsBridge.toJsonFromLegacy("§aSumo", true);
        assertNotNull(json);
        assertTrue(json.contains("\"italic\":false"), "JSON should contain italic:false: " + json);
    }

    @Test
    void testToJsonFromLegacyWithoutRemoveItalic() {
        String json = PacketEventsBridge.toJsonFromLegacy("§aSumo", false);
        assertNotNull(json);
        assertFalse(json.contains("\"italic\":false"), "JSON should not force italic:false: " + json);
    }

    @Test
    void testCreatePacketEventsComponentFromLegacy() {
        Object peCompNoItalic = PacketEventsBridge.createPacketEventsComponentFromLegacy("§aSumo", true);
        assertNotNull(peCompNoItalic);
        assertTrue(peCompNoItalic instanceof net.kyori.adventure.text.Component);
        assertEquals(net.kyori.adventure.text.format.TextDecoration.State.FALSE,
            ((net.kyori.adventure.text.Component) peCompNoItalic).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC));

        Object peCompWithItalic = PacketEventsBridge.createPacketEventsComponentFromLegacy("§a§oSumo", false);
        assertNotNull(peCompWithItalic);
        assertTrue(peCompWithItalic instanceof net.kyori.adventure.text.Component);
        assertNotEquals(net.kyori.adventure.text.format.TextDecoration.State.FALSE,
            ((net.kyori.adventure.text.Component) peCompWithItalic).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC));
    }

    private static class DummyServerManager implements ServerManager {
        @Override
        public ServerVersion getVersion() {
            return ServerVersion.V_1_20_4;
        }
    }

    private static class DummyPacketEventsApi extends PacketEventsAPI<Object> {
        private final ServerManager serverManager = new DummyServerManager();

        @Override
        public void load() {}

        @Override
        public boolean isLoaded() {
            return true;
        }

        @Override
        public void init() {}

        @Override
        public boolean isInitialized() {
            return true;
        }

        @Override
        public void terminate() {}

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public Object getPlugin() {
            return null;
        }

        @Override
        public ServerManager getServerManager() {
            return serverManager;
        }

        @Override
        public ProtocolManager getProtocolManager() {
            return null;
        }

        @Override
        public PlayerManager getPlayerManager() {
            return null;
        }

        @Override
        public NettyManager getNettyManager() {
            return null;
        }

        @Override
        public ChannelInjector getInjector() {
            return null;
        }
    }
}
