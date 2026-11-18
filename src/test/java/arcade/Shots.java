package arcade;

import arcade.client.LobbyPanel;
import arcade.client.LoginPanel;
import arcade.net.Protocol;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/** 창 없이 패널을 PNG로 그리는 보고서용 캡처 도구. 사용: Shots <출력폴더> <서버포트> <로그폴더> */
public class Shots {
    static File out;

    public static void main(String[] args) throws Exception {
        out = new File(args[0]); out.mkdirs();
        int port = Integer.parseInt(args[1]);
        File logs = new File(args[2]);

        // 1) 접속 화면
        SwingUtilities.invokeAndWait(() -> {
            LoginPanel login = new LoginPanel(null);
            login.nickField.setText("seo");
            login.portField.setText("5000");
            paint(login, 340, 200, "login.png");
        });

        // 2) 실제 서버에 두 클라이언트 접속 후 채팅
        LobbyPanel a = new LobbyPanel("cheon"), b = new LobbyPanel("seo");
        a.net.connect("localhost", port, "cheon"); Thread.sleep(300);
        a.net.send(Protocol.join(Protocol.ROOM_CREATE, "테스트방")); Thread.sleep(300);
        a.net.send(Protocol.ROOM_LIST); Thread.sleep(200);
        b.net.connect("localhost", port, "seo"); Thread.sleep(300);
        b.net.send(Protocol.join(Protocol.ROOM_JOIN, "1")); Thread.sleep(300);
        b.net.send(Protocol.join(Protocol.CHAT, "안녕하세요")); Thread.sleep(300);
        a.net.send(Protocol.join(Protocol.CHAT, "반갑습니다, 로비 연결 테스트 중입니다")); Thread.sleep(300);
        b.net.send(Protocol.join(Protocol.CHAT, "채팅 잘 보입니다")); Thread.sleep(600);
        SwingUtilities.invokeAndWait(() -> {
            a.roomList.setSelectedIndex(0); b.roomList.setSelectedIndex(0);
            paint(a, 640, 420, "lobby_cheon.png");
            paint(b, 640, 420, "lobby_seo.png");
            pair(new File(out, "lobby_cheon.png"), new File(out, "lobby_seo.png"), "lobby_pair.png",
                 "cheon (방장)", "seo");
        });
        a.net.close(); b.net.close();

        // 3) 터미널 캡처: 서버 콘솔 + nc 클라이언트
        terminal("terminal.png", new String[][] {
            {"서버: java -cp build/classes arcade.server.GameServer", read(new File(logs, "server.log"))},
            {"클라이언트 B: nc localhost 5000", read(new File(logs, "termB.txt"))},
            {"클라이언트 C: nc localhost 5000", read(new File(logs, "termC.txt"))},
        });
        terminal("client_console.png", new String[][] {
            {"EchoStub + ClientMain 콘솔", read(new File(logs, "echo_client.txt"))},
        });
        System.exit(0);
    }

    static String read(File f) throws IOException { return Files.readString(f.toPath()); }

    static void paint(JComponent c, int w, int h, String name) {
        c.setSize(w, h); layout(c);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        c.print(g); g.dispose();
        try { ImageIO.write(frame(img, name.startsWith("login") ? "BubbleNet - 접속" : "BubbleNet 대기실 - " + (name.contains("cheon") ? "cheon" : "seo")), "png", new File(out, name)); }
        catch (IOException e) { throw new RuntimeException(e); }
    }
    static void layout(Component c) {
        c.doLayout();
        if (c instanceof Container) for (Component k : ((Container) c).getComponents()) layout(k);
        c.validate();
    }
    /** 창 제목 표시줄을 그려 실제 창처럼 보이게 한다. */
    static BufferedImage frame(BufferedImage body, String title) {
        int bar = 28, w = body.getWidth() + 2, h = body.getHeight() + bar + 1;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0xE8E8E8)); g.fillRect(0, 0, w, bar);
        g.setColor(new Color(0xFF5F57)); g.fillOval(10, 8, 12, 12);
        g.setColor(new Color(0xFEBC2E)); g.fillOval(30, 8, 12, 12);
        g.setColor(new Color(0x28C840)); g.fillOval(50, 8, 12, 12);
        g.setColor(Color.DARK_GRAY); g.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, (w - fm.stringWidth(title)) / 2, 19);
        g.drawImage(body, 1, bar, null);
        g.setColor(new Color(0xA0A0A0)); g.drawRect(0, 0, w - 1, h - 1);
        g.dispose();
        return img;
    }
    static void pair(File l, File r, String name, String lt, String rt) {
        try {
            BufferedImage a = ImageIO.read(l), b = ImageIO.read(r);
            int gap = 16, w = a.getWidth() + b.getWidth() + gap, h = Math.max(a.getHeight(), b.getHeight());
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.WHITE); g.fillRect(0, 0, w, h);
            g.drawImage(a, 0, 0, null); g.drawImage(b, a.getWidth() + gap, 0, null);
            g.dispose();
            ImageIO.write(img, "png", new File(out, name));
        } catch (IOException e) { throw new RuntimeException(e); }
    }
    /** 터미널 창 여러 개를 세로로 쌓아 그린다. */
    static void terminal(String name, String[][] panes) throws IOException {
        Font font = new Font("Monospaced", Font.PLAIN, 13);
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        FontMetrics fm = probe.createGraphics().getFontMetrics(font);
        int lineH = fm.getHeight() + 2, pad = 10, bar = 24, w = 760, gap = 10, y0 = 0;
        int total = 0;
        for (String[] p : panes) total += bar + pad * 2 + p[1].split("\n").length * lineH + gap;
        BufferedImage img = new BufferedImage(w, total, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE); g.fillRect(0, 0, w, total);
        for (String[] p : panes) {
            String[] lines = p[1].split("\n");
            int h = bar + pad * 2 + lines.length * lineH;
            g.setColor(new Color(0x1E1E1E)); g.fillRoundRect(0, y0, w, h, 8, 8);
            g.setColor(new Color(0x3A3A3A)); g.fillRoundRect(0, y0, w, bar, 8, 8); g.fillRect(0, y0 + bar / 2, w, bar / 2);
            g.setColor(new Color(0xFF5F57)); g.fillOval(10, y0 + 6, 12, 12);
            g.setColor(new Color(0xFEBC2E)); g.fillOval(30, y0 + 6, 12, 12);
            g.setColor(new Color(0x28C840)); g.fillOval(50, y0 + 6, 12, 12);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12)); g.setColor(new Color(0xDDDDDD));
            g.drawString(p[0], 72, y0 + 17);
            g.setFont(font);
            int y = y0 + bar + pad + fm.getAscent();
            for (String line : lines) {
                boolean typed = line.startsWith("> ");
                g.setColor(typed ? new Color(0x8BE9FD) : line.startsWith("$") ? new Color(0x50FA7B) : new Color(0xF0F0F0));
                g.drawString(typed ? line.substring(2) : line, pad, y);
                y += lineH;
            }
            y0 += h + gap;
        }
        g.dispose();
        ImageIO.write(img, "png", new File(out, name));
    }
}
