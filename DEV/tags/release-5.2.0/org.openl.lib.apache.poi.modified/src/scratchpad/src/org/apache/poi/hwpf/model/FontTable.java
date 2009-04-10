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

package org.apache.poi.hwpf.model;

import java.io.IOException;
import org.apache.poi.hwpf.model.io.HWPFFileSystem;
import org.apache.poi.hwpf.model.io.HWPFOutputStream;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.LittleEndianConsts;

/**
 * FontTable or in MS terminology sttbfffn is a common data structure written in
 * all Word files. The sttbfffn is an sttbf where each string is an FFN
 * structure instead of pascal-style strings. An sttbf is a string Table stored
 * in file. Thus sttbffn is like an Sttbf with an array of FFN structures that
 * stores the font name strings
 *
 * @author Praveen Mathew
 */
public class FontTable {
    private short _stringCount;// how many strings are included in the string
                                // table
    private short _extraDataSz;// size in bytes of the extra data

    // added extra facilitator members
    private int lcbSttbfffn;// count of bytes in sttbfffn
    private int fcSttbfffn;// table stream offset for sttbfffn

    // FFN structure containing strings of font names
    private Ffn[] _fontNames = null;

    public FontTable(byte[] buf, int offset, int lcbSttbfffn) {
        this.lcbSttbfffn = lcbSttbfffn;
        fcSttbfffn = offset;

        _stringCount = LittleEndian.getShort(buf, offset);
        offset += LittleEndianConsts.SHORT_SIZE;
        _extraDataSz = LittleEndian.getShort(buf, offset);
        offset += LittleEndianConsts.SHORT_SIZE;

        _fontNames = new Ffn[_stringCount]; // Ffn corresponds to a Pascal style
                                            // String in STTBF.

        for (int i = 0; i < _stringCount; i++) {
            _fontNames[i] = new Ffn(buf, offset);
            offset += _fontNames[i].getSize();
        }
    }

    @Override
    public boolean equals(Object o) {
        boolean retVal = true;

        if (((FontTable) o).getStringCount() == _stringCount) {
            if (((FontTable) o).getExtraDataSz() == _extraDataSz) {
                Ffn[] fontNamesNew = ((FontTable) o).getFontNames();
                for (int i = 0; i < _stringCount; i++) {
                    if (!(_fontNames[i].equals(fontNamesNew[i]))) {
                        retVal = false;
                    }
                }
            } else {
                retVal = false;
            }
        } else {
            retVal = false;
        }

        return retVal;
    }

    public String getAltFont(int chpFtc) {
        if (chpFtc >= _stringCount) {
            System.out.println("Mismatch in chpFtc with stringCount");
            return null;
        }

        return _fontNames[chpFtc].getAltFontName();
    }

    public short getExtraDataSz() {
        return _extraDataSz;
    }

    public Ffn[] getFontNames() {
        return _fontNames;
    }

    public String getMainFont(int chpFtc) {
        if (chpFtc >= _stringCount) {
            System.out.println("Mismatch in chpFtc with stringCount");
            return null;
        }

        return _fontNames[chpFtc].getMainFontName();
    }

    public int getSize() {
        return lcbSttbfffn;
    }

    public short getStringCount() {
        return _stringCount;
    }

    public void setStringCount(short stringCount) {
        _stringCount = stringCount;
    }

    public void writeTo(HWPFFileSystem sys) throws IOException {
        HWPFOutputStream tableStream = sys.getStream("1Table");

        byte[] buf = new byte[LittleEndianConsts.SHORT_SIZE];
        LittleEndian.putShort(buf, _stringCount);
        tableStream.write(buf);
        LittleEndian.putShort(buf, _extraDataSz);
        tableStream.write(buf);

        for (int i = 0; i < _fontNames.length; i++) {
            tableStream.write(_fontNames[i].toByteArray());
        }

    }

}
