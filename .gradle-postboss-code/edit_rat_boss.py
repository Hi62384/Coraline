from pathlib import Path
root=Path('src/main/java/com/coraline')
def read(name):return (root/name).read_text()
def put(name,s):
 with (root/name).open('w',newline='\r\n') as f:f.write(s)
name='feast/RatmasterRegion.java'
s=read(name).replace('across, 16, 1.5, 1.65','across, 16, 16, 1.65').replace('for (int lane = -1; lane <= 1; lane++)','for (int lane = tier==0?0:-1; lane <= (tier==0?0:1); lane++)')
put(name,s)
name='feast/RescueBosses.java'
s=read(name)
s=s.replace('state==THEATRE_FIGHT?240:216','state==THEATRE_FIGHT?240:72')
s=s.replace('n.remove("TheatreCycle");n.putInt', 'n.remove("TheatreCycle");n.remove("RatAttackCycle");n.remove("RatStrikeSpent");n.putInt')
before='''        float hp=n.getFloat("BossHealth"),maximum=n.getFloat("BossMaximum");'''
after='''        if(state==SWARM_FIGHT){
            var swarm=RescueEncounters.prop(p,s,"Swarm",RescueProps.SWARM,0,RescueEncounters.center(p,s));
            if(swarm==null||swarm.level()!=p.level()){
                n.putInt("Ticks",Math.max(0,time-1));
                n.putString("Blocker","Waiting for the original Ratmaster swarm.");
                return;
            }
            if(n.getFloat("BossMaximum")>72){
                float fraction=n.getFloat("BossHealth")/n.getFloat("BossMaximum");
                n.putFloat("BossMaximum",72);
                n.putFloat("BossHealth",72*fraction);
                n.remove("RatAttackCycle");
            }
            n.remove("Blocker");
        }
        float hp=n.getFloat("BossHealth"),maximum=n.getFloat("BossMaximum");'''
assert before in s;s=s.replace(before,after,1)
s=s.replace('if(state!=MANTIS_FIGHT)n.remove("WeakUntil");}int clock=', 'if(state!=MANTIS_FIGHT)n.remove("WeakUntil");n.remove("RatAttackCycle");}int clock=')
s=s.replace('        if(state==MANTIS_FIGHT){beat=clock%240;', '''        if(state==SWARM_FIGHT){
            if(!n.contains("RatAttackCycle")&&beat>1){
                n.putInt("BossCycleStart",time);
                clock=0;
                beat=0;
                cycle=0;
            }
            pattern=ratPattern(tier,cycle);
        }
        if(state==MANTIS_FIGHT){beat=clock%240;''')
s=s.replace('state==THEATRE_FIGHT?beat>=200:RescueChallengeMath.vulnerable(clock,tier)','state==THEATRE_FIGHT?beat>=200:ratOpening(tier,beat)')
old='''        if(beat<=1||state==MANTIS_FIGHT&&beat==35){n.putDouble("AttackX",aim.getX());n.putDouble("AttackZ",aim.getZ());n.putDouble("AttackAngle",Math.atan2(toPlayer.z,toPlayer.x));}
        var locked=new Vec3(n.getDouble("AttackX"),aim.getY(),n.getDouble("AttackZ"));
        Vec3 weak=state==MANTIS_FIGHT?mantis(p,s,time,3):state==THEATRE_FIGHT?actresses(p,s,time,false):swarm(p,s,time,pattern,beat);'''
new='''        boolean newRatAttack=state==SWARM_FIGHT&&(!n.contains("RatAttackCycle")||n.getInt("RatAttackCycle")!=cycle);
        if(newRatAttack||state!=SWARM_FIGHT&&(beat<=1||state==MANTIS_FIGHT&&beat==35)){
            n.putDouble("AttackX",aim.getX());
            n.putDouble("AttackZ",aim.getZ());
            n.putDouble("AttackAngle",Math.atan2(toPlayer.z,toPlayer.x));
            if(newRatAttack){
                n.putInt("RatAttackCycle",cycle);
                n.putInt("RatRealCarrier",p.getRandom().nextInt(4));
                n.remove("WeakUntil");
                n.remove("RatStrikeSpent");
                p.serverLevel().playSound(null,BlockPos.containing(center),net.minecraft.sounds.SoundEvents.RAVAGER_STEP,
                        net.minecraft.sounds.SoundSource.HOSTILE,.75F,1.2F-tier*.16F);
            }
        }
        var locked=new Vec3(n.getDouble("AttackX"),aim.getY(),n.getDouble("AttackZ"));
        Vec3 weak=state==MANTIS_FIGHT?mantis(p,s,time,3):state==THEATRE_FIGHT?actresses(p,s,time,false):swarm(p,s,time,pattern,beat,tier);'''
assert old in s;s=s.replace(old,new,1)
s=s.replace('eye.frame(n.getBoolean("Vulnerable")&&n.getLong("WeakUntil")>=p.serverLevel().getGameTime()?1:0,time,0);','eye.frame(state==SWARM_FIGHT&&pattern==4||n.getBoolean("Vulnerable")&&n.getLong("WeakUntil")>=p.serverLevel().getGameTime()?1:0,time,0);')
s=s.replace('state==SWARM_FIGHT&&RescueEncounters.seeing(member)', 'state==SWARM_FIGHT&&n.getBoolean("Vulnerable")&&!n.getBoolean("RatStrikeSpent")&&RescueEncounters.seeing(member)')
s=s.replace('''        if(beat<40){if(state!=MANTIS_FIGHT)RescueChapter.hint(p,warning(state,pattern));if(time%5==0)telegraph(p,locked,pattern,center,n.getDouble("AttackAngle"));}
        if(state==THEATRE_FIGHT?beat<200&&beat%10==0:beat==60||tier==2&&beat==82)for(var member:participants){boolean hit=state==THEATRE_FIGHT?actressContact(p,s,member):hazard(member,state,pattern,center,locked,n.getDouble("AttackAngle"));''','''        boolean impact=state==THEATRE_FIGHT?beat<200&&beat%10==0:beat==60||tier==2&&beat==82;
        if(state==SWARM_FIGHT){
            var region=RatmasterRegion.attack(pattern,tier,center,locked,n.getDouble("AttackAngle"));
            if(beat<=60||tier==2&&beat<=82){
                if(beat<60)RescueChapter.hint(p,warning(state,pattern));
                if(beat%8==0||impact){
                    var particle=impact?net.minecraft.core.particles.ParticleTypes.FLAME:net.minecraft.core.particles.ParticleTypes.END_ROD;
                    for(var member:participants)for(var point:region.warning())
                        p.serverLevel().sendParticles(member,particle,true,point.x,point.y,point.z,1,0,0,0,0);
                }
            }
            if(impact)for(var member:participants)if(region.hits(member.getBoundingBox())&&member.invulnerableTime<=0)
                RescueCombat.bossHarm(member,tier==2?10:8);
        }else{
            if(beat<40){if(state!=MANTIS_FIGHT)RescueChapter.hint(p,warning(state,pattern));if(time%5==0)telegraph(p,locked,pattern,center,n.getDouble("AttackAngle"));}
            if(impact)for(var member:participants){boolean hit=state==THEATRE_FIGHT?actressContact(p,s,member):hazard(member,state,pattern,center,locked,n.getDouble("AttackAngle"));''')
s=s.replace('scale(.18)).add(0,.12,0));}}\n        if(state==MANTIS_FIGHT', 'scale(.18)).add(0,.12,0));}}\n        }\n        if(state==MANTIS_FIGHT')
s=s.replace('RescueProps.hide(p,s,"Swarm");RescueEncounters.startChase', 'RescueProps.hide(p,s,"Swarm");for(int i=0;i<4;i++)RescueProps.hide(p,s,"RatDecoy"+i);RescueEncounters.startChase')
s=s.replace('''        if(!n.getBoolean("Vulnerable")||n.getLong("WeakUntil")<now)''','''        boolean rats=RescueChapter.phase(s)==SWARM_FIGHT;
        if(rats&&(n.getBoolean("RatStrikeSpent")||target!=RescueProps.find(p,s,"WeakPoint")))return true;
        if(!n.getBoolean("Vulnerable")||n.getLong("WeakUntil")<now)''')
s=s.replace('''        n.putLong("HitAt",now);float damage=''','''        if(rats){
            n.putBoolean("RatStrikeSpent",true);
            n.remove("WeakUntil");
        }
        n.putLong("HitAt",now);float damage=''')
s=s.replace('if(unarmed)return 2.0F;', 'if(unarmed)return phase==SWARM_FIGHT?4.0F:2.0F;')
a=s.index('    private static Vec3 swarm(')
b=s.index('    public static void lose(',a)
s=s[:a]+'''    static int ratPattern(int tier,int cycle){
        return switch(tier){
            case 0->cycle%2==0?0:2;
            case 1->new int[]{1,2,4,3}[cycle%4];
            default->new int[]{3,1,4,2,0}[cycle%5];
        };
    }
    static boolean ratOpening(int tier,int beat){
        return beat>=(tier==2?100:90)&&beat<(tier==2?136:160);
    }
    private static Vec3 swarm(ServerPlayer p,VisitData.Scene s,int time,int attack,int beat,int tier){
        var center=RescueEncounters.center(p,s);
        var n=RescueChapter.data(s);
        var swarm=RescueProps.find(p,s,"Swarm");
        if(swarm==null)return null;
        swarm.setPos(center);
        RescueProps.frame(swarm,tier==2&&beat<90?7:attack,time,beat/180F);
        var candidates=RatmasterRegion.carriers(center,n.getDouble("AttackAngle"));
        int real=n.getInt("RatRealCarrier");
        for(int i=0;i<candidates.size();i++){
            if(attack!=4||i==real){RescueProps.hide(p,s,"RatDecoy"+i);continue;}
            var at=candidates.get(i);
            var decoy=RescueEncounters.prop(p,s,"RatDecoy"+i,RescueProps.EYE,5+3*i,at);
            if(decoy!=null){decoy.setPos(at);decoy.frame(1,time,0);}
        }
        if(attack==4)return candidates.get(Math.floorMod(real,candidates.size()));
        double angle=beat*.017+n.getDouble("AttackAngle");
        return center.add(Math.cos(angle)*4,.6,Math.sin(angle)*4);
    }
'''+s[b:]
put(name,s)
name='client/SpectralEyes.java'
s=read(name).replace('e->(e.kind()==RescueProps.EYE||e.kind()==RescueProps.CONTROL_POINT)&&!e.isInvisible()&&owned(e)', 'e->(e.kind()==RescueProps.EYE&&e.slot()<3||e.kind()==RescueProps.CONTROL_POINT)&&!e.isInvisible()&&owned(e)')
put(name,s)
name='client/renderer/RescuePresentation.java'
s=read(name).replace('if(e.cue()==2){x+=(t%80)*.07;z=Math.sin(i+t*.03)*5;}', '''if(e.cue()==2){x+=(t%80)*.07;z=Math.sin(i+t*.03)*5;}
                if(e.cue()==5){x*=Math.max(.18,e.value());z*=Math.max(.18,e.value());y=(i%10)*.21*(1-e.value());}
                if(e.cue()==7){double burst=Math.sin(Math.PI*Math.min(1,e.value()*2));x+=(i%2==0?1:-1)*burst*6;z+=(i%4<2?1:-1)*burst*2.5;y=(i%7)*burst*.15;}''')
put(name,s)
