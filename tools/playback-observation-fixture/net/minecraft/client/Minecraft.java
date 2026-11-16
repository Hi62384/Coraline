package net.minecraft.client;
/** No game is initialized by this unit test. */
public final class Minecraft {
    public Player player;
    public static Minecraft getInstance(){return new Minecraft();}
    public static final class Player {public java.util.UUID getUUID(){return java.util.UUID.randomUUID();}}
}
