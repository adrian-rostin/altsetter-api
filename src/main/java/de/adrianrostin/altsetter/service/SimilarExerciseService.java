package de.adrianrostin.altsetter.service;

import de.adrianrostin.altsetter.dto.ExerciseSummaryDto;
import de.adrianrostin.altsetter.entity.Language;
import de.adrianrostin.altsetter.exception.NotFoundException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Ähnliche Übungen: welche trainieren möglichst dieselben Muskeln in denselben Rollen?
 *
 * <p>Jede Übung wird zu einem Vektor über die Muskeln: Hauptmuskeln zählen {@value #PRIMARY},
 * mitarbeitende {@value #SECONDARY}. Ähnlichkeit ist der Kosinus zwischen zwei Vektoren (0 bis 1),
 * damit Übungen mit vielen Muskeln nicht allein deshalb vorne landen. Infrage kommt eine Übung nur,
 * wenn sie einen Hauptmuskel der Übung trainiert und die Übung umgekehrt einen ihrer Hauptmuskeln;
 * so zählen auch vertauschte Rollen (z. B. Hammer-Curls und Hammer-Curls am Seilzug). Unter
 * {@value #MIN_SCORE} passt es nicht mehr; lieber weniger als {@value #LIMIT} Treffer.
 */
@Service
@RequiredArgsConstructor
public class SimilarExerciseService {

    public static final String CACHE = "exercise-similar";

    static final int LIMIT = 5;
    static final double MIN_SCORE = 0.5;
    private static final int PRIMARY = 2;
    private static final int SECONDARY = 1;

    private final ExerciseService exercises;

    /** Bis zu {@value #LIMIT} ähnliche Übungen, die ähnlichste zuerst, bei Gleichstand nach Name. */
    @Cacheable(CACHE)
    public List<ExerciseSummaryDto> similar(String id, Language language) {
        List<ExerciseSummaryDto> all = exercises.summaries(language);
        ExerciseSummaryDto target = all.stream()
                .filter(exercise -> exercise.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(NotFoundException.Resource.EXERCISE, id));
        return rank(target, all);
    }

    /** Rangliste zu {@code target} aus {@code candidates} (nach Name sortiert, wie die Übungsliste). */
    static List<ExerciseSummaryDto> rank(ExerciseSummaryDto target, List<ExerciseSummaryDto> candidates) {
        Map<String, Integer> targetWeights = weights(target);
        return candidates.stream()
                .filter(candidate -> !candidate.id().equals(target.id()) && related(target, candidate))
                .map(candidate -> new Scored(candidate, cosine(targetWeights, weights(candidate))))
                .filter(scored -> scored.score() >= MIN_SCORE)
                // stabil: bei gleicher Ähnlichkeit bleibt die Reihenfolge nach Name
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .limit(LIMIT)
                .map(Scored::exercise)
                .toList();
    }

    private record Scored(ExerciseSummaryDto exercise, double score) {}

    /** Beide Richtungen: Jede der zwei Übungen trainiert einen Hauptmuskel der anderen. */
    private static boolean related(ExerciseSummaryDto a, ExerciseSummaryDto b) {
        return a.primaryMuscles().stream().anyMatch(trained(b)::contains)
                && b.primaryMuscles().stream().anyMatch(trained(a)::contains);
    }

    private static Set<String> trained(ExerciseSummaryDto exercise) {
        Set<String> muscles = new HashSet<>(exercise.primaryMuscles());
        muscles.addAll(exercise.secondaryMuscles());
        return muscles;
    }

    private static Map<String, Integer> weights(ExerciseSummaryDto exercise) {
        Map<String, Integer> weights = new HashMap<>();
        exercise.secondaryMuscles().forEach(muscle -> weights.put(muscle, SECONDARY));
        exercise.primaryMuscles().forEach(muscle -> weights.put(muscle, PRIMARY));
        return weights;
    }

    private static double cosine(Map<String, Integer> a, Map<String, Integer> b) {
        double dot = 0;
        for (Map.Entry<String, Integer> entry : a.entrySet()) {
            dot += entry.getValue() * b.getOrDefault(entry.getKey(), 0);
        }
        double norms = norm(a) * norm(b);
        return norms == 0 ? 0 : dot / norms;
    }

    private static double norm(Map<String, Integer> weights) {
        return Math.sqrt(weights.values().stream().mapToDouble(w -> w * w).sum());
    }
}
