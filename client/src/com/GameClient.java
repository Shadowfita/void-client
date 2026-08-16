package com;

import net.runelite.client.RuneLite;
import net.runelite.api.Skill;

import java.applet.Applet;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public abstract class GameClient extends Applet {


    public static String local = "185.213.26.59"; // live


    public static void setParams(String ip) {
        local = ip;
        setParams();
    }

    /**
     * Connection settings
     */
    public static final String IP = local;
    public static final String LOBBY_IP = local;

    public static final String JVM_ARGS = "-Xmx1024m";

    public static final Properties client_parameters = new Properties();

    static void setParams() {
        client_parameters.put("separate_jvm", "true");
        client_parameters.put("boxbgcolor", "black");
        client_parameters.put("image", "http://www.runescape.com/img/game/splash2.gif");
        client_parameters.put("centerimage", "true");
        client_parameters.put("boxborder", "false");
        client_parameters.put("java_arguments", JVM_ARGS+ " -Xss2m -Dsun.java2d.noddraw=true -XX:CompileThreshold=1500 -Xincgc -XX:+UseConcMarkSweepGC -XX:+UseParNewGC");
        client_parameters.put("27", "0");
        client_parameters.put("1", "0");
        client_parameters.put("16", "false");
        client_parameters.put("17", "false");
        client_parameters.put("21", "1"); // WORLD ID
        client_parameters.put("30", "false"); //frame
        client_parameters.put("20", LOBBY_IP);
        client_parameters.put("29", "");
        client_parameters.put("11", "true");
        client_parameters.put("25", "1378752098");
        client_parameters.put("28", "0");
        client_parameters.put("8", ".runescape.com");
        client_parameters.put("23", "false");
        client_parameters.put("32", "0");
        client_parameters.put("15", "wwGlrZHF5gKN6D3mDdihco3oPeYN2KFybL9hUUFqOvk");
        client_parameters.put("0", "IjGJjn4L3q5lRpOR9ClzZQ");
        client_parameters.put("2", "");
        client_parameters.put("4", "1"); // WORLD ID
        client_parameters.put("14", "");
        client_parameters.put("5", "8194");
        client_parameters.put("-1", "QlwePyRU5GcnAn1lr035ag");
        client_parameters.put("6", "0");
        client_parameters.put("24", "true,false,0,43,200,18,0,21,354,-15,Verdana,11,0xF4ECE9,candy_bar_middle.gif,candy_bar_back.gif,candy_bar_outline_left.gif,candy_bar_outline_right.gif,candy_bar_outline_top.gif,candy_bar_outline_bottom.gif,loadbar_body_left.gif,loadbar_body_right.gif,loadbar_body_fill.gif,6");
        client_parameters.put("3", "hAJWGrsaETglRjuwxMwnlA/d5W6EgYWx");
        client_parameters.put("12", "false");
        client_parameters.put("13", "0");
        client_parameters.put("26", "0");
        client_parameters.put("9", "77");
        client_parameters.put("22", "false");
        client_parameters.put("18", "false");
        client_parameters.put("33", "");
        client_parameters.put("haveie6", "false");
    }

    public static GameClient getClient() {
        return (GameClient) RuneLite.clientA;
    }

    public String getParameter(String string) {
        return (String) client_parameters.get(string);
    }

    public URL getDocumentBase() {
        return getCodeBase();
    }

    public URL getCodeBase() {
        try {
            return new URL("http://"+local);
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public static void openURL(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (IOException | URISyntaxException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    public abstract byte[][][] getTileSettings();

    public abstract int[][][] getTileHeights();

    public abstract int getPlane();

    public abstract int getBaseX();

    public abstract int getBaseY();

    public abstract boolean isInInstancedRegion();

    public abstract int[][][] getInstanceTemplateChunks();

    public abstract int[][] getCollisionMaps(int plane);

    public abstract Canvas getCanvas();

    public abstract int getCanvasWidth();

    public abstract int getCanvasHeight();

    public abstract Dimension getRealDimensions();

    public abstract Dimension getStretchedDimensions();

    public abstract void setStretchedEnabled(boolean state);

    public abstract boolean isStretchedEnabled();

    public abstract void setStretchedFast(boolean state);

    public abstract boolean isStretchedFast();

    public abstract void setStretchedIntegerScaling(boolean state);

    public abstract boolean isStretchedIntegerScaling();

    public abstract void setStretchedKeepAspectRatio(boolean state);

    public abstract boolean isStretchedKeepAspectRatio();

    public abstract void setScalingFactor(int factor);

    public abstract int getScalingFactor();

    public abstract void setInterfaceScalingFactor(int factor);

    public abstract int getInterfaceScalingFactor();

    public abstract boolean isInterfaceScalingSupported();

    public abstract void invalidateStretching(boolean resize);

    public abstract void setAnimationSmoothingEnabled(boolean state);

    public abstract boolean isAnimationSmoothingEnabled();

    public abstract int getLocalPlayerLocalX();

    public abstract int getLocalPlayerLocalY();

    public abstract int getLocalPlayerSceneX();

    public abstract int getLocalPlayerSceneY();

    public abstract int getLocalPlayerDestinationSceneX();

    public abstract int getLocalPlayerDestinationSceneY();

    public abstract int getLocalPlayerSize();

    public abstract boolean hasLocalPlayer();

    public List<GroundItemInfo> getGroundItems()
    {
        return Collections.emptyList();
    }

    public List<NpcInfo> getNpcs()
    {
        return Collections.emptyList();
    }

    public List<SkillSnapshot> getSkillSnapshots()
    {
        return Collections.emptyList();
    }

    public List<ItemContainerSnapshot> getItemContainers()
    {
        return Collections.emptyList();
    }

    public int getLocalPlayerAnimation()
    {
        return -1;
    }

    public OpponentInfo getOpponentInfo()
    {
        return null;
    }

    public String getObjectName(int id)
    {
        return "Object " + id;
    }
    public abstract boolean isClientThread();

    public static final class GroundItemInfo
    {
        private final int id;
        private final int quantity;
        private final String name;
        private final int price;
        private final int localX;
        private final int localY;
        private final int plane;

        public GroundItemInfo(int id, int quantity, String name, int localX, int localY, int plane)
        {
            this(id, quantity, name, 0, localX, localY, plane);
        }

        public GroundItemInfo(int id, int quantity, String name, int price, int localX, int localY, int plane)
        {
            this.id = id;
            this.quantity = quantity;
            this.name = name;
            this.price = price;
            this.localX = localX;
            this.localY = localY;
            this.plane = plane;
        }

        public int getId()
        {
            return id;
        }

        public int getQuantity()
        {
            return quantity;
        }

        public String getName()
        {
            return name;
        }

        public int getPrice()
        {
            return price;
        }

        public int getStackValue()
        {
            return Math.max(1, quantity) * Math.max(0, price);
        }

        public int getLocalX()
        {
            return localX;
        }

        public int getLocalY()
        {
            return localY;
        }

        public int getPlane()
        {
            return plane;
        }
    }

    public static final class NpcInfo
    {
        private final int index;
        private final int id;
        private final String name;
        private final int combatLevel;
        private final int localX;
        private final int localY;
        private final int plane;
        private final int height;

        public NpcInfo(int id, String name, int localX, int localY, int plane, int height)
        {
            this(-1, id, name, -1, localX, localY, plane, height);
        }

        public NpcInfo(int index, int id, String name, int combatLevel, int localX, int localY, int plane, int height)
        {
            this.index = index;
            this.id = id;
            this.name = name;
            this.combatLevel = combatLevel;
            this.localX = localX;
            this.localY = localY;
            this.plane = plane;
            this.height = height;
        }

        public int getIndex()
        {
            return index;
        }

        public int getId()
        {
            return id;
        }

        public String getName()
        {
            return name;
        }

        public int getCombatLevel()
        {
            return combatLevel;
        }

        public int getLocalX()
        {
            return localX;
        }

        public int getLocalY()
        {
            return localY;
        }

        public int getPlane()
        {
            return plane;
        }

        public int getHeight()
        {
            return height;
        }
    }

    public static final class ItemStackInfo
    {
        private final int id;
        private final int quantity;
        private final String name;
        private final int price;
        private final int slot;

        public ItemStackInfo(int id, int quantity, String name, int price, int slot)
        {
            this.id = id;
            this.quantity = quantity;
            this.name = name;
            this.price = price;
            this.slot = slot;
        }

        public int getId()
        {
            return id;
        }

        public int getQuantity()
        {
            return quantity;
        }

        public String getName()
        {
            return name;
        }

        public int getPrice()
        {
            return price;
        }

        public int getSlot()
        {
            return slot;
        }

        public int getStackValue()
        {
            long value = (long) Math.max(1, quantity) * Math.max(0, price);
            return (int) Math.min(Integer.MAX_VALUE, value);
        }
    }

    public static final class ItemContainerSnapshot
    {
        private final long id;
        private final List<ItemStackInfo> items;
        private final int capacity;

        public ItemContainerSnapshot(long id, List<ItemStackInfo> items, int capacity)
        {
            this.id = id;
            this.items = Collections.unmodifiableList(items);
            this.capacity = capacity;
        }

        public long getId()
        {
            return id;
        }

        public List<ItemStackInfo> getItems()
        {
            return items;
        }

        public int getCapacity()
        {
            return capacity;
        }

        public int getOccupiedSlots()
        {
            return items.size();
        }

        public int getTotalValue()
        {
            long total = 0L;
            for (ItemStackInfo item : items)
            {
                total += item.getStackValue();
            }
            return (int) Math.min(Integer.MAX_VALUE, total);
        }
    }

    public static final class OpponentInfo
    {
        private final String name;
        private final int combatLevel;
        private final int localX;
        private final int localY;
        private final int plane;
        private final boolean npc;

        public OpponentInfo(String name, int combatLevel, int localX, int localY, int plane, boolean npc)
        {
            this.name = name;
            this.combatLevel = combatLevel;
            this.localX = localX;
            this.localY = localY;
            this.plane = plane;
            this.npc = npc;
        }

        public String getName()
        {
            return name;
        }

        public int getCombatLevel()
        {
            return combatLevel;
        }

        public int getLocalX()
        {
            return localX;
        }

        public int getLocalY()
        {
            return localY;
        }

        public int getPlane()
        {
            return plane;
        }

        public boolean isNpc()
        {
            return npc;
        }
    }

    public static final class SkillSnapshot
    {
        private final Skill skill;
        private final int xp;
        private final int level;
        private final int boostedLevel;

        public SkillSnapshot(Skill skill, int xp, int level, int boostedLevel)
        {
            this.skill = skill;
            this.xp = xp;
            this.level = level;
            this.boostedLevel = boostedLevel;
        }

        public Skill getSkill()
        {
            return skill;
        }

        public int getXp()
        {
            return xp;
        }

        public int getLevel()
        {
            return level;
        }

        public int getBoostedLevel()
        {
            return boostedLevel;
        }
    }
}
