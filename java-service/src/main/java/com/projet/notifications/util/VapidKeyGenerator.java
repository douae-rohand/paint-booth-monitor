package com.projet.notifications.util;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.util.encoders.Base64;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Utilitaire autonome pour générer une paire de clés VAPID.
 *
 * Les clés VAPID (publique + privée) doivent être générées UNE SEULE FOIS
 * et stockées en variables d'environnement.
 *
 * Exécution : java -cp target/classes com.projet.notifications.util.VapidKeyGenerator
 * Ou via Maven : mvn exec:java -Dexec.mainClass="com.projet.notifications.util.VapidKeyGenerator"
 *
 * Copiez les valeurs générées dans votre .env :
 * VAPID_PUBLIC_KEY=<valeur générée>
 * VAPID_PRIVATE_KEY=<valeur générée>
 * VAPID_SUBJECT=mailto:contact@votre-domaine.com
 */
public class VapidKeyGenerator {

    public static void main(String[] args) {
        try {
            // Enregistrer BouncyCastle comme provider de sécurité
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

            // Générer une paire de clés ECDSA P-256 (requis pour VAPID)
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
            keyPairGenerator.initialize(new java.security.spec.ECGenParameterSpec("P-256"), new SecureRandom());
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            // Clé publique : extraire les 65 bytes raw (format non compressé 04+X+Y)
            // requis par pushManager.subscribe() côté navigateur
            org.bouncycastle.jce.interfaces.ECPublicKey ecPublicKey =
                    (org.bouncycastle.jce.interfaces.ECPublicKey) keyPair.getPublic();
            byte[] publicKeyRaw = ecPublicKey.getQ().getEncoded(false); // false = non compressé = 65 bytes

            // Clé privée : extraire les 32 bytes raw (format scalaire S)
            // requis par la bibliothèque nl.martijndwars:web-push côté Java
            org.bouncycastle.jce.interfaces.ECPrivateKey ecPrivateKey =
                    (org.bouncycastle.jce.interfaces.ECPrivateKey) keyPair.getPrivate();
            byte[] privateKeyRaw = ecPrivateKey.getD().toByteArray();
            // toByteArray() peut retourner 33 bytes si le bit de signe est présent → tronquer
            if (privateKeyRaw.length == 33 && privateKeyRaw[0] == 0) {
                privateKeyRaw = java.util.Arrays.copyOfRange(privateKeyRaw, 1, 33);
            }

            String publicKey  = base64UrlEncode(publicKeyRaw);
            String privateKey = base64UrlEncode(privateKeyRaw);

            System.out.println("=== Clés VAPID générées ===");
            System.out.println("VAPID_PUBLIC_KEY=" + publicKey);
            System.out.println("VAPID_PRIVATE_KEY=" + privateKey);
            System.out.println();
            System.out.println("Copiez ces valeurs dans votre fichier .env :");
            System.out.println("VAPID_PUBLIC_KEY=" + publicKey);
            System.out.println("VAPID_PRIVATE_KEY=" + privateKey);
            System.out.println("VAPID_SUBJECT=mailto:contact@votre-domaine.com");
        } catch (Exception e) {
            System.err.println("Erreur lors de la génération des clés VAPID : " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Encode un tableau de bytes en Base64 URL-safe (sans padding).
     */
    private static String base64UrlEncode(byte[] data) {
        String base64 = Base64.toBase64String(data);
        return base64
                .replace('+', '-')
                .replace('/', '_')
                .replace("=", "");
    }
}
