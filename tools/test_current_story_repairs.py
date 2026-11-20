"""Source/asset regressions, supplementary to native and rendered checks."""
import hashlib,json,unittest
from pathlib import Path
from import_numbered_original_audio import ogg_measure

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'src/main/java/com/coraline'
ASSETS=ROOT/'src/main/resources/assets/coraline'
def source(name):return (JAVA/name).read_text(encoding='utf-8-sig')

class CurrentStoryRepairs(unittest.TestCase):
    def test_proposal_dismount_is_stopped_on_server_and_before_client_prediction(self):
        text=source('feast/BlackIsTraditional.java')
        self.assertIn('EntityMountEvent event',text);self.assertIn('ClientboundSetPassengersPacket(chair)',text)
        self.assertIn('tag.getInt("VisitEventSerial")==s.loop.event.getInt("Serial")',text)
        self.assertIn('Phase.AWAITING_CHOICE.ordinal()',text);self.assertIn('releaseSeat(p,s);BedroomBarricade.clear',text)
        self.assertIn('event.getInput().shiftKeyDown=false',source('client/TraditionalClient.java'))
        prop=source('feast/ScenePropEntity.java')
        self.assertLess(prop.index('refreshProposalSeatLock();\n'),prop.index('if(VisitEvents.propTick(this))return;'))
    def test_no_old_dark_proposal_packet_on_mother_click(self):
        text=source('feast/VisitEvents.java').split('public static boolean interact(ServerPlayer p,ParentEntity parent)',1)[1]
        self.assertLess(text.index('FirstEscapeStory.engaged(major)||ParentRescueStory.started(major)'),text.index('BlackIsTraditional.sync'))
        self.assertIn('!ParentRescueClient.active()&&!RescueClient.active()',source('client/OtherWorldLighting.java'))
    def test_temporary_arm_and_dolls_have_scoped_retirement(self):
        text=source('feast/CinematicPropEntity.java')
        self.assertIn('retiredCanonicalGhost()||retireFinishedStoryProp()',text)
        self.assertIn('FirstEscapeStory.retiredGrab(this,s)',text);self.assertIn('MissingParentsCat.retiredDolls(this,s)',text)
        self.assertIn('FirstEscapeVisualRules.rescueHand(',source('client/renderer/FirstEscapePresentation.java'))
        self.assertIn('!dollsFinished(s)&&n.contains("BedFoot")',source('feast/MissingParentsCat.java'))
    def test_bike_uses_actual_story_tick_not_only_riding_pose(self):
        text=source('feast/FirstEscapeStory.java')
        self.assertIn('driveStoryBike(bike,p,s,w);',text);self.assertIn('bike.storyDrivenAt==now',text)
        self.assertIn('expanded==0||System.nanoTime()<deadline',source('feast/StoryBikeRoute.java'))
        self.assertIn('best=next;',source('feast/StoryBikeRoute.java'))
    def test_cat_tunnel_is_nonblocking_and_follows_behind(self):
        text=source('entity/CharacterEntity.java')
        self.assertIn('boolean canCollideWith(Entity other)',text);self.assertIn('void push(Entity other)',text)
        guide=source('feast/ParentRescueStory.java').split('static void guideTunnelCat(',1)[1].split('public static void tick(',1)[0]
        self.assertIn('playerProgress-2.5',guide);self.assertIn('cat.storyCatCrawl(true)',guide);self.assertIn('getBlockCollisions(cat',guide)
    def test_first_escape_retries_completion_and_requires_physical_transfers(self):
        text=source('feast/FirstEscapeStory.java')
        tick=text.split('public static boolean tick(',1)[1].split('private static boolean recording(',1)[0]
        self.assertLess(tick.index('finishFirstEscape(p,s,false)'),tick.index('ParentRescueStory.tick(p,s)'))
        self.assertIn('frame>=30&&enterPrison(p,s)',tick)
        push=text.split('private static void pushIntoTunnel(',1)[1].split('private static void tunnel(',1)[0]
        self.assertLess(push.index('if(spawnInEscapeTunnel(p,s,1))'),push.index('go(p,s,ESCAPE_TUNNEL)'))
        finish=text.split('public static boolean finishFirstEscape(',1)[1].split('private static void brickDoor(',1)[0]
        self.assertLess(finish.index('!verifiedTransfer(p,s,p.server.overworld(),d.pos)'),finish.index('n.putBoolean("Complete",true)'))

    def test_reloaded_audio_retries_same_checkpoint_instead_of_silent_wait(self):
        playback=source('feast/ShowPlayback.java')
        start=playback.split('public static boolean start(',1)[1].split('public static void signal(',1)[0]
        tick=playback.split('public static boolean tick(',1)[1].split('public static boolean ownsSession(',1)[0]
        self.assertIn('n.hasUUID("Nonce")&&REQUESTED_AT.containsKey(n.getUUID("Nonce"))',start)
        self.assertIn('if(!n.hasUUID("Nonce")||!REQUESTED_AT.containsKey(n.getUUID("Nonce")))',tick)
        self.assertIn('if(!start(p,s,sound,music)&&n.getString("Blocker").isEmpty())',tick)
        self.assertIn('String sound=n.getString("Sound");boolean music=n.getBoolean("Music");',tick)
        self.assertLess(tick.index('if(!n.hasUUID("Nonce")||!REQUESTED_AT.containsKey('),
                        tick.index('if(!n.getBoolean("Started"))'))
        fixture=(ROOT/'src/wybietest/java/com/coraline/feast/RescuePlaybackGameTests.java').read_text()
        self.assertIn('savedPlaybackCannotStallSilentlyAfterRestart',fixture)
        self.assertIn('for(boolean acknowledged:new boolean[]{false,true})',fixture)

    def test_wybie_shoe_miss_requires_real_hit_and_reuses_original(self):
        story=source('feast/FirstEscapeStory.java')
        shoe=story.split('static void shoe(ServerPlayer p,VisitData.Scene s)',1)[1].split('private static Vec3 escapeTarget(',1)[0]
        self.assertIn('if(e.cue()==1){',shoe)
        self.assertIn('if(e.time()>=80&&p.distanceToSqr(e)>16)',shoe)
        self.assertIn('n.putBoolean("ShoeMissed",true)',shoe)
        self.assertIn('n.putLong("ShoeFlightPosition",e.blockPosition().asLong())',shoe)
        self.assertIn('e.setDeltaMovement(Vec3.ZERO);e.setPos(missedShoePosition(p,e));e.frame(1,0,0);',shoe)
        self.assertIn('w.gameGesture(49,40);finishShoeThrow(p,s,e);return;',shoe)
        self.assertEqual(shoe.count('finishShoeThrow(p,s,e)'),1)
        self.assertLess(shoe.index('if(hit.isPresent()'),shoe.index('finishShoeThrow(p,s,e)'))
        self.assertNotIn('if(e.cue()==1){finishShoeThrow',shoe)
        interaction=story.split('public static boolean interact(ServerPlayer p,CinematicPropEntity prop)',1)[1].split('public static boolean block(',1)[0]
        self.assertIn('relaunchShoe(p,s,prop);',interaction)
        self.assertIn('prop.setDeltaMovement(p.getLookAngle().scale(1.05))',story)
        self.assertNotIn('if(giveTool(p,s,true)){prop.discard()',interaction)
        rendering=source('client/renderer/FirstEscapePresentation.java')
        self.assertIn('case CinematicPropEntity.STORY_SHOE->{if(e.cue()!=1)',rendering)

    def test_missing_story_shoe_requires_native_storage_absence_before_same_uuid_recovery(self):
        story=source('feast/FirstEscapeStory.java')
        lost=story.split('private static CinematicPropEntity recoverMissingShoe(',1)[1].split('private static boolean relaunchShoe(',1)[0]
        self.assertIn('n.getBoolean("ShoeMissed")',lost)
        self.assertIn('n.hasUUID("ShoeFlight")',lost)
        self.assertIn('n.contains("ShoeFlightPosition",Tag.TAG_LONG)',lost)
        self.assertIn('LoopProps.hold(p.server.overworld(),oldAt)',lost)
        self.assertIn('for(var level:p.server.getAllLevels())if(level.getEntity(id)!=null)',lost)
        self.assertIn('LegacyActorRecovery.check(p.server,id)',lost)
        self.assertIn('LegacyActorRecovery.Result.ABSENT',lost)
        self.assertLess(lost.index('LegacyActorRecovery.Result.ABSENT'),lost.index('recreated.setUUID(id)'))
        self.assertIn('recreated.configure(s,CinematicPropEntity.STORY_SHOE,0)',lost)
        self.assertIn('recreated.setUUID(id)',lost)
        self.assertIn('if(!p.server.overworld().addFreshEntity(recreated))return null',lost)
        self.assertNotIn('data(s).putUUID("ShoeFlight"',lost)
        shoe=story.split('static void shoe(ServerPlayer p,VisitData.Scene s)',1)[1].split('private static void finishShoeThrow(',1)[0]
        self.assertIn('int missing=Math.min(240,n.getInt("ShoeMissingTicks")+1)',shoe)
        self.assertIn('if(missing>=60)',shoe)
        self.assertIn('if(p.tickCount%20==0)recoverMissingShoe(p,s)',shoe)
        self.assertIn('n.remove("ShoeMissingTicks")',shoe)
        self.assertIn('data(s).remove("ShoeMissingTicks");data(s).remove("ShoeMissed")',story)
        restart=source('client/FirstEscapeClient.java')
        self.assertIn('SecretsNetwork.request(data.getUUID("Choice"),"escape_shoe","retry")',restart)

    def test_post_escape_tea_visitor_recovery_preserves_original_uuid_and_refuses_unverified_absence(self):
        tea=source('feast/MissingParentsTea.java')
        proof=tea.split('static boolean teaActorMissingProof(',1)[1].split('private static boolean recoverAbsentVisitor(',1)[0]
        self.assertIn('LegacyActorRecovery.Result.ABSENT',proof)
        recover=tea.split('private static boolean recoverAbsentVisitor(',1)[1].split('private static void spawn(',1)[0]
        for token in (
                'n.getBoolean("Completed")','n.hasUUID("Token")','n.hasUUID(key)',
                'for(var world:p.server.getAllLevels())if(world.getEntity(original)!=null)',
                'LegacyActorRecovery.check(p.server,original)',
                'if(!teaActorMissingProof(result))',
                'actor.setUUID(original)', 'actor.getPersistentData().putUUID("TeaOwner",s.owner)',
                'actor.getPersistentData().putUUID("TeaToken",n.getUUID("Token"))',
                'level.addFreshEntity(actor)'):
            self.assertIn(token,recover)
        self.assertLess(recover.index('if(!teaActorMissingProof(result))'),
                        recover.index('actor.setUUID(original)'))
        spawned=tea.split('private static void spawn(ServerPlayer p,VisitData.Scene s)',1)[1].split('static boolean walk(',1)[0]
        self.assertIn('if(actor(p,n,key)==null&&p.tickCount%20==0)recoverAbsentVisitor(p,s,n,key)',spawned)
        self.assertIn('if(created)save(p,s,true);',spawned)
        self.assertNotIn('        save(p,s,true);\n    }',spawned)
        native=(ROOT/'src/gametest/java/com/coraline/feast/RescueSoftlockGameTests.java').read_text()
        self.assertIn('missingTeaVisitorsRequireNativeProofOfAbsenceBeforeOriginalUuidRecovery',native)

    def test_parent_rescue_kitchen_accepts_original_smoker_and_null_safe_registry_id(self):
        story=source('feast/ParentRescueStory.java')
        helper=story.split('static boolean kitchenStoveId(String id)',1)[1].split('private static boolean kitchen(',1)[0]
        for token in ('id!=null','id.contains("smoker")','id.contains("furnace")',
                      'id.contains("oven")','id.contains("stove")'):
            self.assertIn(token,helper)
        lookup=story.split('private static boolean kitchen(ServerPlayer p,VisitData.Scene s)',1)[1].split('public static boolean block(',1)[0]
        self.assertIn('if(id!=null&&kitchenStoveId(id.getPath()))',lookup)
        game=(ROOT/'src/gametest/java/com/coraline/feast/RescueSoftlockGameTests.java').read_text()
        self.assertIn('canonicalKitchenSmokerCanStartParentRescueDinner',game)
        self.assertIn('blast_furnace',game)

    def test_parent_rescue_kitchen_cannot_stall_forever_on_actor_navigation(self):
        story=source('feast/ParentRescueStory.java')
        serve=story.split('case SERVE->{',1)[1].split('case L->{',1)[0]
        self.assertIn('MissingParentsCat.walk(m,Vec3.atBottomCenterOf(target.west()),.65)',serve)
        self.assertIn('!arrived&&plate!=null&&serveRecoveryDue(t)&&t%20==0',serve)
        self.assertIn('arrived=recoverServingActor(p,s,m,target)',serve)
        self.assertIn('plate.setPos(Vec3.atBottomCenterOf(target).add(0,.9,0))',serve)
        self.assertLess(serve.index('arrived=recoverServingActor('),serve.index('go(p,s,Phase.L)'))
        method=story.split('private static boolean recoverServingActor(',1)[1].split('public static BlockPos cookingStand(',1)[0]
        self.assertIn('s.owner.equals(p.getUUID())',method)
        self.assertIn('mother.level()!=p.level()||!mother.isOtherParent()',method)
        self.assertIn('!level.noCollision(mother,box)',method)
        self.assertIn('level.getWorldBorder().isWithinBounds(box)',method)
        self.assertIn('MissingParentsCat.arrived(mother,Vec3.atBottomCenterOf(wanted))',method)
        self.assertIn('Clear a reachable serving space beside the original kitchen table.',method)
        game=(ROOT/'src/gametest/java/com/coraline/feast/RescueSoftlockGameTests.java').read_text()
        self.assertIn('parentRescueKitchenServingWaitsBeforeRecoveringOriginalActor',game)

    def test_finale_open_floor_has_delayed_safe_return_to_actual_hole(self):
        finale=source('feast/BeldamFinale.java')
        floor=finale.split('private static boolean guideOpenedFloorFall(',1)[1].split('public static boolean preflight(',1)[0]
        self.assertIn('n.getBoolean("FloorOpened")',floor)
        self.assertIn('openedFloorFallRecoveryDue(t,duration)',floor)
        self.assertIn('level.getBlockState(room.below()).isAir()',floor)
        self.assertIn('level.getBlockCollisions(p,body).iterator().hasNext()',floor)
        self.assertIn('level.getWorldBorder().isWithinBounds(body)',floor)
        self.assertIn('now=level.getGameTime()',floor)
        self.assertIn('n.putLong("FallGuideRetryAt",now+80)',floor)
        self.assertIn('p.teleportTo(level,landing.x,landing.y,landing.z',floor)
        final_o=finale.split('case FINAL_O->{',1)[1].split('case FINAL_P->',1)[0]
        self.assertLess(final_o.index('!prepareFall(p,s)'),final_o.index('guideOpenedFloorFall('))
        self.assertLess(final_o.index('guideOpenedFloorFall('),final_o.index('&&enterWeb(p,s,m)&&done'))
        native=(ROOT/'src/gametest/java/com/coraline/feast/RescueSoftlockGameTests.java').read_text()
        self.assertIn('finaleOpenedFloorRecoversOnlyAfterRecordedFallCueAndGrace',native)

    def test_stranded_post_escape_checkpoint_recovers_original_world_not_parent_rescue(self):
        story=source('feast/FirstEscapeStory.java')
        tick=story.split('public static boolean tick(ServerPlayer p,VisitData.Scene s)',1)[1].split('private static boolean recording(',1)[0]
        self.assertIn('if(FirstEscapeTimeline.overworld(state)&&state!=ESCAPE_COMPLETE)',tick)
        self.assertIn('var real=source==null?null:FamilyData.level(p.server,source.dimension);',tick)
        self.assertIn('n.putBoolean("Recover",true)',tick)
        recover=story.split('private static boolean recoverCheckpoint(',1)[1].split('private static void prepareSealedRoom(',1)[0]
        self.assertIn('if(FirstEscapeTimeline.overworld(state)&&!ParentRescueStory.started(s))',recover)
        self.assertIn('real==null||p.serverLevel()!=real&&!verifiedTransfer(p,s,real,door.pos)',recover)
        self.assertLess(recover.index('if(FirstEscapeTimeline.overworld(state)&&!ParentRescueStory.started(s))'),recover.index('n.remove("Recover")'))

    def test_original_shoe_retry_works_without_clicking_ground_prop(self):
        story=source('feast/FirstEscapeStory.java')
        retry=story.split('public static boolean retryShoe(ServerPlayer p,UUID nonce)',1)[1].split('private static boolean relaunchShoe(',1)[0]
        self.assertIn('n.getUUID("Choice").equals(nonce)',retry)
        self.assertIn('!n.getBoolean("ShoeMissed")',retry)
        self.assertIn('return relaunchShoe(p,s,shoe);',retry)
        self.assertIn('LoopProps.hold(p.serverLevel()',retry)
        flight=story.split('private static boolean relaunchShoe(',1)[1].split('private static Vec3 missedShoePosition(',1)[0]
        self.assertIn('prop.kind()!=CinematicPropEntity.STORY_SHOE||prop.cue()!=1',flight)
        self.assertIn('n.getUUID("ShoeFlight").equals(prop.getUUID())',flight)
        self.assertIn('prop.setDeltaMovement(p.getLookAngle().scale(1.05))',flight)
        self.assertIn('n.remove("ShoeMissed")',flight)
        position=story.split('private static Vec3 missedShoePosition(',1)[1].split('public static void throwShoe(',1)[0]
        self.assertIn('new double[]{1.45,1.15,1.85}',position)
        self.assertIn('getBlockCollisions(shoe,box)',position)
        self.assertIn('ClipContext.Block.COLLIDER',position)
        self.assertIn('return p.position().add(0,.2,0);',position)
        client=source('client/FirstEscapeClient.java')
        self.assertIn('flag("ShoeMissed")',client)
        self.assertIn('use&&!shoeUseDown',client)
        self.assertIn('SecretsNetwork.request(data.getUUID("Choice"),"escape_shoe","retry")',client)
        network=source('secrets/SecretsNetwork.java')
        self.assertIn('FirstEscapeStory.retryShoe(p,r.nonce)',network)
        self.assertIn('packet.putBoolean("ShoeMissed"',story)
        native=(ROOT/'src/wybietest/java/com/coraline/feast/FirstEscapeStoryGameTests.java').read_text()
        self.assertIn('shoeCanBeRetriedWithoutClickingTinyGroundHitbox',native)

    def test_first_escape_wybie_tea_cat_and_return_tunnel_handoffs(self):
        # Source dispatch checks supplement, but do not replace, an in-game run.
        escape=source('feast/FirstEscapeStory.java')
        self.assertIn('case THROW_SHOE->{ensureTool(p,s,true);shoe(p,s);',escape)
        self.assertIn('if(wall.getType()!=HitResult.Type.MISS||e.time()>45||e.getY()<p.getY()-12||e.distanceToSqr(p)>24*24',escape)
        self.assertIn('case MISSING_G,BICYCLE_DEPARTURE->departure(p,s);',escape)
        self.assertIn('MissingParentsTea.select(p,s,false)',escape)
        tea=source('feast/MissingParentsTea.java')
        self.assertIn('n.putBoolean("Completed",true);n.putBoolean("Active",false)',tea)
        self.assertIn('s.loop.story.putBoolean("SeeingStoneReceipt",true)',tea)
        cat=source('feast/MissingParentsCat.java')
        self.assertIn('tea.getBoolean("Completed")&&!tea.getBoolean("Active")',cat)
        self.assertIn('!s.loop.story.getBoolean("SeeingStonePending")',cat)
        self.assertIn('StoryTunnel.restore(p,s)',cat)
        tunnel=source('feast/StoryTunnel.java')
        self.assertIn('else ParentRescueStory.begin(p,scene);',tunnel)
        self.assertIn('return StoryReturnContext.inCorridor',tunnel)
        supper=source('feast/ParentRescueStory.java')
        self.assertIn('n.putBoolean("KeySwallowed",true)',supper)
        self.assertIn('StoryItemCustody.capture(p,s,"Key"',supper)

    def test_late_rescue_cutscenes_keep_safety_but_bosses_and_crawl_do_not_autocomplete(self):
        chapter=source('feast/RescueChapter.java')
        safety=chapter.split('static void cutsceneSafety(',1)[1].split('public static void recover(',1)[0]
        # A former ordinal check silently disabled every Bobinsky, finale and
        # postboss cinematic watchdog as soon as the Bobinsky chapter began.
        self.assertNotIn('state.ordinal()>=Phase.BOB_A.ordinal()',safety)
        self.assertIn('RescueBosses.fighting(state)||state==Phase.TUNNEL_CHASE',safety)
        for name in ('Phase.FURNACE','Phase.BOB_TENT','Phase.SWARM_TRANSFORM',
                     'Phase.WARDROBE_ASIDE','Phase.HAND_FALL','Phase.BANGING',
                     'Phase.EYES_CLOSE','Phase.CELESTIAL_OPEN','Phase.ORBS'):
            self.assertIn(name,safety)
        self.assertIn('if(voice.isEmpty()&&!motion)return;',safety)
        self.assertIn('RescueEncounters.tick(p,s,state,tail,true)',safety)
        self.assertIn('BeldamFinale.tick(p,s,state,tail,true)',safety)
        self.assertIn('RescueEpilogue.tick(p,s,state,tail,true)',safety)
        self.assertIn('!BeldamFinale.preflight(p,s)',safety)
        self.assertIn('case REUNION_A->"reunion_a"',chapter)
        self.assertIn('case FAREWELL_C->"farewell_c"',chapter)

    def test_old_proposal_cannot_consume_rescue_interactions(self):
        proposal=source('feast/BlackIsTraditional.java')
        interaction=proposal.split('public static boolean interact(ServerPlayer p,VisitData.Scene s,CinematicPropEntity prop)',1)[1].split('public static boolean choice(',1)[0]
        self.assertIn('!active(s)||FirstEscapeStory.engaged(s)||!prop.owned(s)',interaction)
        prop=source('feast/CinematicPropEntity.java')
        general=prop.split('InteractionResult interact(Player actor,InteractionHand hand)',1)[1].split('AABB getBoundingBoxForCulling()',1)[0]
        self.assertLess(general.index('RescueChapter.interact(p,this)'),general.index('BlackIsTraditional.interact(p,s,this)'))

    def test_late_rescue_props_are_not_stolen_by_first_escape_interactions(self):
        first=source('feast/FirstEscapeStory.java')
        handler=first.split('public static boolean interact(ServerPlayer p,CinematicPropEntity prop)',1)[1].split('public static boolean block(',1)[0]
        self.assertIn('if(MissingParentsCat.interact(p,prop))return true;',handler)
        self.assertIn('!ParentRescueStory.started(s)&&phase!=ESCAPE_COMPLETE',handler)
        self.assertIn('prop.kind()<=CinematicPropEntity.STORY_SHOE;',handler)
        self.assertNotIn('return prop.owner.equals(p.getUUID())&&prop.kind()>=CinematicPropEntity.STORY_CANE;',handler)
        prop=source('feast/CinematicPropEntity.java')
        specific=prop.split('InteractionResult interactAt(',1)[1].split('boolean canBeCollidedWith()',1)[0]
        self.assertLess(specific.index('FirstEscapeStory.interact(p,this)'),specific.index('return interact(player,hand);'))
        generic=prop.split('InteractionResult interact(Player actor,InteractionHand hand)',1)[1].split('AABB getBoundingBoxForCulling()',1)[0]
        self.assertIn('if(RescueChapter.interact(p,this))return InteractionResult.CONSUME;',generic)

    def test_ratmaster_clickable_ball_reaches_exact_saved_rat_and_eye_gate(self):
        prop=source('feast/CinematicPropEntity.java')
        # The pick box covers the tiny canonical passenger. Direct rat clicks
        # cannot be the sole way to obtain the required final ghost eye.
        self.assertIn('RescueProps.RAT_BALL)return new AABB(getX()-.30,getY(),getZ()-.30,getX()+.30,getY()+1.35,getZ()+.30)',prop)
        self.assertIn('kind()==RescueProps.RAT_BALL?.45',prop)
        self.assertIn('return RescueProps.kind(kind())',prop)
        self.assertIn('.sized(.3F,.25F)',source('registry/ModEntities.java'))
        encounter=source('feast/RescueEncounters.java')
        interaction=encounter.split('public static boolean interact(ServerPlayer p,VisitData.Scene s,Entity target)',1)[1].split('public static void retry(',1)[0]
        self.assertIn('ball.kind()==RescueProps.RAT_BALL&&ball.owned(s)',interaction)
        self.assertIn('n.hasUUID("ChaseRat")&&n.getUUID("ChaseRat").equals(rider.getUUID())',interaction)
        self.assertIn('s.owner.equals(rider.getPersistentData().getUUID("RescueOwner"))',interaction)
        self.assertLess(interaction.index('return interact(p,s,passenger);'),
                        interaction.index('if(phase==RAT_CHASE&&n.hasUUID("ChaseRat")'))
        self.assertIn('!canClaimRatEye(n.getInt("RatLeg"))||!n.getBoolean("RatHouseMoved")',interaction)
        self.assertIn('RescueEyes.claim(p,s,2)',interaction)

    def test_hidden_rescue_props_do_not_steal_scene_clicks(self):
        source_text=source('feast/CinematicPropEntity.java')
        pick=source_text.split('@Override public boolean isPickable()',1)[1].split('@Override public boolean hurt(',1)[0]
        self.assertIn('if(isInvisible()&&RescueProps.kind(kind()))return false;',pick)
        self.assertLess(pick.index('if(isInvisible()&&RescueProps.kind(kind()))return false;'),
                        pick.index('return RescueProps.kind(kind())'))
        encounter=source('feast/RescueEncounters.java')
        self.assertIn('RescueProps.hide(p,s,"GardenEye")',encounter)
        self.assertIn('RescueProps.hide(p,s,"Swarm")',source('feast/RescueBosses.java'))

    def test_final_rescue_crawl_and_key_lock_require_verified_physical_exit(self):
        finale=source('feast/BeldamFinale.java')
        crawl=finale.split('private static void tunnel(',1)[1].split('public static void input(',1)[0]
        self.assertLess(crawl.index('if(progress<RescuePassage.LENGTH||!done)return;'),
                        crawl.index('if(!RescueChapter.transfer(p,level,source.pos))return;'))
        self.assertLess(crawl.index('if(!RescueChapter.transfer(p,level,source.pos))return;'),
                        crawl.index('RescueChapter.go(p,s,LOCK_DOOR)'))
        lock=finale.split('public static boolean block(ServerPlayer p,BlockPos at)',1)[1].split('public static void retry(',1)[0]
        self.assertIn('!n.getBoolean("CrawlReachedOverworld")||!n.getBoolean("KeyRecovered")',lock)
        self.assertLess(lock.index('if(!p.serverLevel().getBlockState(at).equals(closed))return true;'),
                        lock.index('n.putBoolean("PermanentlySealed",true)'))
        epilogue=source('feast/RescueEpilogue.java')
        self.assertIn('if(!reunionReady(s))return;',epilogue)
        self.assertIn('if(!restoreVision(p,s))return;',epilogue)
        self.assertIn('if(RescueEyes.release(p,s))completeFarewell(p,s)',epilogue)

    def test_accepted_child_crosses_before_escape_is_archived(self):
        text=source('feast/GhostVictimDirector.java')
        escaped=text.rsplit('case VICTIM_ESCAPED->{',1)[1].split('case BETWEEN_ACTIVITIES',1)[0]
        self.assertLess(escaped.index('GhostEscapeHunt.openPassage'),escaped.index('n.putBoolean("VictimReachedDoor",true)'))
        self.assertLess(escaped.index('same.level()!=source'),escaped.index('archiveVictim(n,"ESCAPED")'))

    def test_later_parent_rescue_has_original_door_reentry(self):
        door=source('feast/StoryTunnel.java')
        self.assertIn('else resumeChapter(p,scene);',door)
        resume=door.split('private static void resumeChapter(',1)[1].split('public static boolean controls(',1)[0]
        self.assertIn('SafeReturnPhase',resume)
        self.assertIn('TheatreSite.entrance',resume)
        self.assertIn('MouseCircusSite.audience',resume)
        self.assertIn('RescuePassage.enter',resume)
        self.assertNotIn('ParentRescueStory.begin',resume)

    def test_registration_uses_map_proof_and_supper_recovery_preserves_saved_actors(self):
        transport=source('feast/PalaceTransport.java')
        self.assertIn('PalaceJourney.ownsTransport(p,s)',transport)
        self.assertNotIn('PalaceJourney.owns(p,s)',transport)
        self.assertIn('!sites.palaceArrivalReady()',transport)
        self.assertIn('PalaceJourney.owns(p,s)',source('feast/PalaceWellEncounter.java'))
        for path,method,admit in (
            ('feast/AcceptedButtonEyesWorld.java','recoverSupperParent','level.addFreshEntity(actor)'),
            ('feast/GhostVictimActors.java','recoverSupper','var actor=arrive('),
        ):
            text=source(path).split(method+'(',1)[1]
            self.assertLess(text.index('LegacyActorRecovery.Result.ABSENT'),text.index(admit))

    def test_rescue_reentry_preserves_actual_venue_exits_and_rat_leg(self):
        tunnel=source('feast/StoryTunnel.java')
        recover=tunnel.split('private static void resumeChapter(',1)[1].split('public static boolean controls(',1)[0]
        self.assertIn('ShowTravel.recoverRescueReturn(p,scene,"TheatreReturn"',recover)
        self.assertIn('ShowTravel.recoverRescueReturn(p,scene,"CircusReturn"',recover)
        self.assertIn('chapter.getInt("RatLeg")>=2',recover)
        self.assertIn('level=p.server.getLevel(OtherWorld.KEY);',recover)
        self.assertLess(recover.index('recoverRescueReturn(p,scene,"TheatreReturn"'),
                        recover.index('level=p.server.getLevel(ActivityWorld.KEY);landing=TheatreSite.entrance'))
        travel=source('feast/ShowTravel.java')
        lease=travel.split('static boolean recoverRescueReturn(',1)[1].split('private static CompoundTag ledger(',1)[0]
        self.assertIn('data/coraline_first_visits.dat',lease)
        self.assertIn('record.equals(disk.loop.event.getCompound(key))',lease)
        self.assertIn('destination.distanceToSqr(Vec3.atBottomCenterOf(returnNear))<=64',lease)

    def test_rescue_passage_requires_readback_and_cleanup_retains_failures(self):
        passage=source('feast/RescuePassage.java')
        self.assertIn('PassageVerified',passage)
        self.assertIn('if(!level.getBlockState(at).equals(expected))',passage)
        self.assertIn('if(!expected.equals(NbtUtils.readBlockState(lookup,row.getCompound("Placed"))))',passage)
        self.assertIn('p.server.overworld().getDataStorage().save();',passage)
        cleanup=source('feast/RescueSiteCleanup.java')
        self.assertLess(cleanup.index('if(!level.getBlockState(at).equals(original))continue;'),
                        cleanup.index('cells.remove(position);budget--;changed=true;'))
        self.assertIn('CleanupOffset',cleanup)

    def test_supper_parent_recovery_loads_original_dimension_first(self):
        world=source('feast/AcceptedButtonEyesWorld.java')
        recovery=world.split('static ParentEntity recoverSupperParent(',1)[1].split('private static ParentEntity parent(',1)[0]
        self.assertIn('SupperMotherDimension',recovery)
        self.assertIn('SupperFatherDimension',recovery)
        self.assertIn('FamilyData.level(p.server,dimension)',recovery)
        self.assertLess(recovery.index('LoopProps.hold(recordedWorld,origin)'),
                        recovery.index('LegacyActorRecovery.check(p.server,id)'))
        director=source('feast/GhostVictimDirector.java')
        self.assertIn('n.putString("SupperMotherDimension"',director)
        self.assertIn('n.putString("SupperFatherDimension"',director)

    def test_postboss_company_grade_works_without_palace_well(self):
        generation=source('palace/PalaceWorldGeneration.java')
        grade=generation.split('private static boolean column(',1)[1].split('private static void terrain(',1)[0]
        self.assertIn('PalaceGenerationRules.stage(n)==COMPANY_PREPARE',grade)
        self.assertIn('PalaceGenerationRules.companyFoundationInner(',grade)
        self.assertLess(grade.index('PalaceGenerationRules.stage(n)==COMPANY_PREPARE'),
                        grade.index('d.position(Kind.PALACE).orElseThrow()'))
        sites=source('palace/PalaceSiteData.java')
        recover=sites.split('void retryLegacyPause()',1)[1].split('boolean prioritizeCompany()',1)[0]
        self.assertIn('earlyCompanyMissingPalace',recover)
        self.assertIn('COMPANY_PREPARE cursor',recover)
        self.assertIn('generation.contains("DeferredPalace"',recover)

    def test_well_reconnect_reuses_original_bicycle(self):
        bike=source('feast/WybieBicycleEntity.java')
        rebind=bike.split('boolean reconcileWellSession(',1)[1].split('public boolean ownedBy(',1)[0]
        for identity in ('expectedOwner.equals(owner)','expectedDoor.equals(door)',
                         'originalWybie.equals(rider)','PalaceWellOwner','entityData.set(SESSION,'):
            self.assertIn(identity,rebind)
        well=source('feast/PalaceWellEncounter.java')
        rider=well.split('private static WybieBicycleEntity bike(',1)[1].split('private static Vec3 entryTarget(',1)[0]
        self.assertIn('b.reconcileWellSession(s.owner,s.door,progress.originalWybie().orElseThrow(),progress.session())',rider)
        self.assertLess(rider.index('reconcileWellSession('),rider.index('LegacyActorRecovery.check(p.server,original)'))

    def test_lost_map_is_recoverable_under_original_map_prerequisites(self):
        journey=source('feast/PalaceJourney.java')
        recovery=journey.split('public static void recoverMap(',1)[1]
        self.assertIn('mapEligible(p,scene)',recovery)
        self.assertNotIn('!owns(p,scene)',recovery)

    def test_well_resume_reconciles_authenticated_finished_combat(self):
        encounter=source('feast/PalaceWellEncounter.java')
        resume=encounter.split('public static int resume(',1)[1].split('@SubscribeEvent public static void tick(',1)[0]
        self.assertIn('PalaceHandCombat.load(s.owner,s.door,h.getUUID()',resume)
        self.assertIn('progress.session().equals(fight.session())',resume)
        self.assertIn('fight.complete()&&progress.phase()==BOSS',resume)
        self.assertIn('fight.complete()&&progress.phase().ordinal()>BOSS.ordinal()',resume)
        self.assertIn('!oldSession&&!interruptedVictory&&!completedEarlier',resume)
        self.assertIn('progress.defeated(s.owner,progress.session(),h.getUUID())',resume)
        self.assertIn('fight.resume(s.owner,progress.session())',resume)
        self.assertLess(resume.index('PalaceHandCombat.load('),
                        resume.index('GlobalHandEncounter.claim('))
        self.assertLess(resume.index('GlobalHandEncounter.claim('),
                        resume.index('progress.resume(s.owner)'))

    def test_accepted_escape_dialogue_and_room_exit_recovery(self):
        mirrors=source('feast/GhostVictimMirrors.java')
        dispatch=mirrors.split('public static boolean choice(',1)[1].split('private static boolean escapeChoice(',1)[0]
        self.assertIn('String extra=indexedEscapeAction(n,selected);',dispatch)
        self.assertIn('return extra!=null&&escapeChoice(p,s,n,child,nonce,extra);',dispatch)
        self.assertIn('wireAction.startsWith("escape:")',dispatch)
        self.assertIn('return wireAction.substring(7);',dispatch)
        rooms=source('feast/GhostEscapeRooms.java')
        self.assertIn('!authoredExit(s,id,door)',rooms)
        self.assertIn('if(at.equals(excluded))continue;',rooms)
        covert=source('feast/GhostEscapeCovert.java')
        leaving=covert.split('case LEAVE_ROOM->{',1)[1].split('case APPROACH_EXIT->',1)[0]
        self.assertIn('travel==GhostVictimNavigation.Result.BLOCKED',leaving)
        self.assertIn('GhostEscapeRooms.reachableExit(s,roomId,level,child,exit)',leaving)
        self.assertLess(leaving.index('if(travel==GhostVictimNavigation.Result.BLOCKED)'),
                        leaving.index('GhostEscapeProgress.leftRoom(ghost,true)'))
        director=source('feast/GhostVictimDirector.java')
        requests=director.split('public static void request(',1)[1].split('public static boolean parent(',1)[0]
        self.assertIn('||value.equals("escape_door")||value.equals("escape_go")',requests)
        self.assertIn('if(!GhostEscapePerception.destinationSignalPoint(p,s,n,child,at))return;',requests)

    def test_accepted_escape_retries_physically_return_to_the_bedroom_mirror(self):
        # These are source-dispatch regressions, NOT proof of a Minecraft playthrough.
        progress=source('feast/GhostEscapeProgress.java')
        self.assertIn('phase==Phase.WAIT_GO||phase==Phase.WAIT_LURE||phase==Phase.STEAL_KEY?Phase.READY',progress)
        self.assertIn('move(n,Phase.READY,GhostVictimProgress.clock(ghost));return true;',progress)
        covert=source('feast/GhostEscapeCovert.java')
        planning=covert.split('switch(GhostEscapeProgress.phase(e)){',1)[1].split('case APPROACH_ROOM->{',1)[0]
        self.assertIn('case READY,STONE_REPORT,ROOM_REPORT,KEY_REPORT->{',planning)
        self.assertIn('GhostVictimRoutines.travel(p,s,ghost,child,destination,false)',planning)
        self.assertIn('GhostVictimMirrors.childAtMirror(ghost,"bedroom",child,true)',planning)
        self.assertIn('case READY->',planning)
        self.assertLess(planning.index('GhostVictimRoutines.travel(p,s,ghost,child,destination,false)'),
                        planning.index('GhostVictimMirrors.childAtMirror(ghost,"bedroom",child,true)'))
        mirrors=source('feast/GhostVictimMirrors.java')
        self.assertIn('!childPresent(p,n,n.getString("Mirror"),child)',mirrors)

    def test_accepted_activity_parent_landing_can_be_obstructed_without_crashing(self):
        world=source('feast/ActivityWorld.java')
        entry=world.split('public static void enter(ServerPlayer p,VisitData.Scene s)',1)[1].split('public static String enterDebug(',1)[0]
        self.assertIn('if(at!=null){mom.getNavigation().stop();mom.teleportTo(',entry)
        returned=world.split('private static void finishActivityReturn(',1)[1].split('static boolean recoverCompletedReturn(',1)[0]
        self.assertIn('if(safe!=null)mom.teleportTo(',returned)

    def test_pending_stone_errand_cannot_be_skipped_by_night_watchdog(self):
        director=source('feast/GhostVictimDirector.java')
        self.assertGreaterEqual(director.count('if(GhostEscapeProgress.homeErrandPending(n))'),2)
        progress=source('feast/GhostEscapeProgress.java')
        self.assertIn('static boolean homeErrandPending(CompoundTag ghost)',progress)
        self.assertIn('owns(ghost,escape)&&phase(escape)==Phase.HOME_ERRAND',progress)
        covert=source('feast/GhostEscapeCovert.java')
        self.assertIn('visitorLanding(home,front,1,null)',covert)
        self.assertIn('visitorLanding(home,front,-1,first)',covert)
        self.assertIn('first==null||second==null',covert)

    def test_accepted_escape_options_match_actual_server_gates(self):
        mirror=source('feast/GhostVictimMirrors.java')
        display=mirror.split('private static void screen(',1)[1].split('private static void addOption(',1)[0]
        self.assertIn('escapePlanOfferReady(n)',display)
        self.assertIn('ambushOfferReady(escape)',display)
        self.assertIn('ghost.contains("BedtimeWindowStarted",Tag.TAG_LONG)',display)
        self.assertIn('GhostVictimProgress.readyToHelp(ghost)',display)
        progress=source('feast/GhostEscapeProgress.java')
        self.assertIn('static boolean keyAmbushReady(CompoundTag n)',progress)
        self.assertIn('(phase(n)==Phase.ROOM_REPORT||phase(n)==Phase.READY)',progress)
        self.assertIn('!keyAmbushReady(n)',progress)
        self.assertIn('return GhostEscapeProgress.keyAmbushReady(escape)',display)
        native=ROOT/'src/wybietest/java/com/coraline/feast/GhostVictimProgressGameTests.java'
        self.assertIn('acceptedEscapeChoiceVisibilityMatchesProgressPrerequisites',native.read_text(encoding='utf-8'))

    def test_original_room_door_fallback_does_not_issue_fake_lockpick_receipt(self):
        rooms=source('feast/GhostEscapeRooms.java')
        self.assertIn('static BlockPos liveDoor(VisitData.Scene s,String id,ServerLevel level)',rooms)
        self.assertIn('var door=liveDoor(s,room.id(),level)',rooms)
        self.assertIn('var at=liveDoor(s,id,level)',rooms)
        self.assertIn('var at=liveDoor(s,id,level);if(at==null||child.distanceToSqr',rooms)
        self.assertIn('boolean selectedOpened=false;',rooms)
        self.assertIn('if(!selectedOpened)return false;',rooms)
        self.assertLess(rooms.index('if(!selectedOpened)return false;'),rooms.index('permits.putBoolean(id,true)'))
        covert=source('feast/GhostEscapeCovert.java')
        self.assertIn('GhostEscapeRooms.liveDoor(s,id,level)',covert)
        native=(ROOT/'src/wybietest/java/com/coraline/feast/GhostEscapeRecoveryGameTests.java').read_text(encoding='utf-8')
        self.assertIn('basementCanUseItsSecondOriginalDoorWhenFirstMissing',native)

    def test_chase_spots_are_not_permanently_exhausted(self):
        hiding=source('feast/GhostEscapeHiding.java')
        mother=source('feast/GhostEscapeMother.java')
        self.assertIn('row.putLong("CompromisedAt",marked)',hiding)
        self.assertIn('stillCompromised(now,marked,nearby)',hiding)
        self.assertIn('now-markedAt<1200||motherNearby',hiding)
        self.assertIn('node.putLong("CompromisedAt",clock)',mother)
        native=(ROOT/'src/wybietest/java/com/coraline/feast/GhostEscapeRecoveryGameTests.java').read_text(encoding='utf-8')
        self.assertIn('compromisedHidingSpotsEventuallyBecomeReusable',native)

    def test_complete_music_master_timing_and_one_shot_playback(self):
        receipt=json.loads((ASSETS/'missing_parents_music_audio.json').read_text())
        self.assertEqual(receipt['source_sha256'],hashlib.sha256((ROOT/receipt['source']).read_bytes()).hexdigest())
        ogg=ASSETS/'sounds/scenes/missing_parents_music.ogg'
        self.assertEqual(receipt['ogg_sha256'],hashlib.sha256(ogg.read_bytes()).hexdigest())
        self.assertEqual(receipt['export'],ogg_measure(ogg.read_bytes()));self.assertGreater(receipt['export']['ticks'],100)
        sounds=json.loads((ASSETS/'sounds.json').read_text());self.assertTrue(sounds['missing_parents_music']['sounds'][0]['stream'])
        for table in ('visit_audio_lengths.json','scene_audio_lengths.json'):
            self.assertEqual(json.loads((ASSETS/table).read_text())['missing_parents_music'],receipt['export']['ticks'])
        track=source('client/MissingParentsMusic.java');self.assertIn('looping=false',track);self.assertIn('ShowPlaybackClient.observed',track)
        self.assertIn('return 45*20',source('feast/MissingParentsCat.java'))

if __name__=='__main__':unittest.main()
