package org.exoplatform.onlyoffice;

import org.apache.commons.chain.Context;

import org.exoplatform.services.listener.Event;
import org.exoplatform.wcm.ext.component.activity.listener.FileUpdateActivityListener;

public class DocumentUpdateActivityListener extends FileUpdateActivityListener {

  @Override
  public void onEvent(Event<Context, String> event) throws Exception {
    // TODO add logic for events
   // super.onEvent(event);
  }

}
