"""Focused cached-API compile; the release still requires normal Gradle checks/optimizer."""
import check_missing_parents_cat_compile as compiler
compiler.SOURCES=[
    'feast/DinnerActivities.java','feast/FamilyDinner.java','feast/VisitEvents.java',
    'feast/VisitAudio.java','feast/PartyEntrance.java','feast/PartyGuests.java',
    'feast/PartyBanquet.java','feast/ScenePropEntity.java','feast/PartyDecorations.java',
    'feast/HouseParty.java','client/VisitEventClient.java','client/VisitAudioClient.java',
    'client/PartyAmbienceClient.java','client/StoryMusicGuard.java','client/PartyChatterClient.java',
    'client/OtherWorldNature.java','client/renderer/ScenePropRenderer.java',
    'client/renderer/PartyDecorationRenderer.java','secrets/CatEncounter.java',
    'feast/PartyCelebration.java','feast/PartyKidsGames.java','feast/PartyConversations.java',
    'item/WrappedPartyGiftItem.java','registry/ModItems.java','registry/ModBlocks.java',
    'feast/WybieOptionalVisits.java','feast/FamilyGameNight.java','feast/MouseCircusVisit.java',
    'secrets/SecretSpies.java','feast/CinematicPropEntity.java','client/renderer/TheatreOceanPresentation.java',
    'feast/FirstEscapeCarry.java','feast/FirstEscapeStory.java','feast/FirstEscapeScene.java',
    'client/FirstEscapeClient.java','client/OtherWorldLighting.java',
    'client/renderer/ParentRenderer.java','client/renderer/FirstEscapePresentation.java',
    'client/renderer/CharacterModel.java','client/renderer/MotherMorphGeometry.java']
compiler.TESTS=['feast/FirstEscapeCarryGameTests.java','feast/CarryStepGeometrySmoke.java','feast/DinnerPartyRepairGameTests.java','feast/CocoaBeetlesChoreographySmoke.java','feast/StoryBacklogGameTests.java']
compiler.EXTRA_SOURCES=['src/rendercheck/java/com/coraline/qa/LeanWorldVisualCheck.java']
# A cancelled Gradle incremental compile can remove cached dependent classes.
# Compile the complete source closure in the same capped, isolated javac instead.
compiler.SOURCES=sorted(str(path.relative_to(compiler.ROOT/'src/main/java/com/coraline')).replace('\\','/') for path in (compiler.ROOT/'src/main/java/com/coraline').rglob('*.java'))
if __name__=='__main__':raise SystemExit(compiler.main())
