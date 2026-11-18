package arcade.client;

import arcade.net.Protocol;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/** 서버 주소, 포트, 닉네임을 입력받아 접속하는 첫 화면의 내용. 창 없이도(헤드리스) 그릴 수 있다. */
public class LoginPanel extends JPanel {
    public final JTextField hostField = new JTextField("localhost");
    public final JTextField portField = new JTextField(String.valueOf(Protocol.PORT));
    public final JTextField nickField = new JTextField();
    public final JButton connectButton = new JButton("접속");
    public final JLabel status = new JLabel(" ");

    final JFrame owner;

    public LoginPanel(JFrame owner) {
        super(new BorderLayout(10, 10));
        this.owner = owner;
        build();
        connectButton.addActionListener(e -> connect());
        nickField.addActionListener(e -> connect());
    }

    void build() {
        JPanel root = this;
        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.add(new JLabel("서버 주소")); form.add(hostField);
        form.add(new JLabel("포트"));      form.add(portField);
        form.add(new JLabel("닉네임"));    form.add(nickField);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JLabel title = new JLabel("BubbleNet", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        root.add(title, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout());
        south.add(connectButton, BorderLayout.EAST);
        south.add(status, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);
        root.setPreferredSize(new Dimension(340, 200));
    }

    void connect() {
        String nick = nickField.getText().trim();
        if (nick.isEmpty()) { status.setText("닉네임을 입력하세요"); return; }
        LobbyPanel lobby = new LobbyPanel(nick);
        try {
            lobby.net.connect(hostField.getText().trim(), Integer.parseInt(portField.getText().trim()), nick);
        } catch (IOException | NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "접속 실패: " + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }
        new LobbyFrame(lobby).setVisible(true);
        if (owner != null) owner.dispose();
    }
}
