// Main routine for Daikon invariant detector

package daikon;

import daikon.split.*;
import daikon.inv.Invariant;

import java.util.*;
import java.io.*;

import org.apache.oro.text.regex.*;
import gnu.getopt.*;
import utilMDE.*;

public final class Daikon {
  public static final String lineSep = Global.lineSep;

  // All these variables really need to be organized better.

  public final static boolean disable_splitting = false;
  // public final static boolean disable_splitting = true;

  public final static boolean disable_ternary_invariants = false;
  // public final static boolean disable_ternary_invariants = true;

  // Change this at your peril; high costs in time and space for "false",
  // because so many more invariants get instantiated.
  public final static boolean check_program_types = true;
  // public final static boolean check_program_types = false;

  // If true, variables of declared type byte or char will be compared
  // to variables of declared type int.
  public final static boolean compare_byte_char_to_integer = false;

  // Problem with setting this to true:
  //  get no invariants over any value that can ever be missing
  // Problem with setting this to false:
  //  due to differrent number of samples, IsEqualityComparison is
  //  non-transitive (that is specially handled in the code)
  public final static boolean invariants_check_canBeMissing = false;
  // public final static boolean invariants_check_canBeMissing = true;

  // Specialized version for array elements; only examined if
  // invariants_check_canBeMissing is false
  // public final static boolean invariants_check_canBeMissing_arrayelt = false;
  public final static boolean invariants_check_canBeMissing_arrayelt = true;

  public final static boolean disable_modbit_check_message = false;
  // Not a good idea to set this to true, as it is too easy to ignore the
  // warnings and the modbit problem can cause an error later.
  public final static boolean disable_modbit_check_error = false;

  // When true, don't print textual output.
  public static boolean no_text_output = false;

  // When true, don't print invariants when their controlling ppt
  // already has them.  For example, this is the case for invariants
  // in public methods which are already given as part of the object
  // invariant for that class.
  public static boolean suppress_implied_controlled_invariants = false;
  // public static boolean suppress_implied_controlled_invariants = true;

  // When true, don't print EXIT invariants over strictly orig()
  // variables when the corresponding entry ppt already has the
  // invariant.
  public static boolean suppress_implied_postcondition_over_prestate_invariants = false;
  // public static boolean suppress_implied_postcondition_over_prestate_invariants = false;

  // When true, use the Simplify theorem prover (not part of Daikon)
  // to locate logically redundant invariants, and flag them as
  // redundant, so that they are removed from the printed output.
  public static boolean suppress_redundant_invariants_with_simplify = false;
  // public static boolean suppress_redundant_invariants_with_simplify = true;

  // Set what output style to use.  NORMAL is the default; ESC style
  // is based on JML; SIMPLIFY style uses first order logical
  // expressions with lots of parens
  public static final int OUTPUT_STYLE_NORMAL = 0;
  public static final int OUTPUT_STYLE_ESC = 1;
  public static final int OUTPUT_STYLE_SIMPLIFY = 2;
  public static int output_style = OUTPUT_STYLE_NORMAL;
  // public static int output_style = OUTPUT_STYLE_ESC;
  // public static int output_style = OUTPUT_STYLE_SIMPLIFY;

  // When true, output numbers of values and samples (also names of variables)
  public static boolean output_num_samples = false;

  public static boolean ignore_comparability = false;

  // Controls which program points/variables are used/ignored.
  public static Pattern ppt_regexp;
  public static Pattern ppt_omit_regexp;
  public static Pattern var_omit_regexp;

  // I appear to need both of these variables.  (Or do I?)
  public static FileOutputStream inv_ostream;
  public static ObjectOutputStream inv_oostream;

  // Public so other programs can reuse the same command-line options
  public static final String help_SWITCH = "help";
  public static final String ppt_regexp_SWITCH = "ppt";
  public static final String ppt_omit_regexp_SWITCH = "ppt_omit";
  public static final String var_omit_regexp_SWITCH = "var_omit";
  public static final String no_text_output_SWITCH = "no_text_output";
  public static final String suppress_cont_SWITCH = "suppress_cont";
  public static final String suppress_post_SWITCH = "suppress_post";
  public static final String suppress_redundant_SWITCH = "suppress_redundant";
  public static final String prob_limit_SWITCH = "prob_limit";
  public static final String esc_output_SWITCH = "esc_output";
  public static final String simplify_output_SWITCH = "simplify_output";
  public static final String output_num_samples_SWITCH = "output_num_samples";

  static String usage =
    UtilMDE.join(new String[] {
      "Daikon invariant detector.",
      "Copyright 1998-2001 by Michael Ernst <mernst@lcs.mit.edu>",
      "Usage:",
      "    java daikon.Daikon [flags...] files...",
      "  Each file is a declaration file or a data trace file; the file type",
      "  is determined by the file name (containing \".decls\" or \".dtrace\").",
      "  For a list of flags, see the Daikon manual, which appears in the ",
      "  Daikon distribution and also at http://sdg.lcs.mit.edu/daikon/."},
                 lineSep);

  /**
   * The arguments to daikon.Daikon are file names; declaration file names end
   * in ".decls" and data trace file names end in ".dtrace".
   **/
  public static void main(String[] args) {
    Set decl_files = new HashSet();
    Set dtrace_files = new HashSet();

    if (args.length == 0) {
      System.out.println("Daikon error: no files supplied on command line.");
      System.out.println(usage);
      System.exit(1);
    }

    LongOpt[] longopts = new LongOpt[] {
      new LongOpt(help_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(ppt_regexp_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(ppt_omit_regexp_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(var_omit_regexp_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(no_text_output_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(suppress_cont_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(suppress_post_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(suppress_redundant_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(prob_limit_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(esc_output_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(simplify_output_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(output_num_samples_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
    };
    Getopt g = new Getopt("daikon.Daikon", args, "ho:", longopts);
    int c;
    while ((c = g.getopt()) != -1) {
      switch(c) {
      case 0:
	// got a long option
	String option_name = longopts[g.getLongind()].getName();
        if (help_SWITCH.equals(option_name)) {
          System.out.println(usage);
          System.exit(1);
        } else if (ppt_regexp_SWITCH.equals(option_name)) {
          if (ppt_regexp != null)
            throw new Error("multiple --" + ppt_regexp_SWITCH
                            + " regular expressions supplied on command line");
          try {
            String regexp_string = g.getOptarg();
            // System.out.println("Regexp = " + regexp_string);
            ppt_regexp = Global.regexp_compiler.compile(regexp_string);
          } catch (Exception e) {
            throw new Error(e.toString());
          }
          break;
        } else if (ppt_omit_regexp_SWITCH.equals(option_name)) {
          if (ppt_omit_regexp != null)
            throw new Error("multiple --" + ppt_omit_regexp_SWITCH
                            + " regular expressions supplied on command line");
          try {
            String regexp_string = g.getOptarg();
            // System.out.println("Regexp = " + regexp_string);
            ppt_omit_regexp = Global.regexp_compiler.compile(regexp_string);
          } catch (Exception e) {
            throw new Error(e.toString());
          }
          break;
        } else if (var_omit_regexp_SWITCH.equals(option_name)) {
          if (var_omit_regexp != null)
            throw new Error("multiple --" + var_omit_regexp_SWITCH
                            + " regular expressions supplied on command line");
          try {
            String regexp_string = g.getOptarg();
            // System.out.println("Regexp = " + regexp_string);
            var_omit_regexp = Global.regexp_compiler.compile(regexp_string);
          } catch (Exception e) {
            throw new Error(e.toString());
          }
          break;
	} else if (no_text_output_SWITCH.equals(option_name)) {
	  no_text_output = true;
	} else if (suppress_cont_SWITCH.equals(option_name)) {
	  suppress_implied_controlled_invariants = true;
	} else if (suppress_post_SWITCH.equals(option_name)) {
	  suppress_implied_postcondition_over_prestate_invariants = true;
	} else if (suppress_redundant_SWITCH.equals(option_name)) {
	  suppress_redundant_invariants_with_simplify = true;
	} else if (prob_limit_SWITCH.equals(option_name)) {
	  Invariant.probability_limit = 0.01 * Double.parseDouble(g.getOptarg());
	} else if (esc_output_SWITCH.equals(option_name)) {
	  output_style = OUTPUT_STYLE_ESC;
	} else if (simplify_output_SWITCH.equals(option_name)) {
	  output_style = OUTPUT_STYLE_SIMPLIFY;
	} else if (output_num_samples_SWITCH.equals(option_name)) {
	  output_num_samples = true;
	} else {
	  throw new RuntimeException("Unknown long option received: " + option_name);
	}
	break;
      case 'h':
        System.out.println(usage);
        System.exit(1);
        break;
      case 'o':
        if (inv_ostream != null)
          throw new Error("multiple serialization output files supplied on command line");
        try {
          String inv_filename = g.getOptarg();
          System.out.println("Inv filename = " + inv_filename);
          inv_ostream = new FileOutputStream(inv_filename);
          inv_oostream = new ObjectOutputStream(inv_ostream);
          // This sends the header immediately; irrelevant for files.
          // inv_oostream.flush();
        } catch (Exception e) {
          throw new Error(e.toString());
        }
        break;
        //
      case '?':
        break; // getopt() already printed an error
        //
      default:
        System.out.print("getopt() returned " + c + lineSep);
        break;
      }
    }

    // First check that all the file names are OK, so we don't do lots of
    // processing only to bail out at the end.
    for (int i=g.getOptind(); i<args.length; i++) {
      String arg = args[i];
      // These aren't "endsWith()" because there might be a suffix on the end
      // (eg, a date).
      if (! new File(arg).exists()) {
        throw new Error("File " + arg + " not found.");
      }
      if (arg.indexOf(".decls") != -1) {
        decl_files.add(arg);
      } else if (arg.indexOf(".dtrace") != -1) {
        dtrace_files.add(arg);
      } else {
        throw new Error("Unrecognized argument: " + arg);
      }
    }

    PptMap all_ppts = new PptMap();

    int num_decl_files = decl_files.size();
    int num_dtrace_files = dtrace_files.size();

    try {
      System.out.print("Reading declaration files ");
      FileIO.read_declaration_files(decl_files, all_ppts);
      System.out.println();
      add_combined_exits(all_ppts);

      System.out.print("Reading data trace files ");
      FileIO.read_data_trace_files(dtrace_files, all_ppts);
      System.out.println();
    } catch (IOException e) {
      System.out.println();
      e.printStackTrace();
      throw new Error(e.toString());
    }
    // Jikes complains when I put this all in one big string.
    System.out.print("Read " + num_decl_files + " declaration file" +
                       ((num_decl_files == 1) ? "" : "s"));
    System.out.print(", " + num_dtrace_files + " dtrace file");
    System.out.println(((num_dtrace_files == 1) ? "" : "s") + ".");

    // Old location; but we want to add these before reading trace files.
    // add_combined_exits(all_ppts);

    // Retrieve Ppt objects in sorted order.
    // Use a custom comparator for a specific ordering
    Comparator comparator = new Ppt.NameComparator();
    TreeSet all_ppts_sorted = new TreeSet(comparator);
    all_ppts_sorted.addAll(all_ppts.asCollection());
    for (Iterator itor = all_ppts_sorted.iterator() ; itor.hasNext() ; ) {
      PptTopLevel ppt = (PptTopLevel) itor.next();
      if (ppt.has_samples()) {
        // System.out.println(ppt.name + ": " + ppt.num_samples() + " samples, "
        //                    + ppt.num_values() + " values, "
        //                    + "ratio = " + ((double)ppt.num_samples()) / ((double)ppt.num_values()));
        // System.out.println("start dump-----------------------------------------------------------------");
        // System.out.println(ppt.name);
        // ppt.values.dump();
        // System.out.println("end dump-------------------------------------------------------------------");
        ppt.initial_processing();
        if (! disable_splitting) {
          Splitter[] pconds = SplitterList.get(ppt.name);
          if (Global.debugPptSplit)
            System.out.println("Got " + ((pconds == null)
                                         ? "no"
                                         : Integer.toString(pconds.length))
                               + " splitters for " + ppt.name);
          if (pconds != null)
            ppt.addConditions(pconds);
        }
        ppt.addImplications();
        {
          // Clear memory
          ppt.set_values_null();
          ppt.clear_view_caches();
          for (int i=0; i<ppt.views_cond.size(); i++) {
            PptConditional pcond = (PptConditional) ppt.views_cond.elementAt(i);
            pcond.set_values_null();
            pcond.clear_view_caches();
          }
        }
      }
    }

    if (suppress_redundant_invariants_with_simplify) {
      System.out.print("Invoking Simplify to identify redundant invariants... ");
      System.out.flush();
      long start = System.currentTimeMillis();
      for (Iterator itor = all_ppts_sorted.iterator() ; itor.hasNext() ; ) {
	PptTopLevel ppt = (PptTopLevel) itor.next();
	ppt.mark_implied_via_simplify();
      }
      long end = System.currentTimeMillis();
      double elapsed = (end - start) / 1000.0;
      System.out.println((new java.text.DecimalFormat("#.#")).format(elapsed) + "s");
    }

    print_invariants(all_ppts);

    if (output_num_samples) {
      Global.output_statistics();
    }

    // Old implementation that didn't interleave invariant inference and
    // reporting.
    // for (Iterator itor = all_ppts.values().iterator() ; itor.hasNext() ; ) {
    //   PptTopLevel ppt = (PptTopLevel) itor.next();
    //   ppt.initial_processing();
    // }
    // // Now examine the invariants.
    // System.out.println("Examining the invariants.");
    // for (Iterator itor = new TreeSet(all_ppts.keySet()).iterator() ; itor.hasNext() ; ) {
    //   String ppt_name = (String) itor.next();
    //   PptTopLevel ppt_tl = (PptTopLevel) all_ppts.get(ppt_name);
    //   ppt_tl.print_invariants_maybe();
    // }

    if (inv_ostream != null) {
      try {
        inv_oostream.writeObject(all_ppts);
        inv_oostream.flush();
        inv_oostream.close();
        // inv_ostream.close();    // is this necessary?
      } catch (IOException e) {
        e.printStackTrace();
        throw new Error(e.toString());
      }
    }

    System.out.println("Exiting");

  }

  public static void print_invariants(PptMap ppts) {
    // Retrieve Ppt objects in sorted order.
    // Use a custom comparator for a specific ordering
    Comparator comparator = new Ppt.NameComparator();
    TreeSet ppts_sorted = new TreeSet(comparator);
    ppts_sorted.addAll(ppts.asCollection());

    for (Iterator itor = ppts_sorted.iterator() ; itor.hasNext() ; ) {
      PptTopLevel ppt = (PptTopLevel) itor.next();
      if (ppt.has_samples() && ! no_text_output) {
        ppt.print_invariants_maybe(System.out, ppts);
      }
    }
  }

  public static void add_combined_exits(PptMap ppts) {
    // For each collection of related :::EXITnn ppts, add a new ppt (which
    // will only contain implication invariants).

    Vector new_ppts = new Vector();
    for (Iterator itor = ppts.iterator() ; itor.hasNext() ; ) {
      PptTopLevel enter_ppt = (PptTopLevel) itor.next();
      Vector exits = enter_ppt.exit_ppts;
      if (exits.size() > 1) {
        Assert.assert(enter_ppt.ppt_name.isEnterPoint());
        String exit_name = enter_ppt.ppt_name.makeExit().getName();
        Assert.assert(ppts.get(exit_name) == null);
        VarInfo[] comb_vars = VarInfo.arrayclone_simple(Ppt.common_vars(exits));
        PptTopLevel exit_ppt = new PptTopLevel(exit_name, comb_vars);
        // {
        //   System.out.println("Adding " + exit_ppt.name + " because of multiple expt_ppts for " + enter_ppt.name + ":");
        //   for (int i=0; i<exits.size(); i++) {
        //     Ppt exit = (Ppt) exits.elementAt(i);
        //     System.out.print(" " + exit.name);
        //   }
        //   System.out.println();
        // }
        new_ppts.add(exit_ppt);
        exit_ppt.entry_ppt = enter_ppt;
        exit_ppt.set_controlling_ppts(ppts);
        for (Iterator exit_itor=exits.iterator() ; exit_itor.hasNext() ; ) {
          PptTopLevel line_exit_ppt = (PptTopLevel) exit_itor.next();
          line_exit_ppt.controlling_ppts.add(exit_ppt);
          VarInfo[] line_vars = line_exit_ppt.var_infos;
          line_exit_ppt.combined_exit = exit_ppt;
          {
            // Indices will be used to extract values from a ValueTuple.
            // ValueTuples do not include static constant values.
            // comb_index doesn't include those, either.

            int[] indices = new int[comb_vars.length];
            int new_len = indices.length;
            int new_index = 0;
            int comb_index = 0;
            for (int lv_index=0; lv_index<line_vars.length; lv_index++) {
              if (line_vars[lv_index].isStaticConstant()) {
                continue;
              }
              if (line_vars[lv_index].name == comb_vars[comb_index].name) {
                indices[new_index] = comb_index;
                new_index++;
              }
              comb_index++;
            }
            line_exit_ppt.combined_exit_var_indices = indices;
            Assert.assert(new_index == new_len);

            // System.out.println("combined_exit_var_indices " + line_exit_ppt.name);
            // for (int i=0; i<indices.length; i++) {
            //   // System.out.print(" " + indices[i]);
            //   System.out.print(" " + indices[i] + " " + comb_vars[indices[i]].name);
            // }
            // System.out.println();
          }
        }

        // exit_ppt.num_samples = enter_ppt.num_samples;
        // exit_ppt.num_values = enter_ppt.num_values;
      }
    }

    // System.out.println("add_combined_exits: " + new_ppts.size() + " " + new_ppts);

    // Avoid ConcurrentModificationException by adding after the above loop
    for (int i=0; i<new_ppts.size(); i++) {
      ppts.add((PptTopLevel) new_ppts.elementAt(i));
    }

  }

}
