from pathlib import Path
root=Path('src/main/java/com/coraline')
def read(path): return (root/path).read_text()
def save(path,s):
 with (root/path).open('w',newline='\r\n') as f:f.write(s)
path='feast/PalaceKeyCustody.java'
s=read(path)
s=s.replace('    private static final String INVENTORY_RETRY=', '    private static final String INVENTORY_RETRY=')
s=s.replace('    public static boolean place(','''    private static final String PREPARED_RETRY="Could not verify prepared original-key journal; original retained.";
    private static final String RETRY="KeyInventoryCheckpointPending";

    private static boolean originalKeyConflict(ServerPlayer p,VisitData.Scene s,ItemStack expected){
        int amount=0;
        for(int i=0;i<p.getInventory().getContainerSize();i++){
            var stack=p.getInventory().getItem(i);
            if(ParentRescueStory.key(stack,s)){
                if(stack!=expected)return true;
                amount+=stack.getCount();
            }
        }
        for(int i=0;i<p.getEnderChestInventory().getContainerSize();i++)
            if(ParentRescueStory.key(p.getEnderChestInventory().getItem(i),s))return true;
        return ParentRescueStory.key(p.containerMenu.getCarried(),s)||amount!=(expected==null?0:1);
    }

    public static boolean place(''')
s=s.replace('        if(original.getOrCreateTag().contains(IDENTITY)', '''        if(originalKeyConflict(p,s,original)){
            blocked(p,root,"Multiple or contradictory original-key stacks; review required.");
            return false;
        }
        if(original.getOrCreateTag().contains(IDENTITY)''')
s=s.replace('if(!saveInventory(p)){blocked(p,root,INVENTORY_RETRY);return false;}','''if(!saveInventory(p)){
            root.putBoolean(RETRY,true);
            OtherMotherLoop.changed(p);
            p.displayClientMessage(net.minecraft.network.chat.Component.literal("Could not save the original key. Try placing it again."),true);
            return false;
        }
        root.remove(RETRY);''')
s=s.replace('blocked(p,root,"Could not verify prepared original-key journal; original retained.")','blocked(p,root,PREPARED_RETRY)')
s=s.replace('if(oldBlocker.startsWith("Could not verify original-key inventory checkpoint")||oldBlocker.startsWith("Could not verify prepared original-key journal"))','if(PREPARED_RETRY.equals(oldBlocker))')
s=s.replace('if(external||count>1){','if(external||count>1||originalKeyConflict(p,s,found<0?null:p.getInventory().getItem(found))){')
s=s.replace('||root.contains("KeyCustodyBlocker")||!boundBundle(p,s,root,bundle)||!reconcile(p,s)', '||!boundBundle(p,s,root,bundle)||!reconcile(p,s)')
save(path,s)
path='palace/PalaceSiteData.java'
s=read(path)
i=s.index('    /** Only the real streamed builder')
s=s[:i]+'''    boolean reselectUnbuiltSite(){
        if(incompatibleOriginal!=null||PalaceGenerationRules.stage(generation).ordinal()>PalaceGenerationRules.Stage.SURVEY_COMPANY.ordinal()
                ||Arrays.stream(Kind.values()).anyMatch(this::generated))return false;
        var palace=sites.get(Kind.PALACE);
        int next=palace==null?0:palace.getInt("Candidate");
        sites.clear();
        if(next>0){var cursor=new CompoundTag();cursor.putInt("Candidate",next);sites.put(Kind.PALACE,cursor);}
        generation=new CompoundTag();
        blocker="";
        setDirty();
        return true;
    }
    void retryLegacyPause(){
        if(incompatibleOriginal!=null)return;
        if(blocker.equals("Palace generation stopped safely: IllegalStateException: Landmark chunk request failed safely")){
            blocker="";
            setDirty();
        }else if(blocker.startsWith("Player exploration reached the pending region; job stopped")
                ||blocker.startsWith("Player/world edit inside pending landmark footprint;")){
            reselectUnbuiltSite();
        }
    }
    public boolean companyMapReady(){return incompatibleOriginal==null&&generated(Kind.COMPANY)&&position(Kind.COMPANY).isPresent();}
'''+s[i:]
save(path,s)
path='palace/PalaceWorldGeneration.java'
s=read(path)
s=s.replace(' * Player entry/edit after claiming stops the job instead of overwriting a build.', ' * Player proximity pauses work. An edit abandons a survey or protects a partial build.')
s=s.replace('    private static int loadedThisTick;', '''    private static int loadedThisTick;
    private static long retryAt;
    private static final class RetryableGeneration extends RuntimeException {
        RetryableGeneration(String message){super(message);}
    }''')
s=s.replace('throw new IllegalStateException("Landmark chunk request failed safely")', 'throw new RetryableGeneration("Landmark chunk unavailable")')
s=s.replace('if(n.getBoolean("CompanyClaimed")&&Math.abs(p.getX()', 'if(n.getBoolean("CompanyClaimed")&&!d.generated(Kind.COMPANY)&&Math.abs(p.getX()',1)
s=s.replace('            enter(d,TERRAIN);save(l,d);return;', '            n.putBoolean("CompanyFirst",true);enter(d,COMPANY_PREPARE);save(l,d);return;',1)
s=s.replace('enter(d,verify?COMPANY_PREPARE:HOUSE_VERIFY);','enter(d,verify?(d.generated(Kind.COMPANY)?WELL:COMPANY_PREPARE):HOUSE_VERIFY);')
s=s.replace('enter(d,company?verify?WELL:COMPANY_VERIFY:verify?SIGNS:WELL_VERIFY);', 'enter(d,company?verify?(n.getBoolean("CompanyFirst")?TERRAIN:WELL):COMPANY_VERIFY:verify?SIGNS:WELL_VERIFY);')
s=s.replace('if(chunk(l,base.getX(),base.getZ())==null)return;', '''if(l.players().stream().anyMatch(player->player.distanceToSqr(Vec3.atCenterOf(base))<16*16))return;
        if(chunk(l,base.getX(),base.getZ())==null||!l.areEntitiesLoaded(new ChunkPos(base).toLong()))return;''')
s=s.replace('throw new IllegalStateException("Shared sign creation unavailable")','throw new RetryableGeneration("Shared sign creation unavailable")')
s=s.replace('throw new IllegalStateException("Shared sign spawn refused")','throw new RetryableGeneration("Shared sign spawn refused")')
s=s.replace('currentServer=server;currentChunk=null;', 'currentServer=server;retryAt=0;currentChunk=null;')
s=s.replace('var l=server.overworld();var d=PalaceSiteData.get(server);if(!d.initialized()||!d.blocker().isEmpty()){releasePending();return;}', '''var l=server.overworld();var d=PalaceSiteData.get(server);
        if(l.getGameTime()<retryAt)return;
        d.retryLegacyPause();
        if(!d.initialized()||!d.blocker().isEmpty()){releasePending();return;}''')
s=s.replace('                stage=PalaceGenerationRules.stage(d.generation());','                stage=d.generation().isEmpty()?SELECT_PALACE:PalaceGenerationRules.Stage.valueOf(d.generation().getString("Stage"));')
s=s.replace('if(!d.blocker().isEmpty()||loadedThisTick>0)break;', 'if(!d.blocker().isEmpty()||loadedThisTick>0||stage==SIGNS)break;')
s=s.replace('        }catch(Exception failure){releasePending();d.stopGeneration("Palace generation stopped safely: "+failure.getClass().getSimpleName()+": "+failure.getMessage());save(l,d);}', '''        }catch(RetryableGeneration|java.io.IOException|java.util.concurrent.CompletionException failure){
            releasePending();
            retryAt=l.getGameTime()+100;
            d.setDirty();
        }catch(RuntimeException failure){
            releasePending();
            d.stopGeneration("Palace generation needs review at "+d.generation().getString("Stage")
                    +" cursor "+d.generation().getInt("Cursor")+": "+failure.getMessage());
            save(l,d);
        }''')
s=s.replace('||n.getBoolean("CompanyClaimed")&&Math.abs(p.getX()-n.getInt("CompanyX"))<=80&&Math.abs(p.getZ()-n.getInt("CompanyZ"))<=80){d.stopGeneration("Player/world edit inside pending landmark footprint; original edit preserved and generation stopped.");save(l,d);}', '''||n.getBoolean("CompanyClaimed")&&!d.generated(Kind.COMPANY)&&Math.abs(p.getX()-n.getInt("CompanyX"))<=80&&Math.abs(p.getZ()-n.getInt("CompanyZ"))<=80){
            releasePending();
            if(!d.reselectUnbuiltSite())d.stopGeneration("Player edit at "+p.toShortString()+" intersects the partially built landmark. Inspect the original generation checkpoint before resuming.");
            save(l,d);
        }''')
s=s.replace('@SubscribeEvent public static void placed', '@SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST) public static void placed')
s=s.replace('@SubscribeEvent public static void broken', '@SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST) public static void broken')
save(path,s)
path='palace/PalaceGenerationRules.java'
s=read(path).replace('"ColumnChanged","ProfileSurveyed"', '"ColumnChanged","ProfileSurveyed","CompanyFirst"')
save(path,s)
path='feast/PalaceJourney.java'
s=read(path)
s=s.replace('return sites.blocker().isEmpty()&&sites.generated(Kind.COMPANY)&&sites.position(Kind.COMPANY).isPresent();','return sites.companyMapReady();')
s=s.replace('''if(p.tickCount%80==0)p.displayClientMessage(Component.literal("Moving Company map pending: "
                    +(PalaceSiteData.get(p.server).generated(Kind.COMPANY)?"keep your key and a free inventory slot.":"its destination is still being prepared.")),true);''','''if(p.tickCount%80==0){
                    var sites=PalaceSiteData.get(p.server);
                    String status=sites.companyMapReady()?"Keep your key and a free inventory slot."
                            :sites.blocker().isEmpty()?"The destination is still being prepared.":sites.blocker();
                    p.displayClientMessage(Component.literal("Moving Company map pending. "+status),true);
                }''')
save(path,s)
