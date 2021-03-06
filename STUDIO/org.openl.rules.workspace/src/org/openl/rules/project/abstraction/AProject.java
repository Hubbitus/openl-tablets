package org.openl.rules.project.abstraction;

import org.openl.rules.common.*;
import org.openl.rules.common.impl.PropertyImpl;
import org.openl.rules.repository.api.ArtefactProperties;
import org.openl.rules.repository.api.FolderAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.UserTransaction;

public class AProject extends AProjectFolder {
    private final Logger log = LoggerFactory.getLogger(AProject.class);

    public AProject(FolderAPI api) {
        super(api, null);
    }

    @Override
    public AProject getProject() {
        return this;
    }

    @Override
    public void delete() throws ProjectException {
        throw new ProjectException("Unsupported operation.");
    }

    public void delete(CommonUser user) throws ProjectException {
        if (isLocked() && !isLockedByUser(user)) {
            throw new ProjectException("Cannot delete project ''{0}'' while it is locked by other user!", null,
                    getName());
        }

        if (isDeleted()) {
            throw new ProjectException("Project ''{0}'' is already marked for deletion!", null, getName());
        }

        try {
            addProperty(new PropertyImpl(ArtefactProperties.PROP_PRJ_MARKED_4_DELETION, ValueType.BOOLEAN, true));
        } catch (PropertyException e) {
            throw new ProjectException("Failed to mark project as deleted.", e);
        }
    }

    public void save(CommonUser user) throws ProjectException {
        commit(user);
        unlock(user);
        refresh();
    }

    public void edit(CommonUser user) throws ProjectException {
        lock(user);
    }

    public void close(CommonUser user) throws ProjectException {
        if (isLockedByUser(user)) {
            unlock(user);
        }
        refresh();
    }

    public void erase(CommonUser user) throws ProjectException {
        getAPI().delete(user);
    }

    public boolean isDeleted() {
        return getAPI().hasProperty(ArtefactProperties.PROP_PRJ_MARKED_4_DELETION);
    }

    public void undelete() throws ProjectException {
        if (!isDeleted()) {
            throw new ProjectException("Cannot undelete non-marked project ''{0}''!", null, getName());
        }

        try {
            removeProperty(ArtefactProperties.PROP_PRJ_MARKED_4_DELETION);
        } catch (PropertyException e) {
            throw new ProjectException("Failed to undelete project.", e);
        }
    }

    protected UserTransaction beginTransaction() throws ProjectException {
        UserTransaction transaction = getAPI().createTransaction();
        try {
            transaction.begin();
        } catch (Exception e) {
            throw new ProjectException("Could not start tranaction.", e);
        }
        return transaction;
    }

    protected void rollbackTransaction(UserTransaction transaction) {
        try {
            transaction.rollback();
        } catch (Exception e1) {
            log.error("Could not roll back changes.", e1);
        }
    }

    protected void finalizeTransaction(UserTransaction transaction) throws ProjectException {
        try {
            transaction.commit();
        } catch (Exception e) {
            rollbackTransaction(transaction);
            throw new ProjectException("Could not commit tranaction.", e);
        }
    }

    @Override
    public void update(AProjectArtefact artefact, CommonUser user) throws ProjectException {
        UserTransaction transaction = beginTransaction();
        try {
            super.update(artefact, user);
        } catch (Exception e) {
            rollbackTransaction(transaction);
            throw new ProjectException("Failed to save project: " + e.getMessage(), e);
        }
        finalizeTransaction(transaction);
    }

    @Override
    public void update(AProjectArtefact artefact, CommonUser user, int revision) throws ProjectException {
        UserTransaction transaction = beginTransaction();
        try {
            super.update(artefact, user, revision);
        } catch (Exception e) {
            rollbackTransaction(transaction);
            throw new ProjectException("Failed to save project: " + e.getMessage(), e);
        }
        finalizeTransaction(transaction);
    }

    @Override
    public void smartUpdate(AProjectArtefact artefact, CommonUser user) throws ProjectException {
        if (artefact.isModified()) {
            UserTransaction transaction = beginTransaction();
            try {
                super.smartUpdate(artefact, user);
            } catch (Exception e) {
                rollbackTransaction(transaction);
                throw new ProjectException("Failed to save project: " + e.getMessage(), e);
            }
            finalizeTransaction(transaction);
        }
    }

    public AProject getProjectVersion(CommonVersion version) throws ProjectException {
        return new AProject(getAPI().getVersion(version));
    }

    public boolean getOpenedForEditing() {
        return false;
    }
}
