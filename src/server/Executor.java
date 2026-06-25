package server;

import datatypes.KeyCommands;
import datatypes.ListTypes;
import datatypes.StringTypes;
import exceptions.TypeException;

import java.util.Arrays;
import java.util.List;

public class Executor {

    private final ListTypes lists;
    private final StringTypes strings;
    private final KeyCommands keys ;
    
    public Store store;
    public Executor(Store store){
       this.store=store;
       lists= new ListTypes(store);
       strings= new StringTypes(store);
       keys= new KeyCommands(store);
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
            case "EXPIRE":  keys.expire(args, writer); break;
            case "PEXPIRE": keys.pexpire(args, writer); break;
            case "TTL":     keys.ttl(args, writer); break;
            case "PTTL":    keys.pttl(args, writer); break;
            case "PERSIST": keys.persist(args, writer); break;
            default: writer.writeError("ERR unknown command '" + cmd.op() + "'");
        }

    }
}
