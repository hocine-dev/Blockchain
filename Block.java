// On importe les bibliothèques Java pour le hachage
import java.security.MessageDigest;
import java.util.Date;

public class Block {

    // 1. Les données du bloc
    public String hash;             // Le hash de ce bloc (son "sceau")
    public String previousHash;     // Le hash du bloc précédent (la "chaîne")
    public String transactions;     // Les données (ex: "Alice envoie 10 à Bob")
    public long timestamp;          // L'heure de création
    public int nonce;              // Le "nombre magique" trouvé par le mineur

    // 2. Le constructeur (la "recette" pour créer un bloc)
    public Block(String transactions, String previousHash) {
        this.transactions = transactions;
        this.previousHash = previousHash;
        this.timestamp = new Date().getTime(); // Met l'heure actuelle
        this.nonce = 0; // On commence à chercher à partir de 0
        this.hash = calculateHash(); // Calcule le hash au moment de la création
    }

    // 3. La fonction de hachage (le "minage")
    // C'est le cœur du Proof of Work
    public String calculateHash() {
        try {
            // On assemble toutes les données du bloc en une seule chaîne de texte
            String dataToHash = previousHash +
                                Long.toString(timestamp) +
                                Integer.toString(nonce) +
                                transactions;
            
            // On applique l'algorithme de hachage SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(dataToHash.getBytes("UTF-8"));
            
            // On convertit le résultat en un texte lisible (hexadécimal)
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < hashBytes.length; i++) {
                String hex = Integer.toHexString(0xff & hashBytes[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (Exception e) {
            // En cas d'erreur
            throw new RuntimeException(e);
        }
    }

}
