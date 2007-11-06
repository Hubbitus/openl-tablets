package org.openl.rules.workspace.dtr;

import org.openl.rules.workspace.abstracts.ProjectException;

public class RepositoryException extends ProjectException {
    private static final long serialVersionUID = -7556228015092226646L;

    public RepositoryException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public RepositoryException(String pattern, Throwable cause, Object... params) {
        super(pattern, cause, params);
    }
}
