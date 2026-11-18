package arcade;

import java.util.*;
import java.awt.event.*;

/** No test framework or downloads needed. Run with sh test.sh. */
public final class GameTest {
    static int assertions;
    static void check(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
    static Game empty(Game.Mode mode){Game g=new Game(42,mode,Game.Theme.VILLAGE,0);for(int[] r:g.tiles)Arrays.fill(r,0);g.state=Game.State.PLAYING;for(Game.Player p:g.players)p.decision=999;return g;}
    static void advance(Game g,double seconds){for(int i=0;i<(int)Math.ceil(seconds*60);i++)g.update(1.0/60);}
    static boolean cell(List<int[]> cells,int x,int y){return cells.stream().anyMatch(c->c[0]==x&&c[1]==y);}
    public static void main(String[] args)throws Exception{
        Game g=empty(Game.Mode.SOLO);Game.Player p=g.players.get(0);
        check(!g.move(p,1),"map boundary");g.tiles[0][1]=1;check(!g.move(p,2),"wall collision");g.tiles[0][1]=2;check(!g.move(p,2),"crate collision");g.tiles[0][1]=0;
        check(g.place(p),"place balloon");check(!g.place(p),"no duplicate");check(g.move(p,2),"leave own balloon");advance(g,.21);check(!g.move(p,1),"cannot reenter balloon");check(!g.place(p),"capacity enforced");
        g=empty(Game.Mode.SOLO);g.tiles[3][5]=1;g.tiles[5][3]=2;List<int[]> cells=g.footprint(3,3,5);
        check(!cell(cells,5,3)&&!cell(cells,6,3),"wall stops ray");check(cell(cells,3,5)&&!cell(cells,3,6),"crate stops ray");
        Game.Bomb a=new Game.Bomb(3,3,0,5);g.bombs.add(a);g.explode(a);check(g.tiles[5][3]==0,"crate destroyed");check(g.tiles[3][5]==1,"wall survives");
        g=empty(Game.Mode.SOLO);a=new Game.Bomb(2,2,0,3);Game.Bomb b=new Game.Bomb(4,2,1,2);g.bombs.add(a);g.bombs.add(b);a.fuse=.2;check(g.danger()[2][6]<=.2,"AI chain prediction");g.explode(a);check(g.bombs.isEmpty()&&g.explosionSerial==2,"chain explosion");
        g=empty(Game.Mode.SOLO);p=g.players.get(0);g.place(p);advance(g,2.3);check(p.life==Game.Life.ACTIVE,"fuse not early");advance(g,.15);check(p.life==Game.Life.TRAPPED,"blast traps");check(g.needle(p),"needle escapes");check(p.life==Game.Life.ACTIVE&&p.needles==0,"needle consumed");advance(g,.2);check(p.life==Game.Life.ACTIVE,"escape immunity");
        g=empty(Game.Mode.SOLO);p=g.players.get(0);p.needles=0;g.trap(p);check(!g.needle(p),"empty needle denied");advance(g,6.1);check(p.life==Game.Life.OUT,"trap timeout");
        g=empty(Game.Mode.TEAM);p=g.players.get(0);g.trap(p);g.players.get(1).x=0;g.players.get(1).y=0;g.update(.01);check(p.life==Game.Life.ACTIVE,"teammate rescues");
        g=empty(Game.Mode.SOLO);p=g.players.get(0);g.trap(p);g.players.get(1).x=0;g.players.get(1).y=0;g.update(.01);check(p.life==Game.Life.OUT,"enemy pops");
        g=empty(Game.Mode.SOLO);p=g.players.get(0);for(Game.Item item:Game.Item.values()){g.items[0][0]=item;g.collect(p);check(g.items[0][0]==null,"pickup "+item);}
        check(p.capacity==2&&p.power==3&&p.speed==1&&p.needles==2,"all upgrades");
        for(int i=0;i<20;i++){g.items[0][0]=Game.Item.SPEED;g.collect(p);}check(p.speed==5,"upgrade cap");
        g.place(p);g.pause();double fuse=g.bombs.get(0).fuse,time=g.remaining;advance(g,1);check(g.bombs.get(0).fuse==fuse&&g.remaining==time,"pause freezes time");check(!g.move(p,2)&&!g.needle(p),"paused input rejected");
        g=empty(Game.Mode.SOLO);for(int i=1;i<4;i++)g.players.get(i).life=Game.Life.OUT;g.update(.01);check(g.state==Game.State.FINISHED&&g.result.equals("YOU WIN!"),"winner");
        g=empty(Game.Mode.SOLO);g.remaining=.01;g.update(.02);check(g.result.equals("DRAW"),"time draw");
        g=empty(Game.Mode.TEAM);g.players.get(2).life=g.players.get(3).life=Game.Life.OUT;g.update(.01);check(g.result.equals("YOU WIN!"),"team win");
        // Seeded simulations exercise AI and invariants across all map themes and modes.
        int complete=0;
        for(int seed=0;seed<12;seed++){
            g=new Game(seed,Game.Mode.values()[seed%3],Game.Theme.values()[seed%3],seed%4);g.state=Game.State.PLAYING;
            for(int frame=0;frame<10810&&g.state==Game.State.PLAYING;frame++){
                for(Game.Player player:g.players)if(!player.bot&&frame%6==0)g.think(player);
                g.update(1.0/60);
                for(Game.Player player:g.players)check(g.inside(player.x,player.y)&&g.tiles[player.y][player.x]==0,"simulation collision invariant");
                Set<String> occupied=new HashSet<>();for(Game.Bomb bomb:g.bombs)check(occupied.add(bomb.x+":"+bomb.y),"unique bomb cell");
            }
            check(g.state==Game.State.FINISHED,"simulation finishes");complete++;
        }
        javax.swing.SwingUtilities.invokeAndWait(()->{
            Main.Panel panel=new Main.Panel(false);panel.start();panel.game.state=Game.State.PLAYING;
            panel.pressed(KeyEvent.VK_ESCAPE);check(panel.game.state==Game.State.PAUSED,"escape pauses UI");panel.pressed(KeyEvent.VK_ENTER);check(panel.game.state==Game.State.PLAYING,"enter resumes UI");
            panel.toggleHelp();check(panel.help&&panel.game.state==Game.State.PAUSED,"help pauses UI");panel.pressed(KeyEvent.VK_ESCAPE);check(!panel.help,"escape closes help");
        });
        // Verify semantic audio events, including rejected actions and one-shot results.
        g=empty(Game.Mode.SOLO);p=g.players.get(0);
        check(g.place(p)&&g.audioEvents.removeFirst()==Game.Cue.PLACE,"place audio");
        check(!g.place(p)&&g.audioEvents.isEmpty(),"rejected place is silent");
        g.trap(p);check(g.audioEvents.removeFirst()==Game.Cue.TRAP,"trap audio");
        g.needle(p);check(g.audioEvents.removeFirst()==Game.Cue.RESCUE,"needle rescue audio");
        g.items[p.y][p.x]=Game.Item.SPEED;g.collect(p);check(g.audioEvents.removeFirst()==Game.Cue.PICKUP,"pickup audio");
        g=empty(Game.Mode.SOLO);g.remaining=.001;g.update(.01);
        check(g.audioEvents.removeFirst()==Game.Cue.DRAW,"draw audio");g.update(1);check(g.audioEvents.isEmpty(),"result only once");
        try(Sound audio=new Sound(false)){audio.setEnabled(false);check(!audio.enabled(),"mute control");audio.toggle();check(audio.enabled(),"unmute control");}
        Sound.verify();
        System.out.println("PASS: "+assertions+" assertions, "+complete+" full seeded matches, UI input transitions.");
    }
}
