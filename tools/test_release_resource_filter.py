import unittest
from verify_temporary_event_audio import forbidden_resource

class ReleaseResourceFilterTests(unittest.TestCase):
    def test_runtime_blueprint_reader_is_not_authoring_asset(self):
        self.assertFalse(forbidden_resource('com/coraline/otherworld/RosewoodGarden$Blueprint.class'))
    def test_authoring_and_test_files_remain_rejected(self):
        for name in ('house.blueprint.json','blueprints/house.json','master.WAV','voice.mp3',
                     'model.blend','model.glb','weights.safetensors','weights.pth','model.pt',
                     'Source.java','ExampleGameTests.class','SceneVisualCheck.class','coraline.qa.mixins.json','templates/empty.nbt'):
            with self.subTest(name=name):self.assertTrue(forbidden_resource(name))
    def test_runtime_assets_remain_allowed(self):
        for name in ('assets/coraline/sounds/voice.ogg','assets/coraline/structures/garden.nbt',
                     'assets/coraline/textures/item/key.png'):
            self.assertFalse(forbidden_resource(name))

if __name__=='__main__':unittest.main()
