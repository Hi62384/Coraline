"""Reference-led wearable meshes for the Luck, Moonstone, Miners and Spidervision sets.

Coordinates are Minecraft pixels: Y down, -Z forward. Every surface follows a
humanoid bone, so these pieces animate with the player and load directly from
assets/coraline/sculpted/*.json in the normal resource build.
"""
import math

import sculpted_coat as g
from dreamweave_model import surface, cord, panel, shell, strap, star, crescent
from reference_mesh_finish import finish

T = math.tau


def cap(name, bone, center, rx, rz, material):
    surface(name, bone, lambda u, v: [center[0] + g.roundrect(u*T, rx, rz, 8)[0]*v,
                                      center[1], center[2] + g.roundrect(u*T, rx, rz, 8)[1]*v],
            24, 4, material)


def trim(bone, points, material='brass', width=.11):
    cord('Raised armor edging', bone, [[x, y, z-.04] for x,y,z in points], width, material)


def gem(name, bone, x, y, z, width=1, height=1.5, palette=('crystal', 'ice', 'violet')):
    """Real raised diamond facets, not a flat painted decal."""
    outline = [[x,y-height,z], [x+width,y,z], [x,y+height,z], [x-width,y,z]]
    for i in range(4):
        g.mesh(name+' facet', bone, [outline[i], outline[(i+1)%4], [x,y,z-.38*width]],
               [[0,1,2]], palette[i%len(palette)])
    trim(bone, outline+[outline[0]], 'ivory', .075)


def charm(bone, x, y, z, size=.45, material='brass'):
    cord('Hanging star chain', bone, [[x,y,z],[x,y+.48,z]], .055, material)
    star(bone,x,y+.75,z,size)


def clover(bone,x,y,z,r=1,material='glow'):
    for dx,dy in ((-.42,-.34),(.42,-.34),(-.42,.38),(.42,.38)):
        g.ellipse('Raised four leaf clover',bone,[x+dx*r,y+dy*r,z],.49*r,.53*r,.1,material)
    cord('Clover stem',bone,[[x,y+.4*r,z-.12],[x+.15*r,y+1.05*r,z-.12]],.07,'leaf')


def hood(bone='head', material='cloth', trim_material='brass', pointed=True):
    """Open front keeps the player's own skin visible."""
    def wall(u,v):
        a=-1.02+u*(math.pi+2.04)
        x,z=g.roundrect(a,5.25,5.25,8)
        return [x,-8.85+9.25*v,z]
    surface('Open faced hood shell',bone,wall,28,10,material)
    cap('Squared hood crown',bone,(0,-8.85,0),5.25,5.25,material)
    for edge in (0,1):trim(bone,[wall(edge,i/22) for i in range(23)],trim_material,.13)
    brow=[(-4.7,-8.8),(-2,-9.2),(0,-9.5 if pointed else -8.8),(2,-9.2),(4.7,-8.8),
          (4.2,-7.5),(0,-8.0),(-4.2,-7.5)]
    panel('Angular hood brow',bone,brow,-5.35,trim_material,.18)


def leg_base(bone, material='under'):
    surface('Articulated trouser sleeve',bone,
            lambda u,v:[max(-1.84,min(1.84,g.roundrect(u*T,2.23,2.3,7)[0])),
                        11.75*v,g.roundrect(u*T,2.23,2.3,7)[1]],24,10,material)


def boot_base(bone, material='under'):
    shell('Squared fitted boot',bone,(0,8.15,-.45),2.12,3.02,3.65,material,8)
    cap('Closed boot top',bone,(0,8.15,-.45),2.12,3.02,material)
    shell('Raised boot sole',bone,(0,11.35,-.55),2.23,3.15,.56,'black',8)


def luck(piece):
    if piece=='helmet':
        hood(material='cloth',trim_material='brass')
        panel('Teal crown patch','head',[(-4.1,-8.9),(-.2,-9.25),(1.0,-5.2),(-4.3,-4.9)],-5.43,'teal')
        panel('Midnight cheek patch','head',[(-4.75,-6.1),(-3.75,-6.3),(-3.1,-.2),(-4.75,.4)],-5.45,'navy')
        g.button('Large hood temple button','head',4.6,-5.4,-1.0,.78,'brass')
        charm('head',4.85,-4.3,-1.05,.44)
        star('head',-2.8,-7.5,-5.56,.38)
        for x in (-3.4,-1.5,.7,2.9):cord('Hood patch stitches','head',[[x,-8.2,-5.5],[x+.14,-7.85,-5.5]],.04,'thread')
    elif piece=='chestplate':
        shell('Midnight stitched undercoat','body',(0,-.1,0),4.7,2.74,11.7,'navy',7)
        cap('Closed coat yoke','body',(0,-.1,0),4.7,2.74,'navy')
        panel('Teal layered chest facing','body',[(-4.15,.1),(4.15,.1),(4.0,8.7),(0,10.5),(-4.0,8.7)],-2.97,'cloth')
        panel('Offset midnight quilt patch','body',[(-4.1,3.1),(-.5,2.7),(-.8,8.7),(-4.0,9.1)],-3.1,'navy')
        panel('Raised medallion backing','body',[(-2,2),(0,1.4),(2,2),(2.35,4.7),(0,6.4),(-2.35,4.7)],-3.35,'brass')
        g.ellipse('Circular clover medallion','body',[0,3.95,-3.64],1.73,1.73,.16,'brass')
        g.ellipse('Enamel medallion inset','body',[0,3.95,-3.84],1.37,1.37,.07,'teal')
        clover('body',0,3.7,-3.99,.86)
        cord('Hanging key loop','body',[[0,5.65,-3.9],[0,6.5,-3.9]],.07)
        g.ellipse('Hanging key bow','body',[0,6.6,-3.9],.24,.27,.07,'brass')
        cord('Hanging brass key','body',[[0,6.8,-3.9],[0,8.4,-3.9],[.38,8.4,-3.9]],.12)
        strap('body',(0,9.0,0),4.78,2.83,.85)
        g.button('Clover belt button','body',0,9.42,-3.17,.62,'brass')
        panel('Leather lucky pouch','body',[(2.25,8.3),(4.55,8.3),(4.5,11.1),(2.4,11.25)],-3.08,'leather')
        clover('body',3.35,9.7,-3.29,.34,'leaf')
        for side in (-1,1):
            bone='left_arm' if side>0 else 'right_arm';x=side
            shell('Dark tailored sleeve',bone,(x,-2.1,0),2.28,2.3,10.25,'under',7)
            shell('Layered teal shoulder guard',bone,(x,-2.65,0),3.03,3.0,2.5,'cloth',8)
            cap('Closed angular shoulder',bone,(x,-2.65,0),3.03,3.0,'teal')
            panel('Gold edged shoulder patch',bone,[(x-2.9,-2.4),(x+2.9,-2.4),(x+2.6,.5),(x,.9),(x-2.8,.45)],-3.13,'teal')
            panel('Sewn navy wrist bracer',bone,[(x-2.45,4.1),(x+2.45,4.1),(x+2.4,8.2),(x-2.4,8.2)],-2.65,'navy')
            for y in (1.5,7.8):strap(bone,(x,y,0),2.6,2.64,.38)
            g.button('Shoulder brass button',bone,x,-1.35,-3.36,.63,'brass')
            g.button('Bracer brass button',bone,x,6.1,-2.96,.59,'brass')
            charm(bone,x+side*2.4,.45,-3.1,.38)
            clover(bone,x,6.2,-3.15,.43)
        def cape(u,v):return [(u-.5)*(8.6+v*5.7),1.1+v*(18.5+.65*math.sin(u*T*7)),3.1+v*3.0]
        surface('Ragged midnight back cape','cape',cape,28,15,'navy')
        surface('Cape teal reverse','cape',lambda u,v:[cape(1-u,v)[0],cape(1-u,v)[1],cape(1-u,v)[2]-.08],28,15,'teal')
        for edge in (0,1):trim('cape',[cape(edge,i/28) for i in range(29)],'brass',.06)
        for u in (.15,.42,.7,.86):
            x,y,z=cape(u,.78);star('cape',x,y,z+.12,.4)
    elif piece=='leggings':
        for side in (-1,1):
            bone='left_leg' if side>0 else 'right_leg';leg_base(bone,'navy')
            panel('Teal patchwork hip flap',bone,[(-1.85,0),(1.85,0),(2,4.8),(.2,5.6),(-1.85,4.9)],-2.65,'cloth')
            panel('Pointed navy tabard',bone,[(-1.4,.2),(1.4,.2),(1.3,5.8),(0,6.5),(-1.4,5.8)],-2.87,'navy')
            panel('Brass trimmed knee guard',bone,[(-1.82,5.25),(1.82,5.25),(2.0,8.0),(0,8.8),(-2.0,8.0)],-2.74,'teal')
            strap(bone,(0,5.55,0),2.16,2.48,.43)
            g.button('Knee brass fastening',bone,0,6.78,-3.09,.74,'brass')
            charm(bone,.2,7.55,-3.17,.32)
            crescent(bone,0,3.0,-3.0,.76)
            star(bone,side*.9,4.1,-3.01,.34)
            for x in (-1.3,1.2):cord('Gold leg seam',bone,[[x,.8,-2.77],[x,4.8,-2.77]],.04,'thread')
    else:
        for side in (-1,1):
            bone='left_leg' if side>0 else 'right_leg';boot_base(bone,'navy')
            shell('Teal ankle panel',bone,(0,8.25,-.45),2.2,3.08,2.9,'cloth',8)
            panel('Brass square toe cap',bone,[(-2.15,10.55),(2.15,10.55),(2.15,11.8),(-2.15,11.8)],-3.75,'brass')
            for y in (8.25,10.8):strap(bone,(0,y,-.45),2.25,3.12,.46)
            g.button('Boot luck button',bone,0,9.35,-3.65,.56,'brass')
            charm(bone,1.3,9.6,-3.65,.31)
            crescent(bone,-.9,10.35,-3.65,.43)


def moonstone(piece):
    if piece=='helmet':
        hood(material='under',trim_material='ivory')
        panel('Opal crown plate','head',[(-4.8,-8.8),(4.8,-8.8),(4.5,-6.45),(0,-7.15),(-4.5,-6.45)],-5.48,'crystal')
        gem('Forehead moonstone','head',0,-8.25,-5.82,1.12,1.35)
        for side in (-1,1):
            panel('Ivory cheek frame','head',[(side*4.9,-6.7),(side*3.5,-6.7),(side*3.2,-.1),(side*4.9,.1)],-5.55,'ivory')
            for j in range(3):
                x=side*(4.15+j*.55)
                gem('Side crystal plume','head',x,-8.4-j*.28,-1.0-j*.24,.35,.95-j*.1)
            charm('head',side*4.7,-4.3,-1.2,.3,'ivory')
    elif piece=='chestplate':
        shell('Deep periwinkle inner armor','body',(0,-.1,0),4.68,2.65,11.65,'under',7)
        cap('Moonstone closed yoke','body',(0,-.1,0),4.68,2.65,'under')
        panel('Faceted moonstone breastplate','body',[(-4,.5),(0,-.2),(4,.5),(4.15,6.9),(0,9.3),(-4.15,6.9)],-3.02,'crystal')
        panel('Lower crystal facet','body',[(-3.7,6.45),(0,8.8),(3.7,6.45),(0,10.2)],-3.22,'violet')
        panel('Ivory pointed breast rim','body',[(-4.15,.15),(0,-.5),(4.15,.15),(4.45,1.0),(0,.35),(-4.45,1.0)],-3.32,'ivory')
        gem('Collar gem','body',0,.55,-3.55,.55,.75)
        crescent('body',0,4.65,-3.5,1.55)
        gem('Lower breast crystal','body',0,8.15,-3.48,.48,.62)
        strap('body',(0,9.15,0),4.73,2.75,.62)
        for side in (-1,1):
            bone='left_arm' if side>0 else 'right_arm';x=side
            shell('Periwinkle sleeve',bone,(x,-2.2,0),2.3,2.32,10.3,'under',7)
            shell('Crystal shoulder mantle',bone,(x,-2.72,0),3.05,3.02,2.65,'crystal',8)
            cap('Crystal shoulder crown',bone,(x,-2.72,0),3.05,3.02,'ice')
            panel('Pointed shoulder facet',bone,[(x-3,-2.5),(x+3,-2.5),(x+2.6,.2),(x,1.15),(x-2.6,.2)],-3.15,'violet')
            panel('Tall crystal bracer',bone,[(x-2.25,3.45),(x+2.25,3.45),(x+2.45,8.2),(x,8.75),(x-2.45,8.2)],-2.66,'crystal')
            for y in (3.55,7.7):strap(bone,(x,y,0),2.34,2.42,.32)
            gem('Bracer diamond',bone,x,5.8,-3.02,.52,1.18)
            crescent(bone,x,-1,-3.56,.75)
            charm(bone,x+side*2.4,.15,-3.28,.35,'ivory')
        for side in (-1,1):
            bone='left_leg' if side>0 else 'right_leg'
            panel('Layered pointed waist crystal',bone,[(-1.8,-.2),(1.8,-.2),(1.8,3.6),(0,5.3),(-1.8,3.6)],-2.78,'crystal')
            gem('Waist facet',bone,0,2.1,-3.0,.4,.9)
    elif piece=='leggings':
        for side in (-1,1):
            bone='left_leg' if side>0 else 'right_leg';leg_base(bone,'under')
            panel('Layered opal hip plate',bone,[(-1.85,0),(1.85,0),(1.9,4.4),(0,5.5),(-1.85,4.5)],-2.67,'crystal')
            panel('Pointed violet tabard',bone,[(-1.15,.2),(1.15,.2),(1.15,5.0),(0,6.3),(-1.15,5.0)],-2.9,'violet')
            panel('Knee crystal shield',bone,[(-1.8,5.3),(1.8,5.3),(1.88,8.2),(0,9.2),(-1.88,8.2)],-2.72,'crystal')
            gem('Raised knee diamond',bone,0,7,-3.1,.92,1.25)
            panel('Iridescent shin plate',bone,[(-1.7,8),(1.7,8),(1.7,11.55),(-1.7,11.55)],-2.68,'ice')
            crescent(bone,0,9.7,-3.0,.55)
            strap(bone,(0,5.5,0),2.05,2.42,.34)
    else:
        for side in (-1,1):
            bone='left_leg' if side>0 else 'right_leg';boot_base(bone,'under')
            panel('Full crystal shin guard',bone,[(-2.05,8.05),(2.05,8.05),(2.18,10.9),(0,11.55),(-2.18,10.9)],-3.51,'crystal')
            panel('Faceted square moonstone toe',bone,[(-2.12,10.85),(2.12,10.85),(2.12,11.82),(-2.12,11.82)],-3.84,'ice')
            for y in (8.2,10.9):strap(bone,(0,y,-.45),2.23,3.12,.35)
            gem('Boot crystal diamond',bone,0,9.32,-3.8,.5,.7)
            crescent(bone,1.1,10.5,-3.72,.36)


def lantern(bone,x,y,z,scale=1):
    g.ellipse('Lantern brass housing',bone,[x,y,z],.72*scale,1.05*scale,.17*scale,'brass')
    g.ellipse('Warm lantern glass',bone,[x,y,z-.2*scale],.53*scale,.75*scale,.11*scale,'amber')
    for dx in (-.37,.37):cord('Lantern safety cage',bone,[[x+dx*scale,y-.65*scale,z-.35*scale],[x+dx*scale,y+.65*scale,z-.35*scale]],.065,'brass')


def miners(piece):
    if piece=='helmet':
        shell('Squared dark miner helmet','head',(0,-9,0),5.2,5.18,9.4,'cloth',8)
        cap('Closed miner crown','head',(0,-9,0),5.2,5.18,'cloth')
        panel('Black recessed face','head',[(-4.55,-6.5),(4.55,-6.5),(4.55,.1),(-4.55,.1)],-5.24,'black')
        panel('Forehead leather plate','head',[(-5.15,-8.9),(5.15,-8.9),(5.15,-6.5),(-5.15,-6.5)],-5.42,'leather')
        for y in (-8.8,-1.1):strap('head',(0,y,0),5.28,5.28,.55)
        lantern('head',0,-7.65,-5.95,1.25)
        for x in (-4.7,4.7):g.button('Helmet side button','head',x,-5.1,-2.0,.58,'button')
        charm('head',-4.6,-4.2,-2.1,.31)
        for x in (-3,-1,1):star('head',x,-8.3,-5.54,.3)
    elif piece=='chestplate':
        shell('Dark miner under tunic','body',(0,-.1,0),4.74,2.73,11.7,'under',8)
        cap('Miner closed yoke','body',(0,-.1,0),4.74,2.73,'under')
        panel('Patchwork work vest','body',[(-4.25,.1),(4.25,.1),(4.2,9.7),(0,11.1),(-4.2,9.7)],-3.02,'cloth')
        panel('Ragged violet neck scarf','body',[(-4.3,0),(0,.8),(4.3,0),(3.2,2.2),(0,3),(-3.2,2.2)],-3.32,'violet')
        star('body',0,1.75,-3.53,.58)
        panel('Diagonal leather tool strap','body',[(-3.9,.25),(-2.8,-.2),(3.9,8.7),(2.8,9.2)],-3.45,'leather')
        panel('Strap brass buckle','body',[(-.5,3.6),(.9,4.45),(.1,5.7),(-1.3,4.85)],-3.6,'brass')
        strap('body',(0,8.95,0),4.8,2.84,.88)
        g.button('Belt brass button','body',0,9.35,-3.22,.62,'button')
        lantern('body',-3.1,10.5,-3.7,.75)
        panel('Miner crystal pouch','body',[(2.35,8.35),(4.45,8.35),(4.45,11.55),(2.35,11.55)],-3.2,'leather')
        for x,y in ((2.8,8.55),(3.4,8.25),(4.0,8.5)):gem('Blue mineral shard','body',x,y,-3.36,.23,.48,('blue','ice','blue'))
        for side in (-1,1):
            bone='left_arm' if side>0 else 'right_arm';x=side
            shell('Heavy miner sleeve',bone,(x,-2.1,0),2.32,2.34,10.3,'under',8)
            shell('Boxed shoulder harness',bone,(x,-2.65,0),3.02,3.0,2.8,'leather',8)
            cap('Miner shoulder top',bone,(x,-2.65,0),3.02,3.0,'brass')
            panel('Weathered shoulder fabric',bone,[(x-2.9,-2.4),(x+2.9,-2.4),(x+2.75,.45),(x-2.75,.45)],-3.14,'cloth')
            panel('Heavy forearm guard',bone,[(x-2.45,3.45),(x+2.45,3.45),(x+2.45,8.2),(x-2.45,8.2)],-2.74,'leather')
            for y in (1.4,4.1,7.8):strap(bone,(x,y,0),2.49,2.52,.37)
            g.button('Miner shoulder button',bone,x,-.95,-3.38,.57,'button')
            star(bone,x,5.8,-3.06,.36)
    elif piece=='leggings':
        for side in (-1,1):
            bone='left_leg' if side>0 else 'right_leg';leg_base(bone,'under')
            panel('Miner dark quilted thigh',bone,[(-1.85,0),(1.85,0),(1.85,5.6),(-1.85,5.6)],-2.6,'cloth')
            panel('Ragged work tabard',bone,[(-1.4,.2),(1.45,.2),(1.2,5.7),(0,6.4),(-1.3,5.7)],-2.9,'violet')
            crescent(bone,0,3.35,-3.08,.55)
            panel('Miner knee guard',bone,[(-1.85,5.45),(1.85,5.45),(1.85,8.4),(-1.85,8.4)],-2.73,'leather')
            for y in (5.55,8.05):strap(bone,(0,y,0),2.05,2.43,.45)
            g.button('Knee work button',bone,0,6.7,-3.08,.69,'button')
            star(bone,side*.95,9.6,-2.56,.35)
    else:
        for side in (-1,1):
            bone='left_leg' if side>0 else 'right_leg';boot_base(bone,'cloth')
            shell('Boxed leather boot reinforcement',bone,(0,8.2,-.45),2.2,3.12,3.1,'leather',8)
            panel('Brass reinforced work toe',bone,[(-2.18,10.7),(2.18,10.7),(2.18,11.8),(-2.18,11.8)],-3.8,'brass')
            for y in (8.25,10.75):strap(bone,(0,y,-.45),2.26,3.13,.43)
            g.button('Boot strap button',bone,0,9.4,-3.67,.53,'button')
            star(bone,-1.15,10.9,-3.8,.28)


def web(bone,x,y,z,rx,ry,material='thread'):
    for i in range(6):
        a=i*T/6;cord('Web spoke',bone,[[x,y,z],[x+rx*math.cos(a),y+ry*math.sin(a),z]],.035,material)
    for r in (.36,.67,1):
        cord('Web strand',bone,[[x+rx*r*math.cos(i*T/30),y+ry*r*math.sin(i*T/30),z] for i in range(31)],.03,material)


def spider(bone,x,y,z,size=1):
    g.ellipse('Raised spider abdomen',bone,[x,y+.45*size,z],.42*size,.63*size,.1,'thread')
    g.ellipse('Raised spider head',bone,[x,y-.28*size,z],.32*size,.33*size,.1,'thread')
    for side in (-1,1):
        for i in range(4):
            cord('Embroidered spider leg',bone,[[x+side*.25*size,y+(i-.9)*.22*size,z-.1],
                                                [x+side*(.75+i*.12)*size,y+(i-1.2)*.45*size,z-.1],
                                                [x+side*(1.0+i*.1)*size,y+(i-1.2)*.7*size,z-.1]],.047,'thread')


def spidervision(piece):
    if piece=='helmet':
        shell('Squared spider cowl','head',(0,-9,0),5.2,5.2,9.35,'cloth',8)
        cap('Closed spider cowl crown','head',(0,-9,0),5.2,5.2,'cloth')
        panel('Dark recessed spider face','head',[(-4.7,-6.85),(4.7,-6.85),(4.7,.2),(-4.7,.2)],-5.32,'black')
        panel('Ragged violet brow cloth','head',[(-2.1,-9.0),(2.1,-9.0),(1.6,-6.1),(0,-5.65),(-1.6,-6.1)],-5.49,'violet')
        for side in (-1,1):
            for x,y,r in ((2.15,-6.0,.58),(3.1,-4.95,.48),(1.35,-4.6,.45)):
                g.ellipse('Luminous spider eye','head',[side*x,y,-5.58],r,r,.14,'glow')
            g.button('Temple spider button','head',side*4.3,-7.2,-3.2,.55,'button')
            panel('Hanging ivory fang','head',[(side*3.6,-2.9),(side*2.65,-2.9),(side*3.1,-.2)],-5.62,'ivory')
            charm('head',side*4.6,-3.8,-2.5,.27)
    elif piece=='chestplate':
        shell('Dark woven spider cuirass','body',(0,-.1,0),4.74,2.72,11.7,'cloth',8)
        cap('Spider cuirass closed yoke','body',(0,-.1,0),4.74,2.72,'cloth')
        panel('Ragged violet spider bib','body',[(-3.4,.6),(3.4,.6),(3.2,10.1),(0,11.5),(-3.2,10.1)],-3.15,'violet')
        crescent('body',0,2.65,-3.39,.84)
        spider('body',0,6.3,-3.4,1.3)
        strap('body',(0,9.3,0),4.8,2.84,.9)
        g.ellipse('Spider belt medallion','body',[0,9.7,-3.22],.9,.9,.16,'brass')
        spider('body',0,9.7,-3.43,.39)
        lantern('body',3.3,10.6,-3.56,.56)
        for side in (-1,1):
            bone='left_arm' if side>0 else 'right_arm';x=side
            shell('Black jointed spider sleeve',bone,(x,-2.1,0),2.32,2.34,10.25,'under',8)
            shell('Layered spider shoulder',bone,(x,-2.65,0),3.04,3.02,2.85,'cloth',8)
            cap('Spider shoulder crown',bone,(x,-2.65,0),3.04,3.02,'navy')
            panel('Ragged shoulder veil',bone,[(x-3,-2.3),(x+3,-2.3),(x+2.65,1.55),(x,2.15),(x-2.65,1.55)],-3.22,'violet')
            web(bone,x,-.7,-3.37,2.5,2.1)
            g.button('Shoulder web button',bone,x,-1.1,-3.54,.55,'button')
            panel('Dark gauntlet',bone,[(x-2.45,3.65),(x+2.45,3.65),(x+2.45,8.15),(x-2.45,8.15)],-2.7,'cloth')
            strap(bone,(x,7.7,0),2.47,2.48,.52)
            web(bone,x,5.7,-2.98,1.9,1.6)
        def cape(u,v):return [(u-.5)*(8.5+v*4.6),1.2+v*(18.2+1.1*math.sin(u*T*5)),3.1+v*2.8]
        surface('Tattered violet spider cloak','cape',cape,26,14,'violet')
        surface('Spider cloak reverse','cape',lambda u,v:[cape(1-u,v)[0],cape(1-u,v)[1],cape(1-u,v)[2]-.07],26,14,'navy')
        x,y,z=cape(.5,.56);spider('cape',x,y,z+.15,1.25)
        web('cape',0,4.2,3.95,2.9,2.1)
    elif piece=='leggings':
        for side in (-1,1):
            bone='left_leg' if side>0 else 'right_leg';leg_base(bone,'cloth')
            panel('Torn violet thigh cloth',bone,[(-1.85,.1),(1.85,.1),(1.85,5.4),(0,6),(-1.85,5.4)],-2.7,'violet')
            panel('Spider knee armor',bone,[(-1.85,5.3),(1.85,5.3),(1.85,8.5),(-1.85,8.5)],-2.83,'navy')
            web(bone,0,7.0,-3.04,1.55,1.25)
            g.button('Spider knee button',bone,side*.65,6.7,-3.12,.5,'button')
            for y in (2.1,8.0):strap(bone,(0,y,0),2.1,2.48,.36)
    else:
        for side in (-1,1):
            bone='left_leg' if side>0 else 'right_leg';boot_base(bone,'cloth')
            shell('Reinforced spider boot',bone,(0,8.15,-.45),2.2,3.1,3.45,'navy',8)
            panel('Blue glowing boot rune',bone,[(-.45,10.1),(.45,10.1),(.45,11.7),(-.45,11.7)],-3.82,'glow')
            web(bone,0,9.35,-3.68,1.8,.95)
            for y in (8.35,11.2):strap(bone,(0,y,-.45),2.23,3.14,.4)
            g.button('Spider boot button',bone,side*1.3,9.2,-3.6,.42,'button')


def beldam_weave(piece):
    if piece=='helmet':
        shell('Black beldam hood','head',(0,-9.0,0),5.22,5.22,9.5,'cloth',8)
        cap('Closed beldam hood crown','head',(0,-9.0,0),5.22,5.22,'cloth')
        panel('Ivory cracked doll mask','head',[(-3.65,-7.5),(0,-8.25),(3.65,-7.5),(3.8,-1.15),(0,.25),(-3.8,-1.15)],-5.44,'mask')
        panel('Mask jaw seam','head',[(-3,-1.3),(0,-.25),(3,-1.3),(2.1,.1),(0,.62),(-2.1,.1)],-5.6,'bone')
        for side in (-1,1):
            g.button('Black doll button eye','head',side*1.8,-4.95,-5.72,.76,'black')
            panel('Pointed web crown','head',[(side*2.65,-8.8),(side*4.3,-11.9),(side*5.15,-8.25)],-5.38,'bone')
            web('head',side*4.2,-9.5,-5.46,1.15,1.85,'thread')
            charm('head',side*4.9,-7.55,-2.3,.35)
            panel('Torn hood side drape','head',[(side*4.9,-7.4),(side*5.5,-7.1),(side*5.9,6.2),(side*4.4,4.7)],-4.8,'moss')
        for x in (-2.65,-1.0,.8,2.5):
            cord('Cracked mask seam','head',[[x,-7.35,-5.63],[x+.22,-6.5,-5.66],[x-.1,-5.95,-5.67]],.025,'bone')
    elif piece=='chestplate':
        shell('Shadow woven bodice','body',(0,-.1,0),4.73,2.7,11.75,'cloth',8)
        cap('Closed shadow bodice','body',(0,-.1,0),4.73,2.7,'cloth')
        panel('Ragged green bodice facing','body',[(-4.15,.2),(4.15,.2),(4.05,9.7),(0,11.4),(-4.05,9.7)],-3.08,'moss')
        panel('Dark pointed center vest','body',[(-2.4,.35),(2.4,.35),(2.25,10.2),(0,11.7),(-2.25,10.2)],-3.22,'cloth')
        gem('Beldam collar jewel','body',0,1.35,-3.48,.58,.82,('glow','green','glow'))
        spider('body',0,4.45,-3.54,.85)
        cord('Hanging old brass key','body',[[0,5.5,-3.45],[0,7.3,-3.45],[.4,7.3,-3.45]],.11,'brass')
        strap('body',(0,9.5,0),4.8,2.84,.67)
        for side in (-1,1):
            bone='left_arm' if side>0 else 'right_arm';x=side
            shell('Black beldam sleeve',bone,(x,-2.1,0),2.31,2.32,10.3,'under',8)
            shell('Pointed shoulder structure',bone,(x,-2.6,0),3.1,3.05,3.15,'cloth',8)
            cap('Closed shoulder frame',bone,(x,-2.6,0),3.1,3.05,'bone')
            panel('Webbed pointed shoulder wing',bone,[(x-3.1,-2.5),(x+3.1,-2.5),(x+3.75,-4.55),(x+2.7,1.1),(x-2.7,1.1)],-3.16,'moss')
            web(bone,x,-.7,-3.38,2.9,2.15,'thread')
            panel('Hanging sleeve tatters',bone,[(x-2.6,2.8),(x+2.6,2.8),(x+2.15,8.6),(x,9.4),(x-2.1,8.4)],-2.72,'moss')
            shell('Ivory bracer cuff',bone,(x,7.65,0),2.48,2.52,.52,'bone',8)
            charm(bone,x+side*2.8,1.2,-3.2,.25)
            lantern(bone,x+side*2.75,5.65,-1.8,.5)
        def cape(u,v):
            return [(u-.5)*(8.6+v*8.2),.7+v*(22.4+2.0*math.sin(u*T*8)),3.08+v*4.3]
        surface('Long tattered beldam veil','cape',cape,30,18,'cloth')
        surface('Green torn veil reverse','cape',lambda u,v:[cape(1-u,v)[0],cape(1-u,v)[1],cape(1-u,v)[2]-.07],30,18,'moss')
        for u in (.08,.28,.5,.73,.9):
            x,y,z=cape(u,.78);web('cape',x,y,z+.12,.62,.55,'thread')
        for side in (-1,1):
            bone='left_leg' if side>0 else 'right_leg'
            panel('Layered front long coat tail',bone,[(-1.85,0),(1.85,0),(2.25,10.4),(0,11.9),(-2.15,10.5)],-2.82,'moss')
            panel('Torn black overcloth',bone,[(-1.7,.25),(1.7,.25),(1.4,9.2),(0,10.55),(-1.4,9.15)],-3.0,'cloth')
            web(bone,0,6.1,-3.15,1.4,1.7,'thread')
    elif piece=='leggings':
        for side in (-1,1):
            bone='left_leg' if side>0 else 'right_leg';leg_base(bone,'under')
            panel('Long torn beldam skirt strip',bone,[(-1.85,.1),(1.85,.1),(2.2,10.6),(0,11.75),(-2.2,10.7)],-2.68,'cloth')
            panel('Weathered green side strip',bone,[(side*.15,0),(side*1.8,0),(side*2.1,10),(side*.45,10.9)],-2.88,'moss')
            for x in (-1.2,1.15):cord('X stitched skirt seam',bone,[[x,.5,-3.0],[x+.2,8.8,-3.0]],.035,'thread')
            web(bone,0,8.8,-3.13,1.55,1.25,'thread')
            g.button('Dangling skirt button',bone,side*1.05,3.5,-3.1,.48,'button')
    else:
        for side in (-1,1):
            bone='left_leg' if side>0 else 'right_leg';boot_base(bone,'cloth')
            shell('Beldam green edged boot',bone,(0,8.15,-.45),2.19,3.1,3.25,'moss',8)
            panel('Ivory pointed boot welt',bone,[(-2.15,10.85),(0,11.7),(2.15,10.85),(2.15,11.8),(-2.15,11.8)],-3.8,'bone')
            web(bone,0,9.55,-3.64,1.7,1.05,'thread')
            strap(bone,(0,8.25,-.45),2.25,3.16,.37)
            charm(bone,side*1.5,9.8,-3.55,.27)


def soul(bone,x,y,z,size=.7):
    g.ellipse('Pale captured soul face',bone,[x,y,z],size*.45,size*.48,.05,'soul')
    panel('Fading soul tail',bone,[(x-size*.33,y+size*.25),(x+size*.33,y+size*.25),
                                   (x+size*.25,y+size*.95),(x-size*.12,y+size*1.45),
                                   (x+size*.03,y+size*.75),(x-size*.35,y+size*1.05)],z+.02,'soul',.04)
    for dx in (-.17,.17):g.ellipse('Soul dark eye',bone,[x+dx*size,y-.06*size,z-.1],size*.07,size*.09,.02,'black')


def soulbound(piece):
    if piece=='helmet':
        shell('Dark soulbound hood','head',(0,-9,0),5.22,5.22,9.35,'cloth',8)
        cap('Closed soulbound hood crown','head',(0,-9,0),5.22,5.22,'cloth')
        panel('Bone white owl mask','head',[(-3.65,-7.7),(0,-8.5),(3.65,-7.7),
                                            (3.45,-2.1),(0,-.1),(-3.45,-2.1)],-5.44,'bone')
        panel('Owl mask pointed beak','head',[(-.6,-3.6),(.6,-3.6),(0,-1.2)],-5.58,'ivory')
        for side in (-1,1):
            g.button('Dark stitched mask eye','head',side*1.85,-5.25,-5.7,.82,'button')
            panel('Torn mask cheek','head',[(side*3.45,-5.35),(side*4.2,-5.2),(side*4.65,-.3),
                                          (side*2.45,.25)],-5.48,'cloth')
        for x in (-2.5,-.9,.7,2.7):cord('Mask cracks','head',[[x,-7.3,-5.58],[x+.3,-6.6,-5.6],[x+.1,-5.9,-5.6]],.028,'bone')
        soul('head',-4.4,-3.0,-2.1,.43)
    elif piece=='chestplate':
        shell('Dark soulbound vest','body',(0,-.1,0),4.68,2.72,11.7,'cloth',8)
        cap('Soulbound closed yoke','body',(0,-.1,0),4.68,2.72,'cloth')
        panel('Torn bone edged tunic','body',[(-4.15,.1),(4.15,.1),(4.0,9.5),(0,11.6),(-4.0,9.5)],-3.05,'navy')
        panel('Faded soul patch','body',[(-2.6,1.15),(2.6,1.15),(2.35,9.75),(0,11.1),(-2.35,9.75)],-3.22,'cloth')
        cord('Cross body key chain','body',[[-3.25,1.15,-3.48],[-1,2.5,-3.48],[2.8,6.8,-3.48]],.065,'brass')
        for x,y in ((-2.2,4.4),(1.4,7.0)):
            g.ellipse('Dangling antique key head','body',[x,y,-3.65],.28,.28,.07,'brass')
            cord('Dangling antique key','body',[[x,y+.25,-3.65],[x,y+1.65,-3.65],[x+.38,y+1.65,-3.65]],.105,'brass')
        soul('body',1.2,3.3,-3.48,.61)
        for side in (-1,1):
            bone='left_arm' if side>0 else 'right_arm';x=side
            shell('Dark captive soul sleeve',bone,(x,-2.1,0),2.3,2.32,10.2,'under',8)
            shell('Layered soulbound shoulder',bone,(x,-2.6,0),2.86,2.84,2.45,'cloth',8)
            cap('Shoulder top',bone,(x,-2.6,0),2.86,2.84,'navy')
            panel('Ghost shoulder patch',bone,[(x-2.8,-2.4),(x+2.8,-2.4),(x+2.75,.4),(x-2.75,.4)],-3.0,'navy')
            soul(bone,x,-.95,-3.2,.58)
            panel('Arm bone edged bracer',bone,[(x-2.3,4.0),(x+2.3,4.0),(x+2.3,8.1),(x-2.3,8.1)],-2.68,'cloth')
            for y in (1.0,7.7):strap(bone,(x,y,0),2.48,2.5,.38)
            g.button('Shoulder soul button',bone,x,-1.35,-3.34,.57,'button')
            soul(bone,x,5.35,-2.95,.43)
        def cape(u,v):return [(u-.5)*(8.4+v*3.3),1.1+v*(18.8+1.3*math.sin(u*T*6)),3.0+v*3.15]
        surface('Tattered captive soul cape','cape',cape,26,15,'navy')
        surface('Cape dark reverse','cape',lambda u,v:[cape(1-u,v)[0],cape(1-u,v)[1],cape(1-u,v)[2]-.07],26,15,'cloth')
        for u in (.18,.55,.82):
            x,y,z=cape(u,.63);soul('cape',x,y,z+.14,.62)
    elif piece=='leggings':
        for side in (-1,1):
            bone='left_leg' if side>0 else 'right_leg';leg_base(bone,'under')
            panel('Tattered soulbound leg cloth',bone,[(-1.82,.1),(1.82,.1),(1.8,9.75),(0,11.45),(-1.8,9.7)],-2.66,'cloth')
            panel('Pale worn leg patch',bone,[(-1.4,2),(1.35,2),(1.25,5.8),(0,6.5),(-1.35,5.8)],-2.9,'navy')
            soul(bone,side*.45,7.7,-3.04,.52)
            for y in (2.2,8.2):strap(bone,(0,y,0),2.05,2.4,.34)
            g.button('Soulbound leg button',bone,side*1.3,3.65,-3.1,.43,'button')
    else:
        for side in (-1,1):
            bone='left_leg' if side>0 else 'right_leg';boot_base(bone,'under')
            shell('Dark soulbound boot',bone,(0,8.2,-.45),2.17,3.1,3.3,'cloth',8)
            panel('Pale toe strip',bone,[(-2.05,10.8),(2.05,10.8),(2.05,11.75),(-2.05,11.75)],-3.68,'bone')
            strap(bone,(0,8.25,-.45),2.25,3.16,.37)
            soul(bone,0,9.6,-3.62,.43)
            g.button('Boot button',bone,side*1.3,8.9,-3.55,.42,'button')


def super_spidervision(piece):
    # Shares the reinforced spider underlayer, then adds the extra eye, web
    # arches, lanterns and long veil that distinguish the upgraded set.
    spidervision(piece)
    if piece=='helmet':
        gem('Crown vertical other-eye','head',0,-9.65,-5.65,.86,1.45,('violet','glow','violet'))
        for side in (-1,1):
            for x,y,r in ((1.45,-7.2,.38),(3.2,-6.95,.43)):
                g.ellipse('Extra violet spider eye','head',[side*x,y,-5.7],r,r,.13,'glow')
            for j in range(3):
                x=side*(2.8+j*.95)
                panel('Web crown spear','head',[(x-.3,-8.9),(x,-11.4-j*.25),(x+.3,-8.9)],-5.35,'brass')
            web('head',side*3.4,-8.65,-5.5,1.5,1.55,'thread')
    elif piece=='chestplate':
        gem('Chest other-eye','body',0,4.1,-3.65,.8,1.2,('violet','glow','violet'))
        web('body',0,4.25,-3.75,2.45,2.4,'thread')
        for side in (-1,1):
            bone='left_arm' if side>0 else 'right_arm';x=side
            # Long spined shoulder arch follows the animated arm.
            for j in range(3):
                y=-4.8+j*.75
                cord('Jointed spider shoulder arch',bone,[[x+side*2.4,-2.5,-1.8],
                                                        [x+side*(4.2+j*.55),y,-1.25],
                                                        [x+side*(5.2+j*.4),.65+j*.5,-1.1]],.18,'brass')
            web(bone,x+side*2.7,-1.4,-2.05,2.15,2.2,'thread')
            lantern(bone,x+side*4.8,2.7,-1.65,.55)
        def veil(u,v):return [(u-.5)*(9.1+v*7.2),1.5+v*(21.5+2*math.sin(u*T*6)),3.15+v*3.6]
        surface('Long ragged violet spider veil','cape',veil,28,15,'violet')
        surface('Spider veil reverse','cape',lambda u,v:[veil(1-u,v)[0],veil(1-u,v)[1],veil(1-u,v)[2]-.06],28,15,'navy')
        x,y,z=veil(.5,.58);spider('cape',x,y,z+.18,1.3)
    elif piece=='leggings':
        for side in (-1,1):
            bone='left_leg' if side>0 else 'right_leg'
            panel('Extra torn violet thigh veil',bone,[(-1.85,.1),(1.85,.1),(2.1,6.4),(0,7.3),(-2.1,6.4)],-3.04,'violet')
            web(bone,0,7.2,-3.18,1.8,1.5,'thread')
            g.button('Violet spider leg button',bone,side*1.35,3.9,-3.22,.48,'button')
    else:
        for side in (-1,1):
            bone='left_leg' if side>0 else 'right_leg'
            panel('Violet luminous boot slash',bone,[(-.45,10.15),(.45,10.15),(.45,11.8),(-.45,11.8)],-3.95,'glow')
            web(bone,0,9.05,-3.8,1.8,1.25,'thread')


def potent_beldam_weave(piece):
    # The upgraded silhouette preserves the original fitted geometry while
    # adding its own porcelain faces, soul windows and deeper layered shroud.
    beldam_weave(piece)
    if piece=='helmet':
        panel('Long pale porcelain forehead','head',[(-3.8,-8),(0,-9),(3.8,-8),
              (3.55,-4.55),(0,-4.9),(-3.55,-4.55)],-5.75,'mask')
        for side in (-1,1):
            g.button('Large void button eye','head',side*1.75,-5.1,-5.91,.9,'black')
            panel('Porcelain face fracture','head',[(side*.85,-8.15),(side*1.1,-7.2),
                  (side*.65,-6.65),(side*1.25,-5.55)],-5.94,'bone',.045)
            panel('High pointed shroud','head',[(side*3.7,-8.9),(side*5.35,-11.25),
                  (side*6.05,-8),(side*5.0,-1.8)],-5.24,'moss')
        cord('Stitched porcelain smile','head',[[-1.5,-1.7,-5.7],[0,-1.25,-5.7],
             [1.5,-1.7,-5.7]],.05,'black')
    elif piece=='chestplate':
        for side in (-1,1):
            bone='left_arm' if side>0 else 'right_arm'
            panel('Porcelain shoulder watcher',bone,[(side-1.1,-1.65),(side,-2.75),
                  (side+1.1,-1.65),(side+.95,.6),(side,.95),(side-1,.6)],-3.57,'mask')
            g.button('Shoulder watcher eye',bone,side-.42,-1.12,-3.68,.23,'black')
            g.button('Shoulder watcher eye',bone,side+.42,-1.12,-3.68,.23,'black')
            soul(bone,side,4.55,-3.16,.65)
        for side in (-1,1):
            panel('Translucent green soul window','body',[(side*.65,3.3),
                  (side*3.55,3.2),(side*3.5,6.8),(side*.8,7.2)],-3.47,'green')
            soul('body',side*2.15,4.25,-3.56,.82)
        def veil(u,v):return [(u-.5)*(9.5+v*10.2),.8+v*(22.4+1.8*math.sin(u*T*9)),3.5+v*4.6]
        surface('Wide nightmare back shroud','cape',veil,32,18,'cloth')
        for u in (.18,.48,.78):
            x,y,z=veil(u,.72);soul('cape',x,y,z+.16,.95)
    elif piece=='leggings':
        for side in (-1,1):
            bone='left_leg' if side>0 else 'right_leg'
            panel('Pale soul window in skirt',bone,[(-1.45,5.5),(1.45,5.5),
                  (1.35,10.6),(0,11.2),(-1.35,10.6)],-3.17,'green')
            soul(bone,0,7.2,-3.27,.88)
    else:
        for side in (-1,1):
            bone='left_leg' if side>0 else 'right_leg'
            panel('Porcelain pointed boot tip',bone,[(-1.7,10.65),(0,11.5),
                  (1.7,10.65),(1.55,11.8),(-1.55,11.8)],-3.94,'mask')
            g.button('Dark boot eye',bone,0,9.35,-3.75,.4,'black')


def soulbound_potent_beldam_weave(piece):
    potent_beldam_weave(piece)
    if piece=='helmet':
        for side in (-1,1):
            g.ellipse('Soul lit button eye','head',[side*1.75,-5.1,-6.04],.32,.32,.05,'glow')
            soul('head',side*5.2,-2.4,-4.9,.62)
        crescent('head',0,-10.55,-5.54,.65)
    elif piece=='chestplate':
        panel('Arcane captive soul window frame','body',[(-2.55,2.4),(0,1.55),
              (2.55,2.4),(2.85,6.8),(0,8.55),(-2.85,6.8)],-3.83,'brass')
        panel('Cyan soul window','body',[(-2.25,2.7),(0,1.98),(2.25,2.7),
              (2.4,6.5),(0,8.1),(-2.4,6.5)],-3.95,'green')
        for x,y in ((-1.25,3.5),(0,4.2),(1.25,3.5)):
            soul('body',x,y,-4.08,.69)
        for side in (-1,1):
            bone='left_arm' if side>0 else 'right_arm';x=side
            web(bone,x+side*1.65,-2.25,-3.68,2.3,2.65,'thread')
            soul(bone,x+side*2.6,3.45,-2.9,.78)
            lantern(bone,x+side*3.05,4.5,-1.8,.6)
        for x in (-3.2,3.2):
            cord('Hanging lost key','body',[[x,7.8,-3.6],[x,9.2,-3.6],
                 [x+.4,9.2,-3.6]],.11,'brass')
        for side in (-1,1):
            bone='left_leg' if side>0 else 'right_leg'
            soul(bone,side*.45,6.1,-3.35,.75)
    elif piece=='leggings':
        for side in (-1,1):
            bone='left_leg' if side>0 else 'right_leg'
            for x,y in ((-.65,5.9),(.65,8.25)):
                soul(bone,x,y,-3.38,.56)
            g.button('Soul skirt button',bone,side*1.45,4.35,-3.24,.47,'button')
    else:
        for side in (-1,1):
            bone='left_leg' if side>0 else 'right_leg'
            soul(bone,0,9.35,-3.96,.42)
            web(bone,0,10.6,-3.98,1.35,.75,'thread')


def other_world_void(piece):
    soulbound(piece)
    if piece=='helmet':
        panel('Fractured chalk face','head',[(-3.7,-7.85),(0,-8.7),(3.7,-7.85),
              (3.65,-1.8),(0,.35),(-3.65,-1.8)],-5.76,'bone')
        for side in (-1,1):
            panel('Black leaking eye socket','head',[(side*.85,-5.8),(side*2.65,-5.8),
                  (side*2.5,-3.3),(side*1.0,-3.3)],-5.88,'black')
            g.ellipse('Icy void eye','head',[side*1.75,-4.7,-6.02],.55,.25,.06,'glow')
            cord('Vertical black tear','head',[[side*1.75,-3.2,-5.98],
                 [side*1.7,-1.0,-5.98]],.09,'black')
            charm('head',side*4.8,-5.8,-2.2,.36)
        crescent('head',0,-10.45,-5.45,.72)
    elif piece=='chestplate':
        panel('Jagged center absence','body',[(-1.15,2.35),(.45,1.65),(1.4,3.1),
              (.8,5.55),(-.9,6.2),(-1.45,4.25)],-3.72,'black')
        for x,y in ((-.2,2.7),(.3,4.65)):
            g.ellipse('Cold light inside void','body',[x,y,-3.86],.26,.4,.08,'glow')
        for side in (-1,1):
            bone='left_arm' if side>0 else 'right_arm'
            panel('Broken chalk shoulder panel',bone,[(side-2.4,-1.9),
                  (side+2.4,-1.9),(side+2.35,.6),(side-2.35,.6)],-3.54,'bone')
            g.button('Void shoulder button',bone,side,-1.1,-3.69,.61,'button')
            panel('White torn bracer patch',bone,[(side-2.1,4.8),
                  (side+2.1,4.8),(side+2.0,7.45),(side-2.0,7.45)],-2.95,'bone')
        for x in (-3.05,3.05):
            panel('Silhouette picture charm','body',[(x-.44,7.2),(x+.44,7.2),
                  (x+.44,9.1),(x-.44,9.1)],-3.5,'ivory')
            soul('body',x,7.55,-3.6,.38)
        def shroud(u,v):return [(u-.5)*(8.9+v*7.0),1.1+v*(20.2+1.8*math.sin(u*T*7)),3.3+v*3.5]
        surface('Torn void shroud','cape',shroud,28,17,'cloth')
        for u in (.22,.48,.77):
            x,y,z=shroud(u,.68);soul('cape',x,y,z+.15,.79)
    elif piece=='leggings':
        for side in (-1,1):
            bone='left_leg' if side>0 else 'right_leg'
            panel('Torn void leg strip',bone,[(-1.85,.2),(1.85,.2),
                  (2.1,10.0),(0,11.6),(-2.05,10)],-3.02,'cloth')
            soul(bone,side*.5,4.1,-3.17,.67)
            panel('Cracked white knee patch',bone,[(-1.7,6.6),(1.7,6.6),
                  (1.65,8.5),(-1.65,8.5)],-3.18,'bone')
    else:
        for side in (-1,1):
            bone='left_leg' if side>0 else 'right_leg'
            panel('Chalk white fractured toe',bone,[(-2.1,10.4),(2.1,10.4),
                  (2.1,11.8),(-2.1,11.8)],-3.84,'bone')
            panel('Luminous boot fissure',bone,[(-.2,8.4),(.25,8.4),
                  (.1,10.2),(-.35,10.2)],-3.75,'glow')


def other_diamond(piece):
    moonstone(piece)
    if piece=='helmet':
        panel('Diamond helm V brow','head',[(-4.55,-8.7),(0,-7.0),(4.55,-8.7),
              (4.4,-7.75),(0,-5.95),(-4.4,-7.75)],-5.9,'ivory')
        panel('Deep visor slit','head',[(-3.65,-4.5),(3.65,-4.5),
              (3.65,-1.55),(-3.65,-1.55)],-5.75,'under')
        g.button('Diamond side button','head',4.75,-5.15,-1.25,.52,'button')
    elif piece=='chestplate':
        panel('Deep navy crescent bib','body',[(-2.95,1.0),(2.95,1.0),
              (2.6,7.2),(0,8.75),(-2.6,7.2)],-3.7,'navy')
        crescent('body',0,4.5,-3.85,1.25)
        cord('Diamond armor hanging key','body',[[2.4,6.4,-3.74],
             [2.4,8.0,-3.74],[2.8,8.0,-3.74]],.11,'brass')
        def cape(u,v):return [(u-.5)*(8.4+v*4.5),1.2+v*(18.8+1.3*math.sin(u*T*5)),3.2+v*3.1]
        surface('Other World navy cape','cape',cape,26,14,'navy')
        x,y,z=cape(.5,.52);crescent('cape',x,y,z+.15,1.5)
        for side in (-1,1):
            bone='left_arm' if side>0 else 'right_arm'
            g.button('Other World shoulder button',bone,side,-.9,-3.61,.55,'button')
    elif piece=='leggings':
        for side in (-1,1):
            bone='left_leg' if side>0 else 'right_leg'
            panel('Navy layered diamond tabard',bone,[(-1.5,.1),(1.5,.1),
                  (1.3,5.9),(0,6.6),(-1.3,5.9)],-3.06,'navy')
            g.button('Diamond leg button',bone,side*1.25,6.75,-3.2,.45,'button')
    else:
        for side in (-1,1):
            bone='left_leg' if side>0 else 'right_leg'
            g.button('Diamond boot button',bone,side*1.25,8.85,-3.68,.5,'button')


def other_iron(piece):
    # The silver iron set shares the silhouette but uses its own plate painting.
    other_diamond(piece)
    if piece=='helmet':
        panel('Riveted iron forehead','head',[(-4.75,-8.85),(4.75,-8.85),
              (4.35,-7.15),(0,-6.5),(-4.35,-7.15)],-6.05,'crystal')
        g.button('Iron brow rivet','head',0,-7.8,-6.19,.5,'button')
    elif piece=='chestplate':
        for side in (-1,1):
            bone='left_arm' if side>0 else 'right_arm'
            g.button('Iron shoulder rivet',bone,side,-1.6,-3.7,.57,'button')
        for x,y in ((-3.1,1.5),(3.1,1.5),(-3.2,7.5),(3.2,7.5)):
            g.ellipse('Iron plate rivet','body',[x,y,-3.35],.14,.14,.04,'button')


def perfect(piece):
    other_diamond(piece)
    if piece=='helmet':
        panel('Ivory perfect brow','head',[(-5,-8.95),(0,-9.55),(5,-8.95),
              (4.45,-6.25),(0,-7.3),(-4.45,-6.25)],-6.06,'ivory')
        g.ellipse('Void sun disc','head',[0,-6.75,-6.24],1.2,1.2,.12,'black')
        trim('head',[[1.3*math.cos(i*T/36),-6.75+1.3*math.sin(i*T/36),-6.38]
             for i in range(37)],'glow',.11)
        star('head',0,-6.75,-6.42,1.65)
        for side in (-1,1):
            panel('Halo crown spire','head',[(side*3.1,-9.0),(side*3.45,-12.2),
                  (side*3.85,-8.85)],-5.58,'gold')
        trim('head',[[4.3*math.cos(i*T/36),-9.0+2.5*math.sin(i*T/36),.2]
             for i in range(37)],'gold',.11)
    elif piece=='chestplate':
        panel('Ivory perfect breast mantle','body',[(-4.3,.5),(0,-.35),
              (4.3,.5),(4.1,7.9),(0,9.4),(-4.1,7.9)],-3.78,'ivory')
        panel('Dark center tabard','body',[(-1.75,1.4),(1.75,1.4),
              (1.65,10.7),(0,11.5),(-1.65,10.7)],-3.99,'under')
        g.ellipse('Perfect sun disc','body',[0,4.1,-4.14],1.5,1.5,.13,'black')
        trim('body',[[1.65*math.cos(i*T/40),4.1+1.65*math.sin(i*T/40),-4.28]
             for i in range(41)],'glow',.12)
        star('body',0,4.1,-4.39,1.6)
        for side in (-1,1):
            bone='left_arm' if side>0 else 'right_arm';x=side
            panel('Ivory perfect shoulder',bone,[(x-2.95,-2.65),
                  (x+2.95,-2.65),(x+2.85,.75),(x-2.85,.75)],-3.71,'ivory')
            g.ellipse('Shoulder sun disc',bone,[x,-.95,-3.83],.75,.75,.07,'black')
            trim(bone,[[x+.84*math.cos(i*T/24),-.95+.84*math.sin(i*T/24),-3.95]
                 for i in range(25)],'glow',.075)
        def cape(u,v):return [(u-.5)*(8.7+v*9.6),1.2+v*(22.0+1.5*math.sin(u*T*8)),3.35+v*4]
        surface('Long ivory and black perfect cape','cape',cape,30,18,'cloth')
        for u in (.14,.33,.5,.67,.86):
            x,y,z=cape(u,.68);star('cape',x,y,z+.16,.4)
    elif piece=='leggings':
        for side in (-1,1):
            bone='left_leg' if side>0 else 'right_leg'
            panel('Ivory perfect thigh armor',bone,[(-1.85,.1),(1.85,.1),
                  (1.75,5.6),(0,6.1),(-1.75,5.6)],-3.27,'ivory')
            panel('Black star leg tabard',bone,[(-1.2,.2),(1.2,.2),
                  (1.1,10.7),(0,11.5),(-1.1,10.7)],-3.43,'under')
            for y in (3.25,7.1):star(bone,0,y,-3.55,.38)
    else:
        for side in (-1,1):
            bone='left_leg' if side>0 else 'right_leg'
            panel('Ivory perfect toe',bone,[(-2.15,10.5),(2.15,10.5),
                  (2.15,11.85),(-2.15,11.85)],-4.08,'ivory')
            star(bone,0,9.3,-3.97,.48)


PALETTES={
    'luck':{'cloth':'#265a5d','teal':'#1d6060','navy':'#17223e','under':'#171d2c','brass':'#b5904c','button':'#b5904c','thread':'#c8ae73','gold':'#d7bf7a','leaf':'#3c9c79','glow':'#58e5b5','leather':'#49382f','ivory':'#d5c394','lining':'#54422f','black':'#111423'},
    'moonstone':{'cloth':'#727dbd','under':'#29315f','crystal':'#85c6f4','ice':'#c9f3ff','violet':'#ae98eb','ivory':'#f3ebdf','brass':'#e9dabf','button':'#e9dabf','thread':'#f4e8d7','gold':'#fff1d2','lining':'#a395cc','black':'#161a3e'},
    'miners':{'cloth':'#324257','under':'#232733','leather':'#58402f','violet':'#3e3357','brass':'#ac8044','button':'#a78760','amber':'#ffbb53','blue':'#60a8ed','ice':'#d0f4ff','gold':'#d4b978','thread':'#c1ad86','ivory':'#c9b89b','lining':'#6e5237','black':'#11131b'},
    'spidervision':{'cloth':'#202536','under':'#151923','navy':'#25253d','violet':'#382956','brass':'#95764b','button':'#ab8e64','thread':'#c5b8a2','gold':'#c5ad78','ivory':'#c7bca2','lining':'#45405a','glow':'#6babff','amber':'#6babff','black':'#0c0e17'},
    'beldam_weave':{'cloth':'#151b1a','under':'#101311','moss':'#304a38','mask':'#d7d7bf','bone':'#a4a58a','brass':'#8a774f','button':'#9d8a62','thread':'#a2a78b','gold':'#b8aa79','green':'#55b886','glow':'#60efb1','amber':'#60efb1','ivory':'#d6d6bc','lining':'#333d32','black':'#171913'},
    'soulbound':{'cloth':'#1c2234','under':'#171b2a','navy':'#343b58','bone':'#bdbdb3','ivory':'#e1dfcf','brass':'#9d8059','button':'#282936','thread':'#a5a6b2','gold':'#c3af90','soul':'#91b6f0','lining':'#3e4055','black':'#11131e'},
    'super_spidervision':{'cloth':'#1b1a2d','under':'#141321','navy':'#28223f','violet':'#4b2b72','brass':'#a17c50','button':'#b09268','thread':'#c9b9b0','gold':'#c9ad8e','ivory':'#d7c9b4','lining':'#5c4474','glow':'#b066ff','amber':'#b066ff','black':'#0d0b18'},
    'potent_beldam_weave':{'cloth':'#ffffff','under':'#101311','moss':'#26372a','mask':'#d9d8c4','bone':'#a4a58a','brass':'#8a774f','button':'#a19471','thread':'#a2a78b','gold':'#b8aa79','green':'#5b8066','glow':'#72eab1','amber':'#72eab1','ivory':'#d6d6bc','lining':'#333d32','black':'#141712','soul':'#b9d9b5'},
    'soulbound_potent_beldam_weave':{'cloth':'#ffffff','under':'#0c171b','moss':'#203b3d','mask':'#d8ddd3','bone':'#a8b6ab','brass':'#9d9773','button':'#8da8a3','thread':'#a3c6ba','gold':'#bcba93','green':'#4d8f93','glow':'#8ef3f1','amber':'#8ef3f1','ivory':'#e0e2d6','lining':'#294447','black':'#101819','soul':'#a2f7f7'},
    'other_world_void':{'cloth':'#ffffff','under':'#1a1824','navy':'#45394d','bone':'#e0dcda','ivory':'#bdb8b7','brass':'#8b7e79','button':'#b9b2ac','thread':'#a6a0ad','gold':'#c2bab6','soul':'#8d9eb0','lining':'#564b5b','black':'#0a0b12','glow':'#a8f7ff'},
    'other_diamond':{'cloth':'#283754','under':'#18263c','navy':'#152745','crystal':'#ffffff','ice':'#83d9f5','violet':'#38a5cc','ivory':'#d1e2e8','brass':'#c5b69a','button':'#aec5cc','thread':'#dfd6b5','gold':'#d5cbb1','lining':'#5a7584','black':'#121e32'},
    'other_iron':{'cloth':'#27324a','under':'#1b2538','navy':'#16233c','crystal':'#ffffff','ice':'#b1b7be','violet':'#727f8d','ivory':'#d7d9db','brass':'#a89875','button':'#aeb2b8','thread':'#c9b99c','gold':'#cbb796','lining':'#58646e','black':'#1b202a'},
    'perfect':{'cloth':'#ffffff','under':'#0b0d13','navy':'#12151e','crystal':'#f0eadf','ice':'#d8d2c8','violet':'#bca682','ivory':'#f3ede2','brass':'#c7a664','button':'#d3b77e','thread':'#d7c08a','gold':'#ebc77f','lining':'#a79579','black':'#09090d','glow':'#ffd17a'},
}


def build(style,piece):
    if style not in PALETTES:raise ValueError(style)
    if piece not in ('helmet','chestplate','leggings','boots'):raise ValueError(piece)
    g.meshes.clear()
    {'luck':luck,'moonstone':moonstone,'miners':miners,'spidervision':spidervision,
     'beldam_weave':beldam_weave,'soulbound':soulbound,
     'super_spidervision':super_spidervision,'potent_beldam_weave':potent_beldam_weave,
     'soulbound_potent_beldam_weave':soulbound_potent_beldam_weave,
     'other_world_void':other_world_void,'other_diamond':other_diamond,
     'other_iron':other_iron,'perfect':perfect}[style](piece)
    meshes=g.meshes
    for mesh in meshes:
        mesh['faces']=[list(reversed(f)) for f in mesh['faces']]
        area=0
        for f in mesh['faces']:
            a,b,c=(mesh['vertices'][i] for i in f[:3])
            area+=(b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0])
        if (all(v[2]<-1.8 for v in mesh['vertices']) and area>.1) or (all(v[2]>1.8 for v in mesh['vertices']) and area<-.1):
            mesh['faces']=[list(reversed(f)) for f in mesh['faces']]
        mesh['vertices']=[[round(n,5) for n in p] for p in mesh['vertices']]
        if mesh['material'] in ('glow','amber','blue','ice','soul') and (style!='moonstone' or mesh['material']=='ice'):
            mesh['emissive']=True
        textured=mesh['material'] in ({s:('crystal',) for s in ('moonstone','other_diamond','other_iron')}.get(style,('cloth',)))
        if textured:
            finish(mesh,True)
        else:
            mesh['uv']=[[.9,.5] for _ in mesh['vertices']]
            finish(mesh,False)
    materials=PALETTES[style].copy()
    materials['crystal' if style in ('moonstone','other_diamond','other_iron') else 'cloth']='#ffffff'
    return {'style':style+'_concept','piece':piece,'texture':style,
            'materials':materials,'meshes':meshes}
