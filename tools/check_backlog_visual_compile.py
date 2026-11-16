"""Small capped API check while whole-source/GameTest startup is RAM-blocked."""
import check_missing_parents_cat_compile as compiler
compiler.SOURCES=[
    'feast/CocoaBeetlesChoreography.java','feast/FirstEscapeScene.java','feast/FirstEscapeStory.java',
    'client/StoryEyelids.java','client/TraditionalClient.java','client/SeeingStoneClient.java',
    'client/renderer/SeeingStoneRenderer.java','client/renderer/ParentRenderer.java',
    'client/renderer/FirstEscapePresentation.java','feast/CinematicPropEntity.java',
    'client/SpectralEyes.java','client/renderer/SpectralEyeRenderType.java',
    'client/renderer/GhostEyeRenderer.java','client/renderer/CinematicPropRenderer.java',
    'feast/RescueChallengeMath.java','otherworld/OtherWorldEstate.java',
    'feast/RescueCombat.java','otherworld/RealmProtection.java']
compiler.TESTS=['feast/CocoaBeetlesChoreographySmoke.java']
if __name__=='__main__':raise SystemExit(compiler.main())
