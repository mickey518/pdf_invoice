package lab.dragon.invoice.utils;

import org.junit.Test;


/**
 * @author mickey.wang
 */
public class AmountToChineseTest {

    @Test
    public void numberToChinese() {
        String string = AmountToChinese.numberToChinese("123456789.34");
        assert "壹亿贰仟叁佰肆拾伍万陆仟柒佰捌拾玖元叁角肆分".equals(string);
    }
}