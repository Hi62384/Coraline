package com.coraline.registry;

import com.coraline.CoralineMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, CoralineMod.MOD_ID);

    public static final RegistryObject<SoundEvent> WYBIE_LINE_A = register("wybie_line_a");
    public static final RegistryObject<SoundEvent> WYBIE_LINE_B = register("wybie_line_b");
    public static final RegistryObject<SoundEvent> WYBIE_LINE_C = register("wybie_line_c");
    public static final RegistryObject<SoundEvent> FESTIVAL_HARP_LOW=register("festival_harp_low"), FESTIVAL_HARP=register("festival_harp"), FESTIVAL_HARP_HIGH=register("festival_harp_high");
    public static final RegistryObject<SoundEvent> THREAD_WHIP = register("thread_whip");

    private ModSounds() {
    }
    public static final RegistryObject<SoundEvent> MICE_INTRO=register("mice_intro"), MICE_CHASE=register("mice_chase"), MOUSE_SQUEAK=register("mouse_squeak"), DOOR_PORTAL_OPEN=register("door_portal_open"), TUNNEL_ENTER=register("tunnel_enter"), TUNNEL_HUM=register("tunnel_hum");
    public static final RegistryObject<SoundEvent> ANGRY_PARENT=register("angry_parent");
    public static final RegistryObject<SoundEvent> MOM_CHAT=register("mom_chat");
    public static final RegistryObject<SoundEvent> DAD_CHAT=register("dad_chat");
    public static final RegistryObject<SoundEvent> MOM_NEGLECT=register("mom_neglect"),MOM_TASK=register("mom_task"),MOM_DEATH=register("mom_death");
    public static final RegistryObject<SoundEvent> DAD_NEGLECT=register("dad_neglect"),DAD_NEGLECT_RAIN=register("dad_neglect_rain"),DAD_TASK=register("dad_task"),DAD_DEATH=register("dad_death");
    public static final java.util.Map<String,RegistryObject<SoundEvent>> PARENT_CLIPS=registerParentClips();
    private static java.util.Map<String,RegistryObject<SoundEvent>> registerParentClips(){
        var clips=new java.util.LinkedHashMap<String,RegistryObject<SoundEvent>>();
        try(var in=ModSounds.class.getResourceAsStream("/assets/coraline/parent_voice_groups.json")){
            if(in!=null){var groups=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                for(var group:groups.entrySet())for(var clip:group.getValue().getAsJsonArray())clips.computeIfAbsent(clip.getAsString(),ModSounds::register);
            }
        }catch(Exception e){throw new IllegalStateException("Cannot register parent dialogue clips",e);}
        return clips;
    }

    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CoralineMod.MOD_ID, name);
        return SOUNDS.register(name, () -> SoundEvent.createFixedRangeEvent(id, 64.0F));
    }

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}
