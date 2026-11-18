package arcade;

import java.util.*;

/** Deterministic simulation. Swing and audio never participate in the rules. */
public final class Game {
    public static final int COLS = 15, ROWS = 13;
    public static final double FUSE = 2.4, SPLASH = .55, TRAP = 6, ROUND = 180;
    public enum Mode { SOLO, DUEL, TEAM }
    public enum State { READY, PLAYING, PAUSED, FINISHED }
    public enum Life { ACTIVE, TRAPPED, OUT }
    public enum Item { BALLOON, POWER, SPEED, NEEDLE }
    public enum Theme { VILLAGE, FOREST, CANDY }
    public enum Cue { CLICK, PLACE, EXPLODE, PICKUP, TRAP, RESCUE, DIE, START, WIN, LOSE, DRAW }
    public static final int[][] DIR = {{0,1},{-1,0},{1,0},{0,-1}};
    public final int[][] tiles = new int[ROWS][COLS]; // 0 floor, 1 wall, 2 crate
    public final Item[][] items = new Item[ROWS][COLS];
    public final List<Player> players = new ArrayList<>();
    public final List<Bomb> bombs = new ArrayList<>();
    public final List<Blast> blasts = new ArrayList<>();
    public final List<Particle> particles = new ArrayList<>();
    public final Deque<String> events = new ArrayDeque<>();
    public final Deque<Cue> audioEvents = new ArrayDeque<>();
    public final Random random;
    public final Mode mode;
    public final Theme theme;
    public State state = State.READY;
    public double remaining = ROUND, countdown = 3, elapsed;
    public String result = "";
    public int explosionSerial, pickupSerial;

    public static final class Player {
        public final int id, team, character;
        public final boolean bot;
        public final String name;
        public int x, y, fromX, fromY, facing, capacity = 1, power = 2, speed = 0, needles = 1;
        public double motion, moveDuration = .19, trapped, invulnerable, decision, bombCooldown, walk;
        public Life life = Life.ACTIVE;
        Player(int id, int team, boolean bot, int character, int x, int y, String name) {
            this.id=id; this.team=team; this.bot=bot; this.character=character;
            this.x=fromX=x; this.y=fromY=y; this.name=name;
            if(character==1) capacity=2;
            if(character==2) speed=1;
            if(character==3) power=3;
        }
        public double renderX() { return x+(fromX-x)*Math.max(0,motion/moveDuration); }
        public double renderY() { return y+(fromY-y)*Math.max(0,motion/moveDuration); }
        public boolean moving() { return motion>0.00001; }
    }
    public static final class Bomb {
        public final int x,y,owner,power;
        public double fuse=FUSE;
        Bomb(int x,int y,int owner,int power) {this.x=x;this.y=y;this.owner=owner;this.power=power;}
    }
    public static final class Blast {
        public final int x,y;
        public double ttl=SPLASH;
        Blast(int x,int y){this.x=x;this.y=y;}
    }
    public static final class Particle {
        public double x,y,vx,vy,ttl=.65;
        public final boolean wood;
        Particle(double x,double y,double vx,double vy,boolean wood){this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.wood=wood;}
    }

    public Game(long seed, Mode mode, Theme theme, int character) {
        random=new Random(seed);this.mode=mode;this.theme=theme;
        for(int y=0;y<ROWS;y++) for(int x=0;x<COLS;x++) {
            tiles[y][x]=(x%2==1 && y%2==1)?1:random.nextDouble()<.57?2:0;
        }
        int[][] spawns={{0,0},{14,12},{14,0},{0,12}};
        for(int i=0;i<4;i++) {
            int x=spawns[i][0],y=spawns[i][1];
            for(int yy=0;yy<ROWS;yy++) for(int xx=0;xx<COLS;xx++)
                if(Math.abs(xx-x)+Math.abs(yy-y)<=2) tiles[yy][xx]=0;
            boolean bot = i>0 && !(i==1 && mode!=Mode.SOLO);
            int team=mode==Mode.TEAM?(i<2?0:1):i;
            players.add(new Player(i,team,bot,Math.floorMod(character+i,2),x,y,
                    i==0?"YOU":i==1&&!bot?"PLAYER 2":"CPU "+i));
        }
        log("물풍선을 놓고, 길을 열어보세요!");
    }
    public boolean inside(int x,int y){return x>=0&&y>=0&&x<COLS&&y<ROWS;}
    public Bomb bombAt(int x,int y){for(Bomb b:bombs)if(b.x==x&&b.y==y)return b;return null;}
    public boolean walkable(int x,int y){return inside(x,y)&&tiles[y][x]==0&&bombAt(x,y)==null;}
    public void log(String s){events.addFirst(s);while(events.size()>4)events.removeLast();}
    void cue(Cue cue){if(audioEvents.size()<32)audioEvents.addLast(cue);}
    public void pause(){if(state==State.PLAYING)state=State.PAUSED;else if(state==State.PAUSED)state=State.PLAYING;}

    public void update(double dt) {
        if(dt<=0)return;
        if(state==State.READY){countdown-=dt;if(countdown<=0){state=State.PLAYING;cue(Cue.START);}return;}
        if(state!=State.PLAYING)return;
        elapsed+=dt; remaining=Math.max(0,remaining-dt);
        for(Iterator<Particle> it=particles.iterator();it.hasNext();){Particle p=it.next();p.ttl-=dt;p.x+=p.vx*dt;p.y+=p.vy*dt;p.vy+=2*dt;if(p.ttl<=0)it.remove();}
        for(Iterator<Blast> it=blasts.iterator();it.hasNext();){Blast b=it.next();b.ttl-=dt;if(b.ttl<=0)it.remove();}
        for(Player p:players){
            p.motion=Math.max(0,p.motion-dt);p.invulnerable=Math.max(0,p.invulnerable-dt);
            p.bombCooldown=Math.max(0,p.bombCooldown-dt);p.decision-=dt;
            if(p.moving())p.walk+=dt/.06;
            if(p.life==Life.TRAPPED){p.trapped-=dt;if(p.trapped<=0)eliminate(p);}
        }
        for(Bomb b:bombs)b.fuse-=dt;
        for(Bomb b:new ArrayList<>(bombs))if(b.fuse<=0&&bombs.contains(b))explode(b);
        for(Player p:players){
            if(p.life==Life.ACTIVE&&p.invulnerable<=0&&onBlast(p.x,p.y))trap(p);
            if(p.life==Life.ACTIVE&&!p.moving())collect(p);
        }
        for(Player p:players)if(p.life==Life.TRAPPED)for(Player q:players){
            if(q.life==Life.ACTIVE&&Math.hypot(p.renderX()-q.renderX(),p.renderY()-q.renderY())<.62){
                if(p.team==q.team){release(p);log(q.name+" → "+p.name+" 구출!");}else eliminate(p);
                break;
            }
        }
        for(Player p:players)if(p.bot&&p.decision<=0){p.decision=.10;think(p);}
        Set<Integer> teams=new HashSet<>();for(Player p:players)if(p.life!=Life.OUT)teams.add(p.team);
        if(teams.size()<=1||remaining<=0){
            state=State.FINISHED;
            if(teams.size()!=1)result="DRAW";
            else if(teams.contains(players.get(0).team))result="YOU WIN!";
            else if(mode!=Mode.SOLO&&teams.contains(players.get(1).team))result="PLAYER 2 WINS!";
            else result="CPU WINS!";
            cue(result.equals("DRAW")?Cue.DRAW:result.equals("YOU WIN!")||result.equals("PLAYER 2 WINS!")?Cue.WIN:Cue.LOSE);
        }
    }
    public boolean move(Player p,int dir){
        if(state!=State.PLAYING||p.life!=Life.ACTIVE||p.moving())return false;
        p.facing=dir;int nx=p.x+DIR[dir][0],ny=p.y+DIR[dir][1];
        if(!walkable(nx,ny))return false;
        p.fromX=p.x;p.fromY=p.y;p.x=nx;p.y=ny;
        p.moveDuration=.19-p.speed*.018;p.motion=p.moveDuration;return true;
    }
    public boolean place(Player p){
        if(state!=State.PLAYING||p.life!=Life.ACTIVE||p.moving()||p.bombCooldown>0||bombAt(p.x,p.y)!=null)return false;
        if(bombs.stream().filter(b->b.owner==p.id).count()>=p.capacity)return false;
        bombs.add(new Bomb(p.x,p.y,p.id,p.power));p.bombCooldown=.22;cue(Cue.PLACE);return true;
    }
    public boolean needle(Player p){
        if(state!=State.PLAYING||p.life!=Life.TRAPPED||p.needles<=0)return false;
        p.needles--;release(p);log(p.name+" 바늘 탈출!");return true;
    }
    void release(Player p){p.life=Life.ACTIVE;p.trapped=0;p.invulnerable=1.2;cue(Cue.RESCUE);}
    void trap(Player p){p.life=Life.TRAPPED;p.trapped=TRAP;p.motion=0;cue(Cue.TRAP);log(p.name+" 물방울! 바늘로 탈출하세요");}
    void eliminate(Player p){p.life=Life.OUT;burst(p.x,p.y,false);cue(Cue.DIE);log(p.name+" OUT");}
    boolean onBlast(int x,int y){return blasts.stream().anyMatch(b->b.x==x&&b.y==y);}
    void collect(Player p){
        Item item=items[p.y][p.x];if(item==null||onBlast(p.x,p.y))return;
        items[p.y][p.x]=null;pickupSerial++;cue(Cue.PICKUP);
        switch(item){case BALLOON->p.capacity=Math.min(6,p.capacity+1);case POWER->p.power=Math.min(7,p.power+1);
            case SPEED->p.speed=Math.min(5,p.speed+1);case NEEDLE->p.needles=Math.min(3,p.needles+1);}
    }
    /** Rays use the pre-explosion blocking cells. A destroyed crate stops this ray. */
    public List<int[]> footprint(int x,int y,int power){
        List<int[]> cells=new ArrayList<>();cells.add(new int[]{x,y});
        for(int[] d:DIR)for(int n=1;n<=power;n++){
            int xx=x+d[0]*n,yy=y+d[1]*n;if(!inside(xx,yy)||tiles[yy][xx]==1)break;
            cells.add(new int[]{xx,yy});if(tiles[yy][xx]==2||bombAt(xx,yy)!=null)break;
        }return cells;
    }
    void explode(Bomb bomb){
        List<int[]> cells=footprint(bomb.x,bomb.y,bomb.power);bombs.remove(bomb);explosionSerial++;cue(Cue.EXPLODE);
        for(int[] c:cells){int x=c[0],y=c[1];blasts.add(new Blast(x,y));items[y][x]=null;
            if(tiles[y][x]==2){tiles[y][x]=0;burst(x,y,true);if(random.nextDouble()<.65)items[y][x]=Item.values()[random.nextInt(4)];}
            Bomb chain=bombAt(x,y);if(chain!=null)explode(chain);
        }burst(bomb.x,bomb.y,false);
    }
    void burst(int x,int y,boolean wood){for(int i=0;i<10;i++)particles.add(new Particle(x+.5,y+.5,(random.nextDouble()-.5)*5,-random.nextDouble()*4,wood));}

    /** Earliest blast times, including propagated chain-reaction fuses. */
    double[][] danger(){
        double[][] danger=new double[ROWS][COLS];for(double[] r:danger)Arrays.fill(r,Double.POSITIVE_INFINITY);
        Map<Bomb,Double> times=new IdentityHashMap<>();for(Bomb b:bombs)times.put(b,b.fuse);
        for(int pass=0;pass<bombs.size();pass++)for(Bomb b:bombs)for(int[] c:footprint(b.x,b.y,b.power)){
            Bomb other=bombAt(c[0],c[1]);if(other!=null)times.put(other,Math.min(times.get(other),times.get(b)));
        }
        for(Bomb b:bombs)for(int[] c:footprint(b.x,b.y,b.power))danger[c[1]][c[0]]=Math.min(danger[c[1]][c[0]],times.get(b));
        for(Blast b:blasts)danger[b.y][b.x]=0;return danger;
    }
    void think(Player p){
        if(p.life==Life.TRAPPED){if(p.trapped<TRAP-1)needle(p);return;}
        if(p.life!=Life.ACTIVE||p.moving())return;
        double[][] danger=danger();
        if(danger[p.y][p.x]<3){int d=route(p,danger,true);if(d>=0)move(p,d);return;}
        boolean target=false;
        for(int[] c:footprint(p.x,p.y,p.power)){
            if(tiles[c[1]][c[0]]==2)target=true;
            for(Player q:players)if(q.team!=p.team&&q.life==Life.ACTIVE&&q.x==c[0]&&q.y==c[1])target=true;
        }
        if(target&&p.bombCooldown<=0&&random.nextDouble()<.75){
            Bomb hypothetical=new Bomb(p.x,p.y,p.id,p.power);bombs.add(hypothetical);
            int escape=route(p,danger(),true);bombs.remove(hypothetical);
            if(escape>=0&&place(p)){move(p,escape);return;}
        }
        int d=route(p,danger,false);if(d>=0)move(p,d);
    }
    int route(Player p,double[][] danger,boolean escape){
        // Breadth-first routing. Each cell carries the first direction from the origin.
        boolean[][] seen=new boolean[ROWS][COLS];ArrayDeque<int[]> queue=new ArrayDeque<>();
        queue.add(new int[]{p.x,p.y,-1,0});seen[p.y][p.x]=true;
        int best=-1;double bestScore=-1e9;
        while(!queue.isEmpty()){
            int[] c=queue.remove();int x=c[0],y=c[1],first=c[2],steps=c[3];
            if(first>=0){
                if(escape&&Double.isInfinite(danger[y][x]))return first;
                double score=-steps*.7+random.nextDouble()*.3;
                if(items[y][x]!=null)score+=12;
                for(int[] d:DIR)if(inside(x+d[0],y+d[1])&&tiles[y+d[1]][x+d[0]]==2)score+=2;
                for(Player q:players)if(q!=p&&q.life!=Life.OUT){
                    int dist=Math.abs(x-q.x)+Math.abs(y-q.y);
                    if(q.life==Life.TRAPPED)score+=15.0/(dist+1);
                    else if(q.team!=p.team)score+=4.0/(dist+1);
                }
                if(danger[y][x]<4)score-=30;
                if(!escape&&score>bestScore){bestScore=score;best=first;}
            }
            for(int d=0;d<4;d++){int xx=x+DIR[d][0],yy=y+DIR[d][1];
                if(!walkable(xx,yy)||seen[yy][xx])continue;
                double arrival=(steps+1)*(.19-p.speed*.018);
                if(danger[yy][xx]<=arrival+.22)continue;
                seen[yy][xx]=true;queue.add(new int[]{xx,yy,first<0?d:first,steps+1});
            }
        }return escape?-1:best;
    }
}
