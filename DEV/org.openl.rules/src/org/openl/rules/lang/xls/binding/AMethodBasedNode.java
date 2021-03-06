package org.openl.rules.lang.xls.binding;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.openl.OpenL;
import org.openl.base.INamedThing;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.exception.DuplicatedMethodException;
import org.openl.binding.impl.BindHelper;
import org.openl.binding.impl.NodeType;
import org.openl.binding.impl.NodeUsage;
import org.openl.binding.impl.SimpleNodeUsage;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.exception.OpenLRuntimeException;
import org.openl.message.OpenLMessagesUtils;
import org.openl.meta.IMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.ICell;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.SubTextSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.MethodDelegator;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;
import org.openl.vm.IRuntimeEnv;

public abstract class AMethodBasedNode extends ATableBoundNode implements IMemberBoundNode {

    private OpenL openl;
    private IOpenMethodHeader header;
    private ExecutableRulesMethod method;
    private ModuleOpenClass module;

    public AMethodBasedNode(TableSyntaxNode methodNode, OpenL openl, IOpenMethodHeader header, ModuleOpenClass module) {
        super(methodNode, new IBoundNode[0]);
        this.header = header;
        this.openl = openl;
        this.module = module;
    }

    public OpenL getOpenl() {
        return openl;
    }

    public IOpenMethodHeader getHeader() {
        return header;
    }

    public ExecutableRulesMethod getMethod() {
        return method;
    }

    public ModuleOpenClass getModule() {
        return module;
    }

    @Override
    public boolean isLiteralExpressionParent() {
        return false;
    }

    public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {
        throw new UnsupportedOperationException("Should not be called");
    }

    public IOpenClass getType() {
        return header.getType();
    }

    public void addTo(ModuleOpenClass openClass) {
        method = createMethodShell();
        openClass.addMethod(method);
        getTableSyntaxNode().setMember(method);
        if(hasServiceName()){
            addServiceMethod(openClass, method);
        }
    }

    /**
     * Is method has an "id" property that will be used to generate additional
     * method with name specified in property sutable for direct call of rule
     * avoiding the method dispatching mechanism.
     *
     * @return <code>true</code> if "id" property is specified.
     */
    protected boolean hasServiceName(){
        return StringUtils.isNotBlank(getTableSyntaxNode().getTableProperties().getId());
    }

    protected IOpenMethod getServiceMethod(IOpenMethod originalMethod){
        final String serviceMethodName = getTableSyntaxNode().getTableProperties().getId();
        return new MethodDelegator(originalMethod) {
            @Override
            public String getName() {
                return serviceMethodName;
            }

            @Override
            public String getDisplayName(int mode) {
                return serviceMethodName;
            }
        };
    }

    /**
     * Add auxiliary method with name specified in property "id" for direct call
     * for this rule.
     *
     * @param openClass Module open class
     * @param originalMethod original method
     */
    protected void addServiceMethod(ModuleOpenClass openClass, IOpenMethod originalMethod){
        try{
            openClass.addMethod(getServiceMethod(originalMethod));
        }catch (DuplicatedMethodException e) {
            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(null, e, getTableSyntaxNode());
            getTableSyntaxNode().addError(error);
            OpenLMessagesUtils.addError(error);
        }

    }

    protected abstract ExecutableRulesMethod createMethodShell();

    public void removeDebugInformation(IBindingContext cxt) throws Exception {
        if (cxt.isExecutionMode()) {
            getMethod().setBoundNode(null);
            getMethod().getMethodProperties().setModulePropertiesTable(null);
            getMethod().getMethodProperties().setCategoryPropertiesTable(null);
            getMethod().getMethodProperties().setPropertiesSection(null);
        }
    }

    @Override
    public void finalizeBind(IBindingContext bindingContext) throws Exception {
        if (!bindingContext.isExecutionMode() && header instanceof OpenMethodHeader) {
            OpenMethodHeader tableHeader = (OpenMethodHeader) header;

            List<NodeUsage> nodeUsages = new ArrayList<NodeUsage>();
            ICell cell = getTableSyntaxNode().getGridTable().getCell(0, 0);
            TextInfo tableHeaderText = new TextInfo(cell.getStringValue());

            int startPosition = getSignatureStartIndex();
            // Link to return type
            IOpenClass type = tableHeader.getType();
            IMetaInfo metaInfo = type.getMetaInfo();
            while (metaInfo == null && type.isArray()) {
                type = type.getComponentClass();
                metaInfo = type.getMetaInfo();
            }

            IOpenSourceCodeModule src = new GridCellSourceCodeModule(getTableSyntaxNode().getGridTable(), bindingContext);
            SubTextSourceCodeModule headerSyntaxNode = new SubTextSourceCodeModule(src, startPosition, src.getCode().length());

            ILocation typeLocation = tableHeader.getTypeLocation();
            if (metaInfo != null && typeLocation != null) {
                int start = startPosition + typeLocation.getStart().getAbsolutePosition(tableHeaderText);
                int end = startPosition + typeLocation.getEnd().getAbsolutePosition(tableHeaderText);
                nodeUsages.add(new SimpleNodeUsage(start, end, metaInfo.getDisplayName(INamedThing.SHORT), metaInfo.getSourceUrl(),
                        NodeType.DATATYPE));
                if (type.getInstanceClass() == null) {
                    addTypeError(type, typeLocation, headerSyntaxNode);
                }
            }

            // Link to input parameters
            ILocation[] paramTypeLocations = tableHeader.getParamTypeLocations();
            if (paramTypeLocations != null) {
                for (int i = 0; i < header.getSignature().getNumberOfParameters(); i++) {
                    IOpenClass parameterType = header.getSignature().getParameterType(i);
                    metaInfo = parameterType.getMetaInfo();
                    while (metaInfo == null && parameterType.isArray()) {
                        parameterType = parameterType.getComponentClass();
                        metaInfo = parameterType.getMetaInfo();
                    }

                    if (metaInfo != null) {
                        ILocation sourceLocation = paramTypeLocations[i];
                        int start = startPosition + sourceLocation.getStart().getAbsolutePosition(tableHeaderText);
                        int end = startPosition + sourceLocation.getEnd().getAbsolutePosition(tableHeaderText);
                        nodeUsages.add(new SimpleNodeUsage(start, end, metaInfo.getDisplayName(INamedThing.SHORT), metaInfo.getSourceUrl(),
                                NodeType.DATATYPE));

                        if (parameterType.getInstanceClass() == null) {
                            addTypeError(parameterType, sourceLocation, headerSyntaxNode);
                        }
                    }
                }
            }

            if (!CollectionUtils.isEmpty(nodeUsages)) {
                cell.setMetaInfo(new CellMetaInfo(CellMetaInfo.Type.DT_CA_CODE, null, JavaOpenClass.STRING, false, nodeUsages));
            }
        }
    }

    protected void addTypeError(IOpenClass type, ILocation location, IOpenSourceCodeModule syntaxNode) {
        String message = String.format("Type '%s' was defined with errors", type.getName());
        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, null, location, syntaxNode);
        getTableSyntaxNode().addError(error);
        BindHelper.processError(error);
    }

    protected int getSignatureStartIndex() {
        ICell cell = getTableSyntaxNode().getGridTable().getCell(0, 0);
        TextInfo tableHeaderText = new TextInfo(cell.getStringValue());
        return getTableSyntaxNode().getHeader().getHeaderToken().getLocation().getEnd().getAbsolutePosition(tableHeaderText);
    }
}
