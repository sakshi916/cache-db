import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Store {
    public Store(){
        store=new HashMap<>();
    }
     Map<String,String> store;
     Command parse(String line) {
        if(line==null)return null;
        line=line.trim();
        if(line.isEmpty())return null;
        String[] arr=line.split("\\s+");
        String op=arr[0].toUpperCase();
        return new Command(op, Arrays.asList(arr).subList(1,arr.length));
    }
     String execute(Command cmd) {
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

}
