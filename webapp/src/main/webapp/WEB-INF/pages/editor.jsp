<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<div class="onlyofficeContainer">
  <div class="loading">
    <div class="waitThrobber"></div>
  </div>
  <%-- div class="exitWindow onlyofficeEditorClose" style="display: none;">
    <a class="uiIconClose uiIconWhite" title="Close"></a>
  </div --%>
   <div id="open-drawer-btn" class="btn-primary open-drawer-btn" rel="tooltip" data-placement="bottom" data-original-title="Add new version">
          <a class="icon-place">
            <i class="uiIconSave"></i>
          </a>
   </div>

   <div id="editor-top-bar" class="drawer">

     <div class="header">
           <span>Save version</span>
           <a class="closebtn">×</a>
     </div>

     <div class="content">

       <div class="contentDisplay">
          <div class="spaceAvatar">
               <img alt="" class="space-avatar" rel="tooltip" data-placement="bottom" data-original-title="">
            </div>

         <div class="displayPathTitle">
           <div class="document-path"></div>
           <div class="document-title">
             <a data-placement="bottom" rel="tooltip" data-original-title="Click to edit"> </a>
           </div>
         </div>
       </div>

       <div class="comment">
        <div id="alert-saved" class="alert alert-success" style="display: none;"><i class="uiIconSuccess"></i>Changes saved</div>
         <textarea type="text" id="comment-box" placeholder="Write your summary of changes ..."></textarea>
       </div>
       <div id="save-btn" type="button" class="btn btn-primary">
          <a data-placement="bottom" rel="tooltip" data-original-title="Save">
          </a>
          <a class="nameStyle">Save</a>
       </div>

       <div id="versions"></div>

     </div>

     <div class="footer">
        <button id="see-more-btn" type="button" class="btn lineStyle">See more</button>
     </div>
   </div>


  <div class="editor" style="display: none;">
    <div id="onlyoffice"></div>
  </div>
</div>
