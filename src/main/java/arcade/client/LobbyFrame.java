package arcade.client;

import javax.swing.JFrame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** 대기실 창. 창을 닫으면 소켓도 닫아 서버가 readLine() null로 퇴장을 알 수 있게 한다. */
public class LobbyFrame extends JFrame {
    public LobbyFrame(LobbyPanel panel) {
        super("BubbleNet 대기실 - " + panel.nickname);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(panel);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { panel.net.close(); }   // 수업 13강 WindowAdapter
        });
        pack(); setLocationRelativeTo(null);
    }
}
