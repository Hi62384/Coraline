import struct
import io,json
import tempfile
import unittest
import zipfile
from pathlib import Path
from verify_packaged_mixins import members, verify,verify_accepted


def fixture(name, descriptor, field=False):
    # A bounded class-file fixture: one interface and one declared member.
    strings = ['Example', 'java/lang/Object', 'java/lang/Runnable', name, descriptor]
    pool = b''.join(b'\x01' + struct.pack('>H', len(s)) + s.encode() for s in strings)
    pool += b'\x07\x00\x01\x07\x00\x02\x07\x00\x03'
    header = struct.pack('>IHHH', 0xcafebabe, 0, 61, 9) + pool
    header += struct.pack('>HHHHH', 0x421, 6, 7, 1, 8)
    member = struct.pack('>HHHHH', 1, 0x401, 4, 5, 0)
    return header + (member + b'\x00\x00' if field else b'\x00\x00' + member) + b'\x00\x00'


class PackagedMixinTests(unittest.TestCase):
    def test_accepted_branch_cannot_ship_optional_unregistered_or_unmapped_hooks(self):
        config={'required':True,'injectors':{'defaultRequire':1},'refmap':'coraline.refmap.json','mixins':['AcceptedInventoryDropMixin'],'client':['AcceptedPlayerBodyMixin','AcceptedPlayerHandMixin']}
        for changes in ({'required':False},{'mixins':[]},{}):
            with self.subTest(changes=changes),io.BytesIO() as data:
                with zipfile.ZipFile(data,'w') as archive:
                    archive.writestr('coraline.mixins.json',json.dumps(dict(config,**changes)))
                    archive.writestr('coraline.refmap.json',json.dumps({'mappings':{}}))
                    for name in ('AcceptedInventoryDropMixin','AcceptedPlayerBodyMixin','AcceptedPlayerHandMixin'):archive.writestr('com/coraline/mixin/'+name+'.class',b'fixture')
                data.seek(0)
                with zipfile.ZipFile(data) as archive,self.assertRaises(AssertionError):verify_accepted(archive)

    def test_method_and_interface_parsing(self):
        self.assertEqual(members(fixture('m_7086_', '()Lnet/minecraft/core/NonNullList;')),
                         [('m_7086_', '()Lnet/minecraft/core/NonNullList;')])

    def test_field_parsing(self):
        self.assertEqual(members(fixture('f_9743_', 'Lnet/minecraft/server/level/ServerPlayer;', True)),
                         [('f_9743_', 'Lnet/minecraft/server/level/ServerPlayer;')])

    def release(self, method, player, web='f_9743_', channel='f_83642_', missing=None):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'test.jar'
            with zipfile.ZipFile(path, 'w') as archive:
                archive.writestr('com/coraline/mixin/PartyRandomLootReadMixin.class',
                                 fixture(method, '()Lnet/minecraft/core/NonNullList;'))
                archive.writestr('com/coraline/mixin/PartyPacketMixin.class',
                                 fixture(player, 'Lnet/minecraft/server/level/ServerPlayer;', True))
                for name,member,descriptor in [('RescueWebMovementMixin',web,'Lnet/minecraft/server/level/ServerPlayer;'),('StoryAudioChannelMixin',channel,'I')]:
                    if name!=missing:archive.writestr('com/coraline/mixin/'+name+'.class',fixture(member,descriptor,True))
            verify(path,require_accepted=False)  # This fixture exercises only the legacy shadow declarations.

    def test_production_names_pass(self):
        self.release('m_7086_', 'f_9743_')

    def test_either_unmapped_shadow_fails(self):
        for method, player in [('getItems', 'f_9743_'), ('m_7086_', 'player')]:
            with self.subTest(method=method, player=player), self.assertRaises(AssertionError):
                self.release(method, player)

    def test_new_movement_and_mixer_shadows_must_also_be_reobfuscated(self):
        for web,channel in [('player','f_83642_'),('f_9743_','source')]:
            with self.subTest(web=web,channel=channel),self.assertRaises(AssertionError):self.release('m_7086_','f_9743_',web,channel)

    def test_missing_movement_or_mixer_mixin_cannot_pass(self):
        for name in ('RescueWebMovementMixin','StoryAudioChannelMixin'):
            with self.subTest(name=name),self.assertRaises(KeyError):self.release('m_7086_','f_9743_',missing=name)


if __name__ == '__main__':
    unittest.main()
