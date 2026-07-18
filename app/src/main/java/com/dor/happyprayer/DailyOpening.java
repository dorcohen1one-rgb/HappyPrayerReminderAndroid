package com.dor.happyprayer;

import java.util.Calendar;

/** A deterministic daily opening so the message changes once per calendar day. */
final class DailyOpening {
    private static final String[] OPENINGS = {
            "הלב נפתח כשאנחנו מאחלים טוב, גם בלי להכיר את האדם שמולנו.",
            "היום אפשר לעצור לרגע, לנשום, ולתת לטוב מקום.",
            "מילה טובה בלב היא התחלה קטנה של עולם רך יותר.",
            "נזכור: גם רגע שקט אחד יכול לשנות את המשך היום.",
            "הלוואי שנראה אור בעצמנו ונעביר אותו הלאה.",
            "נשימה אחת עמוקה. כוונה אחת טובה. זה מספיק להתחיל.",
            "הטוב שאנחנו מבקשים לאחרים חוזר ומרחיב גם את הלב שלנו.",
            "אפשר לבחור בעדינות, גם ביום עמוס.",
            "שקט אינו ריק. הוא מקום שבו אפשר להיזכר במה שחשוב.",
            "כל בוקר הוא הזדמנות חדשה לאחל שמחה לכל מי שנפגוש."
    };

    private DailyOpening() { }

    static String today() {
        Calendar calendar = Calendar.getInstance();
        int index = Math.floorMod(
                calendar.get(Calendar.YEAR) * 367 + calendar.get(Calendar.DAY_OF_YEAR),
                OPENINGS.length
        );
        return OPENINGS[index];
    }
}
