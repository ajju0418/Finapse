
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class scratch_test {
    public static void main(String[] args) {
        String[] lines = {
            "07/11/2025 | 12:36 Payu*Swiggy FoodBangalore ? 319.00 .",
            "07/11/2025 12:36 Payu*Swiggy FoodBangalore 319.00",
            "07/11/2025 | 12:36 Payu*Swiggy FoodBangalore ? 319.00",
            "07/11/25 Payu*Swiggy FoodBangalore 319.00 Cr",
            "07/11/2025  Payu*Swiggy FoodBangalore ? 319.00 ."
        };

        Pattern startPattern = Pattern.compile("^(\\d{2}/\\d{2}/\\d{2,4})\\s*(?:\\|?\\s*\\d{2}:\\d{2}\\s+)?(.*)");
        Pattern endPattern = Pattern.compile("^(.*?)\\s+(?:(?:?|Rs\\.?|INR)\\s*)?([\\d,]+\\.\\d{2})\\s*(Cr|Dr)?(?:\\s+\\S+)?\\s*$");

        for (String line : lines) {
            System.out.println("Line: " + line);
            Matcher startMatcher = startPattern.matcher(line);
            if (startMatcher.find()) {
                System.out.println("  Start matches! Date: " + startMatcher.group(1) + ", Rest: " + startMatcher.group(2));
                Matcher endMatcher = endPattern.matcher(startMatcher.group(2));
                if (endMatcher.find()) {
                    System.out.println("  End matches! Narration: " + endMatcher.group(1) + ", Amount: " + endMatcher.group(2) + ", Marker: " + endMatcher.group(3));
                } else {
                    System.out.println("  End does not match.");
                }
            } else {
                System.out.println("  Start does not match.");
            }
        }
    }
}

