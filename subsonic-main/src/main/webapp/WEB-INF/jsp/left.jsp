<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
</head>

<body class="bgcolor2">
<a name="top"/>

<div style="padding-bottom:0.5em">
    <c:forEach items="${model.indexes}" var="index">
        <a href="#${index.index}">${index.index}</a>
    </c:forEach>
</div>

<c:if test="${model.statistics != null}">
    <div class="detail">
        <fmt:message key="left.statistics">
            <fmt:param value="${model.statistics.artistCount}"/>
            <fmt:param value="${model.statistics.albumCount}"/>
            <fmt:param value="${model.statistics.songCount}"/>
            <fmt:param value="${model.bytes}"/>
            <fmt:param value="${model.hours}"/>
        </fmt:message>
    </div>
</c:if>

<c:if test="${fn:length(model.musicFolders) > 1}">
    <div style="padding-top:1em">
        <select name="musicFolderId" style="width:100%" onchange="location='left.view?musicFolderId=' + options[selectedIndex].value;" >
            <option value="-1"><fmt:message key="left.allfolders"/></option>
            <c:forEach items="${model.musicFolders}" var="musicFolder">
                <option ${model.selectedMusicFolder.id == musicFolder.id ? "selected" : ""} value="${musicFolder.id}">${musicFolder.name}</option>
            </c:forEach>
        </select>
    </div>
</c:if>

<c:if test="${not empty model.shortcuts}">
    <h2 class="bgcolor1"><fmt:message key="left.shortcut"/></h2>
    <c:forEach items="${model.shortcuts}" var="shortcut">
        <p class="dense" style="padding-left:0.5em">
            <sub:url value="main.view" var="mainUrl">
                <sub:param name="path" value="${shortcut.path}"/>
            </sub:url>
            <a target="main" href="${mainUrl}">${shortcut.name}</a>
        </p>
    </c:forEach>
</c:if>

<c:if test="${not empty model.radios}">
    <h2 class="bgcolor1"><fmt:message key="left.radio"/></h2>
    <c:forEach items="${model.radios}" var="radio">
        <p class="dense">
            <a target="hidden" href="${radio.streamUrl}">
                <img width="13" height="13" src="<spring:theme code="playImage"/>" alt="<fmt:message key="common.play"/>" title="<fmt:message key="common.play"/>"/></a>
            <c:choose>
                <c:when test="${empty radio.homepageUrl}">
                    ${radio.name}
                </c:when>
                <c:otherwise>
                    <a target="main" href="${radio.homepageUrl}">${radio.name}</a>
                </c:otherwise>
            </c:choose>
        </p>
    </c:forEach>
</c:if>

<c:forEach items="${model.indexedChildren}" var="entry">
    <table class="bgcolor1" style="width:100%;padding:0;margin:1em 0 0 0;border:0">
        <tr style="padding:0;margin:0;border:0">
            <th style="text-align:left;padding:0;margin:0;border:0"><a name="${entry.key.index}"/>
                <h2 style="padding:0;margin:0;border:0">${entry.key.index}
            </th>
            <th style="text-align:right;">
                <a href="#top"><img width="13" height="13" src="<spring:theme code="upImage"/>"/></a>
            </th>
        </tr>
    </table>

    <c:forEach items="${entry.value}" var="child">
        <c:if test="${child.directory}">
            <p class="dense" style="padding-left:0.5em">
                <span title="${child.name}">
                    <sub:url value="main.view" var="mainUrl">
                        <sub:param name="path" value="${child.path}"/>
                    </sub:url>
                    <a target="main" href="${mainUrl}"><str:truncateNicely upper="${model.captionCutoff}">${child.name}</str:truncateNicely></a>
                </span>
            </p>
        </c:if>
    </c:forEach>
</c:forEach>

<div style="padding-top:1em"/>

<c:forEach items="${model.indexedChildren}" var="entry">
    <c:forEach items="${entry.value}" var="child">
        <c:if test="${not child.directory}">
            <p class="dense" style="padding-left:0.5em">
                <span title="${child.title}">
                    <c:import url="playAddDownload.jsp">
                        <c:param name="path" value="${child.path}"/>
                        <c:param name="downloadEnabled" value="${model.downloadEnabled}"/>
                    </c:import>
                    <str:truncateNicely upper="${model.captionCutoff}">${child.title}</str:truncateNicely>
                </span>
            </p>
        </c:if>
    </c:forEach>
</c:forEach>
</body></html>