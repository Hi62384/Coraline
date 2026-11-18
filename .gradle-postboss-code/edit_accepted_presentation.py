from pathlib import Path
b=Path('src/main/java/com/coraline/feast')
def edit(n,o,r):
 p=b/(n+'.java');s=p.read_text(encoding='utf-8');assert o in s,n;p.write_text(s.replace(o,r,1),encoding='utf-8')
edit('GhostEscapeWager','("0".equals(value)||"1".equals(value))','"0".equals(value)')
edit('GhostEscapeWager','java.util.List.of("I accept. Let the child go.","I refuse.")','java.util.List.of("I accept. Let the child go.")')
p=b/'GhostEscapeWager.java';s=p.read_text(encoding='utf-8');a=s.index('        if(!"0".equals(value)){');z=s.index('        if(!GhostEscapeProgress.wagerAgreed',a);p.write_text(s[:a]+s[z:],encoding='utf-8')
p=Path('src/wybietest/java/com/coraline/feast/GhostEscapeStateCheck.java');s=p.read_text(encoding='utf-8').replace('require(GhostEscapeWager.responseMatches(conversation,wagerToken,"1",120)&&!GhostEscapeWager.responseMatches(conversation,wagerToken,"junk",120),"Malformed wager choice consumed the refusal branch");','require(!GhostEscapeWager.responseMatches(conversation,wagerToken,"1",120)&&!GhostEscapeWager.responseMatches(conversation,wagerToken,"junk",120),"Removed refusal or malformed wager response was accepted");');p.write_text(s,encoding='utf-8')
edit('GhostVictimDirector','''            if(!GhostVictimMirrors.childPresent(p,n,id,child)){
                p.displayClientMessage(net.minecraft.network.chat.Component.literal("Wait until the child is near and looking toward this mirror."),true);''','''            if(!GhostVictimMirrors.childPresent(p,n,id,child)){
                String reason=child==null||child.level()!=p.level()?"The child is away from this mirror."
                        :GhostVictimMirrors.childAtMirror(n,id,child,false)?"Wait for the child to face the mirror, then come close."
                        :"The child needs a clear path and sightline to the front of the mirror.";
                p.displayClientMessage(net.minecraft.network.chat.Component.literal(reason),true);''')
edit('GhostVictimMirrors','''        if(GhostVictimProgress.energy(n)<GhostVictimConfig.MIRROR_ENTRY.get()){''','''        var end=center.subtract(delta.normalize().scale(.16));
        if(p.level().clip(new ClipContext(p.getEyePosition(),end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p))
                .getType()!=HitResult.Type.MISS)return false;
        if(GhostVictimProgress.energy(n)<GhostVictimConfig.MIRROR_ENTRY.get()){''')
edit('GhostVictimSnapshot','''            if(preview)points.getCompound(points.size()-1).putString("Label","Tomorrow's mirror");''','''            String label=key.equals("bedroom")?"Bedroom mirror":key.replace('_',' ')+" mirror";
            points.getCompound(points.size()-1).putString("Label",preview?"Tomorrow: "+label:
                    label+(GhostVictimMirrors.childPresent(p,n,key,child)?" — child ready":" — waiting for the child"));''')
edit('GhostVictimActivities','''        if(n.contains("ScoutSites"))return;var level=AcceptedButtonEyesWorld.level(p);if(level==null)return;
        var sites=new ListTag();var planned=GhostVictimSchedule.tomorrow(n).getList("Activities",Tag.TAG_STRING);
        for(int i=0;i<planned.size();i++){
            String id=planned.getString(i);var row=new CompoundTag();var at=housePortal(s).east(i*3);''','''        if(n.contains("ScoutSites")){
            if(n.getInt("ScoutEntranceVersion")>=1||n.contains("ScoutActivity"))return;
            for(Tag entry:n.getList("ScoutSites",Tag.TAG_COMPOUND))remove(p,(CompoundTag)entry,"Portal");
            n.remove("ScoutSites");
        }
        var level=AcceptedButtonEyesWorld.level(p);if(level==null)return;
        var sites=new ListTag();var planned=GhostVictimSchedule.tomorrow(n).getList("Activities",Tag.TAG_STRING);
        for(int i=0;i<Math.min(1,planned.size());i++){
            String id=planned.getString(i);var row=new CompoundTag();var at=housePortal(s);
            LoopProps.hold(level,at);
            if(!RescueEpilogue.safeReturn(level,p,net.minecraft.world.entity.Pose.STANDING,Vec3.atBottomCenterOf(at)))return;''')
edit('GhostVictimActivities','''        n.put("ScoutSites",sites);OtherMotherLoop.changed(p);''','''        n.put("ScoutSites",sites);n.putInt("ScoutEntranceVersion",1);OtherMotherLoop.changed(p);''')
