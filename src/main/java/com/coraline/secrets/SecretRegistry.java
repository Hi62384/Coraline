package com.coraline.secrets;

import java.util.*;

/** Stable IDs are save data. Contributions are integer hundredths to make thresholds exact. */
public final class SecretRegistry {
    public enum Attribute { CODE, TREASURE, HIDABILITY, PATTERN, WEAKNESS, REWARDS }
    public record Secret(String id,String name,String description,double severity,int[] points,int tier,String ability,Set<String> connections) { public String rarity(){return tier==0?"Uncommon":tier==1?"Rare":"Exceptional";} public String discovery(){return tier==0?"House / public structures":tier==1?"Locked workshops and libraries":"Restricted research chambers";} }
    public record Loop(String id,String name,List<String> secrets,String ability) {}
    public static final Map<String,Secret> ALL=new LinkedHashMap<>();
    public static final Map<String,Loop> LOOPS=new LinkedHashMap<>();
    public static final String[] ATTRIBUTES={"Code Knowledge","Hidden Treasure View","Hidability","Pattern Recognition","Weakness","Reward Manipulation"};
    static {
        add("key_drawer","Hidden Key Drawer","One drawer's false bottom conceals the house key. Return borrowed keys to their original drawer.",.10,0,0,"");
        add("house_codes","Hidden House Codes","Door mechanisms reuse the same three pin families. Their clicks reveal when a pin aligns.",.22,0,0,"");
        add("personal_keyring","Mother's Keyring","A copied key identifies the workshop tumblers, but the original must not go missing.",.36,0,1,"");
        add("magical_locks","Magical Locks","A lock reads the order of pins, not the engraved symbols on its cover.",.48,0,1,"");
        add("binding_code","Binding Code","The prison's mechanical seals share the same code as the storage vault.",.70,0,2,"");
        add("world_cipher","World Cipher","Three repeating pulses authenticate the locks protecting the deepest rooms.",.86,0,2,"");
        add("hollow_trees","Hollow Trees","Some perfect trees have no rings inside. Their hollows hold concealed parcels.",.12,1,0,"");
        add("flowerbed_boxes","Flowerbed Boxes","Roots hide sealed boxes whose seams only reveal themselves to a trained observer.",.20,1,0,"");
        add("secret_storage","Secret Storage","Inventory marks on hidden compartments identify valuable supplies.",.41,1,1,"storage");
        add("illusion_mechanism","Illusion Mechanism","A false wall bends light around a real opening. Following its edge reveals the seam.",.54,1,1,"passages");
        add("hidden_vaults","Hidden Vaults","Invisible vaults keep their treasure outside ordinary sight, even when the door is open.",.73,1,2,"");
        add("treasure_ledger","Treasure Ledger","Matching all the concealed storage signs makes hidden caches visible and usable.",.80,1,2,"");
        add("blind_spots","Surveillance Blind Spots","Solid walls interrupt the watchers. Three continuous seconds of exposure is enough to alert them.",.18,2,0,"");
        add("quiet_steps","Quiet Steps","Carpets hide footsteps; the old boards near valuables are deliberately noisy.",.21,2,0,"");
        add("spying_dolls","Spying Dolls","The dolls' button eyes observe a cone in front of them and report through magical threads.",.65,2,1,"surveillance");
        add("rat_communication","Rat Communication","An alerted rat must reach Mother before its report becomes dangerous. Intercept it first.",.46,2,1,"false_signal");
        add("shadow_veil","Shadow Veil","A fold in the world's illusion hides its wearer for a minute, but cannot silence an alarm.",.72,2,2,"invisibility");
        add("magical_threads","Magical Thread Network","The watchers and servants share threads. Their signals can be diverted to a false destination.",.76,2,2,"false_signal");
        add("drawer_marks","Drawer Marks","A tiny repeated notch identifies drawers containing borrowed keys.",.13,3,0,"");
        add("attic_plan","Attic Plan","Rooms were added around the copied house. The older floor plan exposes patterns in their placement.",.25,3,0,"");
        add("hidden_passages","Hidden Passages","False masonry conceals a passage whose shape repeats in other buildings.",.38,3,1,"passages");
        add("observation_room","Observation Room","The observation ledger records every outlying structure, including its access tier.",.58,3,1,"");
        add("world_anchors","World Anchors","The house and its satellites are tied together by fixed points that persist for one visit.",.79,3,2,"");
        add("mother_route","Mother's Route","The surveillance network carries Mother's position as well as her orders.",.88,3,2,"");
        add("sewing_plans","Sewing Room Plans","The stitching around a magical joint is weaker while its threads are stretched.",.20,4,0,"");
        add("attack_patterns","Her Attack Patterns","Watch the pause after an attack: her control briefly loosens while she gathers herself.",.29,4,0,"vulnerability");
        add("soul_binding","Soul Binding","A binding is a mechanism, not the soul itself. Breaking its command can free the captive.",.66,4,1,"");
        add("mother_weakness","Mother's Weakness","Her power depends on tension in the world's threads. Knowledge helps exploit it, not overpower it.",.77,4,1,"vulnerability");
        add("soulglass","Soulglass Formation","Soulglass forms beside the strongest bindings. Only informed hands can harvest it safely.",.91,4,2,"soulglass");
        add("soul_prisons","Soul Prisons","Release the binding at a prison altar to free a captive rather than shatter its vessel.",.95,4,2,"rescue");
        add("gift_preparation","Gift Preparation","Mother selects a gift before the presentation. Subtle knowledge influences the quality she prepares.",.12,5,0,"");
        add("delivery_ledger","Delivery Ledger","The servants follow a stock ledger. Additional quantities can be hidden among legitimate presents.",.24,5,0,"");
        add("gift_storage","Gift Storage","Prepared presents wait in the remote storehouse before being brought to the house.",.39,5,1,"storage");
        add("magical_crafting","Magical Crafting","A prepared delivery can be supplemented with materials from the workshop.",.52,5,1,"");
        add("servant_commands","Servant Commands","A servant accepts a brief move order when the thread carrying Mother's command is interrupted.",.74,5,2,"servant");
        add("servant_delivery","Servant Delivery","A complete delivery instruction can arrange one extra parcel without changing the visit ceremony.",.81,5,2,"");
        loop("surveillance","Surveillance Hijack","hijack","spying_dolls","magical_threads","servant_commands");
        loop("souls","Soul Liberation","liberation","soulglass","soul_prisons","soul_binding");
        loop("illusions","Unravel Illusions","reveal","illusion_mechanism","hidden_passages","world_anchors");
        loop("gifts","Secret Delivery","gift","gift_storage","magical_crafting","servant_delivery");
        loop("combat","Threadbreaker","combat","mother_weakness","soul_binding","soulglass");
        loop("servants","Chorus of Servants","command","servant_commands","magical_threads","rat_communication");
        validate();
    }
    private static void add(String id,String name,String description,double severity,int attribute,int tier,String ability){
        int[] points=new int[6];points[attribute]=18;
        ALL.put(id,new Secret(id,name,description,severity,points,tier,ability,new LinkedHashSet<>()));
    }
    private static void loop(String id,String name,String ability,String... ids){
        LOOPS.put(id,new Loop(id,name,List.of(ids),ability));
        for(int i=0;i<ids.length;i++){ALL.get(ids[i]).connections.add(ids[(i+1)%ids.length]);ALL.get(ids[(i+1)%ids.length]).connections.add(ids[i]);}
    }
    public static int[] totals(Collection<String> ids){int[] values=new int[6];for(String id:new HashSet<>(ids)){var s=ALL.get(id);if(s!=null)for(int i=0;i<6;i++)values[i]=Math.min(100,values[i]+s.points[i]);}return values;}
    public static Set<String> abilities(Collection<String> ids){var result=new LinkedHashSet<String>();for(String id:ids){var s=ALL.get(id);if(s!=null&&!s.ability.isEmpty())result.add(s.ability);}for(var loop:LOOPS.values())if(ids.containsAll(loop.secrets))result.add(loop.ability);if(result.contains("hijack")&&result.contains("reveal"))result.add("unweave");return result;}
    public static double severity(Collection<String> ids){double remainder=1;for(String id:new HashSet<>(ids)){var s=ALL.get(id);if(s!=null)remainder*=1-s.severity;}return 1-remainder;}
    public static int tier(int code){return code>=100?3:code>=66?2:code>=33?1:0;}
    public static void validate(){for(int n:totals(ALL.keySet()))if(n!=100)throw new IllegalStateException("Unattainable secret attribute");for(var l:LOOPS.values())for(int i=0;i<l.secrets.size();i++)if(!ALL.get(l.secrets.get(i)).connections.contains(l.secrets.get((i+1)%l.secrets.size())))throw new IllegalStateException("Open secret loop");}
    private SecretRegistry(){}
}
