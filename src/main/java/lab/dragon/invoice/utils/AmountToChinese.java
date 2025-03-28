package lab.dragon.invoice.utils;

public class AmountToChinese {
    private static final String[] NUMBERS = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};
    private static final String[] UNITS = {"仟", "佰", "拾"};
    private static final String YUAN = "元";

    public static String numberToChinese(String amount) {
        if (amount == null || amount.isEmpty()) {
            return "";
        }

        // 分割整数部分和小数部分
        String[] parts = amount.split("\\.");
        String integerPart = parts[0];
        String decimalPart = parts.length > 1 ? parts[1] : "";
        // 判断小数部分是不是都是 0
        if (isAllZeros(decimalPart)) {
            decimalPart = "";
        }

        // 处理整数部分
        StringBuilder chineseInteger = new StringBuilder();
        int length = integerPart.length();
        for (int i = 0; i < length; i += 4) {
            String segment = integerPart.substring(Math.max(0, length - i - 4), length - i);
            String convertedSegment = convertSegment(segment);
            if (!convertedSegment.isEmpty()) {
                chineseInteger.insert(0, convertedSegment + getUnit(i / 4));
            }
        }

        if ("".equals(decimalPart)) {
            chineseInteger.append(YUAN + "整");
        } else {
            chineseInteger.append(YUAN);
        }
        // 处理小数部分
        String chineseDecimal = convertDecimal(decimalPart);

        return chineseInteger + chineseDecimal;
    }

    private static String convertSegment(String segment) {
        StringBuilder result = new StringBuilder();
        segment = String.format("%04d", Integer.valueOf(segment));
        for (int i = segment.length() - 1; i >= 0; i--) {
            char c = segment.charAt(i);
            if (i == 0) {
                // 处理千位
                if (c != '0') {
                    result.insert(0, NUMBERS[c - '0'] + UNITS[0]);
                }
            } else if (i == 1) {
                // 处理百位
                if (c != '0') {
                    result.insert(0, NUMBERS[c - '0'] + UNITS[1]);
                }
            } else if (i == 2) {
                // 处理十位
                if (c != '0') {
                    result.insert(0, NUMBERS[c - '0'] + UNITS[2]);
                }
            } else {
                // 处理个位
                if (c != '0') {
                    result.append(NUMBERS[c - '0']);
                }
            }
        }
        return result.toString();
    }

    private static String getUnit(int index) {
        switch (index) {
            case 1:
                return "万";
            case 2:
                return "亿";
            default:
                return "";
        }
    }

    private static String convertDecimal(String decimal) {
        if (decimal.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < decimal.length(); i++) {
            char c = decimal.charAt(i);
            if (i == 0) {
                // 处理角位
                if (c != '0') {
                    result.append(NUMBERS[c - '0']).append("角");
                }
            } else {
                // 处理分位
                if (c != '0') {
                    result.append(NUMBERS[c - '0']).append("分");
                }
            }
        }
        return result.toString();
    }

    public static boolean isAllZeros(String decimalPart) {
        if (decimalPart == null || decimalPart.isEmpty()) {
            return false; // 根据需求定义空字符串的处理
        }
        return decimalPart.matches("0+"); // 匹配一个或多个 0
    }
}
