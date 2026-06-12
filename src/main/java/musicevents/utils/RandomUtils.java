package musicevents.utils;

import musicevents.MusicEvents;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;



public class RandomUtils {
    private static final Random random = new SecureRandom(); // Used for higher-quality music shuffling
    /** Return a random value from a comma separated list. */
    public static String getRandomValueFromList(String List) {
        List<String> values = Arrays.asList(List.split(","));
        return values.get(random.nextInt(values.size())).trim();
    }
    /** Return a random value from a comma-separated list, excluding a result. It will return the excluded result if that is the only value or if the list is empty.*/
    public static String getRandomValueFromListExcluding(String List, String Exclude) {
        List<String> values = new LinkedList<>(Arrays.asList(List.split(",")));
        values.remove(Exclude);
        if(values.isEmpty()) {
            MusicEvents.LOGGER.error("empty playlist. list was : {}", List);
            return Exclude;
        }
        return values.get(random.nextInt(values.size())).trim();
    }
}
