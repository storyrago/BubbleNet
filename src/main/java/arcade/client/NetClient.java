package arcade.client;

import arcade.net.Protocol;
import javax.swing.SwingUtilities;
import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/** 소켓 연결, 송신, 수신 전용 스레드. 화면 갱신은 항상 EDT로 넘긴다. */
public class NetClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final Consumer<String[]> onMessage;      // 화면 쪽 콜백
    private final Runnable onDisconnect;

    public NetClient(Consumer<String[]> onMessage, Runnable onDisconnect) {
        this.onMessage = onMessage; this.onDisconnect = onDisconnect;
    }

    public void connect(String host, int port, String nickname) throws IOException {
        socket = new Socket(host, port);                     // 3-way handshake
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        System.out.println("[CLIENT] " + host + ":" + port + " 연결 성공, 닉네임 " + nickname);
        send(Protocol.join(Protocol.JOIN, nickname));
        Thread reader = new Thread(this::readLoop, "net-reader");
        reader.setDaemon(true);                              // 창이 닫히면 같이 종료
        reader.start();
    }

    private void readLoop() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[CLIENT] 수신: " + line);
                String[] f = Protocol.parse(line);
                SwingUtilities.invokeLater(() -> onMessage.accept(f));   // UI 갱신은 EDT에서
            }
        } catch (IOException e) { /* 서버 종료 또는 네트워크 오류 */ }
        System.out.println("[CLIENT] 서버 연결 종료 감지 -> 알림 표시");
        SwingUtilities.invokeLater(onDisconnect);
    }

    public void send(String msg) {
        if (out != null) { System.out.println("[CLIENT] 송신: " + msg); out.println(msg); }
    }

    public void close() { try { if (socket != null) socket.close(); } catch (IOException ignored) {} }
}
