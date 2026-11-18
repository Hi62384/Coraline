package com.coraline.feast;

import com.coraline.tycoon.TycoonNpc;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.phys.Vec3;

/** Bounded real light/object/food effects at authored owner-local points. No
 * trust is awarded by an arbitrary button without a corresponding world effect.
 * Display entities contain no obtainable items; effects restore only their own
 * expected states and never overwrite another player's block modifications. */
public final class GhostVictimManifestations {
    private static CompoundTag points(CompoundTag n){if(!n.contains("Points"))n.put("Points",new CompoundTag());return n.getCompound("Points");}
    public static boolean display(ServerPlayer p,VisitData.Scene s,CompoundTag n,String id,Vec3 at,GhostVictimProgress.Disturbance kind,ItemStack normal){
        return displayAt(p,s,n,AcceptedButtonEyesWorld.level(p),id,at,kind,normal);
    }
    public static boolean displayAt(ServerPlayer p,VisitData.Scene s,CompoundTag n,ServerLevel level,String id,Vec3 at,GhostVictimProgress.Disturbance kind,ItemStack normal){
        if(!GhostVictimMirrors.owned(p,s,n)||id==null||!id.matches("[a-z_]{1,60}")||kind==null||normal.isEmpty())return false;
        var all=points(n);var row=all.getCompound(id);if(level==null)return false;
        if(row.hasUUID("Entity")){
            if(!kind.name().equals(row.getString("Kind"))||!level.dimension().location().toString().equals(row.getString("Dimension")))return false;
            level.getChunkAt(BlockPos.of(row.getLong("Position")));var existing=actor(level,row,s,n);if(existing==null)return false;
            // Authored layout migrations retain the exact owned display, its
            // cooldown and evidence receipts. Never adopt an arbitrary entity.
            if(row.getDouble("X")!=at.x||row.getDouble("Y")!=at.y||row.getDouble("Z")!=at.z){
                level.getChunkAt(BlockPos.containing(at));existing.setPos(at);row.putLong("Position",BlockPos.containing(at).asLong());
                row.putDouble("X",at.x);row.putDouble("Y",at.y);row.putDouble("Z",at.z);OtherMotherLoop.changed(p);
            }
            return true;
        }
        level.getChunkAt(BlockPos.containing(at));
        var actor=EntityType.ITEM_DISPLAY.create(level);if(actor==null)return false;
        var visual=new CompoundTag();visual.put("item",normal.save(new CompoundTag()));visual.putString("item_display","ground");actor.load(visual);actor.setPos(at);actor.setNoGravity(true);
        actor.getPersistentData().putUUID("GhostManifestationOwner",s.owner);actor.getPersistentData().putUUID("GhostManifestationDoor",s.door);
        actor.getPersistentData().putUUID("GhostManifestationSession",n.getUUID("Session"));
        // RealmProtection admits only explicitly authored decorations. Keep
        // its rejection of unrelated vanilla entities intact.
        actor.getPersistentData().putBoolean("CoralineDecoration",true);
        if(!level.addFreshEntity(actor))return false;
        row.putUUID("Entity",actor.getUUID());row.putLong("Position",BlockPos.containing(at).asLong());row.putDouble("X",at.x);row.putDouble("Y",at.y);row.putDouble("Z",at.z);
        row.putString("Dimension",level.dimension().location().toString());row.putString("Kind",kind.name());row.putBoolean("Visible",true);row.put("NormalItem",normal.save(new CompoundTag()));all.put(id,row);OtherMotherLoop.changed(p);return true;
    }
    public static boolean light(ServerPlayer p,VisitData.Scene s,CompoundTag n,String id,BlockPos actualLight){
        if(!GhostVictimMirrors.owned(p,s,n)||id==null||!id.matches("[a-z_]{1,60}"))return false;
        if(points(n).contains(id))return points(n).getCompound(id).getLong("Position")==actualLight.asLong()&&points(n).getCompound(id).getString("Kind").equals("LIGHT");
        var level=AcceptedButtonEyesWorld.level(p);if(level==null||!level.hasChunkAt(actualLight)||level.getBlockEntity(actualLight)!=null||!lightSource(level.getBlockState(actualLight)))return false;
        var row=new CompoundTag();row.putString("Dimension",level.dimension().location().toString());row.putLong("Position",actualLight.asLong());row.putString("Kind",GhostVictimProgress.Disturbance.LIGHT.name());row.put("Original",NbtUtils.writeBlockState(level.getBlockState(actualLight)));
        points(n).put(id,row);return true;
    }
    private static Display.ItemDisplay actor(ServerLevel level,CompoundTag row,VisitData.Scene s,CompoundTag n){
        if(!row.hasUUID("Entity"))return null;var entity=level.getEntity(row.getUUID("Entity"));
        if(!(entity instanceof Display.ItemDisplay display))return null;var tag=display.getPersistentData();
        return tag.hasUUID("GhostManifestationOwner")&&s.owner.equals(tag.getUUID("GhostManifestationOwner"))&&tag.hasUUID("GhostManifestationDoor")&&s.door.equals(tag.getUUID("GhostManifestationDoor"))
            &&n.hasUUID("Session")&&tag.hasUUID("GhostManifestationSession")&&n.getUUID("Session").equals(tag.getUUID("GhostManifestationSession"))?display:null;
    }
    static void retireDisplay(ServerPlayer p,VisitData.Scene s,CompoundTag n,String id){
        var all=points(n);
        if(!all.contains(id))return;
        var row=all.getCompound(id);
        var level=level(p,row);
        if(level==null||!row.hasUUID("Entity")||!row.contains("Position",Tag.TAG_LONG))return;
        var at=BlockPos.of(row.getLong("Position"));
        LoopProps.hold(level,at);
        var display=actor(level,row,s,n);
        if(display!=null)display.discard();
        else if(level.getEntity(row.getUUID("Entity"))!=null
                ||!level.areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(at).toLong()))return;
        all.remove(id);
        OtherMotherLoop.changed(p);
    }
    private static void item(Display.ItemDisplay actor,ItemStack item){var saved=actor.saveWithoutId(new CompoundTag());saved.put("item",item.save(new CompoundTag()));actor.load(saved);}
    static Vec3 interactionPoint(CompoundTag row){
        return row.contains("X")?new Vec3(row.getDouble("X"),row.getDouble("Y")+.12,row.getDouble("Z")):BlockPos.of(row.getLong("Position")).getCenter();
    }
    public static boolean perform(ServerPlayer p,VisitData.Scene s,CompoundTag n,String id,TycoonNpc child){
        return perform(p,s,n,id,child,false);
    }
    public static void inspect(ServerPlayer p,VisitData.Scene s,CompoundTag n,String id){
        if(!GhostVictimMirrors.participant(p,s,n)||!p.isSpectator()||GhostVictimProgress.imprisoned(n)||!GhostVictimSchedule.previewAvailable(n,id)||!points(n).contains(id))return;
        var row=points(n).getCompound(id);var world=level(p,row);if(world==null||p.level()!=world)return;
        var delta=interactionPoint(row).subtract(p.getEyePosition());
        if(delta.lengthSqr()>36||delta.lengthSqr()<.001||p.getLookAngle().dot(delta.normalize())<.92)return;
        String label=row.getString("Label");if(label.isBlank())label=row.getString("Kind").toLowerCase(java.util.Locale.ROOT).replace('_',' ');
        p.displayClientMessage(net.minecraft.network.chat.Component.literal("Tomorrow's opportunity: "+label+". Inspecting uses no energy."),true);
    }
    public static boolean perform(ServerPlayer p,VisitData.Scene s,CompoundTag n,String id,TycoonNpc child,boolean frightenGift){
        if(!GhostVictimMirrors.participant(p,s,n)||!points(n).contains(id)||!GhostVictimActors.owns(child,s,n))return false;
        var row=points(n).getCompound(id);var level=level(p,row);
        if(level==null||p.level()!=level||!p.isSpectator()||!level.hasChunkAt(BlockPos.of(row.getLong("Position"))))return false;
        var position=BlockPos.of(row.getLong("Position"));var target=interactionPoint(row);var delta=target.subtract(p.getEyePosition());
        if(delta.lengthSqr()>36||delta.lengthSqr()<.001||p.getLookAngle().dot(delta.normalize())<.92)return false;
        var phase=GhostVictimProgress.phase(n);
        if(!GhostVictimSchedule.manifestationAvailable(n,id))return false;
        GhostVictimProgress.Disturbance kind;
        try{kind=GhostVictimProgress.Disturbance.valueOf(row.getString("Kind"));}
        catch(IllegalArgumentException invalid){return false;}
        var sight=level.clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(),target,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
        if(sight.getType()!=net.minecraft.world.phys.HitResult.Type.MISS
                &&(kind!=GhostVictimProgress.Disturbance.LIGHT||!position.equals(sight.getBlockPos())))return false;
        var display=actor(level,row,s,n);
        if(kind==GhostVictimProgress.Disturbance.LIGHT){if(!lightSource(level.getBlockState(position))||row.contains("EffectUntil"))return false;}
        else if(display==null||display.isInvisible()||row.contains("EffectUntil"))return false;
        if(frightenGift){if(kind!=GhostVictimProgress.Disturbance.GIFT_SECRET)return false;kind=GhostVictimProgress.Disturbance.GIFT_FRIGHT;}
        boolean witnessed=child.level()==level&&visible(child,target,10,130);var mother=AcceptedButtonEyesWorld.mother(p,s);
        boolean motherSaw=mother!=null&&mother.level()==level&&visible(mother,target,14,120);
        var traits=GhostVictimProgress.child(n).getCompound("Traits");boolean blame=GhostVictimProgress.remembers(n,"ghost_threat")||GhostVictimProgress.remembers(n,"met_ghost")&&GhostVictimProgress.ghostTrust(n)<.2&&traits.getDouble("Sensitivity")>.65;
        boolean reports=witnessed&&blame&&mother!=null&&GhostVictimNavigation.canSee(child,mother,12,180);
        var observation=new GhostVictimProgress.Observation(witnessed,motherSaw,blame,GhostVictimProgress.child(n).getInt("EvidenceCount")>0,reports);
        var snapshot=n.copy();var result=GhostVictimProgress.disturb(n,id,kind,observation,true,GhostVictimConfig.tuning());if(!result.performed()){
            if(result.reaction().equals("Not enough manifestation energy.")||result.reaction().equals("Let the room settle."))p.displayClientMessage(net.minecraft.network.chat.Component.literal(result.reaction()),true);return false;
        }
        boolean applied=true;
        if(kind==GhostVictimProgress.Disturbance.LIGHT){var old=level.getBlockState(position);row.put("EffectOriginal",NbtUtils.writeBlockState(old));var dark=dark(old);applied=level.setBlock(position,dark,3);row.put("Expected",NbtUtils.writeBlockState(dark));}
        else if(kind==GhostVictimProgress.Disturbance.FOOD||kind==GhostVictimProgress.Disturbance.ILLUSION||kind==GhostVictimProgress.Disturbance.GIFT_SECRET)item(display,row.contains("RevealedItem")?ItemStack.of(row.getCompound("RevealedItem")):new ItemStack(Items.ROTTEN_FLESH));
        if(!applied){for(String key:new java.util.HashSet<>(n.getAllKeys()))n.remove(key);n.merge(snapshot);return false;}
        row.putLong("EffectStart",GhostVictimProgress.clock(n));row.putLong("EffectUntil",GhostVictimProgress.clock(n)+(kind==GhostVictimProgress.Disturbance.LIGHT?32:100));
        n.putLong("ManifestingUntil",Math.max(n.getLong("ManifestingUntil"),row.getLong("EffectUntil")));
        row.putString("ActiveKind",kind.name());
        boolean noisy=kind==GhostVictimProgress.Disturbance.OBJECT||kind==GhostVictimProgress.Disturbance.KNIFE||kind==GhostVictimProgress.Disturbance.GIFT_FRIGHT;
        boolean motherHeard=noisy&&mother!=null&&mother.level()==level&&mother.distanceToSqr(target)<18*18;
        if(noisy)level.playSound(null,target.x,target.y,target.z,net.minecraft.sounds.SoundEvents.GLASS_BREAK,net.minecraft.sounds.SoundSource.BLOCKS,.35F,1.2F);
        if(motherHeard&&!motherSaw)n.putDouble("Attention",GhostVictimProgress.unit(GhostVictimProgress.attention(n)+.04));
        if(witnessed){n.putString("ChildWords",result.reaction());n.putLong("ChildTalkingUntil",GhostVictimProgress.clock(n)+80);
            n.putLong("ReactionLook",position.asLong());n.putLong("ReactionLookUntil",GhostVictimProgress.clock(n)+80);
            if(result.investigate()&&phase!=GhostVictimProgress.Phase.FIRST_SUPPER)GhostVictimRoutines.investigate(n,position);}
        if(motherSaw||motherHeard||reports){n.putLong("MotherInvestigation",position.asLong());n.putLong("MotherInvestigateUntil",GhostVictimProgress.clock(n)+200);}
        var escape=n.getCompound(GhostEscapeProgress.KEY);
        if(kind==GhostVictimProgress.Disturbance.LIGHT&&mother!=null&&mother.level()==level
                &&GhostEscapeProgress.owns(n,escape)&&GhostEscapeProgress.phase(escape)==GhostEscapeProgress.Phase.WAIT_LURE
                &&(!escape.contains("AmbushLamp",Tag.TAG_LONG)||escape.getLong("AmbushLamp")==position.asLong())
                &&mother.distanceToSqr(target)<18*18){
            escape.putLong("Distraction",position.asLong());
            escape.putLong("DistractionUntil",GhostVictimProgress.clock(n)+GhostEscapeAmbush.DISTRACTION_TICKS);
            escape.remove("StealSignaled");
            n.putLong("MotherInvestigation",position.asLong());
            n.putLong("MotherInvestigateUntil",escape.getLong("DistractionUntil"));
            p.displayClientMessage(net.minecraft.network.chat.Component.literal("Other Mother is investigating the light. Signal the child when her back is turned."),true);
        }else if(motherSaw||motherHeard){
            p.displayClientMessage(net.minecraft.network.chat.Component.literal("Other Mother noticed the disturbance."),true);
        }
        if(witnessed&&blame&&!reports)GhostVictimProgress.child(n).putBoolean("PendingGhostReport",true);
        OtherMotherLoop.changed(p);return true;
    }
    private static boolean visible(LivingEntity observer,Vec3 target,double distance,double fov){
        Vec3 eye=observer.getEyePosition(),delta=target.subtract(eye);if(delta.lengthSqr()>distance*distance)return false;
        if(delta.lengthSqr()>.001&&GhostVictimNavigation.view(observer).dot(delta.normalize())<Math.cos(Math.toRadians(fov/2)))return false;
        return observer.level().clip(new net.minecraft.world.level.ClipContext(eye,target,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,observer)).getType()==net.minecraft.world.phys.HitResult.Type.MISS;
    }
    public static void tick(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        if(!GhostVictimMirrors.owned(p,s,n))return;
        for(String id:points(n).getAllKeys()){
            var row=points(n).getCompound(id);var level=level(p,row);if(level==null||!row.contains("EffectUntil"))continue;var pos=BlockPos.of(row.getLong("Position"));if(!level.hasChunkAt(pos))continue;
            long age=GhostVictimProgress.clock(n)-row.getLong("EffectStart");boolean done=GhostVictimProgress.clock(n)>=row.getLong("EffectUntil");
            if(row.getString("Kind").equals("LIGHT")){
                var expected=NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK),row.getCompound("Expected"));
                if(level.getBlockState(pos).equals(expected)){
                    var original=NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK),row.getCompound("EffectOriginal"));
                    var next=done||age%8>=4?original:dark(original);level.setBlock(pos,next,3);row.put("Expected",NbtUtils.writeBlockState(next));
                }else done=true;
            }else{
                var display=actor(level,row,s,n);if(display==null)done=true;
                else if(done){display.setPos(row.getDouble("X"),row.getDouble("Y"),row.getDouble("Z"));item(display,ItemStack.of(row.getCompound("NormalItem")));}
                else if(row.getString("ActiveKind").equals("OBJECT")||row.getString("ActiveKind").equals("KNIFE")||row.getString("ActiveKind").equals("GIFT_FRIGHT"))display.setPos(row.getDouble("X")+Math.sin(age*.14)*.3,row.getDouble("Y")+Math.sin(Math.min(1,age/100.)*Math.PI)*.55,row.getDouble("Z"));
            }
            if(done){row.remove("EffectUntil");row.remove("EffectStart");row.remove("Expected");row.remove("EffectOriginal");}
        }
    }
    public static void show(ServerPlayer p,VisitData.Scene s,CompoundTag n,String id,boolean shown){
        var level=level(p,points(n).getCompound(id));if(level==null)return;var display=actor(level,points(n).getCompound(id),s,n);
        if(display!=null){display.setInvisible(!shown);points(n).getCompound(id).putBoolean("Visible",shown);}
    }
    private static ServerLevel level(ServerPlayer p,CompoundTag row){return row.getString("Dimension").isEmpty()?AcceptedButtonEyesWorld.level(p):com.coraline.parents.FamilyData.level(p.server,row.getString("Dimension"));}
    static boolean lightSource(net.minecraft.world.level.block.state.BlockState state){return state.getBlock() instanceof LightBlock
        ||state.is(net.minecraft.world.level.block.Blocks.SHROOMLIGHT)||state.is(net.minecraft.world.level.block.Blocks.GLOWSTONE)
        ||state.is(net.minecraft.world.level.block.Blocks.SEA_LANTERN)||state.is(net.minecraft.world.level.block.Blocks.LANTERN)
        ||state.is(net.minecraft.world.level.block.Blocks.REDSTONE_LAMP);}
    private static net.minecraft.world.level.block.state.BlockState dark(net.minecraft.world.level.block.state.BlockState state){
        if(state.hasProperty(LightBlock.LEVEL))return state.setValue(LightBlock.LEVEL,0);
        if(state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT))return state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT,false);
        return net.minecraft.world.level.block.Blocks.BLACKSTONE.defaultBlockState();
    }
    private GhostVictimManifestations(){}
}
