/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.onlyoffice;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.jcr.Node;

/**
 * Onlyoffice editor config for its JS API. <br>
 * 
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: Editor.java 00000 Feb 12, 2016 pnedonosko $
 */
public class Config {

  protected static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("MM/dd/yyyy");

  public static class Builder {

    protected final String workspace;

    protected final String path;

    protected final String documentType;

    // DocumentServer link
    protected final String documentserverUrl;

    // if set will be used to generate file and callback URLs, for this config and its copies for other users.
    protected String       platformUrl;

    // Document
    protected String       fileType, key, title, url;

    // Document.Info
    protected String       author, created, folder;

    // Editor
    protected String       callbackUrl, lang, mode;

    // Editor.User
    protected String       userId, firstname, lastname;

    protected Builder(String documentserverUrl, String documentType, String workspace, String path) {
      this.documentserverUrl = documentserverUrl;
      this.documentType = documentType;
      this.workspace = workspace;
      this.path = path;
    }

    /**
     * Generate file and callback URLs using given Platform base URL. This will erase these URLs explicitly
     * set previously.
     * 
     * @param platformUrl
     * @return
     */
    public Builder generateUrls(String platformUrl) {
      this.platformUrl = platformUrl;
      return this;
    }

    // Document: fileType, key, title, url

    public Builder title(String title) {
      this.title = title;
      return this;
    }

    public Builder key(String key) {
      this.key = key;
      return this;
    }

    public Builder fileType(String fileType) {
      this.fileType = fileType;
      return this;
    }

    public Builder url(String url) {
      this.url = url;
      return this;
    }

    // Document.info: author, created, folder

    public Builder author(String author) {
      this.author = author;
      return this;
    }

    public Builder created(Calendar createdTime) {
      this.created = DATETIME_FORMAT.format(createdTime.getTime());
      return this;
    }

    public Builder folder(String folder) {
      this.folder = folder;
      return this;
    }

    // Editor: callbackUrl, lang, mode
    public Builder callbackUrl(String callbackUrl) {
      this.callbackUrl = callbackUrl;
      return this;
    }

    public Builder lang(String lang) {
      this.lang = lang;
      return this;
    }

    public Builder mode(String mode) {
      this.mode = mode;
      return this;
    }

    // Editor.User: userId, firstname, lastname

    public Builder userId(String userId) {
      this.userId = userId;
      return this;
    }

    public Builder userFirstName(String firstname) {
      this.firstname = firstname;
      return this;
    }

    public Builder userLastName(String lastname) {
      this.lastname = lastname;
      return this;
    }

    public Config build() {
      if (platformUrl != null) {
        this.url = new StringBuilder(platformUrl).append("/content/").append(userId).append("/").append(key).toString();
        this.callbackUrl = new StringBuilder(platformUrl).append("/status/")
                                                         .append(userId)
                                                         .append("/")
                                                         .append(key)
                                                         .toString();
      }

      Document.Info info = new Document.Info(author, created, folder);
      Document.Permissions permissions = new Document.EditPermissions();
      Document document = new Document(key, fileType, title, url, info, permissions);
      Editor.User user = new Editor.User(userId, firstname, lastname);
      Editor editor = new Editor(callbackUrl, lang, mode, user);
      return new Config(documentserverUrl, platformUrl, workspace, path, documentType, document, editor);
    }
  }

  public static class Document {

    public static class Info {
      protected final String author;

      protected final String created; // '2010-07-07 3:46 PM'

      protected final String folder;  // 'Example Files'

      // TODO there is also sharingSettings array where we can put users with different access/edit
      // permissions: 'Full Access', 'Read Only'

      protected Info(String author, String created, String folder) {
        super();
        this.author = author;
        this.created = created;
        this.folder = folder;
      }

      /**
       * @return the author
       */
      public String getAuthor() {
        return author;
      }

      /**
       * @return the created
       */
      public String getCreated() {
        return created;
      }

      /**
       * @return the folder
       */
      public String getFolder() {
        return folder;
      }

    }

    public static abstract class Permissions {
      protected final boolean download;

      protected final boolean edit;

      protected Permissions(boolean download, boolean edit) {
        this.download = download;
        this.edit = edit;
      }

      /**
       * @return the download
       */
      public boolean isDownload() {
        return download;
      }

      /**
       * @return the edit
       */
      public boolean isEdit() {
        return edit;
      }

    }

    public static class EditPermissions extends Permissions {

      protected EditPermissions() {
        super(true, true);
      }
    }

    protected final String      fileType;

    protected final String      key;

    protected final String      title;

    protected final String      url;

    protected final Info        info;

    protected final Permissions permissions;

    protected Document(String key, String fileType, String title, String url, Info info, Permissions permissions) {
      super();
      this.fileType = fileType;
      this.key = key;
      this.title = title;
      this.url = url;
      this.info = info;
      this.permissions = permissions;
    }

    protected Document forUser(String id, String firstName, String lastName, String url) {
      return new Document(key, fileType, title, url, info, permissions);
    }

    /**
     * @return the fileType
     */
    public String getFileType() {
      return fileType;
    }

    /**
     * @return the key
     */
    public String getKey() {
      return key;
    }

    /**
     * @return the title
     */
    public String getTitle() {
      return title;
    }

    /**
     * @return the url
     */
    public String getUrl() {
      return url;
    }

    /**
     * @return the info
     */
    public Info getInfo() {
      return info;
    }

    /**
     * @return the permissions
     */
    public Permissions getPermissions() {
      return permissions;
    }

  }

  public static class Editor {

    public static class User {
      protected final String     id;

      protected final String     firstname;

      protected final String     lastname;

      protected final String     username;

      protected transient String lockToken;

      protected User(String id, String firstname, String lastname) {
        super();
        this.id = id;
        this.username = id;
        this.firstname = firstname;
        this.lastname = lastname;
      }

      /**
       * @return the id
       */
      public String getId() {
        return id;
      }

      /**
       * @return the username
       */
      public String getUsername() {
        return username;
      }

      /**
       * @return the firstname
       */
      public String getFirstname() {
        return firstname;
      }

      /**
       * @return the lastname
       */
      public String getLastname() {
        return lastname;
      }

      /**
       * @return the lockToken
       */
      protected String getLockToken() {
        return lockToken;
      }

      /**
       * @param lockToken the lockToken to set
       */
      protected void setLockToken(String lockToken) {
        this.lockToken = lockToken;
      }

    }

    protected final String callbackUrl;

    protected final String lang;

    protected final String mode;

    protected final User   user;

    protected Editor(String callbackUrl, String lang, String mode, User user) {
      super();
      this.callbackUrl = callbackUrl;
      this.lang = lang;
      this.mode = mode;
      this.user = user;
    }

    /**
     * @return the callbackUrl
     */
    public String getCallbackUrl() {
      return callbackUrl;
    }

    /**
     * @return the lang
     */
    public String getLang() {
      return lang;
    }

    /**
     * @return the mode
     */
    public String getMode() {
      return mode;
    }

    /**
     * @return the user
     */
    public User getUser() {
      return user;
    }

    protected Editor forUser(String id, String firstName, String lastName, String lang, String callbackUrl) {
      User otherUser = new User(id, firstName, lastName);
      // FYI locks maintenance will introduce complex logic
      // simpler: each user may contain own lock token only, but don't rely on others
      // otherUser.setLockToken(user.getLockToken());
      return new Editor(callbackUrl, lang, mode, otherUser);
    }
  }

  protected static Builder editor(String documentserverUrl, String workspace, String path, String documentType) {
    return new Builder(documentserverUrl, documentType, workspace, path);
  }

  protected static String fileUrl(String platformUrl, String userId, String key) {
    return new StringBuilder(platformUrl).append("/content/").append(userId).append("/").append(key).toString();
  }

  protected static String callbackUrl(String platformUrl, String userId, String key) {
    return new StringBuilder(platformUrl).append("/status/").append(userId).append("/").append(key).toString();
  }

  protected final String      documentserverUrl, documentserverJsUrl;

  protected final String      platformUrl;

  protected final String      workspace;

  protected final String      path;

  protected final String      documentType;

  protected final Document    document;

  protected final Editor      editorConfig;

  protected String            error;

  protected transient Node    node;

  /**
   * Marker of editor state. By default editor state is undefined and will be treated as not open nor not
   * closed. When editor will be open in Onlyoffice it will send a status (1) and then need mark the editor
   * open.
   */
  protected transient Boolean open;

  /**
   * {@link Config} constructor.
   * 
   */
  protected Config(String documentserverUrl,
                   String platformUrl,
                   String workspace,
                   String path,
                   String documentType,
                   Document document,
                   Editor editor) {
    this.workspace = workspace;
    this.path = path;
    this.documentType = documentType;
    this.documentserverUrl = documentserverUrl;
    this.documentserverJsUrl = new StringBuilder(documentserverUrl).append("apps/api/documents/api.js").toString();

    this.platformUrl = platformUrl;

    this.document = document;
    this.editorConfig = editor;
  }

  /**
   * @return the documentserverJsUrl
   */
  public String getDocumentserverJsUrl() {
    return documentserverJsUrl;
  }

  /**
   * @return the documentserverUrl
   */
  public String getDocumentserverUrl() {
    return documentserverUrl;
  }

  /**
   * @return the node in context, can be <code>null</code>
   */
  public Node getContextNode() {
    return node;
  }

  /**
   * @param node the node to set
   */
  protected void setContextNode(Node node) {
    this.node = node;
  }

  /**
   * @return the workspace
   */
  public String getWorkspace() {
    return workspace;
  }

  /**
   * @return the path
   */
  public String getPath() {
    return path;
  }

  /**
   * @return the documentType
   */
  public String getDocumentType() {
    return documentType;
  }

  /**
   * @return the config
   */
  public Document getDocument() {
    return document;
  }

  /**
   * @return the editor
   */
  public Editor getEditorConfig() {
    return editorConfig;
  }

  /**
   * Create a copy of this editor but for another given user.
   * 
   * @param id {@link String}
   * @param firstName {@link String}
   * @param lastName {@link String}
   * @param lang {@link String}
   * @return {@link Config} an instance of config similar to this but with another user in the editor
   */
  public Config forUser(String id, String firstName, String lastName, String lang) {
    return new Config(documentserverUrl,
                      platformUrl,
                      workspace,
                      path,
                      documentType,
                      document.forUser(id, firstName, lastName, fileUrl(platformUrl, id, document.getKey())),
                      editorConfig.forUser(id, firstName, lastName, lang, callbackUrl(platformUrl, id, document.getKey())));
  }

  public boolean isCreated() {
    return open == null;
  }

  public boolean isOpen() {
    return open != null ? open.booleanValue() : false;
  }

  public boolean isClosed() {
    return open != null ? !open.booleanValue() : false;
  }

  /**
   * Mark this this config as open: user opened this editor.
   */
  public void open() {
    this.open = new Boolean(true);
  }

  /**
   * Mark this this config as closed: user already closed this editor.
   */
  public void close() {
    this.open = new Boolean(false);
  }

  public void setError(String error) {
    this.error = error;
  }

  public boolean hasError() {
    return this.error != null;
  }

  public String getError() {
    return this.error;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Config) {
      Config other = (Config) obj;
      return this.documentType.equals(other.documentType) && this.workspace.equals(other.workspace)
          && this.path.equals(other.path);
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    s.append(documentType);
    s.append(' ');
    s.append(workspace);
    s.append(':');
    s.append(path);
    return s.toString();
  }

}
