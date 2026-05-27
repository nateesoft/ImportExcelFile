package com.ics.utils.importexcelfile;

public class ThaiUtil {

    public static String Unicode2ASCII(String unicode) {
        if (unicode == null) {
            return "";
        }
        // Strip invisible/zero-width chars (e.g. U+2063) that latin1/TIS620 columns cannot store
        String cleaned = unicode.replaceAll("[\\u00AD\\u034F\\u200B-\\u200F\\u2060-\\u206F\\uFEFF]", "").trim();
        StringBuilder ascii = new StringBuilder(cleaned);
        for (int i = 0; i < cleaned.length(); i++) {
            int code = (int) cleaned.charAt(i);
            if ((0xE01 <= code) && (code <= 0xE5B)) {
                ascii.setCharAt(i, (char) (code - 0xD60));
            }
        }
        return ascii.toString();
    }

    public static String ASCII2Unicode(String ascii) {
        if (ascii == null) {
            return "";
        }
        StringBuilder unicode = new StringBuilder(ascii);
        int code;
        for (int i = 0; i < ascii.length(); i++) {
            code = (int) ascii.charAt(i);
            if ((0xA1 <= code) && (code <= 0xFB)) // ตรวจสอบว่าอยู่ในช่วงภาษาไทยของ ASCII หรือไม่
            {
                unicode.setCharAt(i, (char) (code + 0xD60)); // หากใช้แปลงเป็นภาษาไทยในช่วงของ Unicode
            }
        }
        return unicode.toString(); // แปลงข้อมูลกลับไปเป็นแบบ String เพื่อใช้งานต่อไป
    }
}
