package com.coraline.secrets;
import java.util.*;
import net.minecraft.world.item.*;
import net.minecraft.world.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.registries.RegistryObject;
import com.coraline.registry.ModItems;

public final class SecretsItems {
    public static final Map<String,RegistryObject<Item>> EVIDENCE=new LinkedHashMap<>();
    public static final RegistryObject<Item> BLACKMAIL=ModItems.ITEMS.register("secret_blackmail",()->new Named("Sealed Blackmail"));
    public static final RegistryObject<Item> KEY=ModItems.ITEMS.register("borrowed_house_key",()->new Named("Borrowed House Key"));
    public static final RegistryObject<Item> SOULGLASS=ModItems.SOULGLASS;
    public static final RegistryObject<Item> JOURNAL=ModItems.ITEMS.register("secret_journal",()->new Named("Secret Journal"));
    public static final RegistryObject<Item> GUIDE=ModItems.ITEMS.register("secret_field_notes",()->new Named("Investigation Field Notes"){
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){if(p instanceof ServerPlayer s)SecretsNetwork.open(s,false,false);return InteractionResultHolder.sidedSuccess(p.getItemInHand(h),l.isClientSide);}
    });
    public static final RegistryObject<Item> DEBUG=ModItems.ITEMS.register("secrets_debugger",()->new Named("Secrets Debugger"){
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){if(p instanceof ServerPlayer s&&s.hasPermissions(2))SecretsNetwork.open(s,true,false);return InteractionResultHolder.sidedSuccess(p.getItemInHand(h),l.isClientSide);}
    });
    public static final RegistryObject<Item> JOURNAL_DISCOVERY_DEBUG=ModItems.ITEMS.register("journal_discovery_debugger",()->new Named("Debug: Mother Finds Journal"){
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){
            if(p instanceof ServerPlayer s&&s.hasPermissions(2)){
                if(SecretsRuntime.active(s))SecretTransactions.discoverJournal(s);
                else s.displayClientMessage(Component.literal("Use during an active Other World visit."),true);
            }
            return InteractionResultHolder.sidedSuccess(p.getItemInHand(h),l.isClientSide);
        }
    });
    static {for(var secret:SecretRegistry.ALL.values())EVIDENCE.put(secret.id(),ModItems.ITEMS.register("secret_"+secret.id(),()->new Named(secret.name())));}
    public static class Named extends Item {private final String name;Named(String name){super(new Properties().stacksTo(1));this.name=name;}@Override public Component getName(ItemStack s){return Component.literal(name);}}
    public static void init(){}
    public static boolean is(ItemStack stack){return stack.getItem() instanceof Named;}
    private SecretsItems(){}
}
