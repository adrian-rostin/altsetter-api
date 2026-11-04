package de.adrianrostin.altsetter.repository;

import de.adrianrostin.altsetter.entity.Exercise;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Nur lesend: bewusst kein JpaRepository, damit save/delete gar nicht erst angeboten werden. */
public interface ExerciseRepository extends Repository<Exercise, String> {

    /** Alle Übungen, sortiert nach ihrem Namen in {@code language}, bei Gleichstand nach ID. */
    @Query("select e from Exercise e join e.texts t where key(t) = :language order by t.name, e.id")
    List<Exercise> findAllOrderedByName(String language);

    Optional<Exercise> findById(String id);
}
