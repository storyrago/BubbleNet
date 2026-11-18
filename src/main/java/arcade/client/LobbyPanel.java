package arcade.client;

import arcade.net.Protocol;
import javax.swing.*;
import java.awt.*;

/** 방 목록, 참가자 목록, 채팅창의 내용. handle()은 항상 EDT에서 실행된다. */
public class LobbyPanel extends JPanel {
    public final NetClient net;
    public final String nickname;
    final DefaultListModel<String> roomModel = new DefaultListModel<>();
    final DefaultListModel<String> memberModel = new DefaultListModel<>();
    public final JList<String> roomList = new JList<>(roomModel);
    public final JList<String> memberList = new JList<>(memberModel);
    public final JTextArea chatArea = new JTextArea();
    public final JTextField inputField = new JTextField();
    final JButton createButton = new JButton("방 만들기");
    final JButton joinButton = new JButton("입장");
    final JButton startButton = new JButton("게임 시작");

    public LobbyPanel(String nickname) {
        super(new BorderLayout(8, 8));
        this.nickname = nickname;
        this.net = new NetClient(this::handle, this::disconnected);
        build();
    }

    void handle(String[] f) {                 // EDT에서 실행됨
        switch (f[0]) {
            case Protocol.CHAT:       chatArea.append(f[1] + ": " + f[2] + "\n"); break;
            case Protocol.SYS:        chatArea.append("* " + f[1] + "\n"); break;
            case Protocol.ROOM_STATE: memberModel.clear();
                                      for (String n : f[2].split(",")) memberModel.addElement(n); break;
            case Protocol.ROOM_LIST:  roomModel.clear();
                                      for (String r : f[1].split(";")) if (!r.isEmpty()) {
                                          String[] c = r.split(",");
                                          roomModel.addElement(c[0] + " " + c[1] + " (" + c[2] + "명)");
                                      }
                                      break;
            default:                  chatArea.append("? " + String.join("|", f) + "\n");
        }
        chatArea.setCaretPosition(chatArea.getDocument().getLength());   // 자동 스크롤
    }

    void disconnected() {
        chatArea.append("* 서버 연결이 끊어졌습니다\n");
        inputField.setEnabled(false);
        createButton.setEnabled(false); joinButton.setEnabled(false);
    }

    void build() {
        JPanel root = this;
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        startButton.setEnabled(false);

        JPanel left = new JPanel(new BorderLayout(4, 4));
        left.setBorder(BorderFactory.createTitledBorder("방 목록"));
        left.add(new JScrollPane(roomList), BorderLayout.CENTER);
        JPanel leftButtons = new JPanel(new GridLayout(1, 2, 4, 4));
        leftButtons.add(createButton); leftButtons.add(joinButton);
        left.add(leftButtons, BorderLayout.SOUTH);
        left.setPreferredSize(new Dimension(200, 0));

        JPanel members = new JPanel(new BorderLayout());
        members.setBorder(BorderFactory.createTitledBorder("참가자"));
        members.add(new JScrollPane(memberList), BorderLayout.CENTER);
        members.setPreferredSize(new Dimension(0, 110));

        JPanel chat = new JPanel(new BorderLayout(4, 4));
        chat.setBorder(BorderFactory.createTitledBorder("채팅"));
        chat.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        chat.add(inputField, BorderLayout.SOUTH);

        JPanel right = new JPanel(new BorderLayout(4, 4));
        right.add(members, BorderLayout.NORTH);
        right.add(chat, BorderLayout.CENTER);

        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(left, BorderLayout.WEST);
        root.add(right, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(startButton);
        root.add(bottom, BorderLayout.SOUTH);
        root.setPreferredSize(new Dimension(640, 420));

        inputField.addActionListener(e -> {               // Enter 키
            String text = inputField.getText().trim();
            if (!text.isEmpty()) { net.send(Protocol.join(Protocol.CHAT, text)); inputField.setText(""); }
        });
        createButton.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "방 이름", nickname + "의 방");
            if (name != null && !name.trim().isEmpty()) net.send(Protocol.join(Protocol.ROOM_CREATE, name.trim()));
        });
        joinButton.addActionListener(e -> {
            String sel = roomList.getSelectedValue();
            if (sel != null) net.send(Protocol.join(Protocol.ROOM_JOIN, sel.split(" ")[0]));
        });
    }
}
