import gzip, struct
from pathlib import Path
def text(s):
    b=s.encode(); return struct.pack('>H',len(b))+b
def tag(t,name,payload): return bytes([t])+text(name)+payload
def ints(values): return bytes([3])+struct.pack('>i',len(values))+b''.join(struct.pack('>i',n) for n in values)
data=tag(10,'',tag(3,'DataVersion',struct.pack('>i',3465))+tag(9,'size',ints([26,12,26]))+tag(9,'entities',bytes([10])+struct.pack('>i',0))+tag(9,'blocks',bytes([10])+struct.pack('>i',0))+tag(9,'palette',bytes([10])+struct.pack('>i',1)+tag(8,'Name',text('minecraft:air'))+bytes([0]))+bytes([0]))
p=Path(__file__).resolve().parents[1]/'src/gametest/resources/data/coraline/structures/empty.nbt'
p.parent.mkdir(parents=True,exist_ok=True)
p.write_bytes(gzip.compress(data))
