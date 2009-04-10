/* ====================================================================
 Copyright 2002-2004   Apache Software Foundation

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 ==================================================================== */

package org.apache.poi.hwpf.usermodel;

import org.apache.poi.util.BitField;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.LittleEndianConsts;

public class BorderCode implements Cloneable {
    public static final int SIZE = 4;
    private final static BitField _dptLineWidth = new BitField(0xff);
    private final static BitField _brcType = new BitField(0xff00);
    private final static BitField _ico = new BitField(0xff);
    private final static BitField _dptDpace = new BitField(0x1f00);
    private final static BitField _fShadow = new BitField(0x2000);
    private final static BitField _fFrame = new BitField(0x4000);
    private short _info;
    private short _info2;

    public BorderCode() {
    }

    public BorderCode(byte[] buf, int offset) {
        _info = LittleEndian.getShort(buf, offset);
        _info2 = LittleEndian.getShort(buf, offset + LittleEndianConsts.SHORT_SIZE);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public boolean equals(Object o) {
        BorderCode brc = (BorderCode) o;
        return _info == brc._info && _info2 == brc._info2;
    }

    public boolean isEmpty() {
        return _info == 0 && _info2 == 0;
    }

    public void serialize(byte[] buf, int offset) {
        LittleEndian.putShort(buf, offset, _info);
        LittleEndian.putShort(buf, offset + LittleEndianConsts.SHORT_SIZE, _info2);
    }

    public int toInt() {
        byte[] buf = new byte[4];
        serialize(buf, 0);
        return LittleEndian.getInt(buf);
    }
}
