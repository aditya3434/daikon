package daikon.inv.unary.scalar;

import daikon.*;
import daikon.inv.*;
import daikon.derive.unary.*;
import daikon.inv.unary.sequence.*;

import utilMDE.*;

import java.util.*;

// *****
// Automatically generated from OneOf-cpp.java
// *****

// States that the value is one of the specified values.

// This subsumes an "exact" invariant that says the value is always exactly
// a specific value.  Do I want to make that a separate invariant
// nonetheless?  Probably not, as this will simplify implication and such.

public final class OneOfScalar  extends SingleScalar  implements OneOf {
  final static int LIMIT = 5;	// maximum size for the one_of list
  // Probably needs to keep its own list of the values, and number of each seen.
  // (That depends on the slice; maybe not until the slice is cleared out.
  // But so few values is cheap, so this is quite fine for now and long-term.)

  private long [] elts;
  private int num_elts;

  private boolean is_boolean;
  private boolean is_hashcode;

  OneOfScalar (PptSlice ppt) {
    super(ppt);

    elts = new long [LIMIT];

    num_elts = 0;

    is_boolean = (var().type == ProglangType.BOOLEAN);
    is_hashcode = var().type.isObject() || var().type.isArray();

  }

  public static OneOfScalar  instantiate(PptSlice ppt) {
    return new OneOfScalar (ppt);
  }

  public int num_elts() {
    return num_elts;
  }

  public Object elt() {
    if (num_elts != 1)
      throw new Error("Represents " + num_elts + " elements");

    // Not sure whether interning is necessary (or just returning an Integer
    // would be sufficient), but just in case...
    return Intern.internedLong(elts[0]);

  }

  private void sort_rep() {
    Arrays.sort(elts, 0, num_elts  );
  }

  // Assumes the other array is already sorted
  public boolean compare_rep(int num_other_elts, long [] other_elts) {
    if (num_elts != num_other_elts)
      return false;
    sort_rep();
    for (int i=0; i < num_elts; i++)
      if (elts[i] != other_elts[i]) // elements are interned
        return false;
    return true;
  }

  private String subarray_rep() {
    // Not so efficient an implementation, but simple;
    // and how often will we need to print this anyway?
    sort_rep();
    StringBuffer sb = new StringBuffer();
    sb.append("{ ");
    for (int i=0; i<num_elts; i++) {
      if (i != 0)
        sb.append(", ");
      sb.append( elts[i]  );
    }
    sb.append(" }");
    return sb.toString();
  }

  public String repr() {
    return "OneOfScalar"  + varNames() + ": "
      + "no_invariant=" + no_invariant
      + ", num_elts=" + num_elts
      + ", elts=" + subarray_rep();
  }

  public String format() {
    String varname = var().name ;
    if (num_elts == 1) {

      if (is_boolean) {
        Assert.assert((elts[0] == 0) || (elts[0] == 1));
        return varname + " == " + ((elts[0] == 0) ? "false" : "true");
      } else if (is_hashcode) {
        if (elts[0] == 0) {
          return varname + " == null";
        } else {
          return varname + " has only one value (hashcode=" + elts[0] + ")";
        }
      } else {
        return varname + " == " +  elts[0]  ;
      }

    } else {
      return varname + " one of " + subarray_rep();
    }
  }

  public String format_esc() {

    String varname = var().esc_name ;

    String result = "";

    if (is_boolean) {
      Assert.assert((elts[0] == 0) || (elts[0] == 1));
      return varname + " == " + ((elts[0] == 0) ? "false" : "true");
    } else if (is_hashcode) {
      if (elts[0] == 0) {
        return varname + " == null";
      } else {
        return varname + " has only one value (hashcode=" + elts[0] + ")";
      }
    } else {
      return varname + " == " +  elts[0]  ;
    }

  }

  public void add_modified(long  v, int count) {

    for (int i=0; i<num_elts; i++)
      if (elts[i] == v)
        return;
    if (num_elts == LIMIT) {
      destroy();
      return;
    }

    if ((is_boolean && (num_elts == 1))
        || (is_hashcode && (num_elts > 2))) {
      destroy();
      return;
    }
    if (is_hashcode && (num_elts == 2)) {
      // Permit two object values only if one of them is null
      if ((elts[0] != 0) && (elts[1] != 0)) {
        destroy();
        return;
      }
    }

    elts[num_elts] = v;
    num_elts++;

  }

  protected double computeProbability() {
    // This is not ideal.
    if (num_elts == 0) {
      return Invariant.PROBABILITY_UNKNOWN;

    } else if (is_hashcode && (num_elts > 1)) {
      // This should never happen
      return Invariant.PROBABILITY_UNJUSTIFIED;

    } else {
      return Invariant.PROBABILITY_JUSTIFIED;
    }
  }

  public boolean isObviousImplied() {
    VarInfo v = var();
    if (v.isDerived() && (v.derived instanceof SequenceLength)) {
      SequenceLength sl = (SequenceLength) v.derived;
      if (sl.shift != 0) {
        return true;
      }
    }

    // For every EltOneOf at this program point, see if this variable is
    // an obvious member of that sequence.
    PptTopLevel parent = (PptTopLevel)ppt.parent;
    for (Iterator itor = parent.invariants_iterator(); itor.hasNext(); ) {
      Invariant inv = (Invariant) itor.next();
      if ((inv instanceof EltOneOf) && inv.justified()) {
        VarInfo v2 = inv.ppt.var_infos[0];
        // System.out.println("isObviousImplied: calling Member.isObviousMember(" + v1.name + ", " + v2.name + ")");

        EltOneOf other = (EltOneOf) inv;
        if (num_elts == other.num_elts()) {
          sort_rep();
          if (other.compare_rep(num_elts, elts))
            return true;
        }
      }
    }

    return false;
  }

  public boolean isSameFormula(Invariant o)
  {
    OneOfScalar  other = (OneOfScalar ) o;
    if (num_elts != other.num_elts)
      return false;

    sort_rep();
    other.sort_rep();
    for (int i=0; i < num_elts; i++)
      if (elts[i] != other.elts[i]) // elements are interned
	return false;

    return true;
  }

  public boolean isExclusiveFormula(Invariant o)
  {
    if (o instanceof OneOfScalar ) {
      OneOfScalar  other = (OneOfScalar ) o;

      for (int i=0; i < num_elts; i++) {
        for (int j=0; j < other.num_elts; j++) {
          if (elts[i] == other.elts[j]) // elements are interned
            return false;
        }
      }
      return true;
    }

    // Many more checks can be added here:  against nonzero, modulus, etc.
    if ((o instanceof NonZero) && (num_elts == 1) && (elts[0] == 0)) {
      return true;
    }
    long elts_min = Long.MAX_VALUE;
    long elts_max = Long.MIN_VALUE;
    for (int i=0; i < num_elts; i++) {
      elts_min = Math.min(elts_min, elts[i]);
      elts_max = Math.max(elts_max, elts[i]);
    }
    if ((o instanceof LowerBound) && (elts_max < ((LowerBound)o).min1))
      return true;
    if ((o instanceof UpperBound) && (elts_min > ((UpperBound)o).max1))
      return true;

    return false;
  }

  // Look up a previously instantiated invariant.
  public static OneOfScalar  find(PptSlice ppt) {
    Assert.assert(ppt.arity == 1);
    for (Iterator itor = ppt.invs.iterator(); itor.hasNext(); ) {
      Invariant inv = (Invariant) itor.next();
      if (inv instanceof OneOfScalar )
        return (OneOfScalar ) inv;
    }
    return null;
  }

}
