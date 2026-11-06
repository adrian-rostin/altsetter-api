package de.adrianrostin.altsetter.service;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import de.adrianrostin.altsetter.controller.ApiPaths;
import de.adrianrostin.altsetter.dto.ExerciseImageDto;
import de.adrianrostin.altsetter.entity.CodedEnum;
import de.adrianrostin.altsetter.entity.ExerciseImage;
import de.adrianrostin.altsetter.entity.ImagePosition;
import de.adrianrostin.altsetter.exception.NotFoundException;
import de.adrianrostin.altsetter.repository.ExerciseImageRepository;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fotos der Übungen. Alle zusammen sind rund 16 MB groß und ändern sich nur von Hand, deshalb bleiben
 * Liste und Bytes im Speicher (siehe {@code CacheConfig}).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseImageService {

    public static final String INFO_CACHE = "exercise-image-infos";
    public static final String FILE_CACHE = "exercise-image-files";

    /** Länge der Version in den URLs; genug, um Bilder zu unterscheiden. */
    private static final int VERSION_LENGTH = 12;

    private final ExerciseImageRepository repository;

    /** Ein Bild mit Inhalt; {@code version} wie in der URL aus {@link ExerciseImageDto}. */
    public record ImageFile(String mimeType, byte[] data, String version) {}

    /** Bilder je Übungs-ID, Start vor Ende. Übungen ohne Bilder fehlen. */
    @Cacheable(INFO_CACHE)
    public Map<String, List<ExerciseImageDto>> byExercise() {
        return repository.findAllInfos().stream()
                .collect(groupingBy(
                        ExerciseImageRepository.Info::getExerciseId,
                        mapping(
                                ExerciseImageService::toDto,
                                collectingAndThen(
                                        toList(),
                                        images -> images.stream()
                                                .sorted(Comparator.comparing(ExerciseImageDto::position))
                                                .toList()))));
    }

    @Cacheable(FILE_CACHE)
    public ImageFile file(String exerciseId, ImagePosition position) {
        ExerciseImage image = repository
                .findByKey(new ExerciseImage.Key(exerciseId, position.getCode()))
                .orElseThrow(() -> new NotFoundException(NotFoundException.Resource.IMAGE, exerciseId));
        return new ImageFile(image.getMimeType(), image.getData(), shortVersion(md5(image.getData())));
    }

    private static ExerciseImageDto toDto(ExerciseImageRepository.Info info) {
        ImagePosition position = CodedEnum.fromCode(ImagePosition.class, info.getPosition());
        String url = "%s/exercises/%s/images/%s?v=%s"
                .formatted(ApiPaths.V1, info.getExerciseId(), position.getCode(), shortVersion(info.getVersion()));
        return new ExerciseImageDto(position, url, info.getWidth(), info.getHeight());
    }

    private static String shortVersion(String md5) {
        return md5.substring(0, VERSION_LENGTH);
    }

    private static String md5(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 fehlt in dieser JVM", e);
        }
    }
}
