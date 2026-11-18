from pathlib import Path
p=Path('src/wybietest/java/com/coraline/feast/RescueBossGameTests.java')
s=p.read_text()
s=s.replace('''n.getInt("BossPattern")==pattern&&n.getInt("BossBeat")==0''','''n.getInt("BossPattern")== (state==RescueChapter.Phase.SWARM_FIGHT?RescueBosses.ratPattern(tier,pattern):pattern)&&n.getInt("BossBeat")==0''')
s=s.replace('"Empty fist damaged the control point"', '"A strike without attack cooldown damaged the control point"')
old='''n.putBoolean("Vulnerable",true);n.putLong("WeakUntil",p.serverLevel().getGameTime()+80);RescueBosses.hit(p,point);float after=n.getFloat("BossHealth");h.assertTrue(after<health,"Owner could not strike the actual revealed weak point");RescueBosses.hit(p,point);h.assertTrue(n.getFloat("BossHealth")==after,"Same-tick attack flood bypassed cooldown");'''
new='''p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
            n.putBoolean("Vulnerable",true);n.putLong("WeakUntil",p.serverLevel().getGameTime()+80);
            RescueBosses.hit(p,point);
            float after=n.getFloat("BossHealth");
            h.assertTrue(after==health-2,"A cooled unarmed strike did not damage the revealed weak point");
            n.putBoolean("Vulnerable",true);n.putLong("WeakUntil",p.serverLevel().getGameTime()+80);
            RescueBosses.hit(p,point);
            h.assertTrue(n.getFloat("BossHealth")==after,"Same-tick unarmed attack bypassed cooldown");
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
            n.putLong("HitAt",p.serverLevel().getGameTime()-15);
            RescueBosses.hit(p,point);
            h.assertTrue(after-n.getFloat("BossHealth")>2,"A cooled weapon strike did not deal more than an unarmed strike");'''
assert old in s
s=s.replace(old,new)
i=s.rfind('}')
s=s[:i]+'''    @GameTest(template="empty") public static void ratmasterWarningsAndHitboxesUseTheSameEdges(GameTestHelper h){
        double[][] inside={{0,8},{10,0},{3,0},{-3,0},{5,0}};
        double[][] outside={{1.7,8},{10,4},{3,2.2},{3,0},{8,0}};
        for(int tier=0;tier<3;tier++)for(int pattern=0;pattern<5;pattern++){
            var region=RatmasterRegion.attack(pattern,tier,Vec3.ZERO,Vec3.ZERO,0);
            h.assertTrue(region.hits(body(inside[pattern],0)),"Inside attack region was marked safe: "+tier+"/"+pattern);
            h.assertFalse(region.hits(body(outside[pattern],0)),"Outside boundary was hit: "+tier+"/"+pattern);
            h.assertFalse(region.hits(body(inside[pattern],3)),"Jump outside vertical danger extent was hit");
            h.assertFalse(region.hits(body(inside[pattern],-3)),"Player below the arena was hit");
            h.assertTrue(region.warning().size()>10,"Shared attack model produced no readable contour");
        }
        var wave=RatmasterRegion.attack(0,0,Vec3.ZERO,Vec3.ZERO,0);
        h.assertTrue(wave.hits(new net.minecraft.world.phys.AABB(1.5,0,15.8,1.9,1.8,16.2)),"Player body overlap at strip corner was missed");
        h.assertFalse(wave.hits(new net.minecraft.world.phys.AABB(1.7,0,15.8,2.1,1.8,16.2)),"Dodging beyond the marked corner still took damage");
        h.succeed();
    }
    private static net.minecraft.world.phys.AABB body(double[] at,double y){
        return new net.minecraft.world.phys.AABB(at[0]-.03,y,at[1]-.03,at[0]+.03,y+1.8,at[1]+.03);
    }
    @GameTest(template="empty") public static void finalRoomDoorsAreLockedAcrossConfrontationButReleaseInTheWeb(GameTestHelper h){
        var p=player(h);
        var s=OtherMotherLoop.scene(p);
        ParentRescueStory.scenery(s).putLong("LittleDoor",new BlockPos(-17,110,18).asLong());
        var n=RescueChapter.data(s);
        n.putBoolean("RoomLocked",true);
        try{
            var entrance=FirstEscapeScene.entrance(s);
            var little=BlockPos.of(ParentRescueStory.scenery(s).getLong("LittleDoor"));
            for(var phase:RescueChapter.Phase.values()){
                n.putString("Phase",phase.name());
                boolean locked=phase.ordinal()>=RescueChapter.Phase.ROOM_LOCKED.ordinal()&&phase.ordinal()<=RescueChapter.Phase.FINAL_O.ordinal();
                for(var at:java.util.List.of(entrance,entrance.above(),entrance.south(),entrance.south().above(),little,little.above()))
                    h.assertTrue(BeldamFinale.lockedDoor(s,at)==locked,"Wrong protection in "+phase+" for "+at);
                h.assertFalse(BeldamFinale.lockedDoor(s,entrance.north()),"Unrelated door was locked");
            }
        }finally{clean(p);}
        h.succeed();
    }
    @GameTest(template="empty") public static void falseFloorReceiptIsClearedAndObstructingBlockEntityIsPreserved(GameTestHelper h){
        var p=player(h);
        var s=OtherMotherLoop.scene(p);
        var level=p.server.getLevel(com.coraline.otherworld.OtherWorld.KEY);
        var house=new BlockPos(200,130,200);
        ParentRescueStory.scenery(s).putLong("House",house.asLong());
        var original=FirstEscapeScene.at(s,23,5,44);
        var obstruction=FirstEscapeScene.at(s,20,5,42);
        var expected=com.coraline.otherworld.RosewoodHouse.palaceCells().stream()
                .filter(cell->cell.offset().equals(original.subtract(house))).findFirst().orElseThrow().state();
        level.getChunkAt(obstruction);
        level.getChunkAt(original);
        level.setBlock(obstruction,Blocks.BARREL.defaultBlockState(),2);
        level.setBlock(original,expected,2);
        try{
            var n=RescueChapter.data(s);
            n.putBoolean("FloorOpened",true);
            h.assertFalse(BeldamFinale.prepareFall(p,s),"Obstructed floor trusted a legacy success receipt");
            h.assertTrue(!n.getBoolean("FloorOpened")&&level.getBlockEntity(obstruction)!=null
                    &&level.getBlockState(original).equals(expected),"Validation modified the floor before checking the whole footprint");
            level.removeBlock(obstruction,false);
            h.assertTrue(BeldamFinale.prepareFall(p,s)&&n.getBoolean("FloorOpened")&&level.isEmptyBlock(original),
                    "Cleared floor could not commit actual falling clearance");
        }finally{clean(p);level.removeBlock(obstruction,false);level.removeBlock(original,false);}
        h.succeed();
    }
'''+s[i:]
p.write_text(s)
