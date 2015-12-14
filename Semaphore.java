//Aquila NCM7 LPSIL 2015-2016

/* NOTE :
   Cette classe se comporte comme un sémaphore classique, à savoir une barrière de synchronisation pour empêcher
    fil pour poursuivre leur exécution jusqu'à ce qu'il n'y jetons dans le sémaphore.
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