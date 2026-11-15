package de.adrianrostin.altsetter.service;

import de.adrianrostin.altsetter.dto.EquipmentDto;
import de.adrianrostin.altsetter.dto.ExerciseSummaryDto;
import de.adrianrostin.altsetter.entity.Language;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Suche nach Übungen für das Suchfeld der App. Groß-/Kleinschreibung, Umlaute, Akzente und
 * Satzzeichen zählen nicht ("schragbank" findet "Schrägbank"). Treffer in Stufen, innerhalb einer
 * Stufe nach Name (wie die Übungsliste):
 *
 * <ol>
 *   <li>der Name beginnt mit der Eingabe ("Landm" → Landmine Press),
 *   <li>jedes Wort der Eingabe beginnt ein Wort im Namen ("curls lang" → Langhantel-Curls),
 *   <li>jedes Wort der Eingabe steht irgendwo im Namen ("drücken" → Schulterdrücken),
 *   <li>jedes Wort beginnt ein Wort in Name, Hauptmuskeln oder Equipment ("bizeps", "kurzhantel"),
 *   <li>der Name in einer anderen Sprache passt ("bench press" bei deutscher Oberfläche).
 * </ol>
 *
 * Alles läuft auf der Übungsliste im Speicher (siehe {@link ExerciseService#summaries}).
 */
@Service
@RequiredArgsConstructor
public class ExerciseSearchService {

    private static final int NO_MATCH = Integer.MAX_VALUE;

    private final ExerciseService exercises;
    private final MuscleService muscles;

    /** Alle passenden Übungen, die besten zuerst; leer bei leerer Eingabe. Geblättert wird darin. */
    public List<ExerciseSummaryDto> search(String query, Language language) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            return List.of();
        }
        MuscleCatalog catalog = muscles.catalog(language);
        Map<String, String> otherNames = otherNames(language);
        return exercises.summaries(language).stream()
                .map(exercise -> new Hit(exercise, tier(normalizedQuery, searchable(exercise, catalog, otherNames))))
                .filter(hit -> hit.tier() != NO_MATCH)
                // stabil: innerhalb einer Stufe bleibt die Reihenfolge nach Name
                .sorted(Comparator.comparingInt(Hit::tier))
                .map(Hit::exercise)
                .toList();
    }

    /** Was an einer Übung durchsucht wird, schon normalisiert. */
    record Searchable(String name, String extras, String otherNames) {}

    private record Hit(ExerciseSummaryDto exercise, int tier) {}

    /** Stufe 0 bis 4 wie oben, {@link #NO_MATCH} wenn nichts passt. */
    static int tier(String query, Searchable exercise) {
        List<String> tokens = words(query);
        List<String> nameWords = words(exercise.name());
        if (exercise.name().startsWith(query)) {
            return 0;
        }
        if (tokens.stream().allMatch(token -> startsAWord(nameWords, token))) {
            return 1;
        }
        if (tokens.stream().allMatch(exercise.name()::contains)) {
            return 2;
        }
        List<String> allWords = Stream.concat(nameWords.stream(), words(exercise.extras()).stream())
                .toList();
        if (tokens.stream().allMatch(token -> startsAWord(allWords, token))) {
            return 3;
        }
        List<String> otherWords = words(exercise.otherNames());
        if (tokens.stream()
                .allMatch(token ->
                        startsAWord(otherWords, token) || exercise.otherNames().contains(token))) {
            return 4;
        }
        return NO_MATCH;
    }

    /** Kleinbuchstaben ohne Akzente, ß als ss, alles außer Buchstaben und Ziffern als Leerzeichen. */
    static String normalize(String text) {
        String decomposed = Normalizer.normalize(text.toLowerCase(Locale.ROOT).replace("ß", "ss"), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static Searchable searchable(
            ExerciseSummaryDto exercise, MuscleCatalog catalog, Map<String, String> otherNames) {
        String extras = Stream.concat(
                        exercise.primaryMuscles().stream()
                                .map(id -> catalog.muscle(id).name()),
                        exercise.equipment().stream().map(EquipmentDto::name))
                .collect(Collectors.joining(" "));
        return new Searchable(
                normalize(exercise.name()), normalize(extras), normalize(otherNames.getOrDefault(exercise.id(), "")));
    }

    /** Die Namen jeder Übung in allen anderen Sprachen, je Übung zusammengefasst. */
    private Map<String, String> otherNames(Language language) {
        Map<String, String> names = new HashMap<>();
        Arrays.stream(Language.values())
                .filter(other -> other != language)
                .flatMap(other -> exercises.summaries(other).stream())
                .forEach(exercise -> names.merge(exercise.id(), exercise.name(), (a, b) -> a + " " + b));
        return names;
    }

    private static List<String> words(String normalized) {
        return normalized.isEmpty() ? List.of() : List.of(normalized.split(" "));
    }

    private static boolean startsAWord(List<String> words, String token) {
        return words.stream().anyMatch(word -> word.startsWith(token));
    }
}
