
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
 * Editing document state in local storage.
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ChangeStatus.java 00000 Feb 25, 2016 pnedonosko $
 * 
 */
public class ChangeState {

  /** The saved. */
  protected final boolean  saved;

  /** The error. */
  protected final String   error;

  /** The users. */
  protected final String[] users;

  /**
   * Instantiates a new change state.
   *
   * @param saved the saved
   * @param error the error
   * @param users the users
   */
  public ChangeState(boolean saved, String error, String[] users) {
    this.saved = saved;
    this.users = users;
    this.error = error;
  }

  /**
   * Instantiates a new change state.
   *
   * @param saved the saved
   * @param users the users
   */
  public ChangeState(boolean saved, String[] users) {
    this(saved, null, users);
  }

  /**
   * Gets the error.
   *
   * @return the error
   */
  public String getError() {
    return error;
  }

  /**
   * Checks if is saved.
   *
   * @return the saved
   */
  public boolean isSaved() {
    return saved;
  }

  /**
   * Gets the users.
   *
   * @return the users
   */
  public String[] getUsers() {
    return users;
  }

}
