import math, os, sys
OUT_SVG = sys.argv[1]; OUT_XML = sys.argv[2]
INK = "#16202C"

def hx(c):
    return tuple(int(c[i:i+2],16) for i in (1,3,5))
def tohx(t):
    return "#%02X%02X%02X" % tuple(max(0,min(255,int(round(v)))) for v in t)
def lighten(c,f):
    r=hx(c); return tohx(tuple(v+(255-v)*f for v in r))
def darken(c,f):
    r=hx(c); return tohx(tuple(v*(1-f) for v in r))

class Shape:
    def __init__(s,d,fill=None,stroke=None,sw=0,alpha=1.0):
        s.d=d;s.fill=fill;s.stroke=stroke;s.sw=sw;s.alpha=alpha
def fmt(v):
    s=("%.2f"%v).rstrip("0").rstrip(".")
    return "0" if s in ("-0","") else s
def poly(pts,close=True):
    return "M"+" L".join("%s %s"%(fmt(x),fmt(y)) for x,y in pts)+(" Z" if close else "")
def rrect(x,y,w,h,r=0,rb=None):
    rt=r; rb=r if rb is None else rb
    d="M%s %s"%(fmt(x+rt),fmt(y))
    d+=" H%s"%fmt(x+w-rt)
    if rt: d+=" A%s %s 0 0 1 %s %s"%(fmt(rt),fmt(rt),fmt(x+w),fmt(y+rt))
    d+=" V%s"%fmt(y+h-rb)
    if rb: d+=" A%s %s 0 0 1 %s %s"%(fmt(rb),fmt(rb),fmt(x+w-rb),fmt(y+h))
    d+=" H%s"%fmt(x+rb)
    if rb: d+=" A%s %s 0 0 1 %s %s"%(fmt(rb),fmt(rb),fmt(x),fmt(y+h-rb))
    d+=" V%s"%fmt(y+rt)
    if rt: d+=" A%s %s 0 0 1 %s %s"%(fmt(rt),fmt(rt),fmt(x+rt),fmt(y))
    return d+" Z"
def circ(cx,cy,r):
    return "M%s %s A%s %s 0 1 0 %s %s A%s %s 0 1 0 %s %s Z"%(fmt(cx-r),fmt(cy),fmt(r),fmt(r),fmt(cx+r),fmt(cy),fmt(r),fmt(r),fmt(cx-r),fmt(cy))

def top_halo(shapes_d):
    return [Shape(d,fill="#FFFFFF",stroke="#FFFFFF",sw=3.6) for d in shapes_d]
def body(d,fill): return Shape(d,fill=fill,stroke=INK,sw=0.9)
def fl(d,fill): return Shape(d,fill=fill)
def ln(d,col=INK,sw=0.9): return Shape(d,stroke=col,sw=sw)

def top_arrow(): # viewport 24x24 (как было)
    d="M12 2 L19.5 21.5 L12 17 L4.5 21.5 Z"
    return [Shape(d,fill="#00A0F5",stroke="#FFFFFF",sw=2.2)]

# ---------------- ISOMETRY ----------------
C30=math.cos(math.radians(30)); S30=0.5
def P(x,y,z): return ((x-y)*C30,(x+y)*S30-z)   # X -> вправо-вниз, Y -> влево-вниз, Z вверх
def hull(pts):
    pts=sorted(set((round(a,4),round(b,4)) for a,b in pts))
    def cr(o,a,b): return (a[0]-o[0])*(b[1]-o[1])-(a[1]-o[1])*(b[0]-o[0])
    lo=[]
    for p in pts:
        while len(lo)>=2 and cr(lo[-2],lo[-1],p)<=0: lo.pop()
        lo.append(p)
    up=[]
    for p in reversed(pts):
        while len(up)>=2 and cr(up[-2],up[-1],p)<=0: up.pop()
        up.append(p)
    return lo[:-1]+up[:-1]

class Iso:
    def __init__(s): s.items=[]
    def poly3(s,pts3,fill,alpha=1.0): s.items.append(("poly",[P(*p) for p in pts3],fill,alpha))
    def box(s,x0,x1,y0,y1,z0,z1,base,top=None,side=None,end=None):
        top=top or lighten(base,0.22); side=side or base; end=end or darken(base,0.25)
        s.poly3([(x1,y0,z0),(x1,y1,z0),(x1,y1,z1),(x1,y0,z1)],end)
        s.poly3([(x0,y1,z0),(x1,y1,z0),(x1,y1,z1),(x0,y1,z1)],side)
        s.poly3([(x0,y0,z1),(x1,y0,z1),(x1,y1,z1),(x0,y1,z1)],top)
    def qY(s,x0,x1,z0,z1,y,col): s.poly3([(x0,y,z0),(x1,y,z0),(x1,y,z1),(x0,y,z1)],col)
    def qX(s,y0,y1,z0,z1,x,col): s.poly3([(x,y0,z0),(x,y1,z0),(x,y1,z1),(x,y0,z1)],col)
    def circY(s,cx,cz,r,y,col,n=20):
        s.items.append(("poly",[P(cx+r*math.cos(2*math.pi*i/n),y,cz+r*math.sin(2*math.pi*i/n)) for i in range(n)],col,1.0))
    def circZ(s,cx,cy,r,z,col,n=20):
        s.items.append(("poly",[P(cx+r*math.cos(2*math.pi*i/n),cy+r*math.sin(2*math.pi*i/n),z) for i in range(n)],col,1.0))
    def wheel(s,cx,cz,r,y,rim,hub):
        s.circY(cx,cz,r,y,rim); s.circY(cx,cz,r*0.45,y,hub)
    def cylX(s,x0,x1,cy,cz,r,base,n=24):
        def ring(x,a0,a1,k=12): return [P(x,cy+r*math.cos(a0+(a1-a0)*i/k),cz+r*math.sin(a0+(a1-a0)*i/k)) for i in range(k+1)]
        allp=ring(x0,0,2*math.pi,n)+ring(x1,0,2*math.pi,n)
        s.items.append(("poly",hull(allp),darken(base,0.25),1.0))
        a0,a1=math.radians(30),math.radians(120)
        s.items.append(("poly",hull(ring(x0,a0,a1)+ring(x1,a0,a1)),base,1.0))
        a0,a1=math.radians(70),math.radians(105)
        s.items.append(("poly",hull(ring(x0,a0,a1)+ring(x1,a0,a1)),lighten(base,0.28),1.0))
        s.items.append(("poly",[P(x1,cy+r*math.cos(2*math.pi*i/n),cz+r*math.sin(2*math.pi*i/n)) for i in range(n)],darken(base,0.1),1.0))
    def cylZ(s,cx,cy,z0,z1,r,base,n=20):
        def ring(z): return [P(cx+r*math.cos(2*math.pi*i/n),cy+r*math.sin(2*math.pi*i/n),z) for i in range(n)]
        s.items.append(("poly",hull(ring(z0)+ring(z1)),base,1.0))
        s.items.append(("poly",ring(z1),lighten(base,0.3),1.0))
    def line3(s,p1,p2,col,w): s.items.append(("line",P(*p1),P(*p2),col,w))
    def shadow(s,x0,x1,y0,y1):
        e=0.7
        s.items.insert(0,("poly",[P(x0-e,y0-e+0.8,0),P(x1+e,y0-e+0.8,0),P(x1+e,y1+e+0.8,0),P(x0-e,y1+e+0.8,0)],"#16202C",0.16))

def bogies(iso,xs,y0,y1):
    for x in xs:
        iso.box(x-1.5,x+1.5,y0,y1,0.15,1.15,"#2B2F38")
        for dx in (-0.85,0.85):
            iso.wheel(x+dx,0.6,0.55,y1,"#1B1E24","#8A929C")

def panto(iso,x):
    iso.line3((x-1.2,1.5,3.95),(x,0.5,5.0),"#2B2F38",0.18); iso.line3((x-1.2,1.5,3.95),(x,2.5,5.0),"#2B2F38",0.18)
    iso.line3((x,0.3,5.05),(x,2.7,5.05),"#2B2F38",0.28)

def iso_arrow():
    iso=Iso()
    poly2=[(5.0,0),(-2.2,3.2),(0,0),(-2.2,-3.2)]
    h=0.9
    iso.shadow(-2.2,5,-3.2,3.2)
    area=sum(poly2[i][0]*poly2[(i+1)%4][1]-poly2[(i+1)%4][0]*poly2[i][1] for i in range(4))
    sg=1 if area>0 else -1
    vis=[]
    for i in range(4):
        a=poly2[i]; b=poly2[(i+1)%4]
        ex,ey=(b[1]-a[1])*sg,-(b[0]-a[0])*sg
        if ex+ey>0: vis.append((a,b,ex,ey))
    vis.sort(key=lambda e:(e[0][0]+e[0][1]+e[1][0]+e[1][1]))
    for a,b,ex,ey in vis:
        col="#0079BC" if ey>ex else "#005F96"
        iso.poly3([(a[0],a[1],0.1),(b[0],b[1],0.1),(b[0],b[1],h),(a[0],a[1],h)],col)
    iso.poly3([(x,y,h) for x,y in poly2],"#00A0F5")
    return iso

# ================= V2: новые рисунки =================
GAP="#3A4048"
class V:
    """отображение координат секции: flip=True -> задняя секция (зеркало по Y)"""
    def __init__(s,flip): s.f=flip
    def y(s,y): return 60-y if s.f else y
    def pt(s,x,y): return (x,s.y(y))
    def poly(s,pts): return poly([s.pt(*p) for p in pts])
    def rect(s,x0,y0,x1,y1,r=0):
        a,b=sorted((s.y(y0),s.y(y1))); return rrect(x0,a,x1-x0,b-a,r)
    def circ(s,cx,cy,r): return circ(cx,s.y(cy),r)
    def line(s,pts): return "M"+" L".join("%s %s"%(fmt(x),fmt(s.y(y))) for x,y in pts)

def panto_top(v,cx,cy,col="#2B2F38"):
    return [ln(v.poly([(cx,cy-4),(cx+3,cy),(cx,cy+4),(cx-3,cy)]),col,0.8),ln(v.line([(cx-5,cy-1.2),(cx+5,cy-1.2)]),col,1.1)]

def outline(v,cx,cy,y_end=29,x0=2,x1=20,yf=2):
    return v.poly([(x0,y_end),(x0,yf+cy),(x0+cx,yf),(x1-cx,yf),(x1,yf+cy),(x1,y_end)])

def top_vl(kind):
    green="#2F6B3F"; dark="#1C4128"; yel="#F2C230"; roof="#AEB5BD"; win="#22313F"
    s=[]; secs=[V(False),V(True)]
    s+=top_halo([outline(v,3,4.5) for v in secs])
    s.append(body("M6 28 H16 V32 H6 Z",GAP))
    for v in secs:
        s.append(body(outline(v,3,4.5),green))
        s.append(fl(v.rect(3.4,11,18.6,28.5),lighten(green,0.12)))
        s.append(fl(v.poly([(4.6,6.4),(5.6,3.3),(10.5,3.3),(10.5,6.4)]),win))
        s.append(fl(v.poly([(11.5,6.4),(11.5,3.3),(16.4,3.3),(17.4,6.4)]),win))
        s.append(ln(v.line([(4.2,7.1),(17.8,7.1)]),dark,0.7))
        if kind=="vl80":
            s.append(ln(v.line([(3.6,8.3),(18.4,8.3)]),dark,0.6))
            s.append(fl(v.poly([(4.4,7.2),(4.8,8.2),(17.2,8.2),(17.6,7.2)]),lighten(green,0.25)))
        for x in (2.9,19.1):
            s.append(ln(v.line([(x,9),(x,28.5)]),yel,1.0 if kind=="vl10" else 0.7))
        s.append(fl(v.rect(5.2,11.8,16.8,27.6,0.8),roof))
        if kind=="vl10":
            for y0 in (12.6,19.6): s.append(fl(v.rect(6.2,y0,15.8,y0+3.6,0.5),"#8C949D"))
            s.append(fl(v.rect(6.2,24.8,15.8,26.8,0.5),"#8C949D"))
        else:
            for yy in range(13,27,2): s.append(ln(v.line([(6,yy),(16,yy)]),"#6D767F",0.55))
        s+=panto_top(v,11,19.5)
        s.append(ln(v.line([(5,28),(17,28)]),dark,0.6))
        s.append(fl(v.circ(11,2.9,0.9),"#FFD84A"))
        s.append(fl(v.circ(5.4,3.0,0.7),"#FFFFFF")); s.append(fl(v.circ(16.6,3.0,0.7),"#FFFFFF"))
    return s

def top_es6():
    red="#C8302B"; grey="#D3D8DE"; dg="#56606B"; win="#1F2E40"
    s=[]; secs=[V(False),V(True)]
    s+=top_halo([outline(v,4.5,7) for v in secs])
    s.append(body("M6 28 H16 V32 H6 Z",GAP))
    for v in secs:
        s.append(body(outline(v,4.5,7),red))
        s.append(fl(v.poly([(2.4,28.6),(2.4,15),(3.2,11.6),(4.6,11.6),(4.6,28.6)]),grey))
        s.append(fl(v.poly([(19.6,28.6),(19.6,15),(18.8,11.6),(17.4,11.6),(17.4,28.6)]),grey))
        s.append(fl(v.rect(2.2,18,3.0,28.6),dg)); s.append(fl(v.rect(19.0,18,19.8,28.6),dg))
        s.append(fl(v.poly([(2.4,16.5),(2.4,12.6),(4.6,11.6),(4.6,19.5)]),red)); s.append(fl(v.poly([(19.6,16.5),(19.6,12.6),(17.4,11.6),(17.4,19.5)]),red))
        s.append(fl(v.poly([(2.4,28.6),(2.4,23),(3.5,25.6),(4.6,27.2),(4.6,28.6)]),red)); s.append(fl(v.poly([(19.6,28.6),(19.6,23),(18.5,25.6),(17.4,27.2),(17.4,28.6)]),red))
        s.append(fl(v.rect(4.6,11.4,17.4,28.2,1),"#C3CAD2"))
        s.append(ln(v.line([(4.9,12),(4.9,27.6)]),red,0.9)); s.append(ln(v.line([(17.1,12),(17.1,27.6)]),red,0.9))
        for y0 in (13,19.5):
            s.append(fl(v.rect(5.8,y0,16.2,y0+4.6,0.6),"#A9B1BA"))
        s.append(fl(v.poly([(5.2,7.6),(7,3.6),(15,3.6),(16.8,7.6)]),win))
        s.append(ln(v.line([(11,3.6),(11,7.6)]),red,0.6))
        s.append(fl(v.poly([(4.6,9.9),(5.0,8.7),(17.0,8.7),(17.4,9.9)]),"#8E1F1B"))
        s.append(ln(v.line([(2.9,9.2),(6.9,2.9),(15.1,2.9),(19.1,9.2)]),"#FFFFFF",0.7))
        s+=panto_top(v,11,24,red)
        s.append(fl(v.rect(8.2,12.4,13.8,14.0,0.4),"#2B2F38"))
        s.append(fl(v.circ(7.4,3.2,0.8),"#FFFFFF")); s.append(fl(v.circ(14.6,3.2,0.8),"#FFFFFF"))
        s.append(ln(v.line([(5,28.6),(17,28.6)]),"#7D8793",0.6))
    return s

def top_te25():
    red="#CF3328"; grey="#A5ADB5"; org="#F29A2E"; win="#22313F"
    s=[]; secs=[V(False),V(True)]
    def out(v):
        return v.poly([(2,29),(2,8),(2.4,5),(3.6,3.2),(5.6,2.2),(8.5,1.8),(13.5,1.8),(16.4,2.2),(18.4,3.2),(19.6,5),(20,8),(20,29)])
    s+=top_halo([out(v) for v in secs])
    s.append(body("M6 28 H16 V32 H6 Z",GAP))
    for v in secs:
        s.append(body(out(v),red))
        s.append(fl(v.poly([(2.8,6.4),(3.7,3.9),(5.8,2.8),(8.5,2.4),(13.5,2.4),(16.2,2.8),(18.3,3.9),(19.2,6.4),(17.2,5.6),(15.4,4.6),(6.6,4.6),(4.8,5.6)]),"#2E343C"))
        s.append(fl(v.poly([(5.4,9.6),(6.2,5.8),(10.5,5.2),(10.5,9.6)]),win))
        s.append(fl(v.poly([(11.5,9.6),(11.5,5.2),(15.8,5.8),(16.6,9.6)]),win))
        s.append(fl(v.circ(11,4.2,0.8),"#FFE27A"))
        s.append(fl(v.rect(4,10.4,18,13.6,0.8),lighten(red,0.15)))
        s.append(fl(v.rect(3.4,14,18.6,28.6,0.8),grey))
        s.append(fl(v.poly([(3.4,14),(5.4,14),(5.4,16.6),(4.4,18.6),(3.4,18.6)]),red)); s.append(fl(v.poly([(18.6,14),(16.6,14),(16.6,16.6),(17.6,18.6),(18.6,18.6)]),red))
        s.append(fl(v.rect(5.4,14,16.6,28.6,0.6),"#363C44"))
        for xx in (5.4,16.6): s.append(ln(v.line([(xx,14),(xx,28.6)]),"#7C838B",0.6))
        for cy in (18.5,24.5):
            s.append(fl(v.circ(11,cy,2.6),"#1E2228")); s.append(ln(v.circ(11,cy,1.3),"#6B747E",0.6))
            s.append(ln(v.line([(8.6,cy),(13.4,cy)]),"#6B747E",0.5)); s.append(ln(v.line([(11,cy-2.4),(11,cy+2.4)]),"#6B747E",0.5))
        for yy in (15.6,21.4,27.0): s.append(ln(v.line([(7,yy),(15,yy)]),"#4A525B",0.5))
        s.append(ln(v.line([(2,28.6),(20,28.6)]),"#8E2418",0.6))
    return s

def top_swallow():
    w="#F7F8FA"; red="#D9352B"; dg="#3A4350"; win="#1F2E40"
    lens=[11.4,9.9,9.9,9.9,11.4]; gap=0.8
    ys=[2]
    for L in lens: ys.append(ys[-1]+L+gap)
    cars=[(ys[i],ys[i]+lens[i]) for i in range(5)]
    def car(i):
        a,b=cars[i]
        if i==0: return poly([(3,b),(3,a+7),(3.5,a+4.2),(5,a+2),(7.5,a+0.6),(14.5,a+0.6),(17,a+2),(18.5,a+4.2),(19,a+7),(19,b)])
        if i==4: return poly([(3,a),(3,b-7),(3.5,b-4.2),(5,b-2),(7.5,b-0.6),(14.5,b-0.6),(17,b-2),(18.5,b-4.2),(19,b-7),(19,a)])
        return rrect(3,a,16,b-a,1.2)
    s=top_halo([car(i) for i in range(5)])
    for i in range(4):
        s.append(body("M6.5 %s H15.5 V%s H6.5 Z"%(fmt(cars[i][1]-0.3),fmt(cars[i+1][0]+0.3)),dg))
    for i in range(5):
        a,b=cars[i]; s.append(body(car(i),w))
        for x in (3.7,18.3): s.append(ln("M%s %s V%s"%(fmt(x),fmt(a+(6 if i==0 else 1.2)),fmt(b-(6 if i==4 else 1.2))),"#E8542B",1.5))
        y0=a+(7 if i==0 else 1.8); y1=b-(7 if i==4 else 1.8)
        s.append(fl(rrect(5.6,y0,10.8,y1-y0,0.8),"#4B535D"))
        if y1-y0>6:
            for k in range(1,int((y1-y0)//2.4)):
                s.append(ln("M6.4 %s H15.6"%fmt(y0+k*2.4),"#68717C",0.45))
    a,b=cars[0]
    NOSE="M4 %s Q4 %s 5.4 %s Q8 %s 11 %s Q14 %s 16.6 %s Q18 %s 18 %s L18 %s L4 %s Z"%(fmt(a+7),fmt(a+3.8),fmt(a+2.2),fmt(a+1.1),fmt(a+1.1),fmt(a+1.1),fmt(a+2.2),fmt(a+3.8),fmt(a+7),fmt(a+7),fmt(a+7))
    s.append(fl(NOSE,"#F08A2C"))
    s.append(fl("M4 %s Q4 %s 5.4 %s L16.6 %s Q18 %s 18 %s Z"%(fmt(a+7),fmt(a+4.6),fmt(a+3.4),fmt(a+3.4),fmt(a+4.6),fmt(a+7)),red))
    s.append(fl(poly([(4.8,a+7),(4.6,a+5.4),(6.4,a+3.3),(15.6,a+3.3),(17.4,a+5.4),(17.2,a+7)]),"#8C949D"))
    s.append(fl(poly([(5.4,a+6.4),(6.6,a+3.9),(15.4,a+3.9),(16.6,a+6.4)]),win))
    s.append(fl(circ(6.2,a+1.9,0.6),"#FFE27A")); s.append(fl(circ(15.8,a+1.9,0.6),"#FFE27A"))
    a,b=cars[4]
    s.append(fl("M4 %s Q4 %s 5.4 %s Q8 %s 11 %s Q14 %s 16.6 %s Q18 %s 18 %s L18 %s L4 %s Z"%(fmt(b-7),fmt(b-3.8),fmt(b-2.2),fmt(b-1.1),fmt(b-1.1),fmt(b-1.1),fmt(b-2.2),fmt(b-3.8),fmt(b-7),fmt(b-7),fmt(b-7)),"#F08A2C"))
    s.append(fl("M4 %s Q4 %s 5.4 %s L16.6 %s Q18 %s 18 %s Z"%(fmt(b-7),fmt(b-4.6),fmt(b-3.4),fmt(b-3.4),fmt(b-4.6),fmt(b-7)),red))
    s.append(fl(poly([(4.8,b-7),(4.6,b-5.4),(6.4,b-3.3),(15.6,b-3.3),(17.4,b-5.4),(17.2,b-7)]),"#8C949D"))
    s.append(fl(poly([(5.4,b-6.4),(6.6,b-3.9),(15.4,b-3.9),(16.6,b-6.4)]),"#33465C"))
    s.append(fl(circ(6.2,b-1.9,0.6),"#E23B3B")); s.append(fl(circ(15.8,b-1.9,0.6),"#E23B3B"))
    for i in (1,3):
        a,b=cars[i]; cy=(a+b)/2
        s+=[ln("M11 %s L14.2 %s L11 %s L7.8 %s Z"%(fmt(cy-3.4),fmt(cy),fmt(cy+3.4),fmt(cy)),"#2B2F38",0.8),ln("M6 %s H16"%fmt(cy-1),"#2B2F38",1.1)]
    return s

def top_steam():
    blk="#2B2E35"; red="#C0292B"
    s=[]
    halo=[rrect(3,10,16,26,2),rrect(6,4,10,24,4),rrect(4.5,36,13,22,2)]
    s+=top_halo(halo)
    for y in (11,18.5,26):
        for x in (3,16.6): s.append(body(rrect(x,y,2.4,5.2,1),red))
    for y in (39,47,55):
        for x in (3.4,16.2): s.append(body(rrect(x,y-2,2.4,4.4,1),"#7A2A2B"))
    s.append(body(rrect(5.4,8,11.2,28,1),red))
    s.append(body(rrect(4.5,36,13,22,2),blk))
    s.append(fl(rrect(6,38,10,17,1),"#4A4F59"))
    for (cx,cy,r) in ((9,41,1.6),(13,43,1.3),(10,47,1.5),(13.5,50,1.2),(8.5,52,1.1)): s.append(fl(circ(cx,cy,r),"#2A2D33"))
    s.append(ln("M6 38 H16 M6 55 H16","#7C838D",0.6))
    s.append(body(rrect(3.5,26,15,10,2),blk)); s.append(fl(rrect(5,28,12,6,1),"#3B3F48"))
    s.append(ln("M4.5 31 H17.5","#555C66",0.5))
    s.append(body(rrect(6.2,6,9.6,21,4.5),blk))
    s.append(fl(rrect(7.6,9,1.8,16,0.9),"#555C66"))
    for yy in (11,16,21): s.append(ln("M6.4 %s H15.6"%yy,"#4A5059",0.7))
    s.append(fl(circ(11,14.5,1.7),"#767D88")); s.append(fl(circ(11,21,1.5),"#767D88"))
    s.append(body(circ(11,8,2.5),"#14171C")); s.append(fl(circ(11,8,1.1),"#666D78"))
    s.append(body("M11 0.8 L16 6.2 H6 Z",red))
    s.append(fl(circ(11,3.6,0.7),"#FFD84A"))
    return s


def top_chme3():
    G="#1F8A3C"; GD="#146A2C"; GL="#3FB05A"; R="#C8302B"; OR="#F07A2A"; Y="#F5C518"; roof="#AEB5BD"; win="#22313F"
    s=[]
    out=rrect(2,1,18,58,1.6)
    s+=top_halo([out])
    s.append(body(out,R))                                  # красная рама / нижний пояс
    s.append(fl(rrect(2.4,1.4,17.2,3.6,0.8),OR))            # буферная балка спереди
    s.append(fl(rrect(2.4,55.2,17.2,3.4,0.8),OR))           # и сзади
    for x in (3.4,18.6): s.append(fl(circ(x,2.5,0.8),"#FFE9B0")); s.append(fl(circ(x,57.5,0.8),"#FFE9B0"))
    # капот (длинный, ниже кабины)
    s.append(body(rrect(5.4,29,11.2,26,1.2),G))
    s.append(fl(rrect(6.4,30.4,9.2,23.2,0.8),GL))
    for yy in (33,36.5,40,43.5): s.append(ln("M6.6 %s H15.4"%yy,GD,0.6))
    s.append(fl(rrect(7,45,8,5,0.8),"#8C949D")); s.append(ln("M8.4 47.5 H13.6","#5A626B",0.5))     # вентиляционный люк
    s.append(fl(circ(11,52.2,1.8),"#2B2F38")); s.append(fl(circ(11,52.2,0.8),"#6B747E"))            # вентилятор
    s.append(fl(rrect(5.4,29,1.0,26,0.5),GD)); s.append(fl(rrect(15.6,29,1.0,26,0.5),GD))
    # лобовая «тигровая» маска: жёлтый шеврон + красная полоса + звезда
    s.append(body(rrect(3.6,4.2,14.8,5.4,0.8),Y))
    s.append(fl(poly([(3.9,7.6),(11,5.0),(18.1,7.6),(18.1,9.0),(11,6.4),(3.9,9.0)]),R))
    s.append(fl(poly([(11,6.9),(11.5,7.9),(12.5,7.9),(11.7,8.5),(12,9.0),(11,8.9),(10,9.0),(10.3,8.5),(9.5,7.9),(10.5,7.9)]),R))
    s.append(body(rrect(5.4,9.6,11.2,3.6,0.8),G)); s.append(fl(rrect(6.4,10.2,9.2,2.4,0.6),GL))   # короткий капот/площадка перед кабиной
    # кабина (высокая, угловатая, ближе к носу)
    s.append(body(rrect(3.8,12.6,14.4,16.4,1.4),G))
    s.append(fl(rrect(5.0,13.8,12,3.4,0.8),win))                                             # лобовые стёкла
    s.append(ln("M11 13.8 V17.2",GD,0.6))
    s.append(fl(rrect(5.0,18.4,12,9.0,1.0),GL))
    s.append(fl(rrect(7.4,20,7.2,5.2,0.8),roof)); s.append(ln("M8.2 22.6 H13.8","#8C949D",0.55))
    for x in (4.3,16.9): s.append(fl(rrect(x,15,0.8,11,0.4),win))                           # боковые окна
    # жёлтые поручни вдоль рамы и по торцам
    for x in (2.55,19.45): s.append(ln("M%s 4.5 V55.5"%x,Y,0.9))
    s.append(ln("M2.55 4.5 H19.45",Y,0.7)); s.append(ln("M2.55 55.5 H19.45",Y,0.7))
    return s

TOPS={"arrow":(24,24,top_arrow),"vl10":(22,60,lambda:top_vl("vl10")),"vl80":(22,60,lambda:top_vl("vl80")),"es6":(22,60,top_es6),"te25km":(22,60,top_te25),
      "swallow":(22,60,top_swallow),"steam":(22,60,top_steam),"chme3":(22,60,top_chme3)}

# ---------- изометрия ----------
def slope_pt(b,dx,zs,z1,t,y): return (b-dx*t,y,zs+(z1-zs)*t)

def sec(iso,a,b,base,z0,z1,fdx=0,fzs=None,bdx=0,bzs=None,y0=0,y1=3,top=None,side=None,end=None,frame=None,pane="#22313F"):
    top=top or lighten(base,0.22); side=side or base; end=end or darken(base,0.25); frame=frame or lighten(base,0.1)
    fzs=fzs if fzs is not None else z1
    bzs=bzs if bzs is not None else z1
    if not fdx: fzs=z1  # торец без скоса — на полную высоту крыши (иначе щель между крышей кабины и капота)
    iso.poly3([(b,y0,z0),(b,y1,z0),(b,y1,fzs),(b,y0,fzs)],end)
    if fdx:
        iso.poly3([slope_pt(b,fdx,fzs,z1,0,y0),slope_pt(b,fdx,fzs,z1,0,y1),slope_pt(b,fdx,fzs,z1,1,y1),slope_pt(b,fdx,fzs,z1,1,y0)],frame)
        m=(y0+y1)/2
        for (p0,p1) in ((y0+0.28,m-0.07),(m+0.07,y1-0.28)):
            iso.poly3([slope_pt(b,fdx,fzs,z1,0.14,p0),slope_pt(b,fdx,fzs,z1,0.14,p1),slope_pt(b,fdx,fzs,z1,0.86,p1),slope_pt(b,fdx,fzs,z1,0.86,p0)],pane)
            iso.poly3([slope_pt(b,fdx,fzs,z1,0.14,p0),slope_pt(b,fdx,fzs,z1,0.14,p0+0.25),slope_pt(b,fdx,fzs,z1,0.86,p0+0.25),slope_pt(b,fdx,fzs,z1,0.86,p0)],lighten(pane,0.18))
    pts=[(a,y1,z0),(b,y1,z0),(b,y1,fzs)]
    if fdx: pts.append((b-fdx,y1,z1))
    else: pts.append((b,y1,z1))
    if bdx: pts+= [(a+bdx,y1,z1),(a,y1,bzs)]
    else: pts.append((a,y1,z1))
    iso.poly3(pts,side)
    iso.poly3([(a+bdx,y0,z1),(b-fdx,y0,z1),(b-fdx,y1,z1),(a+bdx,y1,z1)],top)

def panto2(iso,x,zr,ys=1.5,arm=1.3,h=1.1,col="#2B2F38"):
    iso.box(x-0.5,x+0.5,ys-0.6,ys+0.6,zr,zr+0.12,"#7C848C")
    iso.line3((x-arm,ys,zr+0.12),(x,ys-0.9,zr+h+0.12),col,0.18); iso.line3((x-arm,ys,zr+0.12),(x,ys+0.9,zr+h+0.12),col,0.18)
    iso.line3((x,ys-1.1,zr+h+0.17),(x,ys+1.1,zr+h+0.17),col,0.28)

def louvre(iso,x0,x1,z0,z1,y,col,n=4):
    iso.qY(x0,x1,z0,z1,y,col)
    lc=lighten(col,0.18)
    for i in range(1,n):
        xx=x0+(x1-x0)*i/n; iso.qY(xx-0.04,xx+0.04,z0+0.1,z1-0.1,y,lc)

def seam(iso,x,z0,z1,col): iso.qY(x-0.05,x+0.05,z0,z1,3,col)

def iso_vl(kind):
    iso=Iso(); iso.shadow(0,18.5,0,3)
    bogies(iso,[2.4,6.6,11.9,16.1],0.35,2.65)
    G="#2F6B3F"; DK="#1C4128"; Y="#F2C230"; z1=3.7
    iso.box(0.3,18.2,0.3,2.7,0.55,1.0,"#2B2F38")
    for (a,b,front) in ((0,9.25,False),(9.25,18.5,True)):
        fdx,fzs = (0.55,2.4) if kind=="vl10" else (0.95,2.1)
        sec(iso,a,b,G,1.0,z1,fdx=fdx if front else 0,fzs=fzs,bdx=0 if front else fdx,bzs=fzs)
        iso.box(a+1.3,b-1.3,0.45,2.55,z1,z1+0.28,"#AEB5BD")
        for i in range(3):
            xx=a+2.2+i*1.9; iso.box(xx,xx+1.1,0.8,2.2,z1+0.28,z1+0.45,"#8C949D")
        panto2(iso,(a+b)/2,z1+0.28,arm=1.4)
        if kind=="vl10":
            iso.qY(a,b,1.85,2.3,3,Y); iso.qY(a,b,1.0,1.25,3,DK)
            for i in range(3): louvre(iso,a+2.4+i*1.9,a+3.5+i*1.9,2.45,3.4,3,DK,3)
        else:
            iso.qY(a+(0 if front else 0.6),b-(0.45 if front else 0),2.65,2.9,3,Y); iso.qY(a,b,1.45,1.65,3,Y)
            for i in range(3): louvre(iso,a+2.2+i*1.9,a+3.5+i*1.9,1.8,2.55,3,DK,4)
            for i in range(3): louvre(iso,a+2.2+i*1.9,a+3.5+i*1.9,2.95,3.5,3,DK,4)
        if front: iso.qY(b-2.6,b-1.3,2.3,3.3,3,"#22313F")
        else: iso.qY(a+0.4,a+1.3,2.3,3.3,3,"#22313F")
    seam(iso,9.25,1.0,3.7,"#12301C")
    iso.qX(0.3,0.9,1.3,1.8,18.5,"#FFD84A"); iso.qX(2.1,2.7,1.3,1.8,18.5,"#FFD84A")
    iso.qX(0.3,2.7,1.0,1.2,18.5,"#C22B2B")
    return iso

def iso_es6():
    iso=Iso(); iso.shadow(0,19,0,3)
    bogies(iso,[2.4,6.8,12.2,16.6],0.35,2.65)
    RED="#C8302B"; GR="#D3D8DE"; DG="#56606B"; z1=3.7
    iso.box(0.3,18.7,0.3,2.7,0.55,1.0,"#2B2F38")
    for (a,b,front) in ((0,9.5,False),(9.5,19,True)):
        sec(iso,a,b,GR,1.0,z1,fdx=1.5 if front else 0,fzs=1.9,bdx=0 if front else 1.5,bzs=1.9,frame=RED,top="#C3CAD2",end=RED)
        iso.qY(a,b,1.0,1.55,3,"#454E58")
        iso.qY(a,b,3.3,3.7,3,RED)
        if front:
            iso.poly3([slope_pt(b,1.5,1.9,z1,0.84,0.0),slope_pt(b,1.5,1.9,z1,0.84,3.0),slope_pt(b,1.5,1.9,z1,1,3.0),slope_pt(b,1.5,1.9,z1,1,0.0)],"#8E1F1B")
            iso.line3((b-1.5,3,z1),(b,3,1.9),"#FFFFFF",0.14)
        if front:
            iso.poly3([(a,3,2.95),(a,3,z1),(b-1.5,3,z1),(b,3,1.9),(b,3,1.55),(b-1.3,3,1.55),(b-2.4,3,2.1),(b-4.2,3,2.75),(b-6,3,2.95)],RED)
        else:
            iso.poly3([(b,3,2.95),(b,3,z1),(a+1.5,3,z1),(a,3,1.9),(a,3,1.55),(a+1.3,3,1.55),(a+2.4,3,2.1),(a+4.2,3,2.75),(a+6,3,2.95)],RED)
        iso.box(a+1.3,b-1.3,0.5,2.5,z1,z1+0.28,"#C9CFD6")
        iso.box(a+3.0,a+5.0,0.8,2.2,z1+0.28,z1+0.45,"#A9B1BA")
        for i in range(3): louvre(iso,a+(2.6 if front else 1.9)+i*1.7,a+(3.5 if front else 2.8)+i*1.7,1.8,2.7,3,"#9AA3AC",3)
        panto2(iso,(b-4.2) if front else (a+4.2),z1+0.28,arm=1.5,col="#C8302B")
        if front: iso.qY(b-2.6,b-1.6,2.35,3.2,3,"#1F2E40")
        else: iso.qY(a+0.5,a+1.2,2.35,3.2,3,"#1F2E40")
    seam(iso,9.5,1.0,3.7,"#8E959D")
    iso.qX(0.3,0.9,1.3,1.7,19,"#FFFFFF"); iso.qX(2.1,2.7,1.3,1.7,19,"#FFFFFF")
    iso.qX(0.1,2.9,1.0,1.12,19,"#FFFFFF")
    return iso

def iso_te25():
    iso=Iso(); iso.shadow(0,18.5,0,3)
    bogies(iso,[2.4,6.6,11.9,16.1],0.35,2.65)
    RED="#CF3328"; GR="#A5ADB5"; DGR="#6F7882"; ORG="#F29A2E"
    iso.box(0.3,18.2,0.3,2.7,0.55,1.0,"#2B2F38")
    for (a,b,front) in ((0,9.25,False),(9.25,18.5,True)):
        h0,h1=(a+3.3,b) if not front else (a,b-3.3)
        ca,cb=(b-3.3,b) if front else (a,a+3.3)
        def hood():
            sec(iso,h0,h1,GR,1.0,3.5,top="#3A4048")
            e=h1 if not front else h1
            if front: iso.poly3([(h1,3,1.0),(h1,3,3.5),(h1-0.8,3,3.5),(h1-1.6,3,2.5),(h1-2.6,3,2.5),(h1-3.2,3,1.5),(h1-3.2,3,1.0)],RED)
            else: iso.poly3([(h0,3,1.0),(h0,3,3.5),(h0+0.8,3,3.5),(h0+1.6,3,2.5),(h0+2.6,3,2.5),(h0+3.2,3,1.5),(h0+3.2,3,1.0)],RED)
            iso.qY(h0,h1,1.0,1.5,3,"#4A525B")
            for i in range(2): louvre(iso,h0+0.8+i*1.8,h0+1.8+i*1.8,1.8,3.1,3,DGR,3)
            iso.qY(h0+3.9,h0+4.4,2.0,3.2,3,"#22313F"); iso.qY(h0+4.7,h0+5.2,2.0,3.2,3,"#22313F")
            for cx in (h0+1.7,h0+4.2):
                iso.circZ(cx,1.5,0.8,3.52,"#1E2228"); iso.circZ(cx,1.5,0.42,3.55,"#6B747E")
            ox=h0+2.2 if front else h0+2.2
            iso.circY(ox,2.3,0.32,3,RED); iso.circY(ox,2.3,0.1,3,"#FFFFFF")
        def cab():
            sec(iso,ca,cb,RED,1.0,4.05,fdx=0.9 if front else 0,fzs=2.5,bdx=0 if front else 0.9,bzs=2.5,top="#2E343C")
            if front: iso.qY(cb-2.5,cb-1.2,2.4,3.5,3,"#22313F")
            else: iso.qY(ca+0.5,ca+1.8,2.4,3.5,3,"#22313F")
        if front: hood(); cab()
        else: cab(); hood()
    iso.qX(0.05,2.95,1.0,1.4,18.5,RED); iso.qX(0.05,2.95,1.0,1.1,18.5,"#2B2F38")
    iso.qX(0.4,0.9,1.5,1.9,18.5,"#FFFFFF"); iso.qX(2.1,2.6,1.5,1.9,18.5,"#FFFFFF")
    seam(iso,9.25,1.0,3.5,"#555C66")
    return iso

def iso_swallow():
    iso=Iso(); n=5; L=5.0; g=0.12; Wd=2.9; z1=3.8; WH="#F7F8FA"; RED="#D9352B"; DG="#3A4350"
    tot=n*L+(n-1)*g
    iso.shadow(0,tot,0,Wd)
    xs=[i*(L+g) for i in range(n)]
    for a in xs:
        xc=a+L/2
        bogies(iso,[xc-1.0,xc+1.0],0.3,Wd-0.3)
    for i,a in enumerate(xs):
        b=a+L; first=i==0; last=i==n-1
        sec(iso,a,b,WH,1.0,z1,y1=Wd,fdx=1.6 if last else 0,fzs=2.0,bdx=1.6 if first else 0,bzs=2.0,top="#4B535D",end="#F08A2C" if last else DG,frame="#7C848E" if (first or last) else WH,pane="#1F2E40")
        sx0=a+(1.8 if first else 0.3); sx1=b-(1.8 if last else 0.3)
        iso.qY(sx0,sx1,2.0,3.2,Wd,"#1F2E40")
        k=max(1,int((sx1-sx0)//1.1))
        for j in range(1,k):
            xx=sx0+(sx1-sx0)*j/k; iso.qY(xx-0.07,xx+0.07,2.0,3.2,Wd,WH)
        iso.qY(a,b,1.15,1.55,Wd,"#E8542B"); iso.qY(a,b,1.0,1.15,Wd,DG)
        if last: iso.poly3([(b,Wd,1.0),(b,Wd,1.95),(b-1.2,Wd,1.95),(b-2.6,Wd,1.7),(b-4.0,Wd,1.2),(b-4.0,Wd,1.0)],"#E8542B")
        iso.box(a+0.8,b-0.8,0.7,2.2,z1,z1+0.22,"#3A4350")
        if i in (1,3): panto2(iso,(a+b)/2+0.3,z1+0.22,ys=1.45,arm=1.2,h=1.0)
        if not first: seam(iso,a-g/2,1.0,3.8,"#7D8793")
    X=tot-1.6*0  # торец на уровне низа; лобовая зона — козырёк
    iso.qX(0.25,Wd-0.25,1.45,2.0,X,RED)
    iso.qX(0.45,0.95,1.7,2.0,X,"#FFE27A"); iso.qX(Wd-0.95,Wd-0.45,1.7,2.0,X,"#FFE27A")
    return iso

def iso_steam():
    iso=Iso(); iso.shadow(-5,8.6,0,3)
    iso.box(-5,8.2,0.2,2.8,0.9,1.6,"#B02427")
    iso.box(-5,-0.4,0,3,1.6,3.6,"#2B2E35")
    iso.box(-4.6,-0.8,0.4,2.6,3.6,3.95,"#1E2025")
    for (x,y,s_) in ((-4.2,1.0,0.5),(-3.1,1.8,0.6),(-2.0,0.9,0.5),(-1.5,1.9,0.45),(-3.7,2.2,0.4)):
        iso.box(x,x+s_,y,y+s_,3.95,4.1,"#3B3F48")
    iso.qY(-5,-0.4,1.7,1.95,3,"#B02427")
    iso.qY(-4.8,-0.6,2.3,2.5,3,"#555C66")
    for x in (-4.0,-1.4): iso.wheel(x,0.75,0.65,3.0,"#C0292B","#2B2E35")
    iso.box(-0.2,2.6,-0.1,3.1,1.6,4.6,"#2B2E35")
    iso.box(-0.5,2.9,-0.3,3.3,4.6,4.9,"#3B3F48")
    iso.qY(0.2,1.1,2.9,3.9,3.1,"#C9D3DC"); iso.qY(1.5,2.3,2.9,3.9,3.1,"#C9D3DC")
    iso.qY(0.2,1.1,2.9,3.0,3.1,"#8A929C")
    iso.cylX(2.6,7.4,1.5,2.7,1.15,"#3B3F48")
    iso.box(7.0,8.2,0.3,2.7,1.0,1.6,"#2B2E35")
    iso.box(5.4,7.2,2.55,3.05,1.2,1.8,"#C0292B")
    iso.box(7.4,8.6,0.1,2.9,0.6,1.1,"#C0292B")
    iso.cylZ(6.5,1.5,3.7,4.7,0.42,"#23262C"); iso.cylZ(6.5,1.5,4.55,4.85,0.62,"#23262C")
    iso.cylZ(4.4,1.5,3.7,4.1,0.5,"#5B616B")
    iso.cylZ(3.2,1.5,3.7,4.0,0.4,"#C9A227")
    iso.box(6.9,7.5,1.2,1.8,3.8,4.2,"#2B2E35"); iso.qX(1.25,1.75,3.85,4.15,7.5,"#FFD84A")
    iso.items.append(("poly",[P(7.42,1.5+0.55*math.cos(2*math.pi*i/20),2.7+0.55*math.sin(2*math.pi*i/20)) for i in range(20)],"#C0292B",1.0))
    iso.items.append(("poly",[P(7.43,1.5+0.2*math.cos(2*math.pi*i/20),2.7+0.2*math.sin(2*math.pi*i/20)) for i in range(20)],"#F2C230",1.0))
    for x in (1.0,3.4,5.8): iso.wheel(x,1.05,0.95,3.0,"#C0292B","#2B2E35")
    iso.wheel(7.4,0.7,0.5,3.0,"#C0292B","#2B2E35")
    iso.line3((1.0,3.05,1.05),(5.8,3.05,1.05),"#8A929C",0.2)
    return iso


def iso_chme3():
    iso=Iso(); iso.shadow(0,14.8,0,3)
    G="#1F8A3C"; DK="#146A2C"; R="#C8302B"; OR="#F07A2A"; Y="#F5C518"
    for x in (1.6,3.8,6.0,8.2,10.4,12.6):                                   # красные колёса
        iso.wheel(x,0.62,0.58,2.65,"#B22E2A","#2B2F38")
    iso.box(0.2,14.4,0.3,2.7,0.5,1.0,R)                                     # красная рама
    iso.box(14.0,14.8,0.2,2.8,0.85,1.7,OR,end=darken(OR,0.15))              # буферная балка
    iso.qX(0.2,0.7,1.1,1.5,14.8,"#FFE9B0"); iso.qX(2.3,2.8,1.1,1.5,14.8,"#FFE9B0")
    sec(iso,0.2,9.4,G,1.0,3.0,y0=0.25,y1=2.75)                              # капот
    for i in range(4): louvre(iso,0.9+i*2.0,2.1+i*2.0,1.5,2.7,2.75,DK,4)
    iso.box(2.0,3.3,0.7,2.3,3.0,3.28,"#8C949D"); iso.circZ(7.4,1.5,0.7,3.0,"#2B2F38"); iso.circZ(7.4,1.5,0.35,3.03,"#6B747E")
    sec(iso,9.4,13.9,G,1.0,4.5,y0=0.1,y1=2.9)                               # кабина
    iso.qY(10.0,13.3,2.7,4.0,2.9,"#22313F"); iso.qY(11.5,11.7,2.7,4.0,2.9,G)
    iso.qX(0.5,1.35,3.0,4.0,13.9,"#22313F"); iso.qX(1.65,2.5,3.0,4.0,13.9,"#22313F")
    iso.poly3([(13.9,0.1,1.0),(13.9,2.9,1.0),(13.9,2.9,2.8),(13.9,1.5,2.2),(13.9,0.1,2.8)],Y)   # жёлтая маска
    iso.poly3([(13.9,0.1,1.9),(13.9,1.5,1.5),(13.9,2.9,1.9),(13.9,2.9,2.25),(13.9,1.5,1.85),(13.9,0.1,2.25)],R)
    iso.qX(1.4,1.6,2.35,2.6,13.9,R)
    iso.line3((0.4,3.0,1.9),(13.8,3.0,1.9),Y,0.14)                          # жёлтые поручни
    for x in (0.6,4.0,7.4,10.8,13.6): iso.line3((x,3.0,1.0),(x,3.0,1.9),Y,0.12)
    iso.line3((14.5,0.0,1.8),(14.5,3.0,1.8),Y,0.14)
    seam(iso,9.4,1.0,3.0,"#0E4A1E")
    return iso

ISOS={"arrow":iso_arrow,"vl10":lambda:iso_vl("vl10"),"vl80":lambda:iso_vl("vl80"),"es6":iso_es6,"te25km":iso_te25,"swallow":iso_swallow,"steam":iso_steam,"chme3":iso_chme3}
def iso_shapes(iso,size=60,margin=3):
    pts=[]
    for it in iso.items:
        if it[0]=="poly": pts+=it[1]
        else: pts+=[it[1],it[2]]
    xs=[p[0] for p in pts]; ys=[p[1] for p in pts]
    minx,maxx,miny,maxy=min(xs),max(xs),min(ys),max(ys)
    k=(size-2*margin)/max(maxx-minx,maxy-miny)
    ox=(size-(maxx-minx)*k)/2-minx*k; oy=(size-(maxy-miny)*k)/2-miny*k
    T=lambda p:(p[0]*k+ox,p[1]*k+oy)
    out=[]
    for it in iso.items:
        if it[0]=="poly":
            out.append(Shape(poly([T(p) for p in it[1]]),fill=it[2],alpha=it[3]))
        else:
            a=T(it[1]); b=T(it[2])
            out.append(Shape("M%s %s L%s %s"%(fmt(a[0]),fmt(a[1]),fmt(b[0]),fmt(b[1])),stroke=it[3],sw=it[4]*k*0.9))
    return out

# ---------------- OUTPUT ----------------
def write(name,w,h,shapes):
    svg=['<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %s %s" width="%s" height="%s">'%(w,h,w*10,h*10)]
    xml=['<?xml version="1.0" encoding="utf-8"?>','<vector xmlns:android="http://schemas.android.com/apk/res/android"',
         '    android:width="%sdp"'%w+' android:height="%sdp"'%h,'    android:viewportWidth="%s" android:viewportHeight="%s">'%(w,h)]
    for s in shapes:
        sa=''; xa=''
        if s.fill: sa+=' fill="%s"'%s.fill; xa+='\n        android:fillColor="%s"'%s.fill
        else: sa+=' fill="none"'
        if s.alpha<1: sa+=' fill-opacity="%s"'%fmt(s.alpha); xa+='\n        android:fillAlpha="%s"'%fmt(s.alpha)
        if s.stroke:
            sa+=' stroke="%s" stroke-width="%s" stroke-linejoin="round" stroke-linecap="round"'%(s.stroke,fmt(s.sw))
            xa+='\n        android:strokeColor="%s"\n        android:strokeWidth="%s"\n        android:strokeLineJoin="round"\n        android:strokeLineCap="round"'%(s.stroke,fmt(s.sw))
        svg.append('  <path d="%s"%s/>'%(s.d,sa))
        xml.append('    <path\n        android:pathData="%s"%s/>'%(s.d,xa))
    svg.append('</svg>'); xml.append('</vector>')
    open(os.path.join(OUT_SVG,name+".svg"),"w").write("\n".join(svg)+"\n")
    open(os.path.join(OUT_XML,"ic_posicon_"+name+".xml"),"w").write("\n".join(xml)+"\n")

for key,(w,h,f) in TOPS.items(): write(key+"_top",w,h,f())
for key,f in ISOS.items(): write(key+"_iso",60,60,iso_shapes(f()))
print("ok")
