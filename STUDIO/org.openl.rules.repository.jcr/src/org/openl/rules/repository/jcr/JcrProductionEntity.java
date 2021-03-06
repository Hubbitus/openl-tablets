package org.openl.rules.repository.jcr;

import org.openl.rules.common.CommonUser;
import org.openl.rules.common.Property;
import org.openl.rules.common.ValueType;
import org.openl.rules.repository.REntity;
import org.openl.rules.repository.RLock;
import org.openl.rules.repository.RVersion;
import org.openl.rules.repository.api.ArtefactProperties;
import org.openl.rules.repository.exceptions.RRepositoryException;
import static org.openl.rules.repository.jcr.NodeUtil.isSame;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public abstract class JcrProductionEntity implements REntity {
    private String name;
    private Node node;
    private Date effectiveDate;
    private Date expirationDate;
    private String lineOfBusiness;

    private Map<String, Object> props;

    public JcrProductionEntity(Node node) throws RepositoryException {
        this.node = node;

        name = node.getName();

        if (node.hasProperty(ArtefactProperties.PROP_EFFECTIVE_DATE)) {
            effectiveDate = new Date(node.getProperty(ArtefactProperties.PROP_EFFECTIVE_DATE).getLong());
        }
        if (node.hasProperty(ArtefactProperties.PROP_EXPIRATION_DATE)) {
            expirationDate = new Date(node.getProperty(ArtefactProperties.PROP_EXPIRATION_DATE).getLong());
        }
        if (node.hasProperty(ArtefactProperties.PROP_LINE_OF_BUSINESS)) {
            lineOfBusiness = node.getProperty(ArtefactProperties.PROP_LINE_OF_BUSINESS).getString();
        }

        props = new HashMap<String, Object>();
        loadProps();
    }

    public void addProperty(String name, ValueType type, Object value) throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    private void buildPath(Node node, StringBuilder sb) throws RepositoryException {
        if (!node.isNodeType(JcrNT.NT_DEPLOYMENT)) {
            buildPath(node.getParent(), sb);
        }

        sb.append("/").append(node.getName());
    }

    /**
     * Deletes entity. Also can delete other entities. For example, deleting a
     * folder will lead to deleting all its sub entities.
     *
     * @throws RRepositoryException if failed
     */
    public void delete() throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets active version of the entity.
     *
     * @return active version
     */
    public RVersion getActiveVersion() {
        return null;
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public String getLineOfBusiness() {
        return lineOfBusiness;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns path of entity.
     *
     * @return path of entity
     * @throws org.openl.rules.repository.exceptions.RRepositoryException if
     *             failed
     */
    public String getPath() throws RRepositoryException {
        StringBuilder sb = new StringBuilder(32);
        try {
            buildPath(node(), sb);
        } catch (RepositoryException e) {
            throw new RRepositoryException("error building path", e);
        }

        return sb.toString();
    }

    public Collection<Property> getProperties() {
        return null;
    }

    public Property getProperty(String name) throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> getProps() {
        return props;
    }

    /**
     * Gets version history of the entity.
     *
     * @return list of versions
     */
    public List<RVersion> getVersionHistory() throws RRepositoryException {
        return null;
    }

    public boolean hasProperty(String name) {
        throw new UnsupportedOperationException();
    }

    private void loadProps() throws RepositoryException {
        Node n = node();
        for (int i = 1; i <= ArtefactProperties.PROPS_COUNT; i++) {
            String propName = ArtefactProperties.PROP_ATTRIBUTE + i;
            if (n.hasProperty(propName)) {
                Value value = n.getProperty(propName).getValue();
                Object propValue;
                int valueType = value.getType();
                switch (valueType) {
                    case PropertyType.DATE:
                        propValue = value.getDate().getTime();
                        break;
                    case PropertyType.DOUBLE:
                        propValue = value.getDouble();
                        break;
                    default:
                        propValue = value.getString();
                        break;
                }
                props.put(propName, propValue);
            }
        }
    }

    protected final Node node() {
        return node;
    }

    public void removeProperty(String name) throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    public void setEffectiveDate(Date date) throws RRepositoryException {
        // do not update JCR if property wasn't changed
        if (isSame(effectiveDate, date)) {
            return;
        }

        Node n = node();

        try {
            n.setProperty(ArtefactProperties.PROP_EFFECTIVE_DATE, date.getTime());
            effectiveDate = date;
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot set effectiveDate", e);
        }
    }

    public void setExpirationDate(Date date) throws RRepositoryException {
        // do not update JCR if property wasn't changed
        if (isSame(expirationDate, date)) {
            return;
        }

        Node n = node();

        try {
            n.setProperty(ArtefactProperties.PROP_EXPIRATION_DATE, date.getTime());
            expirationDate = date;
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot set expirationDate", e);
        }
    }

    public void setLineOfBusiness(String lineOfBusiness) throws RRepositoryException {
        // do not update JCR if property wasn't changed
        if (isSame(this.lineOfBusiness, lineOfBusiness)) {
            return;
        }

        Node n = node();

        try {
            n.setProperty(ArtefactProperties.PROP_LINE_OF_BUSINESS, lineOfBusiness);
            this.lineOfBusiness = lineOfBusiness;
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot set LOB", e);
        }
    }

    /**
     * Sets the property in node.
     *
     * @param propName property name
     * @param propValue property value
     * @return true if the property is set successfully
     */
    private boolean setProperty(String propName, Object propValue) throws RRepositoryException {
        Node n = node();
        try {
            NodeUtil.smartCheckout(n, false);
            if (propValue instanceof Date) {
                n.setProperty(propName, ((Date) propValue).getTime());
            } else if (propValue instanceof Double) {
                n.setProperty(propName, (Double) propValue);
            } else {
                n.setProperty(propName, propValue.toString());
            }
            return true;
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot set property " + propName + ".", e);
        }
    }

    public void setProps(Map<String, Object> props) throws RRepositoryException {
        if (props == null) {
            return;
        }
        // do not update JCR if property wasn't changed
        if (isSame(this.props, props)) {
            return;
        }
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            setProperty(entry.getKey(), entry.getValue());
        }
        this.props = props;
    }

    public RLock getLock() throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    public boolean isLocked() throws RRepositoryException {
        return false;
    }

    public void lock(CommonUser user) throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    public void unlock(CommonUser user) throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    public void riseVersion(int major, int minor) throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    public void commit(CommonUser user) throws RRepositoryException {
        throw new UnsupportedOperationException();
    }
}
