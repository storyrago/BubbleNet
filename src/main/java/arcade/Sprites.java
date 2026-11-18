package arcade;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;

/** Pixel-preserving crops from downloaded sprite sheets. See assets/ATTRIBUTION.md. */
public final class Sprites {
    private Sprites() {}
    private static final BufferedImage[][][] WALK = new BufferedImage[2][4][6];
    private static final BufferedImage[][] DEATH = new BufferedImage[2][11];
    private static final BufferedImage[] BUBBLE = strip("BigPopo",72,72,9);
    private static final BufferedImage[] BALLOON = strip("Popo",44,41,3);
    private static final BufferedImage[] GROUND = strip("TownGround",40,40,7);
    private static final BufferedImage[] ITEM_BALLOON=strip("Gift1",42,45,3);
    private static final BufferedImage[] ITEM_SPEED=strip("Gift2",42,45,3);
    private static final BufferedImage[] ITEM_POWER=strip("Gift3",42,45,3);
    // Inventory screenshot: crop the needle slot, retaining its original gray backing.
    private static final BufferedImage ITEM_NEEDLE=load("ItemAtlas").getSubimage(171,150,26,25);
    private static final BufferedImage CRATE=load("TownBox"), WALL=load("TownBlockRed");
    static {
        for(int type=0;type<2;type++){
            BufferedImage sheet=load("Role"+(type+1));int w=type==0?48:56,h=type==0?64:67;
            if(sheet.getWidth()!=w*6||sheet.getHeight()!=h*4)throw new IllegalStateException("Unexpected character sheet dimensions");
            // Source rows: up, down, left, right. Engine: down, left, right, up.
            int[] rows={1,2,3,0};
            for(int dir=0;dir<4;dir++)for(int frame=0;frame<6;frame++)WALK[type][dir][frame]=sheet.getSubimage(frame*w,rows[dir]*h,w,h);
            DEATH[type]=strip("Role"+(type+1)+"Die",w,type==0?100:98,11);
        }
    }
    static BufferedImage load(String name){
        String path="assets/original/"+name+".png";
        try(InputStream stream=Sprites.class.getResourceAsStream("/"+path)){
            BufferedImage img=stream!=null?ImageIO.read(stream):ImageIO.read(new File(path));
            if(img==null)throw new IOException("Invalid PNG");return img;
        }catch(IOException e){throw new IllegalStateException("Missing sprite: "+path,e);}
    }
    static BufferedImage[] strip(String name,int width,int height,int count){
        BufferedImage sheet=load(name);if(sheet.getWidth()!=width*count||sheet.getHeight()!=height)throw new IllegalStateException("Invalid strip: "+name);
        BufferedImage[] frames=new BufferedImage[count];for(int i=0;i<count;i++)frames[i]=sheet.getSubimage(i*width,0,width,height);return frames;
    }
    static Graphics2D pixel(Graphics2D source){Graphics2D g=(Graphics2D)source.create();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);return g;}
    static void anchored(Graphics2D g,BufferedImage frame,double x,double y,double factor){int w=(int)Math.round(frame.getWidth()*factor),h=(int)Math.round(frame.getHeight()*factor);g.drawImage(frame,(int)Math.round(x-w/2.0),(int)Math.round(y-h),w,h,null);}
    public static void character(Graphics2D source,double x,double y,double scale,int type,int facing,double walk,Game.Life life,double time){
        Graphics2D g=pixel(source);int t=Math.floorMod(type,2);
        if(life==Game.Life.OUT){g.setComposite(AlphaComposite.SrcOver.derive(.35f));anchored(g,DEATH[t][10],x,y+3,scale);}
        else {
            g.setColor(new Color(16,35,50,65));g.fillOval((int)(x-19*scale),(int)(y-4*scale),(int)(38*scale),(int)(10*scale));
            double bob=life==Game.Life.TRAPPED?Math.sin(time*4)*2:0;
            anchored(g,WALK[t][life==Game.Life.TRAPPED?0:facing][life==Game.Life.TRAPPED?0:Math.floorMod((int)walk,6)],x,y+bob,scale);
            if(life==Game.Life.TRAPPED){g.setComposite(AlphaComposite.SrcOver.derive(.64f));anchored(g,BUBBLE[3],x,y+5+bob,scale*1.08);}
        }g.dispose();
    }
    public static void balloon(Graphics2D source,double x,double y,double size,double time){Graphics2D g=pixel(source);anchored(g,BALLOON[Math.floorMod((int)(time*5),3)],x,y+size*.43,size/44);g.dispose();}
    public static void tile(Graphics2D source,int x,int y,int size,int kind,int variant){
        Graphics2D g=pixel(source);g.drawImage(GROUND[0],x,y,size,size,null);
        if(kind!=0){BufferedImage frame=kind==1?WALL:CRATE;anchored(g,frame,x+size/2.0,y+size,size/40.0);}g.dispose();
    }
    static BufferedImage itemFrame(Game.Item item,int frame){
        int index=Math.floorMod(frame,3);
        return switch(item){case BALLOON->ITEM_BALLOON[index];case POWER->ITEM_POWER[index];case SPEED->ITEM_SPEED[index];case NEEDLE->ITEM_NEEDLE;};
    }
    public static void item(Graphics2D source,int x,int y,Game.Item item,double time){
        Graphics2D g=pixel(source);int phase=Math.floorMod((int)Math.floor(time/.1),8),bob=(phase<=4?phase:8-phase)-2;
        g.setColor(new Color(17,43,63,65));g.fillOval(x-12,y+12,24,7);
        BufferedImage frame=itemFrame(item,(int)Math.floor(time/.4));
        anchored(g,frame,x,y+18+bob,item==Game.Item.NEEDLE?1.2:1);g.dispose();
    }
    public static void exportItems(File directory)throws IOException{
        if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Cannot create "+directory);
        BufferedImage preview=new BufferedImage(640,310,BufferedImage.TYPE_INT_ARGB);Graphics2D g=preview.createGraphics();Art.quality(g);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setColor(new Color(226,244,253));g.fillRect(0,0,640,310);
        String[] names={"물풍선 수 +1","물줄기 +1","속도 +1","바늘 탈출"};
        for(Game.Item item:Game.Item.values()){
            int col=item.ordinal();Art.center(g,names[col],80+col*160,34,17,Art.INK);
            for(int f=0;f<3;f++){
                BufferedImage frame=itemFrame(item,f);
                ImageIO.write(frame,"png",new File(directory,item.name().toLowerCase(java.util.Locale.ROOT)+"-"+f+".png"));
                anchored(g,frame,80+col*160,113+f*85,item==Game.Item.NEEDLE?2:1.5);
            }
        }
        g.dispose();ImageIO.write(preview,"png",new File(directory,"items-preview.png"));
    }
    public static void export(File directory)throws IOException{
        if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Cannot create "+directory);
        String[] names={"bazzi","lodumani"},directions={"down","left","right","up"};
        for(int t=0;t<2;t++)for(int d=0;d<4;d++)for(int f=0;f<6;f++)ImageIO.write(WALK[t][d][f],"png",new File(directory,names[t]+"-"+directions[d]+"-"+f+".png"));
        BufferedImage contact=new BufferedImage(6*96,8*96,BufferedImage.TYPE_INT_ARGB);Graphics2D g=contact.createGraphics();
        for(int t=0;t<2;t++)for(int d=0;d<4;d++)for(int f=0;f<6;f++)character(g,f*96+48,(t*4+d)*96+90,1.2,t,d,f,Game.Life.ACTIVE,0);
        g.dispose();ImageIO.write(contact,"png",new File(directory,"contact-sheet.png"));
    }
}
