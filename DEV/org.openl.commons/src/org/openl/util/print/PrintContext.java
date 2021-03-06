/**
 * Created Jan 28, 2007
 */
package org.openl.util.print;

/**
 * @author snshor
 * 
 */
public class PrintContext {

    private IFormat format;
    private OutputFilter filter;

    public PrintContext(IFormat format, OutputFilter filter) {
        this.format = format;
        this.filter = filter;
    }

    public StringBuilder print(Object obj, int mode, StringBuilder out) {
        StringBuilder tmpout = filter == null ? out : new StringBuilder(10);

        if (format != null) {
            format.format(obj, mode, tmpout);
        } else {
            tmpout.append(obj);
        }

        if (filter != null) {
            filter.transform(tmpout, out);
        }

        return out;
    }

}
