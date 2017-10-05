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

  /** The Constant DATETIME_FORMAT. */
  protected static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("MM/dd/yyyy");

  /**
   * The Class Builder.
   */
  public static class Builder {

    /** The workspace. */
    protected final String workspace;

    /** The path. */
    protected final String path;

    /** The document type. */
    protected final String documentType;

    /** The documentserver url. */
    // DocumentServer link
    protected final String documentserverUrl;

    /** The platform url. */
    // if set will be used to generate file and callback URLs, for this config and its copies for other users.
    protected String       platformUrl;

    /** The url. */
    // Document
    protected String       fileType, key, title, url;

    /** The folder. */
    // Document.Info
    protected String       author, created, folder;

    /** The mode. */
    // Editor
    protected String       callbackUrl, lang, mode;

    /** The lastname. */
    // Editor.User
    protected String       userId, firstname, lastname;

    /**
     * Instantiates a new builder.
     *
     * @param documentserverUrl the documentserver url
     * @param documentType the document type
     * @param workspace the workspace
     * @param path the path
     */
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
     * @param platformUrl the platform url
     * @return the builder
     */
    public Builder generateUrls(String platformUrl) {
      this.platformUrl = platformUrl;
      return this;
    }

    // Document: fileType, key, title, url

    /**
     * Title.
     *
     * @param title the title
     * @return the builder
     */
    public Builder title(String title) {
      this.title = title;
      return this;
    }

    /**
     * Key.
     *
     * @param key the key
     * @return the builder
     */
    public Builder key(String key) {
      this.key = key;
      return this;
    }

    /**
     * File type.
     *
     * @param fileType the file type
     * @return the builder
     */
    public Builder fileType(String fileType) {
      this.fileType = fileType;
      return this;
    }

    /**
     * Url.
     *
     * @param url the url
     * @return the builder
     */
    public Builder url(String url) {
      this.url = url;
      return this;
    }

    // Document.info: author, created, folder

    /**
     * Author.
     *
     * @param author the author
     * @return the builder
     */
    public Builder author(String author) {
      this.author = author;
      return this;
    }

    /**
     * Created.
     *
     * @param createdTime the created time
     * @return the builder
     */
    public Builder created(Calendar createdTime) {
      this.created = DATETIME_FORMAT.format(createdTime.getTime());
      return this;
    }

    /**
     * Folder.
     *
     * @param folder the folder
     * @return the builder
     */
    public Builder folder(String folder) {
      this.folder = folder;
      return this;
    }

    /**
     * Callback url.
     *
     * @param callbackUrl the callback url
     * @return the builder
     */
    // Editor: callbackUrl, lang, mode
    public Builder callbackUrl(String callbackUrl) {
      this.callbackUrl = callbackUrl;
      return this;
    }

    /**
     * Lang.
     *
     * @param lang the lang
     * @return the builder
     */
    public Builder lang(String lang) {
      this.lang = lang;
      return this;
    }

    /**
     * Mode.
     *
     * @param mode the mode
     * @return the builder
     */
    public Builder mode(String mode) {
      this.mode = mode;
      return this;
    }

    // Editor.User: userId, firstname, lastname

    /**
     * User id.
     *
     * @param userId the user id
     * @return the builder
     */
    public Builder userId(String userId) {
      this.userId = userId;
      return this;
    }

    /**
     * User first name.
     *
     * @param firstname the firstname
     * @return the builder
     */
    public Builder userFirstName(String firstname) {
      this.firstname = firstname;
      return this;
    }

    /**
     * User last name.
     *
     * @param lastname the lastname
     * @return the builder
     */
    public Builder userLastName(String lastname) {
      this.lastname = lastname;
      return this;
    }

    /**
     * Builds the.
     *
     * @return the config
     */
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

  /**
   * The Class Document.
   */
  public static class Document {

    /**
     * The Class Info.
     */
    public static class Info {
      
      /** The author. */
      protected final String author;

      /** The created. */
      protected final String created; // '2010-07-07 3:46 PM'

      /** The folder. */
      protected final String folder;  // 'Example Files'

      // TODO there is also sharingSettings array where we can put users with different access/edit
      // permissions: 'Full Access', 'Read Only'

      /**
       * Instantiates a new info.
       *
       * @param author the author
       * @param created the created
       * @param folder the folder
       */
      protected Info(String author, String created, String folder) {
        super();
        this.author = author;
        this.created = created;
        this.folder = folder;
      }

      /**
       * Gets the author.
       *
       * @return the author
       */
      public String getAuthor() {
        return author;
      }

      /**
       * Gets the created.
       *
       * @return the created
       */
      public String getCreated() {
        return created;
      }

      /**
       * Gets the folder.
       *
       * @return the folder
       */
      public String getFolder() {
        return folder;
      }

    }

    /**
     * The Class Permissions.
     */
    public static abstract class Permissions {
      
      /** The download. */
      protected final boolean download;

      /** The edit. */
      protected final boolean edit;

      /**
       * Instantiates a new permissions.
       *
       * @param download the download
       * @param edit the edit
       */
      protected Permissions(boolean download, boolean edit) {
        this.download = download;
        this.edit = edit;
      }

      /**
       * Checks if is download.
       *
       * @return the download
       */
      public boolean isDownload() {
        return download;
      }

      /**
       * Checks if is edits the.
       *
       * @return the edit
       */
      public boolean isEdit() {
        return edit;
      }

    }

    /**
     * The Class EditPermissions.
     */
    public static class EditPermissions extends Permissions {

      /**
       * Instantiates a new edits the permissions.
       */
      protected EditPermissions() {
        super(true, true);
      }
    }

    /** The file type. */
    protected final String      fileType;

    /** The key. */
    protected final String      key;

    /** The title. */
    protected final String      title;

    /** The url. */
    protected final String      url;

    /** The info. */
    protected final Info        info;

    /** The permissions. */
    protected final Permissions permissions;

    /**
     * Instantiates a new document.
     *
     * @param key the key
     * @param fileType the file type
     * @param title the title
     * @param url the url
     * @param info the info
     * @param permissions the permissions
     */
    protected Document(String key, String fileType, String title, String url, Info info, Permissions permissions) {
      super();
      this.fileType = fileType;
      this.key = key;
      this.title = title;
      this.url = url;
      this.info = info;
      this.permissions = permissions;
    }

    /**
     * For user.
     *
     * @param id the id
     * @param firstName the first name
     * @param lastName the last name
     * @param url the url
     * @return the document
     */
    protected Document forUser(String id, String firstName, String lastName, String url) {
      return new Document(key, fileType, title, url, info, permissions);
    }

    /**
     * Gets the file type.
     *
     * @return the fileType
     */
    public String getFileType() {
      return fileType;
    }

    /**
     * Gets the key.
     *
     * @return the key
     */
    public String getKey() {
      return key;
    }

    /**
     * Gets the title.
     *
     * @return the title
     */
    public String getTitle() {
      return title;
    }

    /**
     * Gets the url.
     *
     * @return the url
     */
    public String getUrl() {
      return url;
    }

    /**
     * Gets the info.
     *
     * @return the info
     */
    public Info getInfo() {
      return info;
    }

    /**
     * Gets the permissions.
     *
     * @return the permissions
     */
    public Permissions getPermissions() {
      return permissions;
    }

  }

  /**
   * The Class Editor.
   */
  public static class Editor {

    /**
     * The Class User.
     */
    public static class User {
      
      /** The id. */
      protected final String     id;

      /** The firstname. */
      protected final String     firstname;

      /** The lastname. */
      protected final String     lastname;

      /** The username. */
      protected final String     username;

      /** The lock token. */
      protected transient String lockToken;

      /**
       * Instantiates a new user.
       *
       * @param id the id
       * @param firstname the firstname
       * @param lastname the lastname
       */
      protected User(String id, String firstname, String lastname) {
        super();
        this.id = id;
        this.username = id;
        this.firstname = firstname;
        this.lastname = lastname;
      }

      /**
       * Gets the id.
       *
       * @return the id
       */
      public String getId() {
        return id;
      }

      /**
       * Gets the username.
       *
       * @return the username
       */
      public String getUsername() {
        return username;
      }

      /**
       * Gets the firstname.
       *
       * @return the firstname
       */
      public String getFirstname() {
        return firstname;
      }

      /**
       * Gets the lastname.
       *
       * @return the lastname
       */
      public String getLastname() {
        return lastname;
      }

      /**
       * Gets the lock token.
       *
       * @return the lockToken
       */
      protected String getLockToken() {
        return lockToken;
      }

      /**
       * Sets the lock token.
       *
       * @param lockToken the lockToken to set
       */
      protected void setLockToken(String lockToken) {
        this.lockToken = lockToken;
      }

    }

    /** The callback url. */
    protected final String callbackUrl;

    /** The lang. */
    protected final String lang;

    /** The mode. */
    protected final String mode;

    /** The user. */
    protected final User   user;

    /**
     * Instantiates a new editor.
     *
     * @param callbackUrl the callback url
     * @param lang the lang
     * @param mode the mode
     * @param user the user
     */
    protected Editor(String callbackUrl, String lang, String mode, User user) {
      super();
      this.callbackUrl = callbackUrl;
      this.lang = lang;
      this.mode = mode;
      this.user = user;
    }

    /**
     * Gets the callback url.
     *
     * @return the callbackUrl
     */
    public String getCallbackUrl() {
      return callbackUrl;
    }

    /**
     * Gets the lang.
     *
     * @return the lang
     */
    public String getLang() {
      return lang;
    }

    /**
     * Gets the mode.
     *
     * @return the mode
     */
    public String getMode() {
      return mode;
    }

    /**
     * Gets the user.
     *
     * @return the user
     */
    public User getUser() {
      return user;
    }

    /**
     * For user.
     *
     * @param id the id
     * @param firstName the first name
     * @param lastName the last name
     * @param lang the lang
     * @param callbackUrl the callback url
     * @return the editor
     */
    protected Editor forUser(String id, String firstName, String lastName, String lang, String callbackUrl) {
      User otherUser = new User(id, firstName, lastName);
      // FYI locks maintenance will introduce complex logic
      // simpler: each user may contain own lock token only, but don't rely on others
      // otherUser.setLockToken(user.getLockToken());
      return new Editor(callbackUrl, lang, mode, otherUser);
    }
  }

  /**
   * Editor.
   *
   * @param documentserverUrl the documentserver url
   * @param workspace the workspace
   * @param path the path
   * @param documentType the document type
   * @return the builder
   */
  protected static Builder editor(String documentserverUrl, String workspace, String path, String documentType) {
    return new Builder(documentserverUrl, documentType, workspace, path);
  }

  /**
   * File url.
   *
   * @param platformUrl the platform url
   * @param userId the user id
   * @param key the key
   * @return the string
   */
  protected static String fileUrl(String platformUrl, String userId, String key) {
    return new StringBuilder(platformUrl).append("/content/").append(userId).append("/").append(key).toString();
  }

  /**
   * Callback url.
   *
   * @param platformUrl the platform url
   * @param userId the user id
   * @param key the key
   * @return the string
   */
  protected static String callbackUrl(String platformUrl, String userId, String key) {
    return new StringBuilder(platformUrl).append("/status/").append(userId).append("/").append(key).toString();
  }

  /** The documentserver js url. */
  protected final String      documentserverUrl, documentserverJsUrl;

  /** The platform url. */
  protected final String      platformUrl;

  /** The workspace. */
  protected final String      workspace;

  /** The path. */
  protected final String      path;

  /** The document type. */
  protected final String      documentType;

  /** The document. */
  protected final Document    document;

  /** The editor config. */
  protected final Editor      editorConfig;

  /** The error. */
  protected String            error;

  /** The node. */
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
   * @param documentserverUrl the documentserver url
   * @param platformUrl the platform url
   * @param workspace the workspace
   * @param path the path
   * @param documentType the document type
   * @param document the document
   * @param editor the editor
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
   * Gets the documentserver js url.
   *
   * @return the documentserverJsUrl
   */
  public String getDocumentserverJsUrl() {
    return documentserverJsUrl;
  }

  /**
   * Gets the documentserver url.
   *
   * @return the documentserverUrl
   */
  public String getDocumentserverUrl() {
    return documentserverUrl;
  }

  /**
   * Gets the context node.
   *
   * @return the node in context, can be <code>null</code>
   */
  public Node getContextNode() {
    return node;
  }

  /**
   * Sets the context node.
   *
   * @param node the node to set
   */
  protected void setContextNode(Node node) {
    this.node = node;
  }

  /**
   * Gets the workspace.
   *
   * @return the workspace
   */
  public String getWorkspace() {
    return workspace;
  }

  /**
   * Gets the path.
   *
   * @return the path
   */
  public String getPath() {
    return path;
  }

  /**
   * Gets the document type.
   *
   * @return the documentType
   */
  public String getDocumentType() {
    return documentType;
  }

  /**
   * Gets the document.
   *
   * @return the config
   */
  public Document getDocument() {
    return document;
  }

  /**
   * Gets the editor config.
   *
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

  /**
   * Checks if is created.
   *
   * @return true, if is created
   */
  public boolean isCreated() {
    return open == null;
  }

  /**
   * Checks if is open.
   *
   * @return true, if is open
   */
  public boolean isOpen() {
    return open != null ? open.booleanValue() : false;
  }

  /**
   * Checks if is closed.
   *
   * @return true, if is closed
   */
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

  /**
   * Sets the error.
   *
   * @param error the new error
   */
  public void setError(String error) {
    this.error = error;
  }

  /**
   * Checks for error.
   *
   * @return true, if successful
   */
  public boolean hasError() {
    return this.error != null;
  }

  /**
   * Gets the error.
   *
   * @return the error
   */
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
