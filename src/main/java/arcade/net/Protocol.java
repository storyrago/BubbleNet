package arcade.net;

/** 한 줄 = 메시지 하나. 첫 필드는 종류, 나머지는 '|'로 구분한 인자. */
public final class Protocol {
    public static final int PORT = 5555;
    public static final String SEP = "|";

    public static final String JOIN = "JOIN";               // JOIN|닉네임
    public static final String ROOM_CREATE = "ROOM_CREATE"; // ROOM_CREATE|방이름
    public static final String ROOM_JOIN = "ROOM_JOIN";     // ROOM_JOIN|방번호
    public static final String ROOM_LEAVE = "ROOM_LEAVE";   // ROOM_LEAVE
    public static final String ROOM_LIST = "ROOM_LIST";     // ROOM_LIST|번호,이름,인원;...
    public static final String ROOM_STATE = "ROOM_STATE";   // ROOM_STATE|방번호|닉1,닉2,...
    public static final String CHAT = "CHAT";               // CHAT|내용 / CHAT|닉네임|내용
    public static final String SYS = "SYS";                 // SYS|안내문

    private Protocol() {}

    /** split은 정규식을 받으므로 '|'를 이스케이프하고, 빈 마지막 필드도 유지한다(-1). */
    public static String[] parse(String line) { return line.split("\\|", -1); }

    public static String join(String... fields) { return String.join(SEP, fields); }
}
