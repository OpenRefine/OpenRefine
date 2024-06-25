import org.apache.commons.text.similarity.LevenshteinDistance;

public class EditDistance {

    /**
     * Calculates the edit distance between two strings using the Levenshtein distance algorithm.
     * 
     * @param s1        First string.
     * @param s2        Second string.
     * @param threshold Optional threshold for early termination.
     * @return The Levenshtein distance between the two strings.
     */
    public static Integer editDistance(String s1, String s2, Integer... threshold) {
        LevenshteinDistance levenshteinDistance;
        if (threshold.length > 0 && threshold[0] != null) {
            levenshteinDistance = new LevenshteinDistance(threshold[0]);
        } else {
            levenshteinDistance = new LevenshteinDistance();
        }

        return levenshteinDistance.apply(s1, s2);
    }
}