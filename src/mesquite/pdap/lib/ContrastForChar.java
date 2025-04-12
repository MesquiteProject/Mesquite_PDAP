/* PDAP:PDTREE package for Mesquite  copyright 2001-2009 P. Midford & W. MaddisonPDAP:PDTREE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.The web site for PDAP:PDTREE is http://mesquiteproject.org/pdap_mesquite/This source code and its compiled class files are free and modifiable under the terms of GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html) */package mesquite.pdap.lib;import mesquite.lib.*;import mesquite.lib.characters.CharacterDistribution;import mesquite.lib.duties.*;import mesquite.lib.tree.Tree;/* ======================================================================== *//**Suppliies numbers for each node of a tree.*/public abstract class ContrastForChar extends NumbersForNodesAndChar {    // These are the contrast options defined for setOption    public static final int CONTRAST = 0;    public static final int ABSCONTRAST = 1;    public static final int RAWCONTRAST = 2;    public static final int SD =  3;    public static final int VPRIME = 4;    public static final int NODEVALUE = 5;    public static final int CORRECTEDHEIGHT = 6;    public static final int TIPS = 7;    public static final int RESIDUAL = 8;    public static final int ABSRESIDUAL = 9;    public static final int RAWHEIGHT = 10;    public Class getDutyClass() {        return ContrastForChar.class;    }    public String getDutyName() {        return "Contrasts on Tree using a Character Distribution";    }    public abstract double[] calculateStandardDeviations(Tree tree);    public abstract void calculateNumbers(Tree tree, CharacterDistribution charDistribution, NumberArray result, MesquiteString resultString);    public abstract void setOption(int option);    public abstract void setOptionWithChangeParameters(int option);    public abstract void setXCharForReport(int xChar);    public abstract void setYCharForReport(int yChar);    public abstract void takeControl(boolean take);    public abstract String getItemName();}