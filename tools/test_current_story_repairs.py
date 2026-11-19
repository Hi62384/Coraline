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
