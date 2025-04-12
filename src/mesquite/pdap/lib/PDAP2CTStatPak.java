/* PDAP:PDTREE package for Mesquite  copyright 2001-2009 P. Midford & W. Maddison
PDAP:PDTREE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
The web site for PDAP:PDTREE is http://mesquiteproject.org/pdap_mesquite/

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */
package mesquite.pdap.lib;

import mesquite.cont.lib.ContinuousDistribution;
import mesquite.lib.*;
import mesquite.lib.tree.MesquiteTree;
import mesquite.lib.tree.Tree;
import JSci.maths.statistics.TDistribution;

public abstract class PDAP2CTStatPak extends PDAPStatPak {

    // drawing options
    public static final int ROOTCI  = 0;
    public static final int REGRESSIONCIPI = 1;

    private static final int INITINTEGER = MesquiteInteger.unassigned;
    private static final double INITDOUBLE = MesquiteDouble.unassigned;

    protected TDistribution       	    tStats = null;                                  //an object for calculating t statistics from jsci[Numerical Recipes?]

    protected NoPolyTree		  		tree = null;			 	                   //putting the tree here and handling the contrast calculations
    //internally seems to be the only way to implement 
    protected ContinuousDistribution    obs1 = null;	           //more caching
    protected ContinuousDistribution    obs2 = null;	           //more caching
    protected int                       dfReduce = INITINTEGER;    //User specified amount to reduce degrees of freedom
    protected double[]   		        ci;  	  	               //Confidence intervals around root reconstruction
    protected double[]			        se;
    protected double  			        sumresid = INITDOUBLE;     //sum of regression residuals
    protected double     		        slopeCI = INITDOUBLE;      //Confidence interval around least squares slope
    protected double     		        interCI = INITDOUBLE;      //Confidence interval around least squares intercept
    protected double 			        slopeSE = INITDOUBLE;      //Standard error of least squares slope
    protected double     		        interSE = INITDOUBLE;	   //Standard error of least squares intercept
    protected double                    v1 = INITDOUBLE;           // branch length adjustment factor 
    protected double     		        v2 = INITDOUBLE;           // branch length adjustment factor
    protected double  			        v_adjust = INITDOUBLE;     // == (v1+v2)/(v1*v2)
    protected double     		        x2Sum = INITDOUBLE;
    protected double 			        sigma2y = INITDOUBLE;
    protected double     		        Vh = INITDOUBLE;		      //height of added tips for PI calculations 
    protected int				        polytomies = INITINTEGER; 	  //number of polytomies (ZLB's) in the tree
    protected boolean                   ignoreRootTritomies = false;  // if true, a tritomy at the root won't be counted as a polytomy

    public PDAP2CTStatPak() {
        super();
        ci = new double[] {INITDOUBLE, INITDOUBLE}; 
        se = new double[] {INITDOUBLE, INITDOUBLE};
    }

    /*-----------------------------------------*/
    /** Set the tree so we can do the contrast calculations internally 
     *  @param tree used for calculations  
     */
    public void setTree(Tree tree) {
        if (tree instanceof NoPolyTree)
            this.tree = (NoPolyTree)tree;
        else 
            this.tree = new NoPolyTree((MesquiteTree)tree);
    }

    /*-----------------------------------------*/
    /** Set the dataset for the first variable 
     *  @param observed data for the first trait 
     */
    public void setObserved1(ContinuousDistribution observed) {
        obs1 = observed;
    }

    /*-----------------------------------------*/
    /** Set the dataset for the second variable 
     *  @param observed data for the second trait 
     */
    public void setObserved2(ContinuousDistribution observed) {
        obs2 = observed;
    }

    // Although these are currently only used with RootStats (a PDAP2CTStatPak), there is nothing
    // necessarily preventing confidence interval stats, so these are moved up here.  PEM 21 June 2006
    protected double width1 = 0.95;          //Confidence/prediction interval width
    protected double width2 = 0.90;          //Second confidence/prediction interval width

    //The following were moved here along with the corresponding fields.

    
    /** Return the value of the first CI/PI width, which is also used in the
     *  plots in CI/PI plot (screen 9B) 
     *  return width of first interval
     *  @return first width (wider, defaults to 95%)
     */
    public double getWidth1(){
        return width1;
    }

    /**
     * 
     * @return second width (narrower, defaults to 90%)
     */
    public double getWidth2(){
        return width2;
    }
    
    
    /*-----------------------------------------*/
    /** Since the CI/PI widths are never actually used outside the StatPak, the
     * method to allow the user to set them is here too. 
     * @param w - the value assigned to the CI/PI width, which is also used in the 
     * plots in screens 9A and 9B. Ought to be wider than width2
     */
    public void setWidth1(double w){
            width1 = w;         
    }
    
    /**
     * 
     * @param w2 second width, ought to be narrower than w1
     */
    public void setWidth2(double w2){
        width2 = w2;
    }

    /**
     * 
     * @return state of the the ignoreRootTritomies flag
     */
    public boolean getIgnoreRootTritomies(){
        return ignoreRootTritomies;
    }

    /**
     * 
     * @param irt desired value of ignoreRootTritomies flag
     */
    public void setIgnoreRootTritomies(boolean irt){
        ignoreRootTritomies = irt;
    }

    /*-----------------------------------------*/
    /** Vh is used for calculating a prediction interval for an added tip taxon.  It
  	 *  is the length of the branch connecting the added tip (directly) to the root. 
  	 *  @return value of Vh in statpak
  	 */
    public double getVh(){
        return Vh;
    }

    /*-----------------------------------------*/
    /** Vh is used for calculating a prediction interval for an added tip taxon.  It
  	 *  is the length of the branch connecting the added tip (directly) to the root. 
  	 *  @param user requested Vh value
  	 */
    public void setVh(double tmp){
        if (MesquiteDouble.isCombinable(tmp))
            Vh = tmp;	
    }	

    /*-----------------------------------------*/
    /** 
     * @return the user specified number of degrees of freedom reduction.  
     */	
    public int getDFReduction() {
        return dfReduce; 
    }

    /*-----------------------------------------*/
    /** This sets both the user specified reduction in degrees of freedom 
     *  @param r user requested reduction
     * */
    public void setDFReduction(int r){
        if (MesquiteInteger.isCombinable(r)) {
            dfReduce = r;
        }
    }

    /**
     * @return the number of degrees of freedom in the statpak (not reduced)
     */
    public int getDegreesOfFreedom(){
        return df;
    }

    /**
     * 
     * @return count of polytomies set by a prior call to countZLBs
     */
    public int getPolytomies(){
        return polytomies;
    }

    /** 
     * @return array of calculated confidence intervals for each trait
     */
    public double getCI(int which){
        if (which >= 0 && which < 2)
            return ci[which];
        return MesquiteDouble.unassigned;
    }


    /*-----------------------------------------*/

    /*-----------------------------------------*/
    /** This method counts the zero-length-branches in the tree.  Because this is PDAP,
     *  it needs to look for zero length branches, rather than polytomies.  To make sure
     *  this works, the method checks that it is getting an instance of NoPolyTree. 
     *  @param tree tree to count
     *  @return number of zero length branches
     */
    public int countZLBs(Tree tree) {
        if (!(tree instanceof NoPolyTree)) {
            MesquiteMessage.warnProgrammer("Tree is not a NoPolyTree - counting 0 polytomies");
            return 0;
        }
        polytomies = ((NoPolyTree)tree).countZLBs(ignoreRootTritomies); 
        return polytomies;
    }


    /**
     * This specifies how to generate the string that appears in text portions of screens with confidence intervals
     * @param lineEnding for ending limits
     * @param tableDelimiter for delimiting table columns
     * @param convertSpaces true to convert spaces to '_' characters
     * @return confidence interval summary string
     */
    public abstract String CIString(String lineEnding, String tableDelimiter,boolean convertSpaces);

}
