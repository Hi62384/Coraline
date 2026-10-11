"""QA contact sheets only; original generated illustration pixels are preserved."""
from pathlib import Path
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[1]
ART=ROOT/'artwork/sculpted_wardrobe'
sets=['dreamweave','comfy_silk','buttonguard','garden_vines','midnight','miners']
sheet=Image.new('RGB',(1000,6*285),'#292c37');draw=ImageDraw.Draw(sheet)
for row,family in enumerate(sets):
    for col,piece in enumerate(['helmet','chestplate','leggings','boots']):
        name=family+'_'+piece
        with Image.open(ART/'icons'/(name+'.png')) as im:
            im=im.convert('RGBA');im.thumbnail((240,240),Image.Resampling.LANCZOS)
            sheet.paste(im,(col*250+(250-im.width)//2,row*285),im)
        draw.text((col*250+5,row*285+245),name,fill='white')
sheet.save(ART/'reference_icons.jpg',quality=94)
if all((ART/'renders'/(family+'.png')).exists() for family in sets):
    sheet=Image.new('RGB',(1536,1080),'#292c37');draw=ImageDraw.Draw(sheet)
    for i,family in enumerate(sets):
        x=i%3*512;y=i//3*540
        with Image.open(ART/'renders'/(family+'.png')) as im:sheet.paste(im,(x,y),im)
        draw.text((x+12,y+512),family.replace('_',' ').title()+' — actual worn mesh',fill='white')
    sheet.save(ART/'reference_models.jpg',quality=94)
