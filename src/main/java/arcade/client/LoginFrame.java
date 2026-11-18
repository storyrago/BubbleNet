package arcade.client;

import javax.swing.JFrame;

/** 접속 화면 창. 실제 내용은 LoginPanel. */
public class LoginFrame extends JFrame {
    public LoginFrame() {
        super("BubbleNet - 접속");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(new LoginPanel(this));
        pack(); setLocationRelativeTo(null);
    }
}
