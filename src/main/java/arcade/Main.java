package arcade;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import java.util.*;

public final class Main {
    public static void main(String[] args) throws Exception {
        if(args.length>0&&args[0].equals("--crop-items")){
            System.setProperty("java.awt.headless","true");Sprites.exportItems(new File(args.length>1?args[1]:"assets/cropped-items"));return;
        }
        if(args.length>0&&args[0].equals("--check-audio")){Sound.verify();return;}
        if(args.length>0&&args[0].equals("--audio-smoke")){Sound.smoke();return;}
        if(args.length>0&&args[0].equals("--crop-sprites")){
            System.setProperty("java.awt.headless","true");Sprites.export(new File(args.length>1?args[1]:"assets/cropped"));return;
        }
        if(args.length>0&&args[0].equals("--export-art")){
            System.setProperty("java.awt.headless","true");
            File dir=new File(args.length>1?args[1]:"assets");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cannot create "+dir);
            BufferedImage sheet=new BufferedImage(6*96,2*4*96,BufferedImage.TYPE_INT_ARGB);Graphics2D g=sheet.createGraphics();Art.quality(g);
            for(int type=0;type<2;type++)for(int direction=0;direction<4;direction++)for(int frame=0;frame<6;frame++)
                Art.character(g,frame*96+48,(type*4+direction)*96+82,1,type,direction,frame,Game.Life.ACTIVE,0);
            g.dispose();ImageIO.write(sheet,"png",new File(dir,"characters.png"));
            BufferedImage bubbles=new BufferedImage(6*96,2*96,BufferedImage.TYPE_INT_ARGB);g=bubbles.createGraphics();Art.quality(g);
            for(int type=0;type<2;type++)for(int frame=0;frame<6;frame++)Art.character(g,frame*96+48,type*96+82,1,type,0,0,Game.Life.TRAPPED,frame/6.0);
            g.dispose();ImageIO.write(bubbles,"png",new File(dir,"trapped.png"));return;
        }
        if(args.length>0&&args[0].equals("--render")){
            System.setProperty("java.awt.headless","true");
            File dir=new File(args.length>1?args[1]:"build/previews");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cannot create "+dir);
            SwingUtilities.invokeAndWait(()->{try{
                Panel panel=new Panel(false);panel.setSize(Panel.W,Panel.H);panel.render(new File(dir,"lobby.png"));
                panel.start();panel.game.state=Game.State.PLAYING;panel.game.players.get(0).x=4;panel.game.players.get(0).y=4;panel.game.tiles[4][4]=0;
                panel.game.place(panel.game.players.get(0));panel.game.players.get(0).x=2;panel.game.players.get(0).y=4;panel.game.tiles[4][2]=0;
                for(Game.Item item:Game.Item.values()){int x=item.ordinal()*2;panel.game.tiles[2][x]=0;panel.game.items[2][x]=item;}
                panel.render(new File(dir,"game.png"));
                panel.game.trap(panel.game.players.get(0));panel.render(new File(dir,"bubble.png"));
                panel.game.state=Game.State.FINISHED;panel.game.result="YOU WIN!";panel.render(new File(dir,"result.png"));
            }catch(IOException e){throw new UncheckedIOException(e);}});return;
        }
        SwingUtilities.invokeLater(()->{
            JFrame frame=new JFrame("Bubble Arcade · Swing Edition");Panel panel=new Panel(true);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);frame.setContentPane(panel);frame.pack();frame.setMinimumSize(new Dimension(800,600));frame.setLocationRelativeTo(null);
            frame.addWindowFocusListener(new WindowAdapter(){@Override public void windowLostFocus(WindowEvent e){panel.keys.clear();if(panel.game!=null&&panel.game.state==Game.State.PLAYING)panel.game.pause();}});
            frame.addWindowListener(new WindowAdapter(){@Override public void windowClosing(WindowEvent e){panel.sound.close();}});
            frame.setVisible(true);
        });
    }
    static final class Panel extends JPanel {
        static final int W=800,H=600,T=40,BX=20,BY=30;
        static final BufferedImage MATCH_FRAME=Sprites.load("GameBackGround");
        double matchClock;
        final Set<Integer> keys=new HashSet<>();
        final java.util.List<Hit> hits=new ArrayList<>();
        final Sound sound;
        Game game;
        Game.Mode mode=Game.Mode.SOLO;
        Game.Theme theme=Game.Theme.VILLAGE;
        int character,hoverX=-1,hoverY=-1;
        boolean help;
        double clock,accumulator;
        long last=System.nanoTime();
        record Hit(Rectangle box,Runnable action){}
        Panel(boolean animate){
            sound=new Sound(animate);
            setPreferredSize(new Dimension(W,H));setBackground(new Color(18,70,126));setFocusable(true);
            int[] codes={KeyEvent.VK_UP,KeyEvent.VK_DOWN,KeyEvent.VK_LEFT,KeyEvent.VK_RIGHT,KeyEvent.VK_SPACE,KeyEvent.VK_N,KeyEvent.VK_W,KeyEvent.VK_A,KeyEvent.VK_S,KeyEvent.VK_D,KeyEvent.VK_F,KeyEvent.VK_G,KeyEvent.VK_ESCAPE,KeyEvent.VK_ENTER,KeyEvent.VK_M,KeyEvent.VK_H};
            for(int code:codes)for(boolean release:new boolean[]{false,true}){
                String id=code+":"+release;getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(code,0,release),id);
                getActionMap().put(id,new AbstractAction(){public void actionPerformed(ActionEvent e){if(release)keys.remove(code);else if(keys.add(code))pressed(code);}});
            }
            addMouseListener(new MouseAdapter(){@Override public void mousePressed(MouseEvent e){Point p=point(e);for(Hit h:new ArrayList<>(hits))if(h.box.contains(p)){h.action.run();sound.play(Game.Cue.CLICK);repaint();break;}requestFocusInWindow();}});
            addMouseMotionListener(new MouseMotionAdapter(){@Override public void mouseMoved(MouseEvent e){Point p=point(e);hoverX=p.x;hoverY=p.y;repaint();}});
            if(animate)new javax.swing.Timer(16,e->tick()).start();
        }
        Point point(MouseEvent e){double scale=Math.min(getWidth()/(double)W,getHeight()/(double)H);return new Point((int)((e.getX()-(getWidth()-W*scale)/2)/scale),(int)((e.getY()-(getHeight()-H*scale)/2)/scale));}
        void start(){sound.reset();game=new Game(System.nanoTime(),mode,theme,character);keys.clear();help=false;accumulator=0;matchClock=0;}
        void lobby(){sound.reset();game=null;keys.clear();}
        void pressed(int code){
            if(code==KeyEvent.VK_M)sound.toggle();
            if(code==KeyEvent.VK_H){toggleHelp();return;}
            if(code==KeyEvent.VK_ENTER){if(game==null||game.state==Game.State.FINISHED)start();else if(game.state==Game.State.PAUSED){help=false;game.pause();}}
            if(code==KeyEvent.VK_ESCAPE){if(help){help=false;}else if(game!=null)game.pause();}
        }
        void toggleHelp(){help=!help;if(help&&game!=null&&game.state==Game.State.PLAYING)game.pause();}
        void tick(){
            long now=System.nanoTime();double dt=Math.min(.1,(now-last)/1e9);last=now;clock+=dt;accumulator+=dt;
            while(accumulator>=1.0/60){
                if(game!=null){
                    if(!help&&game.state==Game.State.PLAYING){input(game.players.get(0),new int[]{KeyEvent.VK_DOWN,KeyEvent.VK_LEFT,KeyEvent.VK_RIGHT,KeyEvent.VK_UP},KeyEvent.VK_SPACE,KeyEvent.VK_N);
                        if(mode!=Game.Mode.SOLO)input(game.players.get(1),new int[]{KeyEvent.VK_S,KeyEvent.VK_A,KeyEvent.VK_D,KeyEvent.VK_W},KeyEvent.VK_F,KeyEvent.VK_G);}
                    if(!help){if(game.state==Game.State.PLAYING)matchClock+=1.0/60;game.update(1.0/60);}
                }accumulator-=1.0/60;
            }repaint();
            sound.scene(game==null?Sound.Scene.LOBBY:game.state==Game.State.PAUSED?Sound.Scene.PAUSED:game.state==Game.State.FINISHED?Sound.Scene.RESULT:Sound.Scene.GAME);
            if(game!=null)while(!game.audioEvents.isEmpty())sound.play(game.audioEvents.removeFirst());
        }
        void input(Game.Player p,int[] movement,int bomb,int needle){
            // Place before moving so a held bomb key can place at tile centers.
            if(keys.contains(needle))game.needle(p);if(keys.contains(bomb))game.place(p);
            for(int i=0;i<movement.length;i++)if(keys.contains(movement[i])){game.move(p,i);break;}
        }
        void render(File file)throws IOException{BufferedImage image=new BufferedImage(W,H,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();paint(g);g.dispose();ImageIO.write(image,"png",file);}
        @Override protected void paintComponent(Graphics source){
            super.paintComponent(source);Graphics2D g=(Graphics2D)source.create();Art.quality(g);
            double scale=Math.min(getWidth()/(double)W,getHeight()/(double)H);g.translate((getWidth()-W*scale)/2,(getHeight()-H*scale)/2);g.scale(scale,scale);hits.clear();
            Art.gradient(g,0,0,W,H,0,new Color(50,163,216),new Color(19,78,147));
            for(int i=0;i<22;i++){int x=(i*197+37)%W,y=(int)((i*137-clock*9)%H);g.setColor(new Color(218,249,255,20));g.setStroke(new BasicStroke(2));g.drawOval(x,y,24+i%4*18,24+i%4*18);}
            if(game==null)lobby(g);else arena(g);if(help)help(g);g.dispose();
        }
        void button(Graphics2D g,int x,int y,int w,int h,String label,boolean selected,Runnable action){
            boolean hover=new Rectangle(x,y,w,h).contains(hoverX,hoverY);
            Art.gradient(g,x,y,w,h,6,selected?new Color(255,237,123):new Color(53,209,255),selected?new Color(236,155,32):new Color(0,108,207));
            g.setColor(hover?Color.WHITE:new Color(0,64,135));g.setStroke(new BasicStroke(2));g.drawRoundRect(x,y,w,h,6,6);
            Art.center(g,label,x+w/2,y+h/2+5,12,selected?Art.INK:Color.WHITE);hits.add(new Hit(new Rectangle(x,y,w,h),action));
        }
        void panel(Graphics2D g,int x,int y,int w,int h,String title){
            Art.round(g,x,y,w,h,8,new Color(0,62,136));Art.round(g,x+2,y+2,w-4,h-4,6,new Color(10,137,215));
            Art.round(g,x+6,y+6,w-12,23,4,new Color(0,75,150));Art.text(g,title,x+14,y+23,12,new Color(194,247,255));
        }
        void lobby(Graphics2D g){
            g.setColor(new Color(0,99,184));g.fillRect(0,0,W,H);
            Art.text(g,"Crazy Arcade",20,31,25,Color.WHITE);Art.text(g,"비공식 팬 게임 · 로컬 대기실",230,29,12,new Color(151,229,255));
            button(g,616,10,78,26,sound.enabled()?"소리 ON":"소리 OFF",false,sound::toggle);button(g,704,10,76,26,"도움말",false,this::toggleHelp);
            panel(g,16,48,554,346,"대기실  /  캐릭터 선택");
            for(int i=0;i<8;i++){
                int x=28+i%4*134,y=88+i/4*147;
                Art.round(g,x,y,126,137,5,new Color(2,87,165));Art.round(g,x+4,y+4,118,106,4,new Color(137,212,241));
                if(i<2){
                    final int choice=i;boolean selected=character==i;
                    Art.character(g,x+63,y+90,1,i,0,clock/.06,Game.Life.ACTIVE,clock);
                    Art.center(g,Art.NAMES[i],x+63,y+127,13,Color.WHITE);
                    if(selected){g.setColor(new Color(255,226,85));g.setStroke(new BasicStroke(3));g.drawRoundRect(x,y,126,137,5,5);Art.center(g,"1P  READY",x+63,y+22,12,new Color(16,76,152));}
                    hits.add(new Hit(new Rectangle(x,y,126,137),()->character=choice));
                }else{Art.center(g,i<4?"COMPUTER":"CLOSED",x+63,y+62,13,new Color(47,130,185));Art.center(g,i<4?"AI 참가":"빈 슬롯",x+63,y+127,12,new Color(138,207,245));}
            }
            panel(g,584,48,200,210,"MAP  /  "+themeName());
            Graphics2D mini=(Graphics2D)g.create();mini.clipRect(608,88,150,130);
            for(int y=0;y<13;y++)for(int x=0;x<15;x++)Art.tile(mini,608+x*10,88+y*10,10,x%2==1&&y%2==1?1:(x+y)%4==0?2:0,theme,x+y);mini.dispose();
            Art.center(g,"15 × 13  /  4인 / 3분",684,241,12,Color.WHITE);
            panel(g,584,268,200,204,"게임 설정");
            String[] modes={"혼자 · AI 3명","둘이 대결","2인 협동 팀전"};
            for(int i=0;i<3;i++){final int n=i;button(g,596,306+i*34,176,28,modes[i],mode.ordinal()==i,()->mode=Game.Mode.values()[n]);}
            String[] maps={"빌리지","숲","캔디"};for(int i=0;i<3;i++){final int n=i;button(g,596+i*60,420,56,30,maps[i],theme.ordinal()==i,()->theme=Game.Theme.values()[n]);}
            panel(g,16,406,554,132,"안내");
            Art.text(g,"블록을 터뜨려 풍선 · 물약 · 롤러를 모으세요.",30,454,13,Color.WHITE);
            Art.text(g,"1P  방향키 / SPACE 물풍선 / N 바늘",30,481,13,new Color(185,241,255));
            Art.text(g,"2P  WASD / F 물풍선 / G 바늘",30,508,13,new Color(185,241,255));
            button(g,584,486,200,52,"게임 시작  ENTER",true,this::start);
            Art.text(g,"BUBBLE ARCADE   ·   JAVA SWING",20,578,12,new Color(158,226,255));
            Art.text(g,"ESC 일시정지  /  H 도움말  /  M 소리",475,578,12,Color.WHITE);
        }
        void arena(Graphics2D g){
            g.drawImage(MATCH_FRAME,0,0,W,H,null);
            Graphics2D board=(Graphics2D)g.create();board.clipRect(BX,0,Game.COLS*T,BY+Game.ROWS*T);
            for(int y=0;y<Game.ROWS;y++)for(int x=0;x<Game.COLS;x++)Art.tile(board,BX+x*T,BY+y*T,T,0,game.theme,x+y);
            for(Game.Blast b:game.blasts){int x=BX+b.x*T,y=BY+b.y*T;Art.round(board,x,y+6,T,T-12,15,new Color(66,201,255,210));Art.round(board,x+6,y,T-12,T,15,new Color(80,207,255,210));Art.round(board,x+13,y+4,T-26,T-8,12,new Color(215,253,255,230));}
            for(int y=0;y<Game.ROWS;y++){
                for(int x=0;x<Game.COLS;x++){
                    if(game.tiles[y][x]!=0)Art.tile(board,BX+x*T,BY+y*T,T,game.tiles[y][x],game.theme,x+y);
                    if(game.items[y][x]!=null)Art.item(board,BX+x*T+20,BY+y*T+21,game.items[y][x],matchClock);
                }
                for(Game.Bomb b:game.bombs)if(b.y==y){Art.balloon(board,BX+b.x*T+20,BY+b.y*T+21,44,Game.FUSE-b.fuse,Art.COLORS[0]);}
                for(Game.Player p:game.players)if((int)Math.round(p.renderY())==y){
                    double px=BX+p.renderX()*T+20,py=BY+p.renderY()*T+38;
                    if(p.invulnerable<=0||((int)(matchClock*12)%2==0))Art.character(board,px,py,1,p.character,p.facing,p.moving()?p.walk:0,p.life,matchClock);
                    if(p.life!=Game.Life.OUT){Art.round(board,(int)px-16,(int)py+2,32,12,6,p.team==0?new Color(29,126,232):Art.COLORS[p.character].darker());Art.center(board,p.id==0?"1P":p.id==1&&mode!=Game.Mode.SOLO?"2P":"AI",(int)px,(int)py+11,8,Color.WHITE);}
                    if(p.life==Game.Life.TRAPPED){Art.round(board,(int)px-19,(int)py-60,38,5,4,new Color(22,88,144));Art.round(board,(int)px-19,(int)py-60,(int)(38*p.trapped/Game.TRAP),5,4,new Color(255,205,92));}
                }
            }
            for(Game.Particle p:game.particles){board.setColor(p.wood?new Color(242,183,100):new Color(216,252,255));board.fillOval((int)(BX+p.x*T),(int)(BY+p.y*T),p.wood?6:4,p.wood?6:9);}board.dispose();
            sidebar(g);
            Art.text(g,"1P 방향키 · SPACE 풍선 · N 바늘",20,578,11,Color.WHITE);
            Art.text(g,mode==Game.Mode.SOLO?"ESC 일시정지 / H 도움말":"2P WASD · F 풍선 · G 바늘",335,578,11,new Color(174,235,255));
            if(game.state==Game.State.READY){veil(g);Art.center(g,"READY?",320,240,40,Color.WHITE);Art.center(g,""+Math.max(1,(int)Math.ceil(game.countdown)),320,330,80,new Color(255,220,100));}
            if(game.state==Game.State.PAUSED&&!help)pauseOverlay(g);
            if(game.state==Game.State.FINISHED)result(g);
        }
        String themeName(){return switch(theme){case VILLAGE->"빌리지";case FOREST->"포레스트";case CANDY->"캔디";};}
        void sidebar(Graphics2D g){
            Art.center(g,themeName()+"  "+(mode==Game.Mode.TEAM?"TEAM":"FREE"),718,27,12,new Color(172,242,255));
            int time=(int)Math.ceil(game.remaining);
            Art.center(g,String.format("%02d:%02d",time/60,time%60),745,69,20,time<30?new Color(255,196,100):Color.WHITE);
            for(int i=0;i<8;i++){
                int y=83+i*44;
                if(i<game.players.size()){
                    Game.Player p=game.players.get(i);
                    Art.character(g,682,y+34,.55,p.character,0,0,p.life,matchClock);
                    Art.center(g,""+(i+1),717,y+14,10,Color.WHITE);
                    Art.center(g,p.name,754,y+14,10,Color.WHITE);
                    Art.text(g,p.life==Game.Life.TRAPPED?"HELP!":p.life==Game.Life.OUT?"OUT":Art.NAMES[p.character],732,y+33,10,p.life==Game.Life.TRAPPED?Color.YELLOW:new Color(174,233,255));
                }else Art.center(g,"―",753,y+15,11,new Color(79,155,213));
            }
            Game.Player p=game.players.get(0);
            Art.item(g,692,506,Game.Item.NEEDLE,0);Art.text(g,"N ×"+p.needles,731,495,13,Color.WHITE);
            Art.text(g,"풍선 "+p.capacity,731,516,10,Color.WHITE);Art.text(g,"물줄기 "+p.power,731,531,10,Color.WHITE);
            button(g,643,561,149,32,"나가기",false,()->{if(game.state==Game.State.PLAYING)game.pause();else lobby();});
            button(g,643,444,72,19,sound.enabled()?"소리 ON":"소리 OFF",false,sound::toggle);
            button(g,722,444,70,19,"도움말",false,this::toggleHelp);
        }
        void veil(Graphics2D g){g.setColor(new Color(9,38,76,170));g.fillRect(0,0,W,H);}
        void pauseOverlay(Graphics2D g){
            veil(g);hits.clear();panel(g,220,180,360,240,"PAUSE");
            Art.center(g,"잠깐 쉬어가기",400,242,25,Color.WHITE);Art.center(g,"ENTER / ESC 계속하기",400,275,13,new Color(189,238,255));
            button(g,250,298,300,40,"계속하기",true,()->game.pause());button(g,250,355,300,40,"로비로 돌아가기",false,this::lobby);
        }
        void result(Graphics2D g){
            veil(g);hits.clear();panel(g,220,160,360,280,"RESULT");
            Art.star(g,400,220,25,new Color(255,220,79));Art.center(g,game.result,400,280,30,Color.WHITE);
            button(g,250,310,300,42,"다시 대결  ENTER",true,this::start);button(g,250,373,300,42,"로비로 돌아가기",false,this::lobby);
        }
        void help(Graphics2D g){
            veil(g);hits.clear();panel(g,90,85,620,430,"HOW TO PLAY");
            String[] lines={"1P   방향키 이동 / SPACE 물풍선 / N 바늘","2P   WASD 이동 / F 물풍선 / G 바늘","물풍선은 2.4초 뒤 십자 방향으로 터집니다.","상자는 부서지고, 단단한 블록은 물줄기를 막습니다.","물방울에 갇히면 6초 안에 바늘이나 팀원으로 탈출하세요.","팀원이 닿으면 구출! 적이 닿으면 즉시 탈락합니다.","풍선 = 설치 수 / 물약 = 물줄기 / 롤러 = 속도 증가"};
            int y=151;for(String line:lines){Art.text(g,line,116,y,14,Color.WHITE);y+=39;}
            button(g,116,448,568,42,"알겠어요!  H / ESC",true,()->help=false);
        }
    }
}
