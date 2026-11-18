package arcade;

import javax.sound.sampled.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Downloaded WAV clips. All device operations run off Swing's EDT. */
public final class Sound implements AutoCloseable {
    private static final Map<Game.Cue,String> FILES=Map.ofEntries(
        Map.entry(Game.Cue.CLICK,"click"),Map.entry(Game.Cue.PLACE,"lay"),
        Map.entry(Game.Cue.EXPLODE,"explode"),Map.entry(Game.Cue.PICKUP,"get"),
        Map.entry(Game.Cue.TRAP,"appear"),Map.entry(Game.Cue.RESCUE,"save"),
        Map.entry(Game.Cue.DIE,"die"),Map.entry(Game.Cue.START,"start"),
        Map.entry(Game.Cue.WIN,"win"),Map.entry(Game.Cue.LOSE,"lose"),
        Map.entry(Game.Cue.DRAW,"draw"));
    public enum Scene { LOBBY, GAME, PAUSED, RESULT }
    private final ScheduledExecutorService worker;
    private final EnumSet<Game.Cue> pending=EnumSet.noneOf(Game.Cue.class);
    private final EnumMap<Game.Cue,Clip> effects=new EnumMap<>(Game.Cue.class);
    private volatile boolean enabled=true, available=true, closed;
    private volatile Scene desired=Scene.LOBBY;
    private volatile int generation;
    private int appliedGeneration=-1;
    private Clip music;
    private volatile boolean playbackStarted;

    public Sound(boolean hardware) {
        worker=hardware?Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"arcade-audio");t.setDaemon(true);return t;}):null;
        if(worker!=null)worker.scheduleWithFixedDelay(this::pump,0,20,TimeUnit.MILLISECONDS);
    }
    public boolean enabled(){return enabled;}
    public boolean available(){return available;}
    public void toggle(){setEnabled(!enabled);}
    public synchronized void setEnabled(boolean value){enabled=value;pending.clear();}
    public synchronized void reset(){pending.clear();generation++;}
    public void scene(Scene scene){desired=scene;}
    public synchronized void play(Game.Cue cue){if(enabled&&!closed&&available)pending.add(cue);}
    private synchronized EnumSet<Game.Cue> take(){EnumSet<Game.Cue> result=pending.clone();pending.clear();return result;}
    static String file(Game.Cue cue){return FILES.get(cue);}

    /** Also used by the headless asset verifier; never opens an audio device. */
    static AudioInputStream pcm(String name)throws IOException,UnsupportedAudioFileException{
        String path="assets/audio/"+name+".wav";
        InputStream resource=Sound.class.getResourceAsStream("/"+path);
        InputStream input=new BufferedInputStream(resource!=null?resource:new FileInputStream(path));
        try {
            AudioInputStream original=AudioSystem.getAudioInputStream(input);
            AudioFormat f=original.getFormat();
            AudioFormat target=new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,44100,16,f.getChannels(),2*f.getChannels(),44100,false);
            if(f.matches(target))return original;
            try{return AudioSystem.getAudioInputStream(target,original);}
            catch(IllegalArgumentException e){original.close();throw e;}
        }catch(IOException|UnsupportedAudioFileException|RuntimeException e){input.close();throw e;}
    }
    private Clip open(String name,float db)throws IOException,UnsupportedAudioFileException,LineUnavailableException{
        Clip clip=null;
        try(AudioInputStream stream=pcm(name)){
            clip=(Clip)AudioSystem.getLine(new DataLine.Info(Clip.class,stream.getFormat()));
            clip.open(stream);
            if(clip.isControlSupported(FloatControl.Type.MASTER_GAIN)){
                FloatControl gain=(FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN);
                gain.setValue(Math.max(gain.getMinimum(),Math.min(gain.getMaximum(),db)));
            }
            return clip;
        }catch(IOException|UnsupportedAudioFileException|LineUnavailableException|RuntimeException e){if(clip!=null)clip.close();throw e;}
    }
    private void stopEffects(){for(Clip clip:effects.values())clip.stop();}
    private void pump(){
        if(closed||!available)return;
        try {
            if(generation!=appliedGeneration){
                stopEffects();if(music!=null){music.stop();music.setFramePosition(0);}
                appliedGeneration=generation;
            }
            boolean on=enabled;Scene scene=desired;
            if(!on||scene==Scene.PAUSED)stopEffects();
            if(!on||scene==Scene.PAUSED||scene==Scene.RESULT){
                if(music!=null)music.stop();
                if(scene==Scene.RESULT&&music!=null)music.setFramePosition(0);
            }else{
                if(music==null)music=open("bg",-16f);
                if(!music.isRunning())music.loop(Clip.LOOP_CONTINUOUSLY);
            }
            // Drop sounds from the muted/paused interval, never replay a backlog.
            EnumSet<Game.Cue> cues=take();
            if(on&&scene!=Scene.PAUSED){
                for(Game.Cue cue:cues){
                    Clip clip=effects.get(cue);
                    if(clip==null){clip=open(FILES.get(cue),cue==Game.Cue.CLICK?-14f:-7f);effects.put(cue,clip);}
                    clip.stop();clip.setFramePosition(0);clip.start();
                }
            }
            if(music!=null&&music.isRunning())playbackStarted=true;
        }catch(IOException|UnsupportedAudioFileException|LineUnavailableException|IllegalArgumentException|SecurityException e){
            available=false;stopAllAndClose();System.err.println("Audio unavailable: "+e.getMessage());
        }
    }
    private void stopAllAndClose(){
        for(Clip clip:effects.values()){clip.stop();clip.close();}effects.clear();
        if(music!=null){music.stop();music.close();music=null;}
    }
    @Override public void close(){
        closed=true;take();
        if(worker!=null){worker.execute(this::stopAllAndClose);worker.shutdown();}
    }
    public static void verify()throws Exception{
        List<String> names=new ArrayList<>(FILES.values());names.add("bg");
        for(String name:names)try(AudioInputStream stream=pcm(name)){
            long bytes=stream.transferTo(OutputStream.nullOutputStream());
            if(bytes<=0)throw new IOException("Empty audio: "+name);
            double duration=bytes/(stream.getFormat().getFrameRate()*stream.getFormat().getFrameSize());
            System.out.printf(Locale.ROOT,"%s.wav: %.3fs PCM%n",name,duration);
        }
    }
    public static void smoke()throws Exception{
        try(Sound sound=new Sound(true)){
            sound.scene(Scene.GAME);sound.play(Game.Cue.PLACE);
            Thread.sleep(900);sound.play(Game.Cue.EXPLODE);Thread.sleep(700);
            if(!sound.available||!sound.playbackStarted)throw new IllegalStateException("Audio device playback unavailable");
            sound.setEnabled(false);Thread.sleep(80);
            System.out.println("PASS: audio device opened; BGM playback started; effect requests and mute processed.");
        }
    }
}
