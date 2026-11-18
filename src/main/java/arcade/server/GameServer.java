package arcade.server;

import arcade.net.Protocol;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Vector;

/** 접속 요청만 받는 main 스레드 + 클라이언트마다 ClientHandler 스레드 1개. */
public class GameServer {
    static final List<ClientHandler> clients = new Vector<>();   // 여러 서비스 스레드가 동시에 add/remove
    static final Rooms rooms = new Rooms();

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Protocol.PORT;
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("[SERVER] " + port + "번 포트에서 대기 중");
            while (true) {
                Socket socket = server.accept();                 // 접속이 올 때까지 블로킹
                ClientHandler handler = new ClientHandler(socket, rooms, clients);
                clients.add(handler);
                handler.start();                                 // 클라이언트 전용 서비스 스레드
                System.out.println("[SERVER] 접속: " + socket.getInetAddress() + " 현재 " + clients.size() + "명");
            }
        }
    }
}
