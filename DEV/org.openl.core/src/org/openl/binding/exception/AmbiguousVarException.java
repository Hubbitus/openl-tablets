/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.exception;

import java.util.Iterator;
import java.util.List;

import org.openl.base.INamedThing;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.types.IOpenField;

/**
 * @author snshor
 *
 */
public class AmbiguousVarException extends OpenlNotCheckedException {
    /**
     *
     */
    private static final long serialVersionUID = -8752617383143899614L;
    
    private List<IOpenField> matchingFields;
    private String varName;

    public AmbiguousVarException(String varName, List<IOpenField> matchingFields) {
        this.varName = varName;
        this.matchingFields = matchingFields;
    }

    @Override
    public String getMessage() {
        StringBuilder buf = new StringBuilder();

        buf.append("Variable ").append(varName);
        buf.append(" is ambiguous:\n").append("Matching fieldValues:\n");
        boolean first = true;
        for (Iterator<IOpenField> iter = matchingFields.iterator(); iter.hasNext();) {
            IOpenField f = iter.next();
            if (!first) {
                buf.append(", ");
            }
            buf.append(f.getDisplayName(INamedThing.LONG));
            first = false;
        }

        return buf.toString();
    }

}
