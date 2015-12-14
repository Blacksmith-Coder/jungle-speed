/* NOTE :
  Cette classe est complète : aucun ajout n'y est normalement nécéssaire
 */


public class IllegalRequestException extends Exception {

    public IllegalRequestException(String message) {
        super("Requête illégale "+message);
    }
}
