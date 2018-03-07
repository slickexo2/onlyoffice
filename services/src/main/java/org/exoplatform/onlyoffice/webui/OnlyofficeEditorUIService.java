
/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
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
import org.exoplatform.services.cache.CacheListener;
import org.exoplatform.services.cache.CacheListenerContext;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.concurrent.locks.ReentrantLock;

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

  /** The Constant CACHE_NAME. */
  public static final String CACHE_NAME    = "onlyoffice.EditorStateCache".intern();

  /** The Constant LOG. */
  protected static final Log LOG           = ExoLogger.getLogger(OnlyofficeEditorUIService.class);

  /** The Constant STATE_OPENING. */
  public static final String STATE_OPENING = "opening".intern();

  /** The Constant STATE_OPEN. */
  public static final String STATE_OPEN    = "open".intern();

  /** The Constant STATE_CLOSING. */
  public static final String STATE_CLOSING = "closing".intern();

  /**
   * The listener interface for receiving editor events.
   * The class that is interested in processing a editor
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addEditorListener</code> method. When
   * the editor event occurs, that object's appropriate
   * method is invoked.
   */
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

  /** Cache of open by user editors (its states). */
  protected final ExoCache<String, String> editorsCache;

  /** Lock for updating user editors cache. */
  protected final ReentrantLock            editorsLock = new ReentrantLock();

  /** The editor service. */
  protected final OnlyofficeEditorService  editorService;

  /**
   * Instantiates a new onlyoffice editor UI service.
   *
   * @param editorService the editor service
   * @param cacheService the cache service
   */
  public OnlyofficeEditorUIService(OnlyofficeEditorService editorService, CacheService cacheService) {
    this.editorService = editorService;
    this.editorsCache = cacheService.getCacheInstance(CACHE_NAME);
    if (LOG.isDebugEnabled()) {
      editorsCache.addCacheListener(new CacheListener<String, String>() {

        @Override
        public void onExpire(CacheListenerContext context, String key, String obj) throws Exception {
          LOG.debug(CACHE_NAME + " onExpire > " + key + ": " + obj);
        }

        @Override
        public void onRemove(CacheListenerContext context, String key, String obj) throws Exception {
          LOG.debug(CACHE_NAME + " onRemove > " + key + ": " + obj);
        }

        @Override
        public void onPut(CacheListenerContext context, String key, String obj) throws Exception {
          LOG.debug(CACHE_NAME + " onPut > " + key + ": " + obj);
        }

        @Override
        public void onGet(CacheListenerContext context, String key, String obj) throws Exception {
          LOG.debug(CACHE_NAME + " onGet > " + key + ": " + obj);
        }

        @Override
        public void onClearCache(CacheListenerContext context) throws Exception {
          LOG.debug(CACHE_NAME + " onClearCache");
        }
      });
    }
    editorService.addListener(new EditorListener());
  }

  /**
   * Open.
   *
   * @param userId the user id
   * @param workspace the workspace
   * @param path the path
   * @return true, if successful
   */
  public boolean open(String userId, String workspace, String path) {
    String id = editorId(userId, workspace, path);
    String state = editorsCache.get(id);
    if (state == null) {
      editorsLock.lock();
      try {
        state = editorsCache.get(id);
        if (state == null) {
          editorsCache.put(id, STATE_OPENING);
          return true;
        }
      } finally {
        editorsLock.unlock();
      }
    }
    return STATE_OPENING.equals(state);
  }

  /**
   * Opened.
   *
   * @param userId the user id
   * @param workspace the workspace
   * @param path the path
   * @return true, if successful
   */
  public boolean opened(String userId, String workspace, String path) {
    String id = editorId(userId, workspace, path);
    String state = editorsCache.get(id);
    if (STATE_OPENING.equals(state)) {
      editorsLock.lock();
      try {
        editorsCache.put(id, STATE_OPEN);
        return true;
      } finally {
        editorsLock.unlock();
      }
    }
    return false;
  }

  /**
   * Close.
   *
   * @param userId the user id
   * @param workspace the workspace
   * @param path the path
   * @return true, if successful
   */
  public boolean close(String userId, String workspace, String path) {
    String id = editorId(userId, workspace, path);
    String state = editorsCache.get(id);
    if (STATE_OPEN.equals(state)) {
      editorsLock.lock();
      try {
        editorsCache.put(id, STATE_CLOSING);
        return true;
      } finally {
        editorsLock.unlock();
      }
    } else if (STATE_OPENING.equals(state)) {
      // if wasn't OPEN but close requested, ensure document also isn't OPENING
      editorsLock.lock();
      try {
        return STATE_OPENING.equals(editorsCache.remove(id));
      } finally {
        editorsLock.unlock();
      }
    }
    return true;
  }

  /**
   * Closed.
   *
   * @param userId the user id
   * @param workspace the workspace
   * @param path the path
   * @return true, if successful
   */
  public boolean closed(String userId, String workspace, String path) {
    String id = editorId(userId, workspace, path);
    String state = editorsCache.get(id);
    if (STATE_CLOSING.equals(state)) {
      editorsLock.lock();
      try {
        return STATE_CLOSING.equals(editorsCache.remove(id));
      } finally {
        editorsLock.unlock();
      }
    }
    return false;
  }

  /**
   * Reset.
   *
   * @param userId the user id
   * @param workspace the workspace
   * @param path the path
   * @return true, if successful
   */
  public boolean reset(String userId, String workspace, String path) {
    String id = editorId(userId, workspace, path);
    return editorsCache.remove(id) != null;
  }

  /**
   * Checks if is opening.
   *
   * @param userId the user id
   * @param workspace the workspace
   * @param path the path
   * @return true, if is opening
   */
  public boolean isOpening(String userId, String workspace, String path) {
    String id = editorId(userId, workspace, path);
    return STATE_OPENING.equals(editorsCache.get(id));
  }

  /**
   * Checks if is open.
   *
   * @param userId the user id
   * @param workspace the workspace
   * @param path the path
   * @return true, if is open
   */
  public boolean isOpen(String userId, String workspace, String path) {
    String id = editorId(userId, workspace, path);
    return STATE_OPEN.equals(editorsCache.get(id));
  }

  /**
   * Checks if is closing.
   *
   * @param userId the user id
   * @param workspace the workspace
   * @param path the path
   * @return true, if is closing
   */
  public boolean isClosing(String userId, String workspace, String path) {
    String id = editorId(userId, workspace, path);
    return STATE_CLOSING.equals(editorsCache.get(id));
  }

  /**
   * Checks if is closed.
   *
   * @param userId the user id
   * @param workspace the workspace
   * @param path the path
   * @return true, if is closed
   */
  public boolean isClosed(String userId, String workspace, String path) {
    String id = editorId(userId, workspace, path);
    return editorsCache.get(id) == null;
  }

  /**
   * Can show.
   *
   * @param userId the user id
   * @param workspace the workspace
   * @param path the path
   * @return true, if successful
   */
  public boolean canShow(String userId, String workspace, String path) {
    String id = editorId(userId, workspace, path);
    String state = editorsCache.get(id);
    return STATE_OPENING.equals(state) || STATE_OPEN.equals(state) || STATE_CLOSING.equals(state);
  }

  /**
   * Can open.
   *
   * @param userId the user id
   * @param workspace the workspace
   * @param path the path
   * @return true, if successful
   */
  public boolean canOpen(String userId, String workspace, String path) {
    String id = editorId(userId, workspace, path);
    String state = editorsCache.get(id);
    return state == null || STATE_CLOSING.equals(state);
  }

  /**
   * Can close.
   *
   * @param userId the user id
   * @param workspace the workspace
   * @param path the path
   * @return true, if successful
   */
  public boolean canClose(String userId, String workspace, String path) {
    String id = editorId(userId, workspace, path);
    String state = editorsCache.get(id);
    return STATE_OPENING.equals(state) || STATE_OPEN.equals(state);
  }

  /**
   * Editor id.
   *
   * @param userId the user id
   * @param workspace the workspace
   * @param path the path
   * @return the string
   */
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
