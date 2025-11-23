import java.security.MessageDigest;
import java.util.Date;

/**
 * Représente un Bloc dans la chaîne.
 * <p>
 * Cette classe est le cœur "passif" de la simulation. Elle définit la structure de données
 * immuable (une fois minée) et contient les méthodes cryptographiques (SHA-256)
 * nécessaires pour garantir l'intégrité et la sécurité de la blockchain.
 */
public class Block {

    // --- 1. Variables Statiques (Configuration du consensus) ---

    /**
     * Niveau de difficulté du Proof-of-Work.
     * Définit le nombre de zéros requis au début du hash.
     * Plus ce chiffre est élevé, plus la probabilité de trouver un hash valide est faible.
     */
    public static int difficulty = 3; 

    /**
     * La chaîne cible générée dynamiquement (ex: "000").
     * Le hash du bloc doit commencer par cette chaîne pour être considéré comme valide.
     */
    public static String difficultyTarget = new String(new char[difficulty]).replace('\0', '0');

 
    // --- 2. Variables d'Instance (Les données du bloc) ---

    /** L'empreinte numérique unique du bloc (SHA-256). */
    public String hash;

    /** * Le hash du bloc précédent. 
     * C'est ce lien qui crée la "chaîne" et empêche toute modification de l'historique.
     */
    public String previousHash;

    /** Les données utiles stockées dans le bloc (ex: "Alice envoie 10 à Bob"). */
    public String transactions;

    /** L'horodatage de la création du bloc (en millisecondes depuis 1970). */
    public long timestamp;

    /** * Le "Nonce" (Number used ONCE).
     * C'est la variable que le mineur modifie à chaque tentative pour essayer de trouver un hash valide.
     */
    public int nonce;

 
    // --- 3. Le Constructeur ---

    /**
     * Crée un nouveau bloc.
     * * @param transactions Les données à inscrire dans le registre.
     * @param previousHash L'empreinte du dernier bloc de la chaîne actuelle.
     */
    public Block(String transactions, String previousHash) {
        this.transactions = transactions;
        this.previousHash = previousHash;
        this.timestamp = new Date().getTime();
        this.nonce = 0; // On initialise le compteur de recherche à 0
        this.hash = calculateHash(); // Calcul initial (probablement invalide avant minage)
    }

 
    // --- 4. La Fonction de Hachage (Moteur Cryptographique) ---

    /**
     * Calcule l'empreinte numérique (Hash) du bloc en utilisant l'algorithme SHA-256.
     * <p>
     * Cette fonction concatène toutes les propriétés du bloc (y compris le nonce)
     * et retourne une chaîne hexadécimale unique.
     * * @return La signature SHA-256 du bloc sous forme de chaîne de caractères.
     */
    public String calculateHash() {
        try {
            // 1. Concaténation de toutes les données du bloc
            String dataToHash = previousHash +
                                Long.toString(timestamp) +
                                Integer.toString(nonce) +
                                transactions;
            
            // 2. Application de l'algorithme SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(dataToHash.getBytes("UTF-8"));
            
            // 3. Conversion des octets en chaîne hexadécimale lisible
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < hashBytes.length; i++) {
                String hex = Integer.toHexString(0xff & hashBytes[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    // --- 5. Fonction de Minage (Tentative Unique) ---

    /**
     * Effectue UNE SEULE tentative de minage avec un nonce donné.
     * <p>
     * Contrairement à une boucle `while` classique, cette fonction est conçue pour
     * être appelée répétitivement par un agent (Statechart) sans bloquer la simulation.
     * * @param nonceToTry Le nombre entier à tester pour cette tentative.
     * @return Le hash validé si la difficulté est atteinte, ou {@code null} si la tentative a échoué.
     */
    public String mineBlock(int nonceToTry) { 
	    
	    // 1. Mettre à jour le nonce et calculer le hash
	    this.nonce = nonceToTry;
	    this.hash = calculateHash();
	    
	    // 2. Vérifier si ce hash est "gagnant" (commence par le nombre de zéros requis)
	    if (hash.substring(0, difficulty).equals(difficultyTarget)) {
	        // OUI ! C'est un succès. On retourne le hash gagnant.
	        return hash;
	    } else {
	        // NON. C'est un échec. On retourne 'null'.
	        return null;
	    }
	}
}