package lab.dragon.invoice.utils;

public class AmountToChinese {
    private static final String[] NUMBERS = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};
    private static final String[] UNITS = {"仟", "佰", "拾"};
    
    public static String numberToChinese(String amount) {
        if (amount == null || amount.isEmpty()) {
            return "";
        }
        
        // 分割整数部分和小数部分
        String[] parts = amount.split("\\.");
        String integerPart = parts[0];
        String decimalPart = parts.length > 1 ? parts[1] : "";
        
        // 处理整数部分
        StringBuilder chineseInteger = new StringBuilder();
        int length = integerPart.length();
        for (int i = 0; i < length; i += 4) {
            String segment = integerPart.substring(Math.max(0, length - i - 4), length - i);
            String convertedSegment = convertSegment(segment);
            if (!convertedSegment.isEmpty()) {
                chineseInteger.append(convertedSegment).append(getUnit(i / 4));
            }
        }

        if ("".equals(decimalPart)) {
            chineseInteger.append("元整");
        } else {
            chineseInteger.append("元");
        }
        // 处理小数部分
        String chineseDecimal = convertDecimal(decimalPart);
        
        return chineseInteger + chineseDecimal;
    }
    
    private static String convertSegment(String segment) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < segment.length(); i++) {
            char c = segment.charAt(i);
            if (i == 0) {
                // 处理千位
                if (c != '0') {
                    result.append(NUMBERS[c - '0']).append(UNITS[2]);
                }
            } else if (i == 1) {
                // 处理百位
                if (c != '0') {
                    result.append(NUMBERS[c - '0']).append(UNITS[1]);
                }
            } else if (i == 2) {
                // 处理十位
                if (c != '0') {
                    result.append(NUMBERS[c - '0']).append(UNITS[0]);
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
    
    public static void main(String[] args) {
        System.out.println(numberToChinese("123456789")); // 壹亿贰仟叁佰肆拾伍元
        System.out.println(numberToChinese("1000")); // 壹仟
        System.out.println(numberToChinese("123.45")); // 壹佰贰拾叁元捌角伍分
    }
}
