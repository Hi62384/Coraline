"""Install the original individually illustrated 2D armor icons.

The rich artwork in artwork/armor, advanced_armor, materials and weapons is
authoritative for the 13 reference-led armor sets. It is not a model render.
"""
from pathlib import Path
import shutil
from PIL import Image
import draw_armor_icons

ROOT=Path(__file__).resolve().parents[1]
ART=ROOT/'artwork/sculpted_wardrobe/icons'
GAME=ROOT/'src/main/resources/assets/coraline/textures/item'
GENERATED=Path(r'C:/Users/User1/.codex/generated_images/01a0ff56-811d-75c0-98db-c628015951c7')
ORIGINAL_FOLDERS={
 'luck':'armor','moonstone':'armor','miners':'armor',
 'spidervision':'advanced_armor','beldam_weave':'advanced_armor',
 'soulbound':'advanced_armor','super_spidervision':'advanced_armor',
 'potent_beldam_weave':'advanced_armor',
 'soulbound_potent_beldam_weave':'advanced_armor',
 'other_world_void':'advanced_armor',
 'other_diamond':'materials','other_iron':'materials','perfect':'weapons',
}
SOURCES={
 'raincoat':'exec-54cd092b-78ac-4f30-bce8-982c27af2231.png',
 'luck_helmet':'exec-e19d28d0-b300-4cc1-b182-d75648cfce0c.png',
 'luck_chestplate':'exec-9c80325d-5342-4063-847e-098fd5aad36c.png',
 'luck_leggings':'exec-146ce710-3ec7-44af-83d4-a10cb22c9436.png',
 'luck_boots':'exec-2d4563a6-d2b9-48a5-b6cd-3ff05aad07ed.png',
 'moonstone_helmet':'exec-9df524d4-1784-4ef6-a8ba-56555e0220de.png',
 'moonstone_chestplate':'exec-bd6a5022-f862-4bba-82d2-ed4fe3c0e5bf.png',
 'moonstone_leggings':'exec-0fcddfa1-5283-4d91-9653-275dd5ba00db.png',
 'moonstone_boots':'exec-042bf0e4-4294-46f1-ba3e-b851a666d703.png',
 'miners_helmet':'exec-4f182944-72c3-44d9-9b44-085426a3f489.png',
 'miners_chestplate':'exec-3dcaed41-a990-4ba9-988f-d7b6aa1c081d.png',
 'miners_leggings':'exec-efdcd957-2d7d-4d96-9aa6-5f4a62f9f04d.png',
 'miners_boots':'exec-b565c366-eae3-4433-bbfe-f5975f044fbe.png',
 'spidervision_helmet':'exec-49d159a0-87bc-450a-a40d-1fe0e083f0ea.png',
 'spidervision_chestplate':'exec-f2db2094-5082-42fc-96a3-6d77b10bc3a5.png',
 'spidervision_leggings':'exec-9f946bfa-6d47-444e-9e6a-315b68787256.png',
 'spidervision_boots':'exec-0daa8e17-898d-4317-8d62-7703833cf333.png',
 'beldam_weave_helmet':'exec-605f5fa5-e8ef-439f-bdd2-7ed3516488fe.png',
 'beldam_weave_chestplate':'exec-1c27903e-d745-4056-a7a9-5a22a6761981.png',
 'beldam_weave_leggings':'exec-445a0316-ff4b-43b9-88c4-95f90952382d.png',
 'beldam_weave_boots':'exec-c5f4628b-8c6d-40be-b481-ed468e6bcd1b.png',
 'soulbound_helmet':'exec-6bff757c-a1c6-43a0-b7af-595ca25c9c0d.png',
 'soulbound_chestplate':'exec-7020f8c1-4a5a-42ae-aef8-fdea6c192543.png',
 'soulbound_leggings':'exec-a24666a0-3eaf-4b18-8a0e-70714f1222fa.png',
 'soulbound_boots':'exec-adb1bdca-494c-466d-95b0-97119291f786.png',
 'super_spidervision_helmet':'exec-89e18055-c0c7-4458-a6f0-d354b7f8a024.png',
 'super_spidervision_chestplate':'exec-8e88c3be-7fdd-414e-9aaa-6033894e7c26.png',
 'super_spidervision_leggings':'exec-e124dfc1-0785-4371-9f69-f5610ae483ba.png',
 'super_spidervision_boots':'exec-0147365b-e71a-4456-95e5-af41bdbafaf5.png',
 'potent_beldam_weave_helmet':'exec-5901838a-5546-4620-81ca-c3bc949a15c0.png',
 'potent_beldam_weave_chestplate':'exec-b432d640-c1ba-482a-9d36-26a612ee36f5.png',
 'potent_beldam_weave_leggings':'exec-37e21379-7ede-4ef7-8730-f161d041379e.png',
 'potent_beldam_weave_boots':'exec-321eeb9b-3e69-47ec-bab0-b06385073fa5.png',
 'soulbound_potent_beldam_weave_helmet':'exec-22d45046-e55d-4f6f-9560-5871bc6dba3d.png',
 'soulbound_potent_beldam_weave_chestplate':'exec-5c7f4960-e62a-4a46-ac5b-4cdfd9a1c204.png',
 'soulbound_potent_beldam_weave_leggings':'exec-1b94131f-b5f6-410a-a7c7-fcd0dec1872a.png',
 'soulbound_potent_beldam_weave_boots':'exec-e9a5f119-6caa-48ee-ab7b-e0dd51420fe4.png',
 'other_world_void_helmet':'exec-ee478b01-806e-4fd0-9975-f54459906c05.png',
 'other_world_void_chestplate':'exec-d89e9aee-5dab-413d-bbba-15d2ca1efd8c.png',
 'other_world_void_leggings':'exec-fe3ead01-ccff-4ae1-bacf-72a416dbdca8.png',
 'other_world_void_boots':'exec-89fd4e0e-d756-46a2-bdb6-faf0ec18dde8.png',
 'other_diamond_helmet':'exec-cc1a2c55-98b8-4f01-b80b-68c85e3e4d16.png',
 'other_diamond_chestplate':'exec-a02c0589-c4f3-4e57-a4bb-43ef73c4d305.png',
 'other_diamond_leggings':'exec-ec3f4f07-670e-4401-b066-cfef5ec04c0e.png',
 'other_diamond_boots':'exec-0516483f-0a5c-44ff-b23a-47771acbbda1.png',
 'other_iron_helmet':'exec-c0fe543d-183b-4a37-b05c-016f1e34e491.png',
 'other_iron_chestplate':'exec-f77f3a86-1516-4ba8-a452-a9cf0d34d591.png',
 'other_iron_leggings':'exec-b66c14f1-32f8-4bc4-a133-4723ad2b4e8f.png',
 'other_iron_boots':'exec-24aa5713-84b9-4fb4-809a-173ca39b5bec.png',
 'perfect_helmet':'exec-7212738b-ee03-438b-8846-950719a09e65.png',
 'perfect_chestplate':'exec-426739d6-2144-41a7-9ebb-7ab74170b58e.png',
 'perfect_leggings':'exec-8202e7ef-b612-40d3-8acb-8123a8ea4c24.png',
 'perfect_boots':'exec-c5867ff8-2c6a-45aa-bdf5-e41cea7cc73d.png',
}

for name,filename in SOURCES.items():
    original=ART/(name+'.png')
    style=name.rsplit('_',1)[0]
    if style in ORIGINAL_FOLDERS:
        rich_source=ROOT/'artwork'/ORIGINAL_FOLDERS[style]/(name+'.png')
        if not rich_source.exists():raise FileNotFoundError(rich_source)
        shutil.copy2(rich_source,original)
    elif not original.exists():
        if name=='raincoat':draw_armor_icons.draw_raincoat().save(original)
        else:
            style,piece=name.rsplit('_',1)
            if style in draw_armor_icons.PALETTES:
                draw_armor_icons.draw_armor(style,piece).save(original)
            else:shutil.copy2(GENERATED/filename,original)
    with Image.open(original) as image:
        rgba=image.convert('RGBA')
        if rgba.getextrema()[3][0]!=0 or not rgba.getbbox():
            raise ValueError('Icon needs a transparent nonempty background: '+name)
        rgba.resize((256,256),Image.Resampling.LANCZOS).save(GAME/(name+'.png'))
    print(name)
