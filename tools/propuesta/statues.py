"""Renderiza las skins de los creadores como estatuas de metal (pixel art) con z-buffer."""
import numpy as np, math, sys, json, base64, io
from PIL import Image

SK = '/mnt/project-files/propuestas/skins/'

def rot(ax, deg):
    a = math.radians(deg); c, s = math.cos(a), math.sin(a)
    if ax == 'x': return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])
    if ax == 'y': return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])

# part: (uv origin base, uv origin overlay, min corner, size, pivot, inflate overlay)
PARTS = {
    'head': ((0, 0), (32, 0), (-4, 24, -4), (8, 8, 8), (0, 24, 0), .5),
    'body': ((16, 16), (16, 32), (-4, 12, -2), (8, 12, 4), (0, 24, 0), .25),
    'rarm': ((40, 16), (40, 32), (-8, 12, -2), (4, 12, 4), (-5, 22, 0), .25),
    'larm': ((32, 48), (48, 48), (4, 12, -2), (4, 12, 4), (5, 22, 0), .25),
    'rleg': ((0, 16), (0, 32), (-4, 0, -2), (4, 12, 4), (-2, 12, 0), .25),
    'lleg': ((16, 48), (0, 48), (0, 0, -2), (4, 12, 4), (2, 12, 0), .25),
}

def faces(uv, mn, sz, inf):
    """yield (texel u,v, 4 corners, normal) for each texel of the box"""
    u, v = uv; w, h, d = sz
    x0, y0, z0 = mn[0]-inf, mn[1]-inf, mn[2]-inf
    x1, y1, z1 = mn[0]+w+inf, mn[1]+h+inf, mn[2]+d+inf
    W, H, Dd = x1-x0, y1-y0, z1-z0
    out = []
    def add(tu, tv, p, du, dv, n):
        out.append((tu, tv, [p, p+du, p+du+dv, p+dv], n))
    for j in range(h):
        for i in range(w):
            sx, sy = W/w, H/h
            add(u+d+i, v+d+j, np.array([x0+i*sx, y1-j*sy, z1]), np.array([sx, 0, 0]), np.array([0, -sy, 0]), (0, 0, 1))  # front
            add(u+2*d+w+i, v+d+j, np.array([x1-i*sx, y1-j*sy, z0]), np.array([-sx, 0, 0]), np.array([0, -sy, 0]), (0, 0, -1))  # back
    for j in range(h):
        for i in range(d):
            sz_, sy = Dd/d, H/h
            add(u+i, v+d+j, np.array([x0, y1-j*sy, z0+i*sz_]), np.array([0, 0, sz_]), np.array([0, -sy, 0]), (-1, 0, 0))  # right
            add(u+d+w+i, v+d+j, np.array([x1, y1-j*sy, z1-i*sz_]), np.array([0, 0, -sz_]), np.array([0, -sy, 0]), (1, 0, 0))  # left
    for j in range(d):
        for i in range(w):
            sx, sz_ = W/w, Dd/d
            add(u+d+i, v+j, np.array([x0+i*sx, y1, z0+j*sz_]), np.array([sx, 0, 0]), np.array([0, 0, sz_]), (0, 1, 0))  # top
            add(u+d+w+i, v+j, np.array([x0+i*sx, y0, z1-j*sz_]), np.array([sx, 0, 0]), np.array([0, 0, -sz_]), (0, -1, 0))  # bottom
    return out

def render(skin, pose, S=3, pad=6, yaw=0, pitch=7):
    tex = np.array(Image.open(SK+skin+'_skin.png').convert('RGBA')).astype(float)/255
    G = rot('x', pitch) @ rot('y', yaw)
    quads = []
    for name, (uvb, uvo, mn, sz, piv, inf) in PARTS.items():
        R = np.eye(3)
        for ax, deg in pose.get(name, []): R = R @ rot(ax, deg)
        piv = np.array(piv, float)
        for layer, uv, infl in ((0, uvb, 0), (1, uvo, inf)):
            for tu, tv, pts, n in faces(uv, mn, sz, infl):
                c = tex[tv, tu]
                if c[3] < .5: continue
                P = [G @ (R @ (p - piv) + piv) for p in pts]
                N = G @ (R @ np.array(n, float))
                quads.append((c[:3], P, N, layer, (tu, tv)))
    allp = np.array([p for q in quads for p in q[1]])
    minx, maxx = allp[:, 0].min(), allp[:, 0].max(); miny, maxy = allp[:, 1].min(), allp[:, 1].max()
    Wd = int(math.ceil((maxx-minx)*S))+2*pad; Hd = int(math.ceil((maxy-miny)*S))+2*pad
    zb = np.full((Hd, Wd), -1e9); col = np.zeros((Hd, Wd, 3)); nrm = np.zeros((Hd, Wd, 3)); msk = np.zeros((Hd, Wd), bool)
    tid = np.full((Hd, Wd), -1)
    for qi, (c, P, N, layer, t) in enumerate(quads):
        if N[2] < -0.05: continue  # backface
        sp = [((p[0]-minx)*S+pad, (maxy-p[1])*S+pad, p[2]) for p in P]
        xs = [p[0] for p in sp]; ys = [p[1] for p in sp]
        bx0, bx1 = max(0, int(math.floor(min(xs)))), min(Wd-1, int(math.ceil(max(xs))))
        by0, by1 = max(0, int(math.floor(min(ys)))), min(Hd-1, int(math.ceil(max(ys))))
        if bx1 < bx0 or by1 < by0: continue
        gx, gy = np.meshgrid(np.arange(bx0, bx1+1)+.5, np.arange(by0, by1+1)+.5)
        for tri in ((0, 1, 2), (0, 2, 3)):
            (ax_, ay, az), (bx, by, bz), (cx, cy, cz) = [sp[k] for k in tri]
            den = (by-cy)*(ax_-cx)+(cx-bx)*(ay-cy)
            if abs(den) < 1e-9: continue
            l1 = ((by-cy)*(gx-cx)+(cx-bx)*(gy-cy))/den
            l2 = ((cy-ay)*(gx-cx)+(ax_-cx)*(gy-cy))/den
            l3 = 1-l1-l2
            inside = (l1 >= -1e-6) & (l2 >= -1e-6) & (l3 >= -1e-6)
            z = l1*az+l2*bz+l3*cz + layer*0.01
            sub = zb[by0:by1+1, bx0:bx1+1]
            upd = inside & (z > sub)
            sub[upd] = z[upd]
            col[by0:by1+1, bx0:bx1+1][upd] = c
            nrm[by0:by1+1, bx0:bx1+1][upd] = N
            msk[by0:by1+1, bx0:bx1+1][upd] = True
            tid[by0:by1+1, bx0:bx1+1][upd] = t[0]*64+t[1]
    return col, nrm, msk, tid

def lerp(a, b, t): return a+(b-a)*t
def hexc(h): return np.array([int(h[i:i+2], 16) for i in (1, 3, 5)], float)/255

def ramp(stops, t):
    t = np.clip(t, 0, 1); out = np.zeros(t.shape+(3,))
    for k in range(len(stops)-1):
        p0, c0 = stops[k]; p1, c1 = stops[k+1]
        m = (t >= p0) & (t <= p1)
        f = ((t-p0)/(p1-p0))[..., None]
        out[m] = (lerp(hexc(c0), hexc(c1), f))[m]
    return out

MATS = {
    # acero pulido
    'acero': dict(ramp=[(0, '#11161b'), (.35, '#3c4650'), (.65, '#7f8c97'), (.88, '#c6d0d8'), (1, '#f2f7fa')], rim='#3dff7a', rimk=.55, glow=False, quant=7, lk=.32),
    # titanio oscuro con vetas verdes que brillan
    'titanio': dict(ramp=[(0, '#07090b'), (.4, '#1d2329'), (.7, '#3b454e'), (.9, '#6c7a86'), (1, '#a9b6c0')], rim='#2bff6a', rimk=.75, glow=True, quant=6, lk=.3),
    # cromo / plata con reflejos de entorno
    'cromo': dict(ramp=[(0, '#0b0f10'), (.2, '#4e5a5f'), (.45, '#d9e4e8'), (.55, '#9fb0b6'), (.75, '#2c3a3e'), (.9, '#cfe9dc'), (1, '#ffffff')], rim='#59ff95', rimk=.45, glow=False, quant=8, env=True),
}

def metal(col, nrm, msk, tid, mat):
    m = MATS[mat]
    lum = col @ np.array([.3, .55, .15])
    L = np.array([-.45, .7, .55]); L /= np.linalg.norm(L)
    nd = np.clip(nrm @ L, 0, 1)
    if m.get('env'):
        t = .5 + .4*nrm[..., 1] - .32*np.abs(nrm[..., 0]) + (lum-.5)*.45
    else:
        t = .2 + .55*nd + (lum-.45)*m.get('lk',.4)
    t = np.round(np.clip(t, 0, 1)*m['quant'])/m['quant']
    out = ramp(m['ramp'], t)
    # luz verde de contorno desde abajo/detrás
    rim = np.clip(-nrm[..., 1]*.4 + np.abs(nrm[..., 0])*.75 - nrm[..., 2]*.3, 0, 1)**1.5
    rim = np.round(rim*4)/4
    out = out + hexc(m['rim'])[None, None, :]*(rim*m['rimk'])[..., None]*.6
    if m['glow']:
        # los texeles más claros (ojos, detalles) brillan en verde
        g = (lum > .68) | ((col[..., 1] > col[..., 0]+.25) & (lum > .5))
        out[g] = hexc('#62ff9a')*.85 + out[g]*.15
    out = np.clip(out, 0, 1)
    # contorno oscuro
    img = np.zeros(msk.shape+(4,))
    img[..., :3] = out; img[..., 3] = msk
    o = msk.copy()
    edge = np.zeros_like(msk)
    for dy, dx in ((0, 1), (0, -1), (1, 0), (-1, 0)):
        edge |= np.roll(np.roll(msk, dy, 0), dx, 1) & ~msk
    img[edge] = [.02, .04, .03, 1]
    return (img*255).astype(np.uint8)

POSES = {
    'MrYotu04': dict(rarm=[('z', -150), ('x', -15)], larm=[('z', 12)], head=[('x', -8), ('y', -10)], rleg=[('z', -4)], lleg=[('z', 4)]),
    'Samugodin': dict(rarm=[('z', -35), ('x', -95)], larm=[('z', 8)], head=[('y', 8)], rleg=[('x', 6)], lleg=[('x', -6)]),
    'MettaYT': dict(rarm=[('x', -55), ('z', 38)], larm=[('x', -55), ('z', -38)], head=[('x', 4)], rleg=[('z', -3)], lleg=[('z', 3)]),
}
YAW = {'MrYotu04': 0, 'Samugodin': 24, 'MettaYT': -24}

if __name__ == '__main__':
    S = int(sys.argv[1]) if len(sys.argv) > 1 else 3
    res = {}
    for sk in POSES:
        col, nrm, msk, tid = render(sk, POSES[sk], S=S, yaw=YAW[sk])
        raw = np.zeros(msk.shape+(4,), np.uint8); raw[..., :3] = (col*255).astype(np.uint8); raw[..., 3] = msk*255
        Image.fromarray(raw).save(f'raw_{sk}.png')
        for mat in MATS:
            im = Image.fromarray(metal(col, nrm, msk, tid, mat))
            im.save(f'st_{sk}_{mat}.png')
            b = io.BytesIO(); im.save(b, 'PNG'); res[f'{sk}_{mat}'] = 'data:image/png;base64,'+base64.b64encode(b.getvalue()).decode()
    json.dump(res, open('statues.json', 'w'))
    print('ok')
