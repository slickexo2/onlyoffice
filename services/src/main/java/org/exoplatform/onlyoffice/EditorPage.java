package org.exoplatform.onlyoffice;

// TODO: Auto-generated Javadoc
/**
 * The Class EditorPage.
 */
public class EditorPage {

  /** The comment. */
  protected String comment;
  
  /** The rename allowed. */
  protected Boolean renameAllowed;
  
  /** The display path. */
  protected String displayPath;


  /**
   * Instantiates a new editor page.
   *
   * @param comment the comment
   * @param renameAllowed the rename allowed
   * @param displayPath the display path
   */
  public EditorPage(String comment, Boolean renameAllowed, String displayPath) {
    super();
    this.comment = comment;
    this.renameAllowed = renameAllowed;
    this.displayPath = displayPath;
  }

  /**
   * Instantiates a new editor page.
   */
  public EditorPage() {
  }

  /**
   * Gets the comment.
   *
   * @return the comment
   */
  public String getComment() {
    return comment;
  }

  /**
   * Sets the comment.
   *
   * @param comment the new comment
   */
  public void setComment(String comment) {
    this.comment = comment;
  }

  /**
   * Gets the rename allowed.
   *
   * @return the rename allowed
   */
  public Boolean getRenameAllowed() {
    return renameAllowed;
  }

  /**
   * Sets the rename allowed.
   *
   * @param renameAllowed the new rename allowed
   */
  public void setRenameAllowed(Boolean renameAllowed) {
    this.renameAllowed = renameAllowed;
  }

  /**
   * Gets the display path.
   *
   * @return the display path
   */
  public String getDisplayPath() {
    return displayPath;
  }

  /**
   * Sets the display path.
   *
   * @param displayPath the new display path
   */
  public void setDisplayPath(String displayPath) {
    this.displayPath = displayPath;
  }
 
}
