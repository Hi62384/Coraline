from pathlib import Path
p=Path('src/main/java/com/coraline/feast/PalaceWellEncounter.java')
s=p.read_text()
old='''                    int leg=n.getInt("ThrowStage");Vec3 target=leg==0?new Vec3(bundle.getX(),c.y+3.5,bundle.getZ()):leg==1?c.add(0,3.5,0):new Vec3(c.x,Math.max(c.y-16,bundle.getY()-.45),c.z);
                    if(move(p,bundle,target,.30)&&arrived(bundle,target)&&leg<2)n.putInt("ThrowStage",leg+1);
                    bundle.frame(1,t,1);if(heard&&PalaceKeyCustody.dispose(p,s,bundle))progress=state(s);'''
new='''                    int leg=PalaceWellMotion.throwBundle(p.serverLevel(),bundle,c,n.getInt("ThrowStage"));
                    n.putInt("ThrowStage",leg);
                    ParentRescueStory.scenery(s).putLong("RescueWellBundlePosition",bundle.blockPosition().asLong());
                    bundle.frame(1,t,1);
                    if(heard&&leg==2&&PalaceKeyCustody.dispose(p,s,bundle))progress=state(s);'''
assert old in s
s=s.replace(old,new)
s=s.replace('"HandAtWell","Falling","PresentationRetired"','"HandAtWell","Falling","PresentationRetired","BikeArrived"')
s=s.replace('var b=bike(p,s,n,progress,w);if(b==null)return;w=wybie', 'var b=bike(p,s,n,progress,w);if(b==null){if(t%80==0)p.displayClientMessage(Component.literal("Wybie is looking for a clear, hidden approach to the well."),true);return;}w=wybie',1)
with p.open('w',newline='\r\n') as f:f.write(s)
p=Path('src/main/java/com/coraline/feast/PalaceWellMotion.java')
s=p.read_text()
i=s.index('    static boolean seen(')
s=s[:i]+'''    /** Raise, swing over the rim, then lower the original bundle into the shaft. */
    static int throwBundle(ServerLevel level,CinematicPropEntity bundle,Vec3 well,int leg){
        double height=well.y+3.5;
        double horizontal=bundle.position().subtract(well).horizontalDistanceSqr();
        if(leg==2&&horizontal>.04||leg==1&&Math.abs(bundle.getY()-height)>.35)leg=0;
        var target=switch(leg){
            case 0->new Vec3(bundle.getX(),height,bundle.getZ());
            case 1->well.add(0,3.5,0);
            default->new Vec3(well.x,Math.max(well.y-16,bundle.getY()-.45),well.z);
        };
        if(move(level,bundle,target,.30)&&bundle.position().distanceToSqr(target)<.035&&leg<2)return leg+1;
        return leg;
    }
'''+s[i:]
with p.open('w',newline='\r\n') as f:f.write(s)
p=Path('src/wybietest/java/com/coraline/feast/PalaceJourneyGameTests.java')
s=p.read_text()
i=s.rfind('}')
s=s[:i]+'''    @SuppressWarnings("unchecked")
    private static java.util.List<net.minecraft.server.level.ServerPlayer> checkpointPlayers(net.minecraft.server.MinecraftServer server){
        try{
            var players=net.minecraft.server.players.PlayerList.class.getDeclaredField("players");
            players.setAccessible(true);
            return (java.util.List<net.minecraft.server.level.ServerPlayer>)players.get(server.getPlayerList());
        }catch(ReflectiveOperationException unavailable){
            throw new IllegalStateException("GameTest save checkpoint fixture unavailable",unavailable);
        }
    }
    private static void verifyWrappedKeyDisposal(GameTestHelper h,net.minecraft.server.level.ServerPlayer p,VisitData.Scene s,net.minecraft.core.BlockPos rim){
        var level=p.serverLevel();
        var root=PalaceJourney.existing(s);
        var hand=UUID.randomUUID();
        var current=new CompoundTag();
        current.putInt("Version",1);
        current.putUUID("Owner",s.owner);
        current.putUUID("Door",s.door);
        current.putUUID("Hand",hand);
        current.putUUID("Session",UUID.randomUUID());
        current.putString("Phase","WRAPPED");
        current.putString("WybieLife","DEAD_BEFORE_WELL");
        current.putInt("Ticks",0);
        current.putInt("RescueTicks",0);
        current.putInt("Attempts",1);
        current.putLong("LastServerTick",-1);
        root.put("Well",current);
        level.getChunkAt(rim);
        var well=net.minecraft.world.phys.Vec3.atBottomCenterOf(rim);
        var bundle=CinematicPropEntity.TYPE.get().create(level);
        bundle.configure(s,RescueProps.KEY_BUNDLE,0);
        bundle.setPos(well.add(0,.2,2.5));
        h.assertTrue(level.addFreshEntity(bundle),"Wrapped bundle could not spawn");
        root.putUUID("BundleEntity",bundle.getUUID());
        p.setPos(well.add(0,0,3.5));
        var players=checkpointPlayers(p.server);
        players.add(p);
        try{
            h.assertTrue(PalaceKeyCustody.place(p,s,bundle),"Original key could not reach verified bundle custody");
            var receipt=root.getCompound("KeyCustody").copy();
            h.assertTrue(receipt.getString("Stage").equals("HELD")&&p.getMainHandItem().isEmpty(),"Verified custody did not remove the original key");
            h.assertTrue(!PalaceKeyCustody.place(p,s,bundle)&&!PalaceKeyCustody.dispose(p,s,bundle),"Repeated interaction or an above-ground bundle completed disposal");
            int stage=2; // Inject a saved cursor whose entity position rolled back.
            for(int tick=0;tick<110;tick++)stage=PalaceWellMotion.throwBundle(level,bundle,well,stage);
            h.assertTrue(stage==2&&bundle.getY()<well.y-12,"The original bundle never physically descended through the well");
            h.assertTrue(PalaceKeyCustody.dispose(p,s,bundle),"Physical well throw did not commit disposal");
            var disposed=root.getCompound("KeyCustody").copy();
            h.assertTrue(disposed.getString("Stage").equals("DISPOSED")&&!PalaceKeyCustody.dispose(p,s,bundle)
                    &&root.getCompound("KeyCustody").equals(disposed),"Disposal was not exactly once");
        }finally{
            players.remove(p);
            bundle.discard();
        }
    }
'''+s[i:]
s=s.replace('''                h.assertTrue(PalaceTransport.reapCompleted(server,s,ride),"Cleanup was not idempotent");
                net.minecraftforge''','''                h.assertTrue(PalaceTransport.reapCompleted(server,s,ride),"Cleanup was not idempotent");
                verifyWrappedKeyDisposal(h,p,s,placements[2]);
                net.minecraftforge''')
p.write_text(s)
