package daikon.inv;

public final class DiscardInfo {
  /**
   * A class used for holding a DiscardCode and a string
   * that contains more detailed information about why an Invariant
   * was discarded, as well as the classname and what would be returned
   * by the Invariant's format() method.
   */

  /**
   * The DiscardCode describing this DiscardInfo.  It should be set to DiscardCode.not_discarded
   * if the Invariant that created this was not being discarded
   */
  private DiscardCode discardCode;

  /** The detailed reason for discard */
  private String discardString;

  /**
   * The String that would have resulted from calling format() on the Invariant being discarded.
   * This does not have to be maintained if the Invariant isn't discarded.
   */
  private String discardFormat;

  /**
   * The className of the Invariant being discarded
   */
  private String className;

  public DiscardInfo() {
    discardCode = DiscardCode.not_discarded;
    discardString = "";
    discardFormat = "";
    className = "";
  }

  public DiscardInfo(String className, String discardFormat, DiscardCode discardCode, String discardString) {
    this.discardCode = discardCode;
    this.discardString = discardString;
    this.discardFormat = discardFormat;
    this.className = className;
  }

  public DiscardInfo(Invariant inv, DiscardCode discardCode, String discardString) {
    this(inv.getClass().getName(), inv.format(), discardCode, discardString);
  }

  public boolean shouldDiscard() {
    return (this.discardCode != DiscardCode.not_discarded);
  }

  public String discardFormat() {
    return this.discardFormat;
  }

  public DiscardCode discardCode() {
    return this.discardCode;
  }

  public String discardString() {
    return this.discardString;
  }

  public String className() {
    return this.className;
  }

  public String format() {
    return discardFormat + "\n" + discardCode + "\n" + discardString;
  }

}
