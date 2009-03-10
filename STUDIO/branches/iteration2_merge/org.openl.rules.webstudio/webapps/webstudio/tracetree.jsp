<%@ page import = "org.openl.rules.ui.*" %>
<%@page import="org.openl.rules.webstudio.web.util.Constants"%>


<html>
<head>
<title>Trace Tree</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<link rel="stylesheet" type="text/css" href="webresource/css/openl/dtree.css"></link>
<link rel="stylesheet" type="text/css" href="webresource/css/openl/style1.css"></link>
<script language="JavaScript" type="text/JavaScript" src="webresource/dtree.js"></script>


<style type="text/css">

BODY {
	padding: 0;
	margin: 0 0 0 10px;
	background: #eceef8;
	}


#tree {
	text-overflow: ellipsis;
	overflow : hidden
	width: 100%;
	height: 100%;
	}

</style>

</head>
<body>

<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />
<jsp:useBean id="tracer" scope="session" class="org.openl.rules.ui.TraceHelper"/>

<%
	String uri = request.getParameter(Constants.REQUEST_PARAM_URI);

   	if (uri != null && !uri.equals("")) {
     	studio.setTableUri(uri);
	   	String url = studio.getModel().makeXlsUrl(uri);
	   	String text = org.openl.rules.webtools.indexer.FileIndexer.showElementHeader(uri);
//	   	String name = studio.getModel().getDisplayNameFull(uri);
//	   	tracer.setName(name);
	   	org.openl.vm.Tracer t =  studio.getModel().traceElement(uri);
	   	tracer.setRoot(t.getRoot());
    } else {
%>
	<h1>elementUri not found</h1>
<%
    }
%>

<br/>

<p/><p/>

<table width=95% style="border-style: solid; border-width: 1;">
<tr>
<td>
<a href="javascript: top.close()" title="Close Window"><img border=0 src="webresource/images/close.gif"></a>
</td>

<td align=right >
<a href="javascript: d.openAll(); d.o(0);" title="Expand All"><img border="0" src="webresource/images/expandall.gif"/></a>

<a href="javascript: d.closeAll()" title="Collapse All" > <img border="0" src="webresource/images/collapseall.gif"/></a>

</td>
</tr></table>

<p/><p/>



<div class="errmsg" id="msg">
</div>



<div class="dtree" id="tree">
</div>





<script language="JavaScript" defer="defer">
d = new dTree('d');

<%=tracer.renderTraceTree("jsp/showTraceTable.jsp", "mainFrame")%>


document.all['tree'].innerHTML = d;
</script>




</body>
</html>