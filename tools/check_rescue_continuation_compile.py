"""Compile the isolated continuation plus its changed production dependencies. Not native QA."""
import check_missing_parents_cat_compile as compiler
compiler.SOURCES=[
    'feast/StoryItemCustody.java','feast/ParentRescueStory.java','feast/RescueWebLayout.java','feast/CocoaBeetlesChoreography.java',
    'feast/RescueEyes.java','feast/RescueProps.java','feast/RescueChallengeMath.java',
    'feast/RescueCombat.java','feast/CinematicPropEntity.java','feast/FirstEscapeScene.java',
    'otherworld/OtherWorldEstate.java','block/StoryWebBlock.java','registry/ModBlocks.java',
    'client/RescueClient.java','client/RescueWorldEffects.java','client/StoryEyelids.java',
    'client/SpectralEyes.java','client/SeeingStoneClient.java','client/renderer/SeeingStoneRenderer.java',
    'client/renderer/SpectralEyeRenderType.java','client/renderer/GhostEyeRenderer.java',
    'client/renderer/CinematicPropRenderer.java','client/renderer/FirstEscapePresentation.java',
    'client/renderer/RescuePresentation.java','client/renderer/ParentRenderer.java',
    'client/renderer/CharacterRenderer.java','client/renderer/CharacterModel.java',
    'client/renderer/ShowDisguiseLayer.java','client/ShowActorPoseClient.java']
compiler.TESTS=[]
compiler.EXTRA_SOURCES=['src/rescuedevelopment/java/com/coraline/feast/'+name+'.java' for name in
    ('RescueChapter','RescueEncounters','RescueBosses','BeldamFinale','RescueEpilogue','RescueWebSite','RescuePassage')]
if __name__=='__main__':raise SystemExit(compiler.main())
