<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<div class="onlyofficeContainer">
  <div class="loading">
    <div class="waitThrobber"></div>
  </div>
  <%-- div class="exitWindow onlyofficeEditorClose" style="display: none;">
    <a class="uiIconClose uiIconWhite" title="Close"></a>
  </div --%>
  <div id="editor-top-bar" style="display: none">
    <div class="document-path"></div>
    <div class="document-title"></div>
    <div class="last-edited"></div>
    <div class="editors-comment"></div>
    <div class="comment">
      <input type="text" id="comment-box"></input>
      <div id="save-btn">
        <i class="uiIconEcmsLightGray uiIconSave"></i>
      </div>
    </div>
    <div class="forcesave-btn"></div>
    <div class="close-btn"></div>
  </div>
  <div class="editor" style="display: none;">
    <div id="onlyoffice"></div>
  </div>
</div>
