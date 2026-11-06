package de.adrianrostin.altsetter.repository;

import de.adrianrostin.altsetter.entity.ExerciseImage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Nur lesend, wie alle Repositories der API. */
public interface ExerciseImageRepository extends Repository<ExerciseImage, ExerciseImage.Key> {

    /** Angaben zu einem Bild ohne seine Bytes. {@code version} ist der MD5 der Bytes. */
    interface Info {
        String getExerciseId();

        String getPosition();

        int getWidth();

        int getHeight();

        String getVersion();
    }

    /** Alle Bilder ohne ihre Bytes; die Prüfsumme rechnet Postgres aus. */
    @Query(
            value = "select exercise_id as exerciseId, position, width, height, md5(data) as version"
                    + " from {h-schema}exercise_image",
            nativeQuery = true)
    List<Info> findAllInfos();

    Optional<ExerciseImage> findByKey(ExerciseImage.Key key);
}
