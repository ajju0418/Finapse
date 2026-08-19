package com.finapse.service;

import com.finapse.entity.Category;
import com.finapse.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Infers a spending category from a normalised transaction description.
 *
 * Rules are ordered — first match wins. More specific categories appear before
 * broader ones (e.g. GROCERIES before SHOPPING, SUBSCRIPTIONS before ENTERTAINMENT).
 *
 * Tuned for Indian credit-card and bank statements.
 */
@Service
@RequiredArgsConstructor
public class CategoryInferenceService {

    private final CategoryRepository categoryRepository;

    /**
     * Ordered list of (categoryName → keywords).
     * Keywords are tested with String.contains() on the uppercase description.
     */
    private static final List<Map.Entry<String, List<String>>> RULES = List.of(

            // -- Groceries (before Shopping — BigBasket / Blinkit must not fall into Shopping) --
            Map.entry("GROCERIES", List.of(
                    "BIGBASKET", "BIG BASKET", "BLINKIT", "ZEPTO", "DUNZO",
                    "DMART", "D MART", "MORE RETAIL", "RELIANCE FRESH",
                    "NATURE BASKET", "MILKBASKET", "GROFERS", "JIOMART GROCERY"
            )),

            // -- Food & Dining --
            Map.entry("FOOD_DINING", List.of(
                    "SWIGGY", "ZOMATO", "DOMINOS", "PIZZA HUT", "BURGER KING", "KFC",
                    "MCDONALDS", "SUBWAY", "STARBUCKS", "CHAAYOS", "CAFE COFFEE DAY",
                    "RESTAURANT", "RESTAURAN", "CAFE", "COFFEE", "BAKERY",
                    "CAKEBEE", "CAKE", "BAKE", "TIFFIN", "BIRYANI", "DHABA",
                    "FAASOS", "BOX8", "FRESHMENU", "REBEL FOODS", "OVEN STORY",
                    "HALDIRAMS", "UDUPI", "DARSHINI", "EATERY",
                    "HARIBHAVANAM"
            )),

            // -- Travel --
            Map.entry("TRAVEL", List.of(
                    "MAKEMYTRIP", "MAKE MY TRIP", "CLEARTRIP", "YATRA", "REDBUS",
                    "ABHIBUS", "OYO", "IRCTC", "INDIGO", "AIR INDIA", "SPICEJET",
                    "GOIBIBO", "IXIGO", "EASEMYTRIP", "GOAIR", "VISTARA", "AKASA",
                    "BOOKING.COM", "AGODA", "AIRBNB", "TREEBO", "FABHOTELS"
            )),

            // -- Transportation --
            Map.entry("TRANSPORTATION", List.of(
                    "OLA CABS", "OLA AUTO", "UBER", "RAPIDO", "NAMMA YATRI",
                    "BMTC", "METRO RAIL", "FASTAG", "NHAI", "TOLL",
                    "PETROL", "FUEL", "BPCL", "HPCL", "IOCL", "SHELL INDIA",
                    "PARKING"
            )),

            // -- Bills & Utilities --
            Map.entry("BILLS_UTILITIES", List.of(
                    "VODAFONE", "AIRTEL", "RELIANCE JIO", "JIO", "BSNL", "MTNL",
                    "VODAFONEIDEA", "VI MOBILE",
                    "TATAPLAY", "TATA SKY", "DISH TV", "SUN DTH", "D2H",
                    "BESCOM", "MSEDCL", "TANGEDCO", "CESC", "BSES", "TATA POWER",
                    "ADANI ELECTRICITY", "TORRENT POWER", "ELECTRICITY",
                    "MGL", "IGL", "MAHANAGAR GAS", "INDRAPRASTHA GAS",
                    "WATER BOARD", "BBPS", "BPPY", "BILLDESK", "BROADBAND",
                    "INTERNET BILL", "UTILITY"
            )),

            // -- Subscriptions (before Entertainment — Netflix/Spotify are recurring, not one-off) --
            Map.entry("SUBSCRIPTIONS", List.of(
                    "NETFLIX", "SPOTIFY", "HOTSTAR", "DISNEY PLUS",
                    "YOUTUBE PREMIUM", "AMAZON PRIME", "APPLE MUSIC",
                    "ADOBE", "MICROSOFT 365", "MICROSOFT OFFICE",
                    "GOOGLE ONE", "ICLOUD", "DROPBOX", "ZOOM PRO",
                    "LINKEDIN PREMIUM", "NOTION", "SLACK PRO"
            )),

            // -- Entertainment --
            Map.entry("ENTERTAINMENT", List.of(
                    "PVR", "INOX", "CINEPOLIS", "CARNIVAL CINEMA",
                    "BOOKMYSHOW", "INSIDER.IN", "PAYTM MOVIES",
                    "FUN REP", "FUN WORLD", "WONDERLA", "IMAGICA",
                    "THEME PARK", "AMUSEMENT", "GAME ZONE", "GAMING",
                    "ESCAPE ROOM", "BOWLING", "GO KARTING",
                    "GAANA", "JIO CINEMA", "SONYLIV", "ZEE5"
            )),

            // -- Healthcare --
            Map.entry("HEALTHCARE", List.of(
                    "APOLLO", "FORTIS", "MANIPAL HOSPITAL", "NARAYANA HEALTH",
                    "MEDPLUS", "PHARMEASY", "1MG", "TATA 1MG", "NETMEDS",
                    "HOSPITAL", "CLINIC", "PHARMACY", "MEDICAL STORE",
                    "PATHOLOGY", "DIAGNOSTIC", "SCAN CENTRE",
                    "DENTIST", "DENTAL", "EYE CARE", "OPTICIAN",
                    "CULT FIT", "ANYTIME FITNESS", "GOLD GYM"
            )),

            // -- Education --
            Map.entry("EDUCATION", List.of(
                    "BYJU", "UNACADEMY", "VEDANTU", "COURSERA", "UDEMY",
                    "UPGRAD", "SIMPLILEARN", "GREAT LEARNING",
                    "SCHOOL FEE", "COLLEGE FEE", "TUITION", "COACHING CLASS",
                    "EXAM FEE", "BOARD FEE"
            )),

            // -- Shopping (broad — kept last so specific categories win above) --
            Map.entry("SHOPPING", List.of(
                    "MYNTRA", "FLIPKART", "AMAZON", "MEESHO", "NYKAA", "AJIO",
                    "ZUDIO", "TRENT", "H&M", "LIFESTYLE", "SHOPPERS STOP",
                    "TATA CLIQ", "SNAPDEAL", "BEWAKOOF", "CLOVIA",
                    "PANTALOONS", "WESTSIDE", "MAX FASHION", "FABINDIA",
                    "PUMA", "NIKE", "ADIDAS", "REEBOK", "DECATHLON", "BATA",
                    "UNLIMITED", "CENTRAL MALL", "RELIANCE TRENDS",
                    "VERO MODA", "JACK JONES", "FOREVER 21",
                    "CROMA", "VIJAY SALES", "RELIANCE DIGITAL", "APPLE STORE"
            ))
    );

    /**
     * Returns the best-matching Category for the given normalised (uppercase, trimmed) description.
     * Returns null if no rule matches — the transaction will appear under "Other" in the UI.
     */
    public Category infer(String normalizedDescription) {
        if (normalizedDescription == null || normalizedDescription.isBlank()) return null;
        String desc = normalizedDescription.toUpperCase();

        for (Map.Entry<String, List<String>> rule : RULES) {
            for (String keyword : rule.getValue()) {
                if (desc.contains(keyword.toUpperCase())) {
                    return categoryRepository.findByName(rule.getKey()).orElse(null);
                }
            }
        }
        return null;
    }
}
