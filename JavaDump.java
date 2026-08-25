import java.io.*;
import java.net.Socket;

public class JavaDump {
    public static void main(String[] args) throws Exception {
        Socket s = new Socket("localhost", 25597);
        DataOutputStream out = new DataOutputStream(s.getOutputStream());
        send(out, 1, 3, "test");
        send(out, 2, 2, "list");
        s.close();
    }
    static void send(DataOutputStream out, int id, int type, String payload) throws IOException {
        byte[] p = payload.getBytes("US-ASCII");
        out.write(le(10 + p.length)); out.write(le(id)); out.write(le(type));
        out.write(p); out.write(0); out.write(0);
        out.flush();
    }
    static byte[] le(int v) {
        return new byte[]{(byte)(v & 0xFF), (byte)((v>>8)&0xFF), (byte)((v>>16)&0xFF), (byte)((v>>24)&0xFF)};
    }
}
