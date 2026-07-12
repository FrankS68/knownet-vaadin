package de.witchcafe.knownet.service;

import de.witchcafe.knownet.domain.ApiKey;
import de.witchcafe.knownet.repo.ApiKeyRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class ApiKeyService {

    private final ApiKeyRepository repository;

    public ApiKeyService(ApiKeyRepository repository) {
        this.repository = repository;
    }

    /**
     * Erzeugt einen neuen API-Key, speichert den Hash und gibt den Klartext-Token zurueck.
     * Der Klartext wird nur einmal angezeigt und danach nicht mehr zugreifbar.
     */
    public String erzeugeApiKey(String name, String rolle) {
        String token = generiereToken();
        String hash = hash(token);
        repository.save(new ApiKey(name, hash, rolle));
        return token;
    }

    public Optional<ApiKey> findByToken(String token) {
        return repository.findByTokenHashAndAktivTrue(hash(token));
    }

    public List<ApiKey> alleKeys() {
        return repository.findAll();
    }

    public void deaktiviere(Long id) {
        repository.findById(id).ifPresent(k -> {
            k.setAktiv(false);
            repository.save(k);
        });
    }

    public void loesche(Long id) {
        repository.deleteById(id);
    }

    private String generiereToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return "kn_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
