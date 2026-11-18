package arcade;

import arcade.client.*;
import arcade.net.Protocol;
import javax.swing.*;
import java.awt.*;

/** 실제 창을 지정 위치에 띄우고 좌표를 출력하는 캡처용 실행기. 사용: Demo login|lobby <port> */
public class Demo {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(args[1]);
        if (args[0].equals("login")) {
            SwingUtilities.invokeAndWait(() -> {
                LoginFrame f = new LoginFrame();
                LoginPanel p = (LoginPanel) f.getContentPane();
                p.nickField.setText("seo"); p.portField.setText(String.valueOf(port));
                f.setLocation(120, 120); f.setVisible(true); f.toFront();
                bounds("login", f);
            });
            return;
        }
        if (args[0].equals("echo")) {                 // EchoStub에 붙는 클라이언트 1개
            LobbyPanel p = new LobbyPanel("seo");
            SwingUtilities.invokeAndWait(() -> { LobbyFrame f = new LobbyFrame(p); f.setLocation(820, 380); f.setVisible(true); });
            p.net.connect("localhost", port, "seo"); Thread.sleep(700);
            p.net.send(Protocol.join(Protocol.CHAT, "테스트 1")); Thread.sleep(700);
            p.net.send(Protocol.join(Protocol.CHAT, "테스트 2")); Thread.sleep(700);
            Thread.sleep(30000); System.exit(0);
        }
        LobbyPanel a = new LobbyPanel("cheon"), b = new LobbyPanel("seo");
        LobbyFrame[] fr = new LobbyFrame[2];
        SwingUtilities.invokeAndWait(() -> {
            int x = args.length > 2 ? Integer.parseInt(args[2]) : 60, y = args.length > 3 ? Integer.parseInt(args[3]) : 100;
            int w = args.length > 4 ? Integer.parseInt(args[4]) : 0, h = args.length > 5 ? Integer.parseInt(args[5]) : 0;
            boolean stack = args.length > 6 && args[6].equals("stack");
            fr[0] = new LobbyFrame(a); if (w > 0) fr[0].setSize(w, h); fr[0].setLocation(x, y); fr[0].setVisible(true);
            fr[1] = new LobbyFrame(b); if (w > 0) fr[1].setSize(w, h);
            if (stack) fr[1].setLocation(x, y + fr[0].getHeight() + 2); else fr[1].setLocation(x + fr[0].getWidth() + 20, y);
            fr[1].setVisible(true);
        });
        a.net.connect("localhost", port, "cheon"); Thread.sleep(400);
        a.net.send(Protocol.join(Protocol.ROOM_CREATE, "테스트방")); Thread.sleep(300);
        a.net.send(Protocol.ROOM_LIST); Thread.sleep(300);
        b.net.connect("localhost", port, "seo"); Thread.sleep(400);
        b.net.send(Protocol.join(Protocol.ROOM_JOIN, "1")); Thread.sleep(400);
        b.net.send(Protocol.join(Protocol.CHAT, "안녕하세요")); Thread.sleep(400);
        a.net.send(Protocol.join(Protocol.CHAT, "반갑습니다, 로비 연결 테스트 중입니다")); Thread.sleep(400);
        b.net.send(Protocol.join(Protocol.CHAT, "채팅 잘 보입니다")); Thread.sleep(600);
        SwingUtilities.invokeAndWait(() -> {
            a.roomList.setSelectedIndex(0); b.roomList.setSelectedIndex(0);
            fr[0].toFront(); fr[1].toFront();
            bounds("lobby_cheon", fr[0]); bounds("lobby_seo", fr[1]);
        });
        for (int i = 0; i < 12; i++) {                      // 캡처 동안 주기적으로 채팅을 보내 패킷이 계속 보이게 한다
            Thread.sleep(5000);
            (i % 2 == 0 ? a : b).net.send(Protocol.join(Protocol.CHAT, "물풍선 " + (i + 1) + "개 설치 테스트"));
        }
        System.exit(0);
    }
    static void bounds(String name, Window w) {
        Rectangle r = w.getBounds();
        System.out.println("BOUNDS " + name + " " + r.x + " " + r.y + " " + r.width + " " + r.height);
        System.out.flush();
    }
}
