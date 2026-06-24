import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static Map<String,String> store;

    // A parsed command: the operation name + its arguments.
    public record Command(String op, List<String> args) {
        // TODO: nothing required here yet. A record gives you op(), args(),
        //       constructor, equals/toString for free.
        //       (Later you might add a helper like arg(int i) — skip for now.)
    }
    static Command parse(String line) {
        if(line==null)return null;
        line=line.trim();
        if(line.isEmpty())return null;
        String[] arr=line.split("\\s+");
        String op=arr[0].toUpperCase();
        return new Command(op, Arrays.asList(arr).subList(1,arr.length));
    }
    static String execute(Command cmd) {
        String op = cmd.op();
        List<String> args = cmd.args();
        String res="";
        switch (op) {
            case "PING":
                res=!args.isEmpty()?args.get(0):"PONG";
                break;
            case "SET":
                if(args.size()!=2)res="ERR wrong number of arguments for 'set'";
                else {
                    store.put(args.get(0),args.get(1));
                    res="OK";
                }
                break;
            case "GET":
                if(args.size()!=1)res="ERR wrong number of arguments for 'get'";
                else res= store.getOrDefault(args.get(0), "(nil)");
                break;

            case "DEL":
                if(args.size()!=1)res="ERR wrong number of arguments for 'del'";
                else res = (store.remove(args.get(0)) != null) ? "1" : "0";
                break;
            default:
                res= "ERR unknown command '" + op + "'";
        }
        return res;
    }
    public static void setStore(){
        store=new HashMap<>();
    }
public static void main(String[] args) throws IOException {
    int port = 6380;
    ServerSocket socket=new ServerSocket(port);
    System.out.println(socket.getLocalSocketAddress());
    store=new HashMap<>();
    while(true) {
        Socket s = socket.accept();
        System.out.println(s.getRemoteSocketAddress());
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(s.getInputStream()));
            PrintWriter out = new PrintWriter(s.getOutputStream(), true);
            String line;
            while ((line = in.readLine()) != null) {
                Command cmd = parse(line);
                if (cmd == null) continue;
                String res=execute(cmd);// blank line → skip, don't crash
                out.println(res);
            }
        }catch(IOException e){
            System.out.println(e.getMessage());
        }finally{
        s.close();
        }
    }

}}