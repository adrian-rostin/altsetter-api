package de.adrianrostin.altsetter.repository;

import de.adrianrostin.altsetter.entity.MuscleFamily;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

/** Nur lesend: bewusst kein JpaRepository, damit save/delete gar nicht erst angeboten werden. */
public interface MuscleFamilyRepository extends Repository<MuscleFamily, String> {

    @EntityGraph(attributePaths = "texts")
    List<MuscleFamily> findAllByOrderBySortOrder();

    /** Für den Health-Check: antwortet die Datenbank, und sind Daten da? */
    long count();
}
