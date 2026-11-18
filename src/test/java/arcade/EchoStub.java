package arcade;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/** 받은 줄을 "CHAT|echo|내용" 형태로 되돌려 주는 1인용 서버. 클라이언트 단독 테스트용 */
public class EchoStub {
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5555;
        try (ServerSocket ss = new ServerSocket(port); Socket s = ss.accept()) {
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"), true);
            String line;
            while ((line = in.readLine()) != null) {
                String[] f = line.split("\\|", -1);
                if (f[0].equals("JOIN")) out.println("SYS|" + f[1] + "님 환영합니다 (stub)");
                else if (f[0].equals("CHAT")) out.println("CHAT|echo|" + f[1]);
            }
        }
    }
}
