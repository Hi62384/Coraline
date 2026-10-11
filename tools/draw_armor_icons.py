"""Legacy uniform icon sketches, retained only for raincoat fallback drawing.

The 13 armor sets use their original individually illustrated artwork via
install_concept_icons.py. Do not regenerate their inventory icons here.
"""
from pathlib import Path
import math, random
from PIL import Image, ImageDraw, ImageColor

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'artwork/sculpted_wardrobe/icons'
PALETTES={
 'luck':('#183944','#2b645f','#bb9c58','#73e6ae','#111e2d'),
 'moonstone':('#5669aa','#9fcbfa','#f3eadb','#9df5ff','#253151'),
 'miners':('#2b3041','#52535c','#b58e5a','#ffd278','#1a1c28'),
 'spidervision':('#1c2031','#3a3152','#b59a70','#64aaff','#0e111d'),
 'beldam_weave':('#18231d','#3a5941','#c3c0a4','#70e8af','#0d1511'),
 'soulbound':('#1d2334','#4a536c','#c7b59d','#a2cdff','#0d121e'),
 'super_spidervision':('#1b172c','#583771','#c6a172','#c47bff','#0c0b19'),
 'potent_beldam_weave':('#16231c','#4b6248','#d3cdb5','#9be1a7','#0c1511'),
 'soulbound_potent_beldam_weave':('#14292b','#397077','#d2d0bb','#8df4f2','#09191c'),
 'other_world_void':('#292433','#665c70','#e5e1dd','#b9f7ff','#0c0b12'),
 'other_diamond':('#193751','#44b9ed','#e3e7df','#a6f5ff','#10233d'),
 'other_iron':('#1e2c3b','#9aa7b2','#e0deda','#e4c99d','#141d2c'),
 'perfect':('#111218','#eee8dc','#e4bb75','#ffe3a2','#08090c'),
}
PIECES=('helmet','chestplate','leggings','boots')

def rgb(value):return ImageColor.getrgb(value)
def shade(value,factor):return tuple(max(0,min(255,round(c*factor))) for c in rgb(value))
def poly(d,points,fill,edge):d.polygon(points,fill=fill);d.line(points+[points[0]],fill=edge,width=2,joint='curve')
def line(d,points,color,width=1):d.line(points,fill=color,width=width,joint='curve')
def gem(d,x,y,r,color,edge):
    poly(d,[(x,y-r),(x+r,y),(x,y+r),(x-r,y)],color,edge)
    d.polygon([(x,y-r),(x+r,y),(x,y)],fill=shade(color,1.23))
    d.polygon([(x,y),(x,y+r),(x-r,y)],fill=shade(color,.67))
    d.point((x-r//3,y-r//3),fill='white')
def button(d,x,y,r,trim,dark):
    d.ellipse((x-r,y-r,x+r,y+r),fill=trim,outline=dark,width=2)
    d.ellipse((x-r+2,y-r+2,x+r-2,y+r-2),fill=shade(trim,.62))
    line(d,[(x-1,y-1),(x+1,y+1)],dark,1);line(d,[(x+1,y-1),(x-1,y+1)],dark,1)
def star(d,x,y,r,color):
    line(d,[(x-r,y),(x+r,y)],color,1);line(d,[(x,y-r),(x,y+r)],color,1)
    d.point((x,y),fill='white')
def crescent(d,x,y,r,light,dark):
    d.ellipse((x-r,y-r,x+r,y+r),fill=light)
    d.ellipse((x-r+3,y-r-3,x+r+5,y+r-3),fill=dark)
def clover(d,x,y,r,color,edge):
    for dx,dy in ((-r//2,-r//2),(r//2,-r//2),(-r//2,r//2),(r//2,r//2)):
        d.ellipse((x+dx-r//2,y+dy-r//2,x+dx+r//2,y+dy+r//2),fill=color,outline=edge)
    line(d,[(x,y+r),(x+1,y+r+4)],edge)
def spider(d,x,y,r,color):
    d.ellipse((x-r//2,y-r//2,x+r//2,y+r),fill=color)
    d.ellipse((x-r//3,y-r,x+r//3,y-r//3),fill=color)
    for side in (-1,1):
        for i in range(3):
            yy=y-2+i*3
            line(d,[(x+side*2,yy),(x+side*(r+2),yy-3+i*3),
                    (x+side*(r+4),yy+2+i*3)],color)
def ghost(d,x,y,r,glow,dark):
    d.ellipse((x-r//2,y-r,x+r//2,y),fill=glow)
    poly(d,[(x-r//2,y-1),(x+r//2,y-1),(x+r//3,y+r),
            (x,y+r//2),(x-r//2,y+r)],glow,glow)
    d.point((x-1,y-r//3),fill=dark);d.point((x+1,y-r//3),fill=dark)
def web(d,x,y,r,color):
    for a in range(0,360,45):
        t=math.radians(a);line(d,[(x,y),(round(x+r*math.cos(t)),round(y+r*math.sin(t)))],color)
    for scale in (.5,1):
        pts=[(round(x+r*scale*math.cos(math.radians(a))),
              round(y+r*scale*math.sin(math.radians(a)))) for a in range(0,361,45)]
        line(d,pts,color)
def key(d,x,y,trim):
    d.ellipse((x-2,y-4,x+2,y),outline=trim,width=1)
    line(d,[(x,y),(x,y+6),(x+3,y+6)],trim)
def sun(d,x,y,r,trim,dark):
    d.ellipse((x-r,y-r,x+r,y+r),fill=dark,outline=trim,width=2)
    for a in range(0,360,45):
        t=math.radians(a)
        line(d,[(round(x+(r+1)*math.cos(t)),round(y+(r+1)*math.sin(t))),
                (round(x+(r+5)*math.cos(t)),round(y+(r+5)*math.sin(t)))],trim,2)
def mask(d,x,y,r,white,dark,eyes):
    poly(d,[(x-r,y-r),(x,y-r-2),(x+r,y-r),(x+r-1,y+r-2),
            (x,y+r),(x-r+1,y+r-2)],white,dark)
    for dx in (-r//2,r//2):button(d,x+dx,y-1,2,eyes,dark)
    line(d,[(x-3,y+r-3),(x,y+r-1),(x+3,y+r-3)],dark)

def base_icon(d,piece,style,c):
    body,plate,trim,glow,dark=c
    if piece=='helmet':
        poly(d,[(15,9),(23,5),(42,5),(50,10),(53,37),(49,49),
                (15,49),(11,38)],body,dark)
        poly(d,[(15,10),(49,10),(51,18),(13,18)],plate,trim)
        poly(d,[(20,18),(44,18),(43,41),(32,45),(21,41)],dark,trim)
        poly(d,[(14,19),(21,21),(21,39),(16,44),(12,37)],plate,dark)
        poly(d,[(50,19),(43,21),(43,39),(48,44),(52,37)],plate,dark)
        line(d,[(16,48),(48,48)],trim,2)
        if style in ('beldam_weave','soulbound','potent_beldam_weave',
                     'soulbound_potent_beldam_weave','other_world_void'):
            mask(d,32,30,10,'#dedbd0' if style!='other_world_void' else '#eee9e6',dark,glow)
        elif style in ('spidervision','super_spidervision'):
            for x,y in ((24,26),(30,30),(36,30),(42,26)):
                gem(d,x,y,3,glow,trim)
            for x in (25,39):poly(d,[(x-3,36),(x+3,36),(x,43)],trim,dark)
        elif style in ('other_diamond','other_iron','perfect'):
            poly(d,[(20,18),(44,18),(40,24),(24,24)],plate,trim)
            poly(d,[(23,25),(41,25),(40,38),(24,38)],dark,dark)
        else:
            poly(d,[(22,22),(42,22),(40,40),(24,40)],shade(body,.8),trim)
    elif piece=='chestplate':
        poly(d,[(19,9),(26,7),(38,7),(45,9),(52,14),(59,19),
                (55,32),(49,31),(47,27),(46,54),(35,58),(29,58),
                (18,54),(17,27),(15,31),(9,32),(5,19),(12,14)],body,dark)
        poly(d,[(12,13),(21,12),(24,21),(18,29),(8,27),(5,19)],plate,trim)
        poly(d,[(52,13),(43,12),(40,21),(46,29),(56,27),(59,19)],plate,trim)
        poly(d,[(21,10),(27,13),(37,13),(43,10),(46,21),(43,48),
                (32,54),(21,48),(18,21)],plate,dark)
        poly(d,[(25,13),(32,17),(39,13),(43,19),(39,44),(32,49),
                (25,44),(21,19)],body,trim)
        poly(d,[(18,48),(29,53),(32,53),(35,53),(46,48),
                (45,55),(35,60),(29,60),(19,55)],body,trim)
        for x in (13,51):button(d,x,17,3,trim,dark)
    elif piece=='leggings':
        poly(d,[(16,6),(48,6),(51,15),(47,31),(46,58),(34,59),
                (32,30),(30,59),(18,58),(17,31),(13,15)],body,dark)
        poly(d,[(15,7),(49,7),(49,14),(15,14)],plate,trim)
        poly(d,[(17,15),(30,15),(29,34),(17,34)],plate,dark)
        poly(d,[(34,15),(47,15),(47,34),(35,34)],plate,dark)
        poly(d,[(18,37),(29,37),(29,49),(18,49)],plate,trim)
        poly(d,[(35,37),(46,37),(46,49),(35,49)],plate,trim)
        for x in (23,41):button(d,x,42,3,trim,dark)
        line(d,[(18,56),(29,56)],trim,2);line(d,[(35,56),(46,56)],trim,2)
    else:
        for x in (5,34):
            poly(d,[(x+4,11),(x+20,11),(x+21,33),(x+25,41),
                    (x+25,54),(x,54),(x,42),(x+4,33)],body,dark)
            poly(d,[(x+3,13),(x+21,13),(x+21,21),(x+3,21)],plate,trim)
            poly(d,[(x+3,36),(x+21,36),(x+25,43),(x+25,52),
                    (x,52),(x,44)],plate,trim)
            poly(d,[(x+1,50),(x+26,50),(x+26,55),(x,55)],dark,dark)
            button(d,x+14,27,3,trim,dark)

def embellish(d,piece,style,c):
    body,plate,trim,glow,dark=c
    center={'helmet':(32,15),'chestplate':(32,33),'leggings':(32,11),'boots':(22,42)}[piece]
    x,y=center
    if style=='luck':
        clover(d,x,y,5,glow,dark)
        for px,py in ((17,24),(46,29),(23,52)):star(d,px,py,2,trim)
        if piece=='chestplate':key(d,39,43,trim)
    elif style=='moonstone':
        gem(d,x,y,6,glow,trim)
        if piece=='chestplate':crescent(d,32,37,6,trim,body)
        for px,py in ((18,25),(46,25)):star(d,px,py,3,'white')
    elif style=='miners':
        if piece=='helmet':
            d.ellipse((24,9,40,25),fill=trim,outline=dark,width=2)
            d.ellipse((27,12,37,22),fill=glow,outline='#fff1c8',width=2)
        else:
            button(d,x,y,5,trim,dark)
            gem(d,23,48,3,'#6bc7ff',trim)
        for px in (16,48):line(d,[(px,24),(px,36)],trim,2)
    elif style in ('spidervision','super_spidervision'):
        if piece!='helmet':web(d,x,y,7,trim);spider(d,x,y,5,glow)
        if style=='super_spidervision':
            gem(d,32,9 if piece=='helmet' else 22,4,glow,trim)
            for px in (12,52):star(d,px,23,2,glow)
    elif style in ('beldam_weave','potent_beldam_weave','soulbound_potent_beldam_weave'):
        if piece!='helmet':
            web(d,19,26,6,trim);web(d,46,26,6,trim)
            gem(d,x,y,5,glow,trim)
            if style!='beldam_weave':
                for px in (24,40):ghost(d,px,42,3,glow,dark)
        if piece=='chestplate':key(d,39,49,trim)
        if style=='soulbound_potent_beldam_weave':
            for px in (19,46):star(d,px,9,3,glow)
    elif style=='soulbound':
        if piece!='helmet':ghost(d,x,y,7,glow,dark)
        key(d,44,45,trim)
        star(d,19,27,2,glow)
    elif style=='other_world_void':
        if piece=='helmet':
            for px in (27,37):star(d,px,29,2,glow)
            line(d,[(27,33),(27,39)],dark,2);line(d,[(37,33),(37,39)],dark,2)
        else:
            poly(d,[(x-4,y-5),(x+3,y-7),(x+5,y+3),(x-2,y+7),(x-6,y+2)],dark,trim)
            star(d,x,y,2,glow)
            for px in (20,43):ghost(d,px,48,3,trim,dark)
    elif style in ('other_diamond','other_iron'):
        if style=='other_diamond':gem(d,x,y,5,glow,trim)
        else:button(d,x,y,4,trim,dark)
        if piece=='chestplate':crescent(d,32,37,5,trim,body);key(d,42,47,trim)
        if piece=='helmet':gem(d,32,12,3,glow,trim)
    else: # perfect
        sun(d,x,y,5,trim,dark)
        if piece=='chestplate':
            for px in (19,45):star(d,px,44,3,trim)
        if piece=='helmet':
            for px in (20,44):star(d,px,8,3,glow)
    if style in ('luck','miners','spidervision','beldam_weave','soulbound',
                 'super_spidervision','potent_beldam_weave','soulbound_potent_beldam_weave',
                 'other_world_void'):
        for px,py in ((15,33),(47,35),(22,51)):
            line(d,[(px,py),(px+2,py+2)],trim)
            line(d,[(px+2,py),(px,py+2)],trim)

def draw_armor(style,piece):
    colors=PALETTES[style]
    image=Image.new('RGBA',(64,64),(0,0,0,0));d=ImageDraw.Draw(image)
    base_icon(d,piece,style,colors)
    embellish(d,piece,style,colors)
    # Sparse hand-inked wear on opaque plates, never on the background.
    rng=random.Random(style+piece)
    for _ in range(55):
        x=rng.randrange(8,56);y=rng.randrange(8,56)
        if image.getpixel((x,y))[3] and rng.random()<.45:
            d.point((x,y),fill=(*shade(colors[2],.88),180))
    return image.resize((256,256),Image.Resampling.NEAREST)

def draw_raincoat():
    image=Image.new('RGBA',(64,64),(0,0,0,0));d=ImageDraw.Draw(image)
    yellow='#e5b329';dark='#6c461a';edge='#a86e1d';orange='#a45321'
    poly(d,[(22,4),(42,4),(47,11),(47,22),(40,23),(39,14),
            (25,14),(24,23),(17,22),(17,11)],yellow,dark)
    poly(d,[(25,12),(39,12),(40,19),(24,19)],orange,edge)
    poly(d,[(17,19),(25,17),(39,17),(47,19),(49,50),
            (42,54),(22,54),(15,50)],yellow,dark)
    poly(d,[(16,19),(8,23),(4,44),(12,48),(19,32),(20,21)],yellow,dark)
    poly(d,[(48,19),(56,23),(60,44),(52,48),(45,32),(44,21)],yellow,dark)
    poly(d,[(16,19),(24,19),(26,27),(19,31),(14,27)],shade(yellow,.92),edge)
    poly(d,[(48,19),(40,19),(38,27),(45,31),(50,27)],shade(yellow,.92),edge)
    poly(d,[(28,20),(35,20),(36,51),(28,51)],shade(yellow,.87),edge)
    for y in (27,34,41):button(d,31,y,2,'#24242a',dark)
    for x in (19,39):
        poly(d,[(x,37),(x+7,37),(x+7,46),(x,46)],yellow,edge)
        line(d,[(x,37),(x+7,37)],dark)
    line(d,[(17,51),(47,51)],edge,2)
    for x in (11,53):line(d,[(x-3,44),(x+3,45)],dark,2)
    return image.resize((256,256),Image.Resampling.NEAREST)

if __name__=='__main__':
    raise SystemExit('Deprecated template generator: use install_concept_icons.py to install the original illustrated armor artwork.')
