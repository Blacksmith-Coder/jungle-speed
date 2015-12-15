//FINI

import java.io.*;
import java.net.*;

class JungleServer {

    public final static int REQ_LISTPARTY = 1;
    public final static int REQ_CREATEPARTY = 2;
    public final static int REQ_JOINPARTY = 3;
    public final static int REQ_WAITPARTYSTARTS = 4;
    public final static int REQ_WAITTURNSTARTS = 5;
    public final static int REQ_PLAY = 6;

    public final static int ACT_NOP = 1;
    public final static int ACT_TAKETOTEM = 2;
    public final static int ACT_HANDTOTEM = 3;
    public final static int ACT_INCORRECT = 4;

    public static void  main(String []args) {

        ServerSocket conn = null;
        Socket comm = null;
        Game game = null;
        int port = 12345;
        ThreadServer t = null;

        if (args.length != 1) {
            System.out.println("usage: JungleServer");
            System.exit(1);
        }

        try {
            conn = new ServerSocket(port);
        }
        catch(IOException e) {
            System.out.println("cannot create server socket: "+e.getMessage());
            System.exit(1);
        }

        game = new Game();

        while (true) {
            try {

                comm = conn.accept();
                t = new ThreadServer(game, comm);
                t.start();

            }
            catch(IOException e) {
                System.out.println("communication problem: "+e.getMessage());
            }
        }
    }
}

