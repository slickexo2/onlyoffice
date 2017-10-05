
/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.onlyoffice;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OnlyofficeEditorListener.java 00000 Mar 4, 2016 pnedonosko $
 */
public interface OnlyofficeEditorListener {
  
  /**
   * New editor just created.
   *
   * @param config the config
   */
  void onCreate(Config config);
  
  /**
   * Existing editor obtained by user.
   *
   * @param config the config
   */
  void onGet(Config config);
  
  /**
   * User joined co-editing document (it's second or more user). 
   *
   * @param config the config
   */
  void onJoined(Config config);
  
  /**
   * User leaved co-editing document (it's at least second user gone). 
   *
   * @param config the config
   */
  void onLeaved(Config config);
  
  /**
   * Document was saved and editor released.
   *
   * @param config the config
   */
  void onSaved(Config config);
  
  /**
   * Error saving document in editor. Error message if found, will be set in the {@link Config#getError()}.
   *
   * @param config the config
   */
  void onError(Config config);

}
