"""Runtime buildability/design corrections. The original user blueprint stays untouched."""
from collections import deque

DIRECTIONS = {'north': (0,-1), 'east': (1,0), 'south': (0,1), 'west': (-1,0)}
# Remove duplicated expansion strips, rather than uniformly scaling stairs,
# doors or furniture. RosewoodHouse.at applies the matching monotone mapping.
DROP_X={18,19}
DROP_Z={46,47,61,62}

def repair(blocks):
    grid = {(b['x'], b['y'], b['z']): dict(b, properties=dict(b['properties'])) for b in blocks}
    def put(x,y,z,id,**properties):
        grid[x,y,z] = dict(x=x,y=y,z=z,id=id if ':' in id else 'minecraft:'+id,properties=properties,phase='runtime_repair')
    def air(x,y,z): grid.pop((x,y,z),None)
    def box(x0,y0,z0,x1,y1,z1,id,**properties):
        for x in range(x0,x1+1):
            for y in range(y0,y1+1):
                for z in range(z0,z1+1):
                    if id=='air': air(x,y,z)
                    else: put(x,y,z,id,**properties)
    def ident(p): return grid.get(p,{}).get('id','minecraft:air')

    # Fill omitted ground below the sparse blueprint. In particular, gravel paths
    # previously fell five blocks into the excavation. Keep the exterior cellar well.
    for x in range(65):
        for z in range(-2,83):
            if 13<=x<=51 and 12<=z<=68 or 52<=x<=63 and 14<=z<=24: continue
            for y in range(-2,4):
                if (x,y,z) not in grid: put(x,y,z,'grass_block' if y==3 else 'dirt')
    for p,b in list(grid.items()):
        x,y,z=p
        if b['id'].endswith('_fence') and y<=4 and b.get('phase')!='fittings' and not (52<=x<=63 and 14<=z<=24):
            put(x,y,z,'stone_bricks')
    # The cellar-well approach has gravel too; support its rim without filling
    # the descending treads or the passage along the bottom of the well.
    for b in list(grid.values()):
        if b['id']=='minecraft:gravel' and b['y']==3:
            for y in range(-2,3):
                if (b['x'],y,b['z']) not in grid: put(b['x'],y,b['z'],'stone_bricks')
    # Complete, sealed basement slab; all rooms retain their checker/stone finish.
    for x in range(13,52):
        for z in range(12,69):
            if (x,-2,z) not in grid: put(x,-2,z,'stone_bricks')
    # Solid cellar perimeter, including missing cells. Retain exterior doors.
    for x,z in ({(x,z) for x in (13,51) for z in range(12,69)}|{(x,z) for x in range(13,52) for z in (12,68)}):
        for y in range(-1,5):
            if ident((x,y,z))=='minecraft:air': put(x,y,z,'stone_bricks')
    # No basement windows. Retain the actual external entrance doors.
    for p,b in list(grid.items()):
        x,y,z=p
        if -1<=y<=4 and (x in (13,51) and 12<=z<=68 or z in (12,68) and 13<=x<=51) and 'glass' in b['id']:
            put(x,y,z,'stone_bricks')

    # Remove cellar-to-house staircase and close its floor/ceiling opening.
    box(14,-1,20,22,4,22,'air')
    box(14,5,20,22,5,22,'spruce_planks')
    box(14,6,20,22,6,22,'air')
    # Fill the enclosed pocket behind the L-stair, without touching its treads,
    # westward approach (z24..26), southward flight (x23..25), or upstairs landing.
    box(14,6,21,22,13,31,'spruce_planks')
    box(26,6,27,30,13,31,'spruce_planks')
    # Stepped timber stringers give every main-stair guard a real attachment.
    for x,y in [(30,6),(29,7),(28,8),(27,9),(26,10)]:
        for z in (23,27):
            put(x,y,z,'stripped_dark_oak_log',axis='y');put(x,y+1,z,'spruce_fence',waterlogged='false')
    for z,y in [(27,11),(28,12),(29,13),(30,14)]:
        for x in (22,26):
            put(x,y,z,'stripped_dark_oak_log',axis='y');put(x,y+1,z,'spruce_fence',waterlogged='false')
    # Boiler room opens from the stair-side wall, not the hallway.
    box(31,6,33,31,7,33,'white_concrete')
    box(30,6,31,31,7,31,'air')
    box(30,6,33,30,7,33,'air')
    for y,half in [(6,'lower'),(7,'upper')]: put(30,y,32,'spruce_door',facing='north',half=half,hinge='left',open='false',powered='false')
    # The attic is reached only by its exterior door, not the upper-floor stair.
    box(37,15,35,41,22,45,'air')
    box(37,14,35,41,14,45,'spruce_planks')
    box(37,23,35,41,23,45,'spruce_planks')
    for x in range(36,43):
        for z in range(34,47):
            if ident((x,24,z)).endswith('_fence'): air(x,24,z)
    # A small exterior landing and one solid-wall doorway replace the glazed
    # attic entrance gallery. Keep the existing story-lock door identity/location.
    for z in range(50,69):
        for y in range(24,30):
            if (y,z) in ((24,65),(25,65)): continue
            put(9,y,z,'stripped_dark_oak_log' if y in (24,29) else 'pink_terracotta',**({'axis':'z'} if y in (24,29) else {}))
    for x in range(0,9):
        for z in range(50,69):
            if 4<=x<=8 and 65<=z<=67: continue
            b=grid.get((x,23,z),{})
            if b.get('phase') in ('floor','fittings','frame'): air(x,23,z)
            if ident((x,24,z))=='minecraft:iron_bars': air(x,24,z)
    box(4,23,65,8,23,67,'spruce_planks')
    # The upper flight finishes at z55: keep a narrow connected walkway to the
    # single door, not an isolated landing or the former full-width gallery.
    box(5,23,55,8,23,64,'spruce_planks')
    box(6,24,55,8,25,67,'air')
    for z in range(55,65): put(5,24,z,'spruce_fence',waterlogged='false')
    for x in range(4,9): put(x,24,67,'spruce_fence',waterlogged='false')
    for z in (65,67): put(4,24,z,'spruce_fence',waterlogged='false')
    # Supported timber stringers and tall wooden posts instead of floating bars.
    for p,b in list(grid.items()):
        x,y,z=p
        if b['id']=='minecraft:iron_bars' and y>=6 and (x<=12 or x>=59):
            put(x,y,z,'spruce_fence',north='true',east='true',south='true',west='true',waterlogged='false')
            if ident((x,y-1,z))=='minecraft:air': put(x,y-1,z,'spruce_planks')

    # Replace the depth-stretched exterior flight with a continuous upper stair.
    # Reach it along a narrow second-storey balcony; the attic keeps one small
    # rear landing rather than a disconnected flight or a giant entrance gallery.
    # The lower flight was also stretched apart by the old width insertion.
    box(12,6,8,28,17,10,'air')
    for index,x in enumerate(range(28,19,-1)):
        y=6+index
        for z in (8,9,10):
            put(x,y,z,'spruce_stairs',facing='west',half='bottom',shape='straight',waterlogged='false')
            box(x,y+1,z,x,y+3,z,'air')
        for z in (8,11): put(x,y,z,'spruce_planks');put(x,y+1,z,'spruce_fence',waterlogged='false')
    box(6,14,8,19,14,10,'spruce_planks')
    box(6,15,8,20,17,10,'air')
    box(6,14,11,8,14,19,'spruce_planks')
    box(5,15,20,8,26,64,'air')
    box(5,14,17,8,14,53,'spruce_planks')
    box(6,15,17,8,17,53,'air')
    for z in range(17,54): put(5,15,z,'spruce_fence',waterlogged='false')
    box(6,14,16,12,14,19,'spruce_planks')
    for z in (20,35,53): box(5,5,z,5,13,z,'stripped_dark_oak_log',axis='y')
    for index,z in enumerate(z for z in range(54,65) if z not in DROP_Z):
        y=15+index
        for x in (6,7,8): put(x,y,z,'spruce_stairs',facing='south',half='bottom',shape='straight',waterlogged='false')
        box(5,14,z,5,y,z,'spruce_planks');put(5,y+1,z,'spruce_fence',waterlogged='false')
    box(4,23,65,8,23,67,'spruce_planks')
    # Restore the wall sections cleared beside the old stair, not the door itself.
    for z in range(50,65):
        for y in range(24,30): put(9,y,z,'pink_terracotta')

    # Exterior cellar entrance beside the main front stairs, with a real sunken
    # approach. Its low passage runs under the porch, leaving the porch aisle open.
    box(52,-2,14,63,3,24,'dirt')
    box(52,3,14,63,3,24,'grass_block')
    box(51,-1,21,51,0,22,'stone_bricks')
    for (x,y,z),b in list(grid.items()):
        if 52<=x<=63 and 14<=z<=24 and y==4 and b['id'].endswith('_fence'): air(x,y,z)
    box(25,-1,-1,28,3,11,'air')
    box(25,4,-1,28,7,4,'air')
    box(24,-2,-1,24,3,11,'stone_bricks');box(29,-2,-1,29,3,11,'stone_bricks')
    box(25,-2,4,28,-2,11,'stone_bricks')
    for index,z in enumerate(range(-1,4)):
        y=3-index
        for x in (25,26,27,28):
            put(x,y,z,'stone_brick_stairs',facing='north',half='bottom',shape='straight',waterlogged='false')
            box(x,-2,z,x,y-1,z,'stone_bricks')
    for x in (24,29):
        for z in range(-1,5): put(x,4,z,'spruce_fence',waterlogged='false')
    for x,hinge in [(26,'left'),(27,'right')]:
        for y,half in [(-1,'lower'),(0,'upper')]: put(x,y,12,'spruce_door',facing='north',half=half,hinge=hinge,open='false',powered='false')

    # Restore timber where chains overwrote the beam itself. Hang below it.
    for p,b in list(grid.items()):
        if b['id']!='minecraft:chain': continue
        x,y,z=p
        for axis,(a,c) in [('x',((x-1,y,z),(x+1,y,z))),('z',((x,y,z-1),(x,y,z+1)))]:
            if ident(a).endswith('_log') and ident(c).endswith('_log'):
                put(x,y,z,ident(a),axis=axis);break

    # Supported, wall-adjacent display cabinets, not hovering slab islands.
    for old_x,new_x in [(15,14),(47,50)]:
        for z in range(28,43):
            for y in range(-1,4):
                if ident((old_x,y,z)).endswith(('_slab','_flower_pot')) or 'potted_' in ident((old_x,y,z)) or ident((old_x,y,z))=='minecraft:flower_pot': air(old_x,y,z)
        for start in (29,34,39):
            for z in range(start,start+3):
                put(new_x,-1,z,'barrel',facing='north',open='false')
                for y in (0,2): put(new_x,y,z,'spruce_slab',type='top',waterlogged='false')
            for z in (start-1,start+3):
                for y in range(-1,3): put(new_x,y,z,'stripped_spruce_log',axis='y')
            for y in (1,3):
                put(new_x,y,start,'potted_oxeye_daisy');put(new_x,y,start+2,'flower_pot')

    # Open timber stage with curtains at the back, no glass cage at its front.
    box(25,0,46,35,2,46,'air')
    box(25,-1,44,35,-1,47,'spruce_planks')
    for x in (25,35): box(x,0,47,x,2,47,'red_wool')
    for x in range(29,32): put(x,-1,43,'spruce_stairs',facing='south',half='bottom',shape='straight',waterlogged='false')

    # These were oversized decorative mirrors, not TVs. Clear both bulky frames
    # and mount compact mirrors against the real wall, away from door approaches.
    box(38,-1,54,39,3,57,'air')
    box(28,-1,64,29,3,66,'air')
    put(37,1,56,'coraline:rosewood_mirror',facing='east')
    put(30,1,65,'coraline:rosewood_mirror',facing='west')

    # Rebuild the washroom with low, recognisable, purpose-modelled fixtures.
    box(37,-1,61,50,4,67,'air')
    for x in range(37,51):
        for z in range(61,68): put(x,-2,z,'white_concrete' if (x+z)%2 else 'polished_andesite')
    for x in range(37,51):
        for y in range(-1,4): put(x,y,68,'white_concrete' if y<=1 else 'light_gray_concrete')
    for z in range(61,68):
        for y in range(-1,4): put(51,y,z,'white_concrete' if y<=1 else 'light_gray_concrete')
    for x in (39,41):
        put(x,-1,66,'coraline:rosewood_sink',facing='north')
        put(x,1,67,'coraline:rosewood_mirror',facing='north')
    put(48,-1,66,'coraline:rosewood_toilet',facing='north')
    put(45,-1,65,'coraline:rosewood_bath',facing='north')
    put(45,-1,66,'coraline:rosewood_bath',facing='south')
    put(38,-1,62,'barrel',facing='north',open='false');put(38,0,62,'barrel',facing='north',open='false')
    put(38,1,62,'spruce_slab',type='top',waterlogged='false')
    for x in (39,45,49): put(x,3,67,'sea_lantern')
    for x in range(37,51):
        for z in range(61,68): put(x,4,z,'quartz_slab',type='top',waterlogged='false')

    # Convert connected fence-and-slab table assemblies throughout every storey.
    pending={p for p,b in grid.items() if b['id'].endswith('_slab') and b.get('phase')=='fittings'}
    groups=0
    while pending:
        seed=pending.pop();group={seed};queue=deque([seed])
        while queue:
            x,y,z=queue.popleft()
            for dx,dz in DIRECTIONS.values():
                q=(x+dx,y,z+dz)
                if q in pending: pending.remove(q);group.add(q);queue.append(q)
        if not any(ident((x,y-1,z)).endswith('_fence') or y==25 and ident((x,y-1,z)).endswith('_log') for x,y,z in group): continue
        groups+=1
        for x,y,z in group:
            above=ident((x,y+1,z));decor='vase' if above=='minecraft:flower_pot' or 'potted_' in above else 'lamp' if above=='minecraft:lantern' else ''
            if decor or y==25 and ident((x,y+1,z))=='minecraft:barrel': air(x,y+1,z)
            air(x,y,z)
            links={name:str((x+dx,y,z+dz) in group).lower() for name,(dx,dz) in DIRECTIONS.items()}
            put(x,y-1,z,'coraline:rosewood_table'+('_'+decor if decor else ''),**links)

    # Replace stair seats/trapdoor backs with coherent furniture geometry.
    seats={p for p,b in grid.items() if b.get('phase')=='fittings' and b['id'].endswith('_stairs') and b['y'] in (-1,6,15,24)}
    seat_facings={p:grid[p]['properties'].get('facing','north') for p in seats}
    for x,y,z in seats:
        source_facing=seat_facings[x,y,z]
        facing={'north':'south','south':'north','east':'west','west':'east'}[source_facing]
        joined=any(seat_facings.get((x+dx,y,z+dz))==source_facing for dx,dz in DIRECTIONS.values())
        put(x,y,z,'coraline:rosewood_sofa' if joined else 'coraline:rosewood_chair',facing=facing)
        for dx,dz in DIRECTIONS.values():
            if ident((x+dx,y,z+dz)).endswith('_trapdoor'): air(x+dx,y,z+dz)
    # Bookcase caps touch their base instead of hovering a full block above it.
    for (x,y,z),b in list(grid.items()):
        if b['id'].endswith('_slab') and ident((x,y-1,z))=='minecraft:air' and ident((x,y-2,z))=='minecraft:bookshelf':
            air(x,y,z);put(x,y-1,z,'dark_oak_slab',type='bottom',waterlogged='false')
        if b['id'].endswith('_fence') and y in (6,15) and ident((x,y+1,z)).endswith('_fence'):
            put(x,y,z,'coraline:rosewood_coat_rack',facing='north');air(x,y+1,z)

    # Clear the old disconnected ten-seat supper assembly and freestanding curio.
    # The real story table and its three complete chairs are supplied by the scene.
    for (x,y,z),b in list(grid.items()):
        if 37<=x<=50 and 21<=z<=51 and 6<=y<=12 and b.get('phase') in ('fittings','decoration','runtime_repair'):
            if b['id'].endswith('_door') or 'glass' in b['id'] or b['id'] in ('minecraft:pink_terracotta','minecraft:white_concrete'): continue
            air(x,y,z)
    for z in range(40,45):
        put(50,6,z,'bookshelf');put(50,7,z,'dark_oak_slab',type='bottom',waterlogged='false')

    # A usable low-cabinet kitchen, with an uncluttered breakfast area and entrances.
    box(37,6,53,50,10,67,'air')
    for x in range(37,51):
        for z in range(53,68): put(x,5,z,'white_concrete' if (x+z)%2 else 'polished_andesite')
    for x in range(38,50): put(x,6,67,'coraline:rosewood_counter',facing='north')
    for z in range(59,67): put(50,6,z,'coraline:rosewood_counter',facing='west')
    put(44,6,67,'coraline:rosewood_sink',facing='north')
    for x in (40,41): put(x,6,67,'coraline:rosewood_oven',facing='north')
    box(40,9,67,41,9,67,'polished_andesite')
    box(40,10,67,41,11,67,'iron_block')
    for z in (64,65): put(37,6,z,'barrel',facing='east',open='false');put(37,7,z,'barrel',facing='east',open='false')
    for x in (43,44):
        for z in (59,60): put(x,6,z,'coraline:rosewood_table_vase' if (x,z)==(43,59) else 'coraline:rosewood_table',north=str(z==60).lower(),south=str(z==59).lower(),west=str(x==44).lower(),east=str(x==43).lower())
    put(42,6,59,'coraline:rosewood_chair',facing='east');put(45,6,60,'coraline:rosewood_chair',facing='west')

    # The single attic entrance is at the rear. Put the tent at the far/front
    # end of its long carpet, facing back toward the arrival aisle.
    box(26,24,49,36,31,59,'air')
    # The working native tent is installed here by MouseCircusSite. Keep its
    # footprint clear rather than building a second nonfunctional concrete tent.
    box(29,24,17,35,29,23,'air')
    box(31,24,18,33,24,22,'red_carpet')
    # Remove intrusive freestanding pink dormer partitions, retaining window walls.
    for p,b in list(grid.items()):
        x,y,z=p
        if b['id']=='minecraft:pink_terracotta' and 16<=x<=49 and 15<=z<=66 and 24<=y<=31: air(x,y,z)
    # Seal roof joints with continuous dark timber lining beneath the shell.
    roof={}
    for b in blocks:
        x,y,z=b['x'],b['y'],b['z']
        if y>=25 and b.get('phase')=='roof':
            roof[x,z]=min(y,roof.get((x,z),y))
            if (x,y,z) not in grid: grid[x,y,z]=dict(b,properties=dict(b['properties']))
    for x in range(10,52):
        for z in range(12,69):
            if x in DROP_X or z in DROP_Z: continue
            height=roof.get((x,z))
            if height is None:
                adjacent=[roof[x+dx,z+dz] for dx,dz in DIRECTIONS.values() if (x+dx,z+dz) in roof]
                if adjacent: height=round(sum(adjacent)/len(adjacent))
            if height is not None and height>=25:
                # After deleting expansion strips, roof slopes can change by
                # several blocks between surviving columns. Bridge that entire
                # vertical seam, not merely the top two cells of each column.
                near=[]
                for dx,dz in DIRECTIONS.values():
                    xx,zz=x+dx,z+dz
                    while xx in DROP_X or zz in DROP_Z: xx+=dx;zz+=dz
                    if (xx,zz) in roof: near.append(roof[xx,zz])
                bottom=min([height-1]+[h-1 for h in near])
                for y in range(max(24,bottom),height+1):
                    if ident((x,y,z))=='minecraft:air': put(x,y,z,'dark_oak_planks')
    # Uneven stored trunks, crates, folded fabric and cobwebs, off the main aisle.
    for index,(x,z) in enumerate([(18,24),(20,27),(46,29),(47,35),(19,43),(21,47),(44,49),(47,55),(18,59),(22,63),(41,62)]):
        put(x,24,z,'barrel',facing=('north','east','south')[index%3],open='false')
        if index%3==0: put(x,25,z,'chest',facing='south',type='single',waterlogged='false')
        if index%2==0: put(x+1,24,z,'gray_wool');put(x+1,25,z,'brown_carpet')
        if index%4==0: put(x-1,24,z+1,'spruce_slab',type='bottom',waterlogged='false')
    for x,z in [(17,16),(49,15),(15,61),(47,64)]: put(x,27,z,'cobweb')
    for index,(x,z) in enumerate([(23,24),(25,28),(22,33),(23,44),(27,51),(26,57),(37,29),(40,33),(44,37),(38,45),(46,60)]):
        put(x,24,z,'barrel',facing=('north','west','east')[index%3],open='false')
        put(x+1,24,z,'chest',facing=('south','east')[index%2],type='single',waterlogged='false')
        if index%2==0: put(x,25,z,'barrel',facing='east',open='false')
        if index%3==0: put(x-1,24,z+1,'gray_wool');put(x-1,25,z+1,'brown_carpet')
        if index%4==0: put(x+2,24,z-1,'coraline:rosewood_chair',facing='east')

    # Main-floor rooms are deliberately restrained; retain only their walls,
    # windows and doors, then place a few complete furnishings against walls.
    def clear_furnishings(x0,z0,x1,z1,low):
        for (x,y,z),b in list(grid.items()):
            if x0<=x<=x1 and z0<=z<=z1 and low<=y<=low+6 and b.get('phase') in ('fittings','decoration','runtime_repair'):
                if b['id'].endswith('_door') or 'glass' in b['id'] or b['id'] in ('minecraft:pink_terracotta','minecraft:white_concrete'): continue
                air(x,y,z)
    clear_furnishings(14,37,30,51,6)
    clear_furnishings(11,53,30,67,6)
    # The piano's entire middle area stays empty, including its approach.
    box(11,6,55,29,12,64,'air')
    for z in range(55,65):
        put(11,6,z,'bookshelf');put(11,7,z,'bookshelf');put(11,8,z,'dark_oak_slab',type='bottom',waterlogged='false')
    for x in (27,28): put(x,6,66,'coraline:rosewood_table',east=str(x==27).lower(),west=str(x==28).lower())
    put(27,6,65,'coraline:rosewood_chair',facing='south')
    for z in (57,58,59): put(29,6,z,'coraline:rosewood_sofa',facing='west')
    # Close the residual service slits between the three rear rooms.
    box(31,6,57,36,13,67,'spruce_planks')
    # The little-door recess ends at solid masonry, not an exterior window.
    box(12,6,37,12,8,39,'stone_bricks')
    # Compact masonry fireplace with a recessed glowing hearth and fitted mantel.
    box(14,6,43,16,10,49,'air')
    for z in (43,49): box(14,6,z,15,9,z,'stone_bricks')
    box(14,6,44,14,8,48,'polished_deepslate')
    box(15,5,43,16,5,49,'stone_bricks')
    box(15,9,43,15,9,49,'stone_brick_slab',type='bottom',waterlogged='false')
    for z in (44,45,48): put(14,6,z,'magma_block')
    for z in (40,41,42): put(29,6,z,'coraline:rosewood_sofa',facing='west')
    for x in (23,24): put(x,6,41,'coraline:rosewood_table',east=str(x==23).lower(),west=str(x==24).lower())

    # Remove the complete obsolete attic-stair room, not just its treads.
    box(36,15,33,42,22,47,'air')
    box(36,14,33,42,14,47,'spruce_planks')
    box(36,23,33,42,23,47,'spruce_planks')
    # New shared bathroom wall and a proper open hall approach.
    box(42,15,33,42,22,47,'white_concrete')
    for y,half in [(15,'lower'),(16,'upper')]: put(42,y,35,'spruce_door',facing='west',half=half,hinge='left',open='false',powered='false')
    # Complete the child-bedroom boundary (the broken window-stair ledge is removed).
    box(37,15,32,50,22,32,'pink_terracotta')
    clear_furnishings(37,13,50,31,15)
    for x in (41,42):
        put(x,15,24,'blue_bed',facing='north',part='foot',occupied='false')
        put(x,15,23,'blue_bed',facing='north',part='head',occupied='false')
    for x in (40,43): put(x,15,23,'barrel',facing='south',open='false');put(x,16,23,'lantern',hanging='false',waterlogged='false')
    for z in (15,16,17): box(50,15,z,50,17,z,'barrel',facing='west',open='false')
    for x in (45,46): put(x,15,30,'coraline:rosewood_table',east=str(x==45).lower(),west=str(x==46).lower())
    put(45,15,29,'coraline:rosewood_chair',facing='south')
    for z in (25,26,27): put(49,15,z,'coraline:rosewood_sofa',facing='west')
    for z in (26,27,28): put(37,15,z,'bookshelf');put(37,16,z,'bookshelf')
    # Purpose-built upstairs bathrooms, including the parents' connecting door.
    for x0,z0,x1,z1 in [(14,59,22,67),(43,33,50,47)]:
        box(x0,15,z0,x1,19,z1,'air')
        for x in range(x0,x1+1):
            for z in range(z0,z1+1): put(x,14,z,'white_concrete' if (x+z)%2 else 'polished_andesite')
        for x in (x0+1,x0+3):
            put(x,15,z0,'coraline:rosewood_sink',facing='south');put(x,17,z0,'coraline:rosewood_mirror',facing='south')
        put(x1-1,15,z0+3,'coraline:rosewood_toilet',facing='west')
        put(x1-1,15,z1-2,'coraline:rosewood_bath',facing='north');put(x1-1,15,z1-1,'coraline:rosewood_bath',facing='south')
    box(14,15,58,22,22,58,'white_concrete')
    for y,half in [(15,'lower'),(16,'upper')]: put(21,y,58,'spruce_door',facing='north',half=half,hinge='left',open='false',powered='false')
    # Repair any bed pairs that crossed the removed repeated expansion strips.
    for p,b in list(grid.items()):
        if b['id'].endswith('_bed') and b['y']==15 and 14<=b['x']<=30 and 37<=b['z']<=47: air(*p)
    for y,z,part in [(15,42,'head'),(15,43,'foot')]: put(21,y,z,'red_bed',facing='north',part=part,occupied='false')
    # Fill narrow wall-end gaps where an interior partition should meet the shell.
    for low in (6,15):
        for z in (32,36,48,52,58):
            for x in (14,30,37,50):
                for y in range(low,low+8):
                    if ident((x,y,z))=='minecraft:air' and any(ident((x+dx,y,z+dz)) in ('minecraft:white_concrete','minecraft:pink_terracotta') for dx,dz in DIRECTIONS.values()): put(x,y,z,'pink_terracotta')

    # Replace jagged primary-colour carpet maps with restrained rectangular rugs.
    for x in range(14,51):
        for z in range(13,68):
            for y in (5,14,23):
                if y==14 and 23<=x<=25 and 28<=z<=30: continue
                if ident((x,y,z))=='minecraft:air': put(x,y,z,'spruce_planks')
    for p,b in list(grid.items()):
        if b['y'] in (6,15) and b['id'].endswith('_carpet'): air(*p)
    def rug(x0,z0,x1,z1,y,center='brown_carpet',edge='gray_carpet'):
        for x in range(x0,x1+1):
            for z in range(z0,z1+1):
                if ident((x,y,z))=='minecraft:air' and ident((x,y-1,z))!='minecraft:air': put(x,y,z,edge if x in (x0,x1) or z in (z0,z1) else center)
    rug(32,14,35,54,6,'gray_carpet','brown_carpet')
    rug(32,14,35,66,15,'gray_carpet','brown_carpet')
    rug(20,39,27,49,6)
    rug(16,55,25,64,6,'blue_carpet','gray_carpet')
    rug(39,21,46,27,15,'blue_carpet','light_gray_carpet')
    rug(17,50,26,55,15)
    rug(39,54,47,60,15)
    rug(39,29,47,43,6)

    # The end-hall mirror must be opaque, not a glass window into another room.
    for (x,y,z),b in list(grid.items()):
        if 32<=x<=35 and 54<=z<=56 and 6<=y<=13 and b['id']=='minecraft:glass_pane': put(x,y,z,'packed_ice')

    # Every doorway gets a solid threshold and a clear two-block-wide approach.
    for (x,y,z),b in list(grid.items()):
        if not b['id'].endswith('_door') or b['properties'].get('half')!='lower': continue
        dx,dz=DIRECTIONS[b['properties']['facing']]
        for distance in (-2,-1,1,2):
            xx,zz=x+dx*distance,z+dz*distance
            if ident((xx,y-1,zz))=='minecraft:air': put(xx,y-1,zz,'spruce_planks')
            for yy in range(y,y+3):
                if not ident((xx,yy,zz)).endswith('_door'): air(xx,yy,zz)
            if ident((xx,y+3,zz))=='minecraft:lantern': air(xx,y+3,zz)
        for dx,dz in DIRECTIONS.values():
            for yy in (y,y+1):
                if ident((x+dx,yy,z+dz)).endswith('_trapdoor'): air(x+dx,yy,z+dz)

    # Break the stretched front-porch pillar wall into individual supports.
    for x in range(20,29):
        if x==24: continue
        for y in range(6,13):
            if ident((x,y,5))=='minecraft:stripped_birch_log': air(x,y,5)
    # Continuous porch deck, no projecting slab benches pinching its narrow aisle.
    for x in range(16,59):
        for z in range(5,12):
            if (x,5,z) not in grid: put(x,5,z,'spruce_planks')
            b=grid.get((x,6,z),{})
            if b.get('phase')=='fittings' and b.get('id','').endswith(('_slab','_stairs')):
                air(x,6,z)
                if 'potted_' in ident((x,7,z)) or ident((x,7,z))=='minecraft:flower_pot': air(x,7,z)
    for x in range(16,59):
        put(x,5,5,'stripped_birch_log',axis='x')
        if 30<=x<=38:
            if ident((x,6,5)).endswith('_fence'): air(x,6,5)
        elif ident((x,6,5))=='minecraft:air': put(x,6,5,'oak_fence',north='false',east='true',south='false',west='true',waterlogged='false')
    # Repair side/rear decks, railings and actual two-step exits to the ground.
    for x0,x1,z0,z1 in [(60,62,5,52),(52,62,52,62),(12,22,69,73)]:
        for x in range(x0,x1+1):
            for z in range(z0,z1+1):
                put(x,4,z,'stone_bricks');put(x,5,z,'spruce_planks')
                for y in (6,7):
                    if not ident((x,y,z)).endswith('_door'): air(x,y,z)
    def railing(x,z): put(x,6,z,'oak_fence',north='true',east='true',south='true',west='true',waterlogged='false')
    for z in range(5,63):
        if z not in (56,57): railing(62,z)
    for x in range(52,63): railing(x,62)
    for z in range(69,74): railing(12,z);railing(22,z)
    for x in range(12,23):
        if x not in (16,17): railing(x,73)
    for z in (56,57):
        put(63,4,z,'spruce_stairs',facing='west',half='bottom',shape='straight',waterlogged='false')
        put(64,3,z,'spruce_stairs',facing='west',half='bottom',shape='straight',waterlogged='false')
    for x in (16,17):
        put(x,4,74,'spruce_stairs',facing='north',half='bottom',shape='straight',waterlogged='false')
        put(x,3,75,'spruce_stairs',facing='north',half='bottom',shape='straight',waterlogged='false')
    # Exterior kitchen/house shell: seal accidental missing wall cells while
    # preserving doors, window panes, and the projecting front turret interior.
    door_cells={p for p,b in grid.items() if b['id'].endswith('_door')}
    for low in (6,15):
        boundary={(13 if z<50 else 10,z) for z in range(12,69)}|{(51,z) for z in range(12,69)}|{(x,z) for x in range(13 if z==12 else 10,52) for z in (12,68)}
        for x,z in boundary:
            if x==51 and z<20: continue
            for y in range(low,low+8):
                if (x,y,z) not in grid and not any((x+dx,y,z+dz) in door_cells for dx,dz in DIRECTIONS.values()): put(x,y,z,'pink_terracotta')
    # Coherent facade palette: repeated timber bays, pink infill, masonry base.
    for p,b in list(grid.items()):
        x,y,z=p
        if not (x in (13,51) or z in (12,68)) or b['id'] not in ('minecraft:stripped_birch_log','minecraft:pink_terracotta'): continue
        low=6 if 6<=y<=13 else 15 if 15<=y<=22 else None
        if low is None: continue
        coordinate=z-12 if x in (13,51) else x-13
        if any('glass' in ident((x+dx,y,z+dz)) or ident((x+dx,y,z+dz)).endswith('_door') for dx,dz in DIRECTIONS.values()): continue
        if coordinate%6==0: put(x,y,z,'stripped_dark_oak_log',axis='y')
        elif y==low+7: put(x,y,z,'stripped_dark_oak_log',axis='z' if x in (13,51) else 'x')
        elif y==low: put(x,y,z,'dark_oak_planks')
        else: put(x,y,z,'pink_terracotta')
    # Thin ivy accents, rather than a cloned sheet covering entire wall bays.
    for p,b in list(grid.items()):
        if b['id']=='minecraft:vine' and ((b['z']%7!=0 and b['x']%7!=0) or b['y']>18): air(*p)
    # Proper glass doors in the conservatory, and clear/floored approaches to
    # exterior doors instead of fences or rising steps immediately in their face.
    for p,b in list(grid.items()):
        x,y,z=p
        if not b['id'].endswith('_door') or not (6<=y<=7): continue
        if x==51 and 20<=z<=51 or z==51 and 52<=x<=58: b['id']='coraline:rosewood_glass_door'
        if b['properties'].get('half')!='lower': continue
        if not (x in (13,51) or z in (12,68) or x>=52): continue
        dx,dz=DIRECTIONS.get(b['properties'].get('facing'),(0,0))
        for distance in (1,2):
            for side in (-1,0,1):
                xx,zz=x+dx*distance-dz*side,z+dz*distance+dx*side
                put(xx,5,zz,'spruce_planks')
                for yy in (6,7):
                    if not ident((xx,yy,zz)).endswith('_door'): air(xx,yy,zz)
    # Finish sparse tall window columns up to their existing lintel/ceiling.
    for low in (6,15):
        columns={ (x,z) for (x,y,z),b in grid.items() if low<=y<low+8 and b['id']=='minecraft:glass_pane' }
        for x,z in columns:
            levels=[y for y in range(low,low+8) if ident((x,y,z))=='minecraft:glass_pane']
            if len(levels)<3: continue
            for y in range(min(levels),low+8):
                if (x,y,z) not in grid: put(x,y,z,'glass_pane',north='false',east='false',south='false',west='false',waterlogged='false')
    # Replace the distorted stretched pediment with a symmetric timbered gable.
    for x in range(20,45):
        for z in range(4,12):
            for y in range(14,22):
                b=grid.get((x,y,z),{})
                if b.get('phase') in ('roof','frame','walls') or z==5: air(x,y,z)
    for x in range(26,42):
        height=13+min(x-26,41-x)
        for z in range(4,12): put(x,height,z,'dark_oak_stairs',facing='east' if x<=33 else 'west',half='bottom',shape='straight',waterlogged='false')
        for y in range(13,height):
            put(x,y,5,'stripped_birch_log' if x in (33,34) or y in (13,16) else 'pink_terracotta',**({'axis':'y' if x in (33,34) else 'x'} if x in (33,34) or y in (13,16) else {}))
    # Orient every roof stair toward the higher adjacent roof surface.
    for p,b in list(grid.items()):
        if not b['id'].endswith('_stairs') or b.get('phase')!='roof': continue
        x,y,z=p
        if x in DROP_X or z in DROP_Z: continue
        higher=[]
        for name,(dx,dz) in DIRECTIONS.items():
            xx,zz=x+dx,z+dz
            while xx in DROP_X or zz in DROP_Z: xx+=dx;zz+=dz
            if any(grid.get((xx,yy,zz),{}).get('phase')=='roof' for yy in range(y+1,y+5)): higher.append(name)
        if higher:
            facing=b['properties'].get('facing');b['properties']['facing']=facing if facing in higher else higher[0]

    # Buildability invariants, independent of Minecraft rendering.
    # Remove isolated projecting stair-corbel fragments, not the actual roofs
    # or either rebuilt stair flight. These were decorative frame pieces.
    for p,b in list(grid.items()):
        x,y,z=p
        if b.get('phase')=='frame' and b['id'].endswith('_stairs') and (x==12 and z<50 or x==9 and z>=50 or z==69): air(*p)
    # A continuous two-block front porch aisle and complete outer railing.
    box(16,5,6,58,5,7,'spruce_planks')
    box(16,6,6,58,8,7,'air')
    for x in range(16,59):
        if 30<=x<=38: continue
        if not ident((x,6,5)).endswith('_log'): put(x,6,5,'oak_fence',waterlogged='false')
    for x in range(30,39):
        put(x,4,4,'stone_brick_stairs',facing='south',half='bottom',shape='straight',waterlogged='false')
        put(x,5,5,'stone_brick_stairs',facing='south',half='bottom',shape='straight',waterlogged='false')
    # Retire the obsolete rear kitchen stair/path, which led to no doorway.
    for p,b in list(grid.items()):
        x,y,z=p
        if 52<=x<=59 and 69<=z<=82 and y<=5 and (b['id'].endswith('_stairs') or b['id']=='minecraft:gravel'): air(*p)
    # Vanilla pink terracotta reads as brick-red in daylight. Use actual pink
    # concrete consistently, retaining the contrasting timber structure.
    for b in grid.values():
        if b['id']=='minecraft:pink_terracotta': b['id']='minecraft:pink_concrete'
    # Remove the remaining low decorative iron bars and orphan chains in the
    # cleaned rooms; proper stair guards are wooden fences and stay intact.
    for p,b in list(grid.items()):
        x,y,z=p
        if y in (6,7,15,16) and b['id']=='minecraft:iron_bars' and 14<=x<=50 and 13<=z<=67: air(*p)
    # Sweep a standing player's headroom along both flights and the turn.
    # The floor patch previously sealed the top of the stairwell at y=14.
    for (x,y,z),b in list(grid.items()):
        if b['id'].endswith('_stairs') and 23<=x<=30 and 24<=z<=30 and 6<=y<=14:
            for above in range(1,4): air(x,y+above,z)
    for x in range(23,26):
        for z in range(24,27):
            for y in range(11,15): air(x,y,z)
    changed=True
    while changed:
        changed=False
        for (x,y,z),b in list(grid.items()):
            if b['id']=='minecraft:chain' and ident((x,y-1,z))=='minecraft:air': air(x,y,z);changed=True
    for p,b in grid.items():
        if b['id']=='minecraft:gravel' and -1<=b['y']<=3:
            assert (b['x'],b['y']-1,b['z']) in grid, ('unsupported gravel',p)
    assert not any('glass' in b['id'] and b['y']<5 and (b['x'] in (13,51) or b['z'] in (12,68)) for b in grid.values())
    assert all(ident((x,5,z))=='minecraft:spruce_planks' for x in range(14,23) for z in range(20,23))
    print('Runtime repairs:',groups,'low table assemblies; supported terrain, solid cellar walls, fixtures, stage, beams and porch')
    def keep(b):
        x,y,z=b['x'],b['y'],b['z']
        if x in DROP_X or z in DROP_Z: return False
        # The game builder grades local terrain. Do not export an entire flat
        # grass/dirt/cobblestone plot or its monolithic below-ground pedestal.
        if y<=3 and not (13<=x<=51 and 12<=z<=68 or 24<=x<=29 and -1<=z<=12):
            return b['id']=='minecraft:gravel' or b['id'].endswith('_stairs') and b.get('phase')=='runtime_repair' and (x==64 and z in (56,57) or x in (16,17) and z==75)
        return True
    return [b for b in grid.values() if keep(b)]
