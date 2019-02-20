/**
 * Onlyoffice Editor client.
 */
(function($) {

  function getHtmlLink(link) {
    return '<li><a href="' + link + '" target="_blank"><i class="uiIconEcmsOnlyofficeOpen uiIconEcmsLightGray uiIconEdit"></i> Edit Online</a></li>'
  };

  function OnlyOfficeButtons() {
    var self = this;

    this.addButtonToActivity = function(activityId, link) {
      $("#activityContainer" + activityId).find(".statusAction.pull-left").append(getHtmlLink(link));
    };

    this.addButtonToPreview = function(activityId, link, index) {
      $("#Preview" + activityId + "-" + index).click(function() {
        setTimeout(function() {
          var previewBtn = $(".previewBtn");
          previewBtn.find(".remoteEditBtn").css({
            "float" : "right",
            "padding-left" : "15px"
          });
          previewBtn.append('<div style="float:right; padding-left:15px;">' + getHtmlLink(link) + '</div>');
        }, 500);
      });

    };

  }

  return new OnlyOfficeButtons();

})($);