from pathlib import Path
b=Path('src/main/java/com/coraline/feast')
p=b/'AcceptedButtonEyesInventory.java';s=p.read_text()
a=s.index('        if(!reconcileSnapshots',s.index('public static boolean restoreAfterGhostEscape'))
z=s.index('        if(!backup.getBoolean("Stored")',a)
s=s[:a]+'''        if(!backup.hasUUID("Door")||!scene.door.equals(backup.getUUID("Door"))
                ||!validSnapshot(snapshot,p,scene.door)||!escape.hasUUID("OriginalEscrow")
                ||!escape.getUUID("OriginalEscrow").equals(snapshot.getUUID("Transaction")))return false;
        if(backup.getBoolean("Restored")&&backup.hasUUID("RestoredTransaction")
                &&backup.getUUID("RestoredTransaction").equals(snapshot.getUUID("Transaction"))){
            if(n.contains("InventoryEscrow")&&!sameBase(n.getCompound("InventoryEscrow"),snapshot))return false;
            n.put("InventoryEscrow",snapshot.copy());
            n.putBoolean("EscrowStored",true);
            restoredFlags(n,backup,snapshot.getUUID("Transaction"));
            OtherMotherLoop.changed(p);
            return true;
        }
        if(!reconcileSnapshots(p,scene,n,backup)||!n.getBoolean("EscrowStored"))return false;
        snapshot=backup.getCompound("Snapshot");
'''+s[z:]
p.write_text(s)
p=b/'AcceptedButtonEyes.java';s=p.read_text()
s=s.replace('''case RAISE_NEEDLE->{holdSeat(p,s);props(p,s,phase,(int)age);if(sewingBeat(p,s,.58)){''','''case RAISE_NEEDLE->{holdSeat(p,s);props(p,s,phase,(int)age);
                var needle=FirstEscapeScene.prop(p,s,"SewingNeedle");
                if(needle!=null&&needle.grabTarget().lengthSqr()>.9
                        &&needle.position().add(needle.grabTarget().scale(.33)).distanceToSqr(p.getEyePosition())<.04
                        &&sewingBeat(p,s,.58)){''')
p.write_text(s)
p=Path('src/wybietest/java/com/coraline/feast/AcceptedButtonEyesSewingGameTests.java');s=p.read_text().replace('actor.makeOtherParent(0);actor.setPos','actor.makeOtherParent(0);actor.getPersistentData().putUUID("RealmDoor",s.door);actor.setPos');p.write_text(s)
p=Path('src/wybietest/java/com/coraline/feast/GhostEscapeRecoveryGameTests.java');s=p.read_text().replace('GhostEscapeProgress.move(e,GhostEscapeProgress.Phase.CRAWL,0);','e.putBoolean("PlayerEyesHeld",true);\n        GhostEscapeProgress.move(e,GhostEscapeProgress.Phase.CRAWL,0);');p.write_text(s)
p=b/'GhostEscapeCovert.java';s=p.read_text()
a=s.index('                if(!e.contains("ErrandHomeAt"))');z=s.index('                GhostEscapeProgress.homeErrand',a)
s=s[:a]+'''                if(!retrieveStoneFromVisitors(p,s,ghost,e,child))return true;
'''+s[z:]
i=s.index('    private static',s.index('case HOME_ERRAND'))
s=s[:i]+'''    private static boolean retrieveStoneFromVisitors(ServerPlayer p,VisitData.Scene s,CompoundTag ghost,CompoundTag escape,
                                                   com.coraline.tycoon.TycoonNpc child){
        if(!stone(escape).isEmpty())return true;
        var home=(net.minecraft.server.level.ServerLevel)child.level();
        var front=child.blockPosition();
        var first=com.coraline.otherworld.OtherWorld.safePlayer(home,front.east(2));
        var second=com.coraline.otherworld.OtherWorld.safePlayer(home,front.west(2));
        if(first==null||second==null||first.equals(second)||first.distSqr(front)>25||second.distSqr(front)>25)return false;
        var spink=ShowActors.acquire(p,s,"miss_spink",home,net.minecraft.world.phys.Vec3.atBottomCenterOf(first));
        var forcible=ShowActors.acquire(p,s,"miss_forcible",home,net.minecraft.world.phys.Vec3.atBottomCenterOf(second));
        if(spink==null||forcible==null||spink.level()!=home||forcible.level()!=home
                ||spink.distanceToSqr(child)>25||forcible.distanceToSqr(child)>25)return false;
        if(!escape.hasUUID("StoneReceipt")){
            escape.putUUID("StoneReceipt",java.util.UUID.randomUUID());
            escape.putUUID("StoneSpink",spink.getUUID());
            escape.putUUID("StoneForcible",forcible.getUUID());
        }
        if(!spink.getUUID().equals(escape.getUUID("StoneSpink"))||!forcible.getUUID().equals(escape.getUUID("StoneForcible")))return false;
        var item=new ItemStack(ModItems.SEEING_STONE.get());
        var tag=item.getOrCreateTag();
        tag.putUUID("StoryOwner",s.owner);
        tag.putUUID("StoryDoor",s.door);
        tag.putUUID("GhostStoneReceipt",escape.getUUID("StoneReceipt"));
        forcible.setItemSlot(EquipmentSlot.MAINHAND,item);
        child.getLookControl().setLookAt(forcible,30,30);
        forcible.getLookControl().setLookAt(child,30,30);
        var inventory=escape.getCompound("QuestInventory");
        inventory.put("Stone",forcible.getMainHandItem().save(new CompoundTag()));
        escape.put("QuestInventory",inventory);
        forcible.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
        escape.putString("StoneSource","SPINK_AND_FORCIBLE_HANDOFF");
        ShowActors.releaseHidden(p,s,"miss_spink");
        ShowActors.releaseHidden(p,s,"miss_forcible");
        OtherMotherLoop.changed(p);
        return true;
    }
'''+s[i:]
p.write_text(s)
