package arcade.server;

import arcade.net.Protocol;
import java.io.*;
import java.net.Socket;
import java.util.List;

/** 클라이언트 한 명을 전담하는 서비스 스레드. readLine()이 null이면 상대가 연결을 끊은 것. */
public class ClientHandler extends Thread {
    private final Socket socket;
    private final Rooms rooms;
    private final List<ClientHandler> clients;
    private BufferedReader in;
    private PrintWriter out;
    String nickname = "?";
    Room room;

    ClientHandler(Socket socket, Rooms rooms, List<ClientHandler> clients) {
        this.socket = socket; this.rooms = rooms; this.clients = clients;
    }

    @Override public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true); // autoFlush
            String line;
            while ((line = in.readLine()) != null) {          // null = 상대가 연결을 끊음
                String[] f = Protocol.parse(line);
                switch (f[0]) {
                    case Protocol.JOIN:
                        nickname = f.length > 1 ? f[1] : "?";
                        send(Protocol.join(Protocol.SYS, nickname + "님 환영합니다"));
                        send(Protocol.join(Protocol.ROOM_LIST, rooms.list()));
                        break;
                    case Protocol.ROOM_CREATE: rooms.create(f.length > 1 ? f[1] : "방", this); break;
                    case Protocol.ROOM_JOIN:   rooms.join(Integer.parseInt(f[1].trim()), this); break;
                    case Protocol.ROOM_LEAVE:  if (room != null) room.leave(this); break;
                    case Protocol.ROOM_LIST:   send(Protocol.join(Protocol.ROOM_LIST, rooms.list())); break;
                    case Protocol.CHAT:
                        if (room != null) room.broadcast(Protocol.join(Protocol.CHAT, nickname, f.length > 1 ? f[1] : ""));
                        else send(Protocol.join(Protocol.SYS, "먼저 방에 입장하세요"));
                        break;
                    default: send(Protocol.join(Protocol.SYS, "알 수 없는 메시지: " + f[0]));
                }
            }
            System.out.println("[SERVER] " + nickname + " 연결 종료 (readLine null) 현재 " + (clients.size() - 1) + "명");
        } catch (IOException e) {
            System.out.println("[SERVER] " + nickname + " 통신 오류: " + e.getMessage());
        } finally {
            if (room != null) room.leave(this);                 // 방에서 제거 + 퇴장 알림
            clients.remove(this);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    void send(String msg) { out.println(msg); }
}
