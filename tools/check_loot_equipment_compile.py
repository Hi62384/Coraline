"""Focused cached-API compile; does not admit native or release execution."""
import check_missing_parents_cat_compile as compiler

compiler.HEAP_MIB = 192
compiler.SOURCES = ['feast/VisitGiftPools.java', 'feast/VisitGiftHistory.java',
                    'feast/VisitGiftConfig.java', 'feast/LoopProps.java',
                    'feast/FirstEscapeItems.java', 'feast/CraftedShoeThrow.java',
                    'clothing/ArtifactItems.java', 'clothing/OtherVoidSwordItem.java',
                    'secrets/HouseCabinets.java', 'secrets/CabinetTreasure.java',
                    'clothing/WeaponConfig.java', 'clothing/RapierCadence.java', 'clothing/RapierCombat.java',
                    'client/clothing/RapierInput.java', 'mixin/RapierPrimaryHurtMixin.java',
                    'postgame/OtherWorldLootConfig.java', 'postgame/OtherWorldButtonDrops.java', 'CoralineMod.java',
                    'feast/GhostVictimNavigation.java', 'clothing/MinerLoot.java', 'clothing/ClothingConfig.java']
compiler.TESTS = ['feast/VisitGiftHistoryStateCheck.java', 'feast/VisitGiftBalanceGameTests.java',
                  'feast/CraftedShoeGameTests.java', 'feast/CoralineEquipmentRecipeGameTests.java',
                  'clothing/OtherVoidSwordGameTests.java', 'secrets/CabinetTreasureGameTests.java',
                  'clothing/RapierCombatGameTests.java', 'postgame/OtherWorldButtonDropsGameTests.java',
                  'secrets/CabinetLootRollGameTests.java', 'feast/VisitGiftPreferenceGameTests.java',
                  'feast/GhostFurnitureApproachGameTests.java', 'clothing/EquipmentAcquisitionGameTests.java',
                  'clothing/MinerEconomyGameTests.java', 'clothing/BottomlessBucketGameTests.java']
compiler.EXTRA_SOURCES = ['src/visittest/java/com/coraline/feast/VisitGiftGameTests.java']

if __name__ == '__main__':
    raise SystemExit(compiler.main())
