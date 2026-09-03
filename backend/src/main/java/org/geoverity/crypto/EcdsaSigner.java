package org.geoverity.crypto;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Component
public class EcdsaSigner {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private static final String ECDSA_ALGORITHM = "SHA256withECDSA";
    private static final String EC_CURVE = "secp256r1"; // NIST P-256

    @Value("${geoverity.security.ecdsa-key-directory:./keys}")
    private String keyDirectory;

    private PrivateKey privateKey;

    @Getter
    private PublicKey publicKey;

    @PostConstruct
    public void init() {
        try {
            loadOrGenerateKeys();
            log.info("GeoVerity ECDSA P-256 Signing Engine initialized successfully.");
        } catch (Exception e) {
            log.error("Failed to initialize ECDSA keys: {}", e.getMessage(), e);
            throw new RuntimeException("Could not initialize ECDSA Signing Engine", e);
        }
    }

    private synchronized void loadOrGenerateKeys() throws Exception {
        File dir = new File(keyDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File privFile = new File(dir, "geoverity_ecdsa_private.key");
        File pubFile = new File(dir, "geoverity_ecdsa_public.key");

        if (privFile.exists() && pubFile.exists()) {
            byte[] privBytes = Files.readAllBytes(privFile.toPath());
            byte[] pubBytes = Files.readAllBytes(pubFile.toPath());

            KeyFactory kf = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
            this.privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
            this.publicKey = kf.generatePublic(new X509EncodedKeySpec(pubBytes));
            log.info("Loaded existing ECDSA P-256 keys from {}", dir.getAbsolutePath());
        } else {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
            kpg.initialize(new ECGenParameterSpec(EC_CURVE), new SecureRandom());
            KeyPair kp = kpg.generateKeyPair();
            this.privateKey = kp.getPrivate();
            this.publicKey = kp.getPublic();

            try (FileOutputStream fosPriv = new FileOutputStream(privFile);
                 FileOutputStream fosPub = new FileOutputStream(pubFile)) {
                fosPriv.write(this.privateKey.getEncoded());
                fosPub.write(this.publicKey.getEncoded());
            }
            log.info("Generated new ECDSA P-256 server keypair at {}", dir.getAbsolutePath());
        }
    }

    /**
     * Signs data using server-side ECDSA P-256 with SHA-256.
     *
     * @param data Raw payload bytes (e.g. SHA-256 hash or composite payload)
     * @return Base64-encoded digital signature
     */
    public String sign(byte[] data) {
        try {
            Signature ecdsaSign = Signature.getInstance(ECDSA_ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
            ecdsaSign.initSign(this.privateKey);
            ecdsaSign.update(data);
            byte[] signatureBytes = ecdsaSign.sign();
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (Exception e) {
            log.error("Signing failed: {}", e.getMessage(), e);
            throw new SecurityException("Failed to generate ECDSA signature", e);
        }
    }

    /**
     * Verifies ECDSA signature using server public key.
     *
     * @param data Raw payload bytes
     * @param base64Signature Base64-encoded signature
     * @return true if signature is valid and authentic
     */
    public boolean verify(byte[] data, String base64Signature) {
        return verifyWithKey(data, base64Signature, this.publicKey);
    }

    /**
     * Verifies ECDSA signature with specific public key.
     */
    public boolean verifyWithKey(byte[] data, String base64Signature, PublicKey pubKey) {
        try {
            byte[] sigBytes = Base64.getDecoder().decode(base64Signature);
            Signature ecdsaVerify = Signature.getInstance(ECDSA_ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
            ecdsaVerify.initVerify(pubKey);
            ecdsaVerify.update(data);
            return ecdsaVerify.verify(sigBytes);
        } catch (Exception e) {
            log.warn("ECDSA verification failed: {}", e.getMessage());
            return false;
        }
    }

    public String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(this.publicKey.getEncoded());
    }
}
