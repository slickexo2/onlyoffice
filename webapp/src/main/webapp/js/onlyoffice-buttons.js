/**
 * Onlyoffice Editor client.
 */
(function($) {

  function getHtmlLink(link, label) {
    return '<li><a href="' + link + '" target="_blank"><i class="uiIconEcmsOnlyofficeOpen uiIconEcmsLightGray uiIconEdit"></i> '
        + label + '</a></li>';
  }

  function addPreviewButton(link, label, attempts, delay) {
    var $elem = $(".previewBtn");
    if ($elem.length == 0 || !$elem.is(":visible")) {
      if (attempts > 0) {
        setTimeout(function() {
          addPreviewButton(link, label, attempts - 1, delay);
        }, delay);

      } else {
        console.log("Cannot find element " + $elem);
      }
    } else {
      $(".previewBtn").append('<div class="onlyOfficeEditBtn">' + getHtmlLink(link, label) + '</div>');
    }
  }

  function OnlyOfficeButtons() {
    var self = this;

    this.addButtonToActivity = function(activityId, link, label) {
      $("#activityContainer" + activityId).find("div[id^='ActivityContextBox'] > .actionBar .statusAction.pull-left").append(
          getHtmlLink(link, label));
    };

    this.addButtonToPreview = function(activityId, link, index, label) {
      $("#Preview" + activityId + "-" + index).click(function() {
        // We set timeout here to avoid the case when the element is rendered but is going to be updated soon
        setTimeout(function() {
          addPreviewButton(link, label, 100, 100);
        }, 100);

      });

    };
  }
  return new OnlyOfficeButtons();

})($);