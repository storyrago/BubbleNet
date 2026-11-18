package arcade.server;

import arcade.net.Protocol;
import java.util.ArrayList;
import java.util.List;

/** 방 하나. 참가자 목록을 바꾸는 메소드와 순회하는 메소드를 모두 synchronized로 묶는다. */
public class Room {
    final int id;
    final String name;
    ClientHandler host;
    private final Rooms rooms;
    private final List<ClientHandler> members = new ArrayList<>();

    Room(int id, String name, Rooms rooms) { this.id = id; this.name = name; this.rooms = rooms; }

    synchronized void join(ClientHandler c) {
        members.add(c); c.room = this;
        if (host == null) host = c;
        broadcast(Protocol.join(Protocol.SYS, c.nickname + "님이 입장했습니다"));
        broadcast(Protocol.join(Protocol.ROOM_STATE, String.valueOf(id), names()));
    }

    synchronized void leave(ClientHandler c) {
        members.remove(c); c.room = null;
        if (members.isEmpty()) { rooms.remove(id); return; }
        if (host == c) host = members.get(0);          // 방장 위임
        broadcast(Protocol.join(Protocol.SYS, c.nickname + "님이 퇴장했습니다"));
        broadcast(Protocol.join(Protocol.ROOM_STATE, String.valueOf(id), names()));
    }

    synchronized void broadcast(String msg) {
        for (ClientHandler c : members) c.send(msg);
    }

    synchronized int size() { return members.size(); }

    synchronized String names() {
        StringBuilder sb = new StringBuilder();
        for (ClientHandler c : members) { if (sb.length() > 0) sb.append(','); sb.append(c.nickname); }
        return sb.toString();
    }
}
