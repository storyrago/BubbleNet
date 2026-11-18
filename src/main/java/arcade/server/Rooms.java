package arcade.server;

import arcade.net.Protocol;
import java.util.Map;
import java.util.TreeMap;

/** 서버 전체 방 목록. */
public class Rooms {
    private final Map<Integer, Room> map = new TreeMap<>();
    private int nextId = 1;

    synchronized Room create(String name, ClientHandler c) {
        if (c.room != null) c.room.leave(c);
        Room room = new Room(nextId++, name, this);
        map.put(room.id, room);
        System.out.println("[SERVER] " + c.nickname + " 방 생성: " + room.id + "(" + name + ")");
        room.join(c);
        return room;
    }

    synchronized boolean join(int id, ClientHandler c) {
        Room room = map.get(id);
        if (room == null) { c.send(Protocol.join(Protocol.SYS, "존재하지 않는 방입니다: " + id)); return false; }
        if (c.room != null) c.room.leave(c);
        System.out.println("[SERVER] " + c.nickname + " 방 입장: " + id);
        room.join(c);
        return true;
    }

    synchronized void remove(int id) { map.remove(id); }

    synchronized String list() {
        StringBuilder sb = new StringBuilder();
        for (Room r : map.values()) {
            if (sb.length() > 0) sb.append(';');
            sb.append(r.id).append(',').append(r.name).append(',').append(r.size());
        }
        return sb.toString();
    }
}
