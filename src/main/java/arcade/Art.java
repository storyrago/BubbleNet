package arcade;

import java.awt.*;
import java.awt.geom.*;

/** UI artwork plus downloaded in-game sprite rendering. */
public final class Art {
    private Art() {}
    public static final Color INK=new Color(24,53,91), WHITE=new Color(248,253,255);
    public static final Color[] COLORS={new Color(39,163,244),new Color(255,103,114),new Color(104,200,121),new Color(177,130,241)};
    public static final String[] NAMES={"배찌", "로두마니"};
    public static void quality(Graphics2D g){g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);}
    public static void round(Graphics2D g,int x,int y,int w,int h,int r,Color c){g.setColor(c);g.fillRoundRect(x,y,w,h,r,r);}
    public static void text(Graphics2D g,String s,int x,int y,int size,Color c){g.setFont(new Font("Dialog",Font.BOLD,size));g.setColor(c);g.drawString(s,x,y);}
    public static void center(Graphics2D g,String s,int x,int y,int size,Color c){g.setFont(new Font("Dialog",Font.BOLD,size));g.setColor(c);g.drawString(s,x-g.getFontMetrics().stringWidth(s)/2,y);}
    public static void gradient(Graphics2D g,int x,int y,int w,int h,int r,Color a,Color b){g.setPaint(new GradientPaint(x,y,a,x,y+h,b));g.fillRoundRect(x,y,w,h,r,r);}
    static void oval(Graphics2D g,int x,int y,int w,int h,Color c){g.setColor(c);g.fillOval(x,y,w,h);}

    public static void balloon(Graphics2D source,double x,double y,double size,double time,Color color){
        Sprites.balloon(source,x,y,size,time);
    }
    public static void character(Graphics2D source,double x,double y,double scale,int type,int facing,double walk,Game.Life life,double time){
        Sprites.character(source,x,y,scale,type,facing,walk,life,time);
    }
    static void star(Graphics2D g,int x,int y,int radius,Color c){Path2D p=new Path2D.Double();for(int i=0;i<10;i++){double a=i*Math.PI/5-Math.PI/2,r=i%2==0?radius:radius*.45;double xx=x+Math.cos(a)*r,yy=y+Math.sin(a)*r;if(i==0)p.moveTo(xx,yy);else p.lineTo(xx,yy);}p.closePath();g.setColor(c);g.fill(p);}
    public static void tile(Graphics2D g,int x,int y,int size,int kind,Game.Theme theme,int variant){
        if(theme==Game.Theme.VILLAGE){Sprites.tile(g,x,y,size,kind,variant);return;}
        Color ground=theme==Game.Theme.VILLAGE?new Color(177,217,151):theme==Game.Theme.FOREST?new Color(133,191,155):new Color(236,208,228);
        round(g,x,y,size,size,0,(variant%2==0)?ground:ground.brighter());
        g.setColor(new Color(255,255,255,65));g.drawLine(x,y,x+size-1,y);g.drawLine(x,y,x,y+size-1);
        if(kind==0){if(variant%5==0){g.setColor(new Color(77,140,95,40));g.drawLine(x+10,y+30,x+14,y+24);g.drawLine(x+14,y+24,x+17,y+29);}return;}
        round(g,x+3,y+8,size-3,size-6,8,new Color(26,65,77,60));
        if(kind==1){
            Color c=theme==Game.Theme.CANDY?new Color(181,149,211):theme==Game.Theme.FOREST?new Color(60,144,113):new Color(100,159,184);
            gradient(g,x+2,y-5,size-4,size+3,9,c.brighter(),c.darker());round(g,x+4,y-7,size-8,size-12,8,c.brighter());
            g.setColor(new Color(255,255,255,90));g.drawRoundRect(x+7,y-4,size-14,size-18,5,5);
            if(theme==Game.Theme.FOREST){oval(g,x+6,y-14,size-12,size-8,new Color(73,166,116));oval(g,x+11,y-18,size-22,size-15,new Color(111,192,131));}
            else {round(g,x+size/2-5,y+7,10,15,4,c);g.setColor(c.darker());g.drawLine(x+4,y+size-12,x+size-5,y+size-12);}
        }else{
            Color c=theme==Game.Theme.CANDY?new Color(239,146,172):new Color(214,154,80);
            gradient(g,x+3,y-3,size-6,size,5,c.brighter(),c.darker());round(g,x+5,y-5,size-10,10,4,c.brighter());
            g.setStroke(new BasicStroke(2));g.setColor(c.darker());g.drawRoundRect(x+7,y+2,size-14,size-11,3,3);
            g.drawLine(x+9,y+5,x+size-9,y+size-13);g.drawLine(x+size-9,y+5,x+9,y+size-13);
            g.setColor(new Color(255,239,181));g.fillOval(x+7,y+4,3,3);g.fillOval(x+size-11,y+size-14,3,3);
        }
    }
    public static void item(Graphics2D g,int x,int y,Game.Item item,double t){
        Sprites.item(g,x,y,item,t);
    }
}
