package de.adrianrostin.altsetter.service;

import de.adrianrostin.altsetter.repository.MuscleFamilyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

/** Prüft, ob die API ihre Arbeit machen kann: Die Datenbank antwortet und enthält die Muskeldaten. */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthService {

    private final MuscleFamilyRepository families;

    /**
     * Ohne eigene Transaktion: Ist die Datenbank weg, scheitert schon das Öffnen der Transaktion im
     * Repository ({@link TransactionException}), und auch das soll hier als "nicht gesund" enden.
     */
    public boolean isHealthy() {
        try {
            return families.count() > 0;
        } catch (DataAccessException | TransactionException e) {
            log.warn("Health-Check: Datenbank nicht erreichbar: {}", e.getMessage());
            return false;
        }
    }
}
