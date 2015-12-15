//FINI

/* NOTE :
    Sémaphore : En informatique comportement qui permet l'exclusion mutuelle.

   Cette classe se comporte comme une barrière de synchronisation pour empêcher
    les threads de poursuivre leur exécution.
    Un sémaphore est une boîte contenant des jetons. Le comportement est le suivant:
       - Un thread peut directement prendre un jeton dans la case si il ya au moins un.
       - Si il n'y a pas de jetons, le thread doit attendre que l'un soit mis.
       - Un thread peut mettre autant de jetons qu'il veut dans la boîte.
 */

class Semaphore {

    int nbTokens; // nombre de jetons

    public Semaphore(int nbTokens) {
        this.nbTokens = nbTokens;
    }

    public synchronized void put(int nb) {
        nbTokens += nb;
        notifyAll();
    }

    public synchronized void get() {
        while (nbTokens == 0) {
            try {
                wait();
            }
            catch(InterruptedException e) {}
        }
        nbTokens -= 1;
    }
}