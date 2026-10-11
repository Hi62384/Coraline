"""Shared validation-friendly finish, not shared outfit geometry.

Box-project cloth at a consistent scale while retaining smooth vertex normals.
Prevents angle-based UVs from streaking across squared hoods and padded sleeves.
"""
import math

def finish(mesh,textured=False):
    vertices=mesh['vertices'];faces=mesh['faces']
    normals=[[0.,0.,0.] for _ in vertices];face_normals=[]
    for f in faces:
        a,b,c=(vertices[i] for i in f[:3]);ab=[b[i]-a[i] for i in range(3)];ac=[c[i]-a[i] for i in range(3)]
        n=[ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0]];face_normals.append(n)
        for index in f:
            for i in range(3):normals[index][i]+=n[i]
    if not textured:return
    for n in normals:
        length=math.sqrt(sum(v*v for v in n))
        if length>1e-9:
            for i in range(3):n[i]/=length
    verts=[];uv=[];ns=[];fs=[]
    for face,n in zip(faces,face_normals):
        axis=max(range(3),key=lambda i:abs(n[i]));a,b=(0,1) if axis==2 else (2,1) if axis==0 else (0,2)
        # A twelve-pixel cloth patch is a readable motif scale on a Minecraft body.
        mid=[sum(vertices[i][j] for i in face)/len(face) for j in range(3)]
        offsets=[math.floor((mid[j]+6)/12) for j in (a,b)]
        base=len(verts)
        for i in face:
            p=vertices[i];u=(p[a]+6)/12-offsets[0];v=(p[b]+6)/12-offsets[1]
            verts.append(p);ns.append([round(x,6) for x in normals[i]])
            uv.append([.02+.69*max(0,min(1,u)),.02+.96*max(0,min(1,v))])
        fs.append(list(range(base,base+len(face))))
    mesh.update(vertices=verts,faces=fs,uv=uv,normals=ns)
