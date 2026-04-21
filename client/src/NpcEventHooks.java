final class NpcEventHooks {
    private NpcEventHooks() {
    }

    static void spawned(Npc npc) {
        com.GameClient.NpcInfo info = info(npc);
        if (info != null) {
            net.runelite.client.game.GameEventBridgeHooks.postNpcSpawned(info);
        }
    }

    static void despawned(Npc npc) {
        com.GameClient.NpcInfo info = info(npc);
        if (info != null) {
            net.runelite.client.game.GameEventBridgeHooks.postNpcDespawned(info);
        }
    }

    private static com.GameClient.NpcInfo info(Npc npc) {
        if (npc == null || npc.definition == null) {
            return null;
        }

        NPCType definition = npc.definition.multinpcs == null ? npc.definition : npc.definition.method794(Class318_Sub1_Sub3_Sub3.aClass170_10209, -1);
        if (definition == null || definition.name == null || definition.name.length() == 0 || "null".equalsIgnoreCase(definition.name)) {
            return null;
        }

        return new com.GameClient.NpcInfo(
            definition.id,
            definition.name,
            npc.x >> 2,
            npc.y >> 2,
            npc.plane,
            Math.max(64, definition.height >> 2)
        );
    }
}
