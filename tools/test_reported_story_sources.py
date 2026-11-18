"""Low-memory source guards only; these do NOT replace compilation/native QA."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/com/coraline'

def source(path):
    return (JAVA / path).read_text(encoding='utf-8-sig')

class ReportedSourceGuards(unittest.TestCase):
    def test_changed_java_delimiters(self):
        paths = [
            'palace/PalaceWorldGeneration.java', 'otherworld/RealmCopy.java',
            'otherworld/OtherWorld.java', 'feast/BlackIsTraditional.java',
            'feast/FirstEscapeStory.java', 'feast/FirstEscapeScene.java',
            'feast/FirstEscapeVisualRules.java', 'feast/MissingParentsCat.java',
            'feast/MissingParentsTea.java', 'feast/CinematicPropEntity.java',
            'feast/WybieBicycleEntity.java', 'feast/WybieBicyclePhysics.java',
            'parents/ParentEntity.java', 'entity/CharacterEntity.java',
            'tea/TeaSeatEntity.java', 'client/FirstEscapeClient.java',
            'client/OtherWorldLighting.java', 'client/SpectralEyes.java',
            'client/SeeingStoneClient.java', 'client/renderer/CharacterModel.java',
            'client/renderer/CharacterRenderer.java', 'client/renderer/SpectralMobTint.java',
            'client/renderer/FirstEscapePresentation.java', 'client/renderer/SeeingStoneRenderer.java',
            'mixin/PrisonLightmapMixin.java',
            'feast/StoryFloorRoute.java', 'feast/StoryBikeRoute.java',
            'feast/ParentRescueStory.java', 'client/FirstEscapeMusic.java',
            'client/SeeingStoneRenderState.java',
        ]
        # Strip Java literals/comments before balancing; not a Java type checker.
        ignored = re.compile(r'""".*?"""|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*.*?\*/', re.S)
        for path in paths:
            with self.subTest(path=path):
                stack = []
                for char in ignored.sub('', source(path)):
                    if char in '({[':
                        stack.append(char)
                    elif char in ')}]':
                        self.assertTrue(stack, path)
                        self.assertEqual(stack.pop(), {')': '(', '}': '{', ']': '['}[char], path)
                self.assertEqual(stack, [], path)

    def test_background_generation_does_not_sync_generate_in_chunk_helper(self):
        text = source('palace/PalaceWorldGeneration.java')
        helper = text.split('private static LevelChunk chunk(', 1)[1].split('private static boolean put(', 1)[0]
        self.assertIn('RealmCopy.requestChunk', helper)
        self.assertIn('pendingChunk.isDone()', helper)
        self.assertNotIn('l.getChunk(', helper)
        self.assertNotIn('.join()', helper)
        self.assertIn('OtherWorld.weavingEntrance(server)', text)

    def test_snapshot_surface_is_cached_outside_section_loop(self):
        text = source('otherworld/RealmCopy.java').split('private void copy(', 1)[1]
        self.assertLess(text.index('terrainSurface(from,x,z)'), text.index('for(int i=0;i<to.getSections().length;i++)'))
        self.assertIn('terrainHeights[x*16+z]', text)

    def test_rescue_no_velocity_nudge_and_destination_is_inside(self):
        text = source('feast/FirstEscapeStory.java')
        guide = source('feast/FirstEscapeGuide.java')
        self.assertIn('GuideGrip', guide)
        self.assertIn('FirstEscapeCarry.route', guide)
        self.assertIn('stuck>=40', guide)
        self.assertIn('SPEED=.29', guide)
        self.assertIn('CarryStepGeometry.floor', guide)
        self.assertIn('held>600', guide)
        self.assertNotIn('p.hurtMarked', guide)
        self.assertNotIn('normalize().scale(.025)', guide)
        self.assertIn('FirstEscapeScene.inRoom(s,w.blockPosition())', guide)
        self.assertIn('if(phase(s)!=ESCAPE_C&&phase(s)!=FOLLOW_WYBIE)', text)

    def test_rescue_restores_brightness_and_mask_click_stands_up(self):
        client = source('client/FirstEscapeClient.java')
        dark = client.split('boolean inheritedDarkHouse()', 1)[1].split('\n', 1)[0]
        self.assertNotIn('Rescued', dark)
        knock = client.split('boolean knockedDown()', 1)[1].split('\n', 1)[0]
        self.assertNotIn('MASK_ANIMATION', knock)
        self.assertNotIn('STITCH_REMOVAL', knock)
        self.assertIn('if(next==MASK_ANIMATION){n.remove("KnockdownAt")', source('feast/FirstEscapeStory.java'))

    def test_custom_family_bed_and_hidden_parent_are_supported(self):
        text = source('feast/MissingParentsCat.java')
        self.assertIn('ParentsBedBlock.anchorPos', text)
        self.assertIn('is(ModBlocks.PARENTS_BED.get())', text)
        parents = source('parents/ParentEntity.java')
        self.assertIn('f.dead||f.storyMissing', parents)
        self.assertIn('isInvisible()&&!isOtherParent()', parents)

    def test_no_double_darkening_and_ghost_halo(self):
        self.assertIn('prisonPixel(int pixel){return pixel;}', source('feast/FirstEscapeVisualRules.java'))
        ghosts=source('client/renderer/FirstEscapePresentation.java').split('private static void ghost(',1)[1].split('private static void evil(',1)[0]
        self.assertIn('0x041B0B', ghosts)
        self.assertIn('b=buffers.getBuffer(RenderType.entityTranslucentEmissive(WHITE))', ghosts)
        self.assertIn('!flag("Rescued")', source('client/FirstEscapeClient.java'))

    def test_seat_unlock_is_server_authoritative(self):
        seat = source('tea/TeaSeatEntity.java')
        mount = seat.split('public static void mount(', 1)[1].split('@Override', 1)[0]
        self.assertIn('instanceof net.minecraft.server.level.ServerPlayer', mount)
        release = source('feast/MissingParentsTea.java').split('public static void release(', 1)[1].split('public static boolean seatLocked(', 1)[0]
        self.assertLess(release.index('putBoolean("Seated",false)'), release.index('p.stopRiding()'))

    def test_lens_colors_every_loaded_normally_rendered_mob(self):
        text = source('client/renderer/SpectralMobTint.java')
        self.assertIn('mc.level.entitiesForRendering()', text)
        self.assertIn('mob.shouldRender(origin.x,origin.y,origin.z)', text)
        self.assertNotIn('count++>=48', text)
        self.assertNotIn('inflate(48)', text)
        self.assertIn('mob instanceof Enemy||mob.getTarget()==mc.player', text)
        self.assertIn('SpectralEyeRenderType.EYES', text)
        self.assertIn('renderRightHand', source('client/renderer/SeeingStoneRenderer.java'))

    def test_chair_does_not_wait_for_father_to_leave_and_mount_is_confirmed(self):
        text=source('feast/ParentRescueStory.java')
        entry=text.split('private static void fatherEntry(',1)[1].split('private static void stageBlocked(',1)[0]
        self.assertIn('StoryFloorRoute.move',entry)
        self.assertIn('if(!p.startRiding(chair,true))',entry)
        self.assertNotIn('walk(dad,FirstEscapeScene.entrance',entry)
        self.assertIn('KeyTakeStand',text);self.assertIn('KeyLockStand',text)
        self.assertIn('release(p,s);n.putString("Blocker","Bring the original Little Door Key',text)
        self.assertIn('little.relative(facing,distance)',text)

    def test_escape_loop_stops_at_tunnel_and_handles_explicit_stop(self):
        text=source('client/FirstEscapeMusic.java')
        self.assertIn('looping=true',text);self.assertIn('mc.getConnection()==null',text)
        story=source('feast/FirstEscapeStory.java')
        tunnel=story.split('private static void tunnel(',1)[1].split('private static ',1)[0]
        self.assertIn('go(p,s,MISSING_A)',tunnel)
        self.assertIn('if(next==MISSING_A)VisitAudio.stopMusic(p,"first_escape_music")',story)
        self.assertIn('public static void stop()',text)
        accept=source('client/VisitAudioClient.java').split('public static void accept(',1)[1]
        self.assertIn('if(cue.stop())FirstEscapeMusic.stop();else FirstEscapeMusic.play();return;',accept)
        self.assertIn('EscapeMusicStarted',story)

    def test_offscreen_lens_restores_sky_state_and_avoids_equal_depth_rejection(self):
        state=source('client/SeeingStoneRenderState.java')
        for name in ('GL_COLOR_CLEAR_VALUE','GL_VIEWPORT','GL_READ_FRAMEBUFFER_BINDING','setShaderFogColor','setShaderTexture','applyModelViewMatrix','depthFunc'):
            self.assertIn(name,state)
        self.assertIn('VIEW_OFFSET_Z_LAYERING',source('client/renderer/SpectralEyeRenderType.java'))
        self.assertIn('COLOR_WRITE',source('client/renderer/SpectralEyeRenderType.java'))

    def test_lens_never_clears_world_depth_or_persistent_sky_color(self):
        client=source('client/SeeingStoneClient.java')
        self.assertNotIn('chain.addPass("blit"',client)
        self.assertIn('SeeingStoneFramebuffer.copyColor',client)
        copy=source('client/SeeingStoneFramebuffer.java')
        self.assertIn('GL_COLOR_BUFFER_BIT',copy)
        self.assertNotIn('GL_DEPTH_BUFFER_BIT',copy)
        self.assertNotIn('.clear(',copy)
        capture=source('client/SpectralEyes.java')
        self.assertIn('camera.getXRot()',capture)
        self.assertIn('camera.getYRot()+180F',capture)

    def test_cat_lease_protection_and_departure_reward_removal_are_scoped(self):
        self.assertIn('ShowActors.storyCat(this)',source('entity/CharacterEntity.java'))
        goal=source('feast/ShowActors.java').split('public static final class ControlGoal',1)[1]
        self.assertLess(goal.index('!n.hasUUID("ShowActorOwner")'),goal.index('n.getUUID("ShowActorOwner")'))
        payout=source('feast/FirstEscapeStory.java').split('private static void payChaseReward(',1)[1].split('\n',1)[0]
        self.assertNotIn('DIAMOND_BLOCK',payout)
        self.assertIn('finishBlockedDeparture',source('feast/FirstEscapeStory.java'))

    def test_bike_uses_wheelbase_support_a_swept_step_and_an_actual_exit_from_spawn(self):
        route = source('feast/StoryBikeRoute.java')
        self.assertIn('footprint.minX', route)
        self.assertIn('for(double wheel:new double[]{-1.02,-.72,0,.72,1.02})', route)
        self.assertIn('step(bike,at,at.x+forward.x,at.z+forward.z,angle)!=null', route)
        self.assertIn('step(bike,node.feet,cell.getX()+.5,cell.getZ()+.5,yaw)', route)
        self.assertIn('containsAnyLiquid(body)', route)
        self.assertIn('StoryBikeRoute.step(this,position(),next.x,next.z,heading)', source('feast/WybieBicycleEntity.java'))
        model = source('client/renderer/CharacterModel.java')
        rider = model.split('bike&&id.equals("wybie")', 1)[1].split('com.coraline.client.ShowActorPoseClient', 1)[0]
        self.assertIn('bike.previousWheelRotation,bike.wheelRotation', rider)
        self.assertNotIn('Mth.sin(age', rider)

    def test_cat_opening_has_a_real_open_air_mirror_fallback_before_safety_claims_progress(self):
        cat = source('feast/MissingParentsCat.java')
        mirror = cat.split('static boolean mirror(', 1)[1].split('static void shatter(', 1)[0]
        self.assertIn('wallRequired:new boolean[]{true,false}', mirror)
        self.assertIn('mirrorSpace(p,chosen,facing)', mirror)
        self.assertIn('mirrorApproach(p,chosen,facing,bed.getY())', mirror)
        self.assertNotIn('safePlayer(', mirror)
        self.assertNotIn('.setBlock(', mirror)
        safety = cat.split('static void cutsceneSafety(', 1)[1].split('static boolean mirror(', 1)[0]
        self.assertLess(safety.index('!mirror(p,s,family.bed)'), safety.index('StoryCutsceneWatchdog.overdue'))
        self.assertIn('case A->go(p,s,Phase.FOLLOW_MIRROR)', safety)

    def test_cane_reveal_has_no_global_blackout_and_prepares_behind_closed_doors(self):
        self.assertNotIn('roomLight()', source('client/FirstEscapeClient.java'))
        self.assertNotIn('color.mul(reveal)', source('client/OtherWorldLighting.java'))
        story=source('feast/FirstEscapeStory.java')
        self.assertIn('if(next==FIND_CANE||next==PRY_DOOR)prepareSealedRoom(p,s)', story)
        sealed=story.split('private static void prepareSealedRoom(',1)[1].split('private static DoorData.Door',1)[0]
        self.assertLess(sealed.index('setRoomDoors(p,s,false)'),sealed.index('FirstEscapeScene.prepareRoom(p,s)'))
        pry=story.split('case PRY_DOOR->',1)[1].split('case ROOM_DARK->',1)[0]
        self.assertLess(pry.index('go(p,s,ROOM_DARK)'),pry.index('setRoomDoors(p,s,true)'))

    def test_water_cannot_wash_door_and_legacy_recovery_does_not_replace_solids(self):
        door=source('block/TinyDoorBlock.java')
        self.assertIn('implements LiquidBlockContainer',door)
        for name in ('canPlaceLiquid','placeLiquid','canBeReplaced'):
            method=door.split('boolean '+name+'(',1)[1].split('}',1)[0]
            self.assertIn('return false;',method)
        repair=source('feast/FirstEscapeStory.java').split('static boolean repairLittleDoor(',1)[1].split('private static boolean openLittleDoor(',1)[0]
        self.assertIn('instanceof LiquidBlock',repair)
        self.assertIn('l.getBlockEntity(at)!=null',repair)
        self.assertIn('door.doorId=s.door',repair)
        self.assertIn('s.door.equals(DoorData.id(e.getItem()))',repair)
        self.assertNotIn('UUID.randomUUID()',repair)

    def test_early_throw_preserves_original_voice_and_syncs_prison_lighting(self):
        story=source('feast/FirstEscapeStory.java')
        throw=story.split('static void timedMirrorThrow(',1)[1].split('private static void mirrorThrow(',1)[0]
        self.assertIn('FirstEscapeVisualRules.mirrorThrowCue',throw)
        self.assertIn('if(frame>=30)enterPrison(p,s)',throw)
        self.assertNotIn('ShowPlayback.stop',throw)
        self.assertNotIn('go(p,s,MIRROR_A)',throw)
        self.assertIn('duration-40',source('feast/FirstEscapeVisualRules.java'))
        self.assertIn('"CocoaMusic","Rescued","MirrorPrisonEntered"',story)
        self.assertIn('flag("MirrorPrisonEntered")',source('client/FirstEscapeClient.java'))

    def test_concealed_parents_do_not_render_detached_button_eyes(self):
        renderer=source('client/renderer/ParentRenderer.java')
        self.assertIn('parent.isOtherParent()&&!parent.isInvisible()',renderer)
        render=renderer.split('@Override public void render(ParentEntity p,',1)[1]
        self.assertLess(render.index('if(p.isInvisible())return'),render.index('super.render('))

    def test_mirror_parent_models_explicitly_disable_default_baby_proportions(self):
        text=source('client/renderer/MissingParentsPresentation.java')
        self.assertIn('model.young=false',text)
        self.assertIn('.36F,.63F,.96F,1',text)
        self.assertIn('Writing is on the front glass',text)

if __name__ == '__main__':
    unittest.main()
