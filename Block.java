// Importe les bibliothèques Java nécessaires
import java.security.MessageDigest;
import java.util.Date;

/**
 * La classe Block définit la structure d'un bloc dans la blockchain.
 * Elle contient les données, les hashs, et les fonctions pour
 * calculer le hash (calculateHash) et pour miner (mineBlock).
 */
public class Block {

    // --- 1. Variables Statiques (pour la difficulté) ---
    
    /**
     * Définit le nombre de zéros requis au début du hash.
     * Augmenter ce nombre rend le minage exponentiellement plus difficile.
     */
    public static int difficulty = 4; // Vous pouvez ajuster ce nombre (ex: 4 ou 5)
    
    /**
     * La chaîne cible de zéros (ex: "0000" si difficulty = 4).
     * C'est ce que le hash doit préfixer pour être valide.
     */
    public static String difficultyTarget = new String(new char[difficulty]).replace('\0', '0');

    
    // --- 2. Variables d'Instance (les données du bloc) ---
    
    public String hash;             // L'empreinte digitale (hash) de ce bloc.
    public String previousHash;     // L'empreinte digitale du bloc précédent.
    public String transactions;     // Les données (ex: "Alice envoie 10 à Bob").
    public long timestamp;          // L'heure de création (en millisecondes).
    public int nonce;              // Le "nombre magique" (compteur) du Proof of Work.

    
    // --- 3. Le Constructeur (la "recette" pour créer un bloc) ---
    
    /**
     * Construit un nouveau bloc.
     * @param transactions Les données à stocker dans ce bloc.
     * @param previousHash Le hash du bloc qui précède celui-ci.
     */
    public Block(String transactions, String previousHash) {
        this.transactions = transactions;
        this.previousHash = previousHash;
        this.timestamp = new Date().getTime(); // Met l'heure actuelle
        this.nonce = 0; // On commence toujours à chercher à partir de nonce = 0
        this.hash = calculateHash(); // Calcule le hash initial (ne sera pas valide)
    }

    
    // --- 4. La Fonction de Hachage (le "moteur" cryptographique) ---
    
    /**
     * Calcule le hash SHA-256 de ce bloc.
     * Le hash est basé sur TOUTES les données du bloc.
     * @return Le hash calculé sous forme de chaîne hexadécimale.
     */
    public String calculateHash() {
        try {
            // On assemble toutes les données en une seule chaîne de texte
            String dataToHash = previousHash +
                                Long.toString(timestamp) +
                                Integer.toString(nonce) +
                                transactions;
            
            // On applique l'algorithme de hachage SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(dataToHash.getBytes("UTF-8"));
            
            // On convertit le résultat (octets) en un texte lisible (hexadécimal)
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < hashBytes.length; i++) {
                String hex = Integer.toHexString(0xff & hashBytes[i]);
                if (hex.length() == 1) hexString.append('0'); // Ajoute un '0' si nécessaire
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (Exception e) {
            // En cas d'erreur (ex: SHA-256 non trouvé)
            throw new RuntimeException(e);
        }
    }

    
    // --- 5. La Fonction de Minage (le "Proof of Work") ---
    
    /**
     * Exécute la boucle de minage.
     * Augmente le 'nonce' jusqu'à ce que le hash du bloc commence
     * par le nombre de zéros défini par 'difficultyTarget'.
     */
    public void mineBlock() {
        // Continue de chercher tant que le début du hash n'est pas "0000..."
        while (!hash.substring(0, difficulty).equals(difficultyTarget)) {
            nonce++; // Incrémente le compteur
            hash = calculateHash(); // Recalcule le hash avec le nouveau nonce
        }
        
        // Une fois trouvé, on affiche la bonne nouvelle dans la console
        System.out.println("Bloc Miné !!! (Nonce trouvé: " + nonce + ") Hash: " + hash);
    }

}
