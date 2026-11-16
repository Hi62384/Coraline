"""Verify shadow declarations in the actual reobfuscated release, not dev classes."""
import argparse
import json
import struct
import zipfile


def verify_accepted(archive):
    """Require the new branch's production registrations and mapped targets."""
    config=json.loads(archive.read('coraline.mixins.json'))
    if config.get('required') is not True or config.get('injectors',{}).get('defaultRequire')!=1:
        raise AssertionError('Accepted branch injections must remain required')
    mappings=json.loads(archive.read(config['refmap']))['mappings']
    expected={
        'AcceptedInventoryDropMixin':('mixins',{'drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;':'Lnet/minecraft/world/entity/player/Player;m_7197_(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;'}),
        'AcceptedPlayerBodyMixin':('client',{
            'getRenderType':'Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;m_7225_(Lnet/minecraft/world/entity/LivingEntity;ZZZ)Lnet/minecraft/client/renderer/RenderType;',
            'render':'Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;m_7392_(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V',
            'Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V':'Lnet/minecraft/client/model/EntityModel;m_7695_(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V'}),
        'AcceptedPlayerHandMixin':('client',{
            'renderHand':'Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;m_117775_(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/model/geom/ModelPart;)V',
            'Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;':'Lnet/minecraft/client/renderer/MultiBufferSource;m_6299_(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;'})}
    for name,(side,targets) in expected.items():
        if name not in config.get(side,[]):raise AssertionError('Missing accepted branch registration: '+name)
        archive.getinfo('com/coraline/mixin/'+name+'.class')
        mapped=mappings.get('com/coraline/mixin/'+name,{})
        if any(mapped.get(key)!=value for key,value in targets.items()):raise AssertionError('Incomplete accepted branch production mappings: '+name)


def members(data):
    """Return declared field/method names from a Java class without loading it."""
    offset = 8
    def u2():
        nonlocal offset
        value = struct.unpack_from('>H', data, offset)[0]
        offset += 2
        return value
    count = u2()
    pool = [None] * count
    index = 1
    while index < count:
        tag = data[offset]
        offset += 1
        if tag == 1:
            size = u2()
            pool[index] = data[offset:offset + size].decode('utf-8', errors='replace')
            offset += size
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            offset += 4
        elif tag in (5, 6):
            offset += 8
            index += 1
        elif tag in (7, 8, 16, 19, 20):
            offset += 2
        elif tag == 15:
            offset += 3
        else:
            raise ValueError('Unknown constant pool tag: ' + str(tag))
        index += 1
    offset += 6
    interface_count = u2()
    offset += 2 * interface_count
    result = []
    for _ in range(2):  # Fields, then methods.
        for _ in range(u2()):
            u2()
            name, descriptor = pool[u2()], pool[u2()]
            result.append((name, descriptor))
            for _ in range(u2()):
                u2()
                size = struct.unpack_from('>I', data, offset)[0]
                offset += 4 + size
    return result


def verify(path,*,require_accepted=True):
    expected = {
        'PartyRandomLootReadMixin': ('getItems', 'm_7086_', '()Lnet/minecraft/core/NonNullList;'),
        'PartyPacketMixin': ('player', 'f_9743_', 'Lnet/minecraft/server/level/ServerPlayer;'),
        'RescueWebMovementMixin': ('player', 'f_9743_', 'Lnet/minecraft/server/level/ServerPlayer;'),
        'StoryAudioChannelMixin': ('source', 'f_83642_', 'I'),
    }
    with zipfile.ZipFile(path) as archive:
        if require_accepted:verify_accepted(archive)
        for cls, (dev, runtime, descriptor) in expected.items():
            declarations = members(archive.read('com/coraline/mixin/' + cls + '.class'))
            if (dev, descriptor) in declarations or (runtime, descriptor) not in declarations:
                raise AssertionError(f'{cls}: missing runtime shadow {runtime}; declarations={declarations}')
            print(f'{cls}: runtime shadow {runtime} verified')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('jar')
    verify(parser.parse_args().jar)
