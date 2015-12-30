//FINI

import java.io.*;
import java.util.*;

public class OutputStreamPool {

    Map<Integer, ObjectOutputStream> pool = null;

    public OutputStreamPool() {
        pool = new HashMap<Integer, ObjectOutputStream>();
    }

    /**
     * Fonction ajouter un flux prend une id et un flux en paramètre
     * les places dans une collection de type Map (association clé -> valeur)
     * pour faire correspondre l'id joueur au flux
     * @param id
     * @param oos
     */
    public synchronized void addStream(Integer id, ObjectOutputStream oos) {
        pool.put(id, oos);
    }

    /**
     * Enlève un flux en désignant la clé
     * @param id
     */
    public synchronized void removeStream(Integer id) {
        pool.remove(id);
    }

    /**
     * Envoie à tous le monde : le serveur utilise cette fonction pour poster
     * à tous les clients en balayant toute la Map via la clé et en écrivant
     * le même objet dans chaque flux.
     * @param o
     * @throws IOException
     */
    public synchronized void sendToAll(Object o) throws IOException {
        boolean err = false;
        String msg = "Ne peut pas poster à : ";
        for (Integer dest : pool.keySet()) {
            try {
                pool.get(dest).writeObject(o);
                pool.get(dest).flush();
            }
            catch(IOException e) {
                err = true;
                msg += dest;
            }
        }
        if (err) {
            throw new IOException(msg);
        }
    }
}
  
