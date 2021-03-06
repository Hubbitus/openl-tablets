package org.openl.rules.convertor;

import java.text.DecimalFormat;

class String2ByteConvertor extends String2NumberConverter<Byte> {

    @Override
    Byte convert(Number number, String data) {
        double dValue = number.doubleValue();
        if (dValue > Byte.MAX_VALUE || dValue < Byte.MIN_VALUE) {
            throw new NumberFormatException("A number is out of range [-128...+127]");
        }
        return number.byteValue();
    }

    @Override
    DecimalFormat getFormatter(String format) {
        DecimalFormat formatter = super.getFormatter(format);
        formatter.setParseIntegerOnly(true);
        return formatter;
    }
}
