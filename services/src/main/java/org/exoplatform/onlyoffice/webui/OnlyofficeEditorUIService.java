
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
package org.exoplatform.onlyoffice.webui;

import org.exoplatform.onlyoffice.Config;
import org.exoplatform.onlyoffice.OnlyofficeEditorListener;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Support for stateful WebUI: keep tracking requests to open/close editor by users on partical file.
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OnlyofficeEditorUIService.java 00000 Mar 3, 2016 pnedonosko $
 * 
 */
public class OnlyofficeEditorUIService {

  protected static enum State {
    OPENING, OPEN, CLOSING
  }

  protected class EditorListener implements OnlyofficeEditorListener {

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Config config) {
      // FYI creator's editor will be marked open when he'll join in onJoined()
      open(config.getEditorConfig().getUser().getId(), config.getWorkspace(), config.getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onGet(Config config) {
      // FYI user's editor will be marked open when he'll join in onJoined()
      open(config.getEditorConfig().getUser().getId(), config.getWorkspace(), config.getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onJoined(Config config) {
      // FYI mark as open for all users (creator and co-editors)
      opened(config.getEditorConfig().getUser().getId(), config.getWorkspace(), config.getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLeaved(Config config) {
      reset(config.getEditorConfig().getUser().getId(), config.getWorkspace(), config.getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaved(Config config) {
      reset(config.getEditorConfig().getUser().getId(), config.getWorkspace(), config.getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onError(Config config) {
      // on error: do some extra server-side handling here if required
      // FYI in case of error browser user will see it thanks to config state obtained from /state
      // (localState) REST endpoint.
      // TODO reset not required here
      // reset(config.getEditorConfig().getUser().getId(), config.getWorkspace(), config.getPath());
    }
  }

  /**
   * Open editors.
   */
  protected final ConcurrentHashMap<String, State> editors = new ConcurrentHashMap<String, State>();

  protected final OnlyofficeEditorService          editorService;

  /**
   * 
   */
  public OnlyofficeEditorUIService(OnlyofficeEditorService editorService) {
    this.editorService = editorService;
    editorService.addListener(new EditorListener());
  }

  public boolean open(String userId, String workspace, String path) {
    String id = editorId(userId, workspace, path);
    State state = State.OPENING;
    State prev = editors.putIfAbsent(id, state);
    return prev != null ? State.OPENING == prev : true;
  }

  public boolean opened(String userId, String workspace, String path) {
    String id = editorId(userId, workspace, path);
    return editors.replace(id, State.OPENING, State.OPEN);
  }

  public boolean close(String userId, String workspace, String path) {
    String id = editorId(userId, workspace, path);
    if (!editors.replace(id, State.OPEN, State.CLOSING)) {
      // if wasn't OPEN but close requested, ensure document also isn't OPENING 
      return editors.remove(id, State.OPENING);
    }
    return true;
  }

  // TODO this method not used, reset used by listener instead
  public boolean closed(String userId, String workspace, String path) {
    String id = editorId(userId, workspace, path);
    return editors.remove(id, State.CLOSING);
  }

  public boolean reset(String userId, String workspace, String path) {
    String id = editorId(userId, workspace, path);
    State state = editors.remove(id);
    return state != null;
  }

  public boolean isOpening(String userId, String workspace, String path) {
    String id = editorId(userId, workspace, path);
    return State.OPENING == editors.get(id);
  }

  public boolean isOpen(String userId, String workspace, String path) {
    String id = editorId(userId, workspace, path);
    return State.OPEN == editors.get(id);
  }

  public boolean isClosing(String userId, String workspace, String path) {
    String id = editorId(userId, workspace, path);
    return State.CLOSING == editors.get(id);
  }

  public boolean isClosed(String userId, String workspace, String path) {
    String id = editorId(userId, workspace, path);
    return !editors.containsKey(id);
  }

  public boolean canShow(String userId, String workspace, String path) {
    String id = editorId(userId, workspace, path);
    State state = editors.get(id);
    return State.OPENING == state || State.OPEN == state || State.CLOSING == state;
  }

  public boolean canOpen(String userId, String workspace, String path) {
    String id = editorId(userId, workspace, path);
    State state = editors.get(id);
    return state == null || State.CLOSING == state;
  }

  public boolean canClose(String userId, String workspace, String path) {
    String id = editorId(userId, workspace, path);
    State state = editors.get(id);
    return State.OPENING == state || State.OPEN == state;
  }

  protected String editorId(String userId, String workspace, String path) {
    StringBuilder id = new StringBuilder();
    id.append(userId);
    id.append(':');
    id.append(workspace);
    id.append(':');
    id.append(path);
    return id.toString();
  }

}
