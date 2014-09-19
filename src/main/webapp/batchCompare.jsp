<%--
  ~ Wildbook - A Mark-Recapture Framework
  ~ Copyright (C) 2008-2014 Jason Holmberg
  ~
  ~ This program is free software; you can redistribute it and/or
  ~ modify it under the terms of the GNU General Public License
  ~ as published by the Free Software Foundation; either version 2
  ~ of the License, or (at your option) any later version.
  ~
  ~ This program is distributed in the hope that it will be useful,
  ~ but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~ GNU General Public License for more details.
  ~
  ~ You should have received a copy of the GNU General Public License
  ~ along with this program; if not, write to the Free Software
  ~ Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
  --%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page contentType="text/html; charset=utf-8" pageEncoding="UTF-8" language="java"
         import="org.ecocean.servlet.ServletUtilities,java.util.ArrayList,org.ecocean.*, org.ecocean.Util, java.util.GregorianCalendar, java.util.Properties, java.util.List, org.ecocean.BatchCompareProcessor, javax.servlet.http.HttpSession, java.io.File, java.nio.file.Files, java.nio.file.Paths" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>         
<%

boolean isIE = request.getHeader("user-agent").contains("MSIE ");
String context="context0";
context=ServletUtilities.getContext(request);

	String rootDir = getServletContext().getRealPath("/");
	String baseDir = ServletUtilities.dataDir(context, rootDir);

  GregorianCalendar cal = new GregorianCalendar();
  int nowYear = cal.get(1);
//setup our Properties object to hold all properties
  Properties props = new Properties();
  //String langCode = "en";
  String langCode=ServletUtilities.getLanguageCode(request);
  
	//no longer used: lock file (see below) used instead
	//////BatchCompareProcessor proc = (BatchCompareProcessor)session.getAttribute(BatchCompareProcessor.SESSION_KEY_PROCESS);

  //set up the file input stream
  //props.load(getClass().getResourceAsStream("/bundles/" + langCode + "/submit.properties"));
  props = ShepherdProperties.getProperties("submit.properties", langCode,context);



  

%>

<html>
<head>
  <title><%=CommonConfiguration.getHTMLTitle(context) %>
  </title>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
  <meta name="Description"
        content="<%=CommonConfiguration.getHTMLDescription(context) %>"/>
  <meta name="Keywords"
        content="<%=CommonConfiguration.getHTMLKeywords(context) %>"/>
  <meta name="Author" content="<%=CommonConfiguration.getHTMLAuthor(context) %>"/>
  <link href="<%=CommonConfiguration.getCSSURLLocation(request,context) %>"
        rel="stylesheet" type="text/css"/>
  <link rel="shortcut icon"
        href="<%=CommonConfiguration.getHTMLShortcutIcon(context) %>"/>

        
  <script language="javascript" type="text/javascript">
  </script>



<style type="text/css">
.full_screen_map {
position: absolute !important;
top: 0px !important;
left: 0px !important;
z-index: 1 !imporant;
width: 100% !important;
height: 100% !important;
margin-top: 0px !important;
margin-bottom: 8px !important;
</style>


</head>
<body>
<div id="wrapper">
<div id="page">
<jsp:include page="header.jsp" flush="true">

  <jsp:param name="isAdmin" value="<%=request.isUserInRole(\"admin\")%>" />
</jsp:include>

 
<div id="main">

<div id="maincol-wide-solo">

<div id="maintext">

<%
String irm = (String)session.getAttribute("importResultsMessage");
if (irm != null) {
%>
	<div style="margin: 0 80px 15px 40px; padding: 0 8px 0 8px; border: solid 1px #CCC; font-size: 0.85em;"><p><i><b>CSV Data Import Results:</b></i></p>
<%=irm%>
	</div>
<% } %>

  <h1 class="intro">Images Import</h1>
</div>
<form id="encounterForm" action="BatchCompare" method="post" enctype="multipart/form-data" >

<script>
function updateList(inp) {
	var f = '';
	if (inp.files && inp.files.length) {
		var all = [];
		for (var i = 0 ; i < inp.files.length ; i++) {
			all.push(inp.files[i].name + ' (' + Math.round(inp.files[i].size / 1024) + 'k)');
		}
		f = '<b>' + inp.files.length + ' file' + ((inp.files.length == 1) ? '' : 's') + ':</b> ' + all.join(', ');
	} else {
		f = inp.value;
	}
	document.getElementById('input-file-list').innerHTML = f;
}
</script>

<%

String hidden = "";
	File ilock = new File(baseDir + "/encounters/importcsv.lock");

if (ilock.exists()) {
	String[] counts = new String(Files.readAllBytes(Paths.get(baseDir + "/encounters/importcsv.lock"))).split("\\s+");
	out.println("<script>window.setTimeout(function() { window.location.reload(); }, 8000);</script>");
	out.println("<div id=\"batch-waiting\">" + props.getProperty("batchCompareImportNotFinished").replaceFirst("%countTotal", counts[1]).replaceFirst("%countComplete", counts[0]) + "</div>");
	int percent = 0;
	int total = Integer.parseInt(counts[1]);
	if (total > 0) percent = 100 * Integer.parseInt(counts[0]) / total;
	out.println("<div class=\"progress-bar-wrapper\" style=\"border: solid 1px #AAA; width: 85%; height: 20px; margin: 20px; position: relative;\"><div class=\"progress-bar\" style=\"width: " + Integer.toString(100 - percent) + "%; height: 20px; position: absolute; right: 0; background-color: #EEE;\">&nbsp;</div></div>");

	hidden = "style=\"display: none;\"";
} %>
<div <%=hidden%>>
<div class="input-file-drop">
<% if (isIE) { %>
		<div><%=props.getProperty("dragInstructionsIE")%></div>
		<input class="ie" name="theFiles" type="file" accept=".jpg, .jpeg, .png, .bmp, .gif" multiple size="30" onChange="updateList(this);" />
<% } else { %>
		<input class="nonIE" name="theFiles" type="file" accept=".jpg, .jpeg, .png, .bmp, .gif" multiple size="30" onChange="updateList(this);" />
		<div><%=props.getProperty("batchCompareImagesInstructions")%></div>
<% } %>
		<div id="input-file-list"></div>
</div>

<p><input type="submit" value="submit" onClick="$(this).hide(); $('#upmsg').show();" /><div style="display:none;" id="upmsg">uploading images...</div></p>
</div>

<p>&nbsp;</p>
</form>
</div>
<!-- end maintext --></div>
<!-- end maincol -->
<jsp:include page="footer.jsp" flush="true"/>
</div>
<!-- end page --></div>
<!--end wrapper -->
</body>
</html>