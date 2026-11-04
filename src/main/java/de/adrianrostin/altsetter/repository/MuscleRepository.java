package de.adrianrostin.altsetter.repository;

import de.adrianrostin.altsetter.entity.Muscle;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

/** Nur lesend: bewusst kein JpaRepository, damit save/delete gar nicht erst angeboten werden. */
public interface MuscleRepository extends Repository<Muscle, String> {

    @EntityGraph(attributePaths = {"meshNames", "texts"})
    List<Muscle> findAllByOrderBySortOrder();
}
