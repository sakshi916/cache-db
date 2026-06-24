package server;

import datatypes.ListTypes;
import datatypes.StringTypes;
import exceptions.TypeException;

import java.util.*;

import static datatypes.ListTypes.WRONGTYPE;

public class Store {
    private final Map<String, Object> store = new HashMap<>();
    private final ListTypes lists = new ListTypes(store);
    private final StringTypes strings = new StringTypes(store);

    Command parse(String line) {
        if(line==null)return null;
        line=line.trim();
        if(line.isEmpty())return null;
        String[] arr=line.split("\\s+");
        String op=arr[0].toUpperCase();
        return new Command(op, Arrays.asList(arr).subList(1,arr.length));
    }
     void execute(Command cmd, RespWriter writer) throws TypeException {
        String op = cmd.op();
        List<String> args = cmd.args();
        switch (op) {
            case "PING":
                writer.writeSimple(args.isEmpty() ? "PONG" : args.get(0));
                break;
            case "SET":
               strings.set(args,writer);
                break;
            case "GET":
                strings.get(args,writer);
                break;
            case "DEL":
                strings.del(args,writer);
                break;
            case "LPUSH":
                lists.lpush(args,writer);break;

            case "RPUSH":
                lists.rpush(args,writer);
                break;

            case "LPOP":
                lists.lpop(args,writer);
                break;

            case "RPOP":
                lists.rpop(args,writer);
                break;

            case "LLEN":
                lists.llen(args,writer);
                break;

            case "LRANGE":
                lists.range(args,writer);break;

            default: writer.writeError("ERR unknown command '" + cmd.op() + "'");
        }

    }




}
