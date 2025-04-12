/* PDAP:PDTREE package for Mesquite  copyright 2001-2014 P. Midford & W. MaddisonPDAP:PDTREE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.The web site for PDAP:PDTREE is http://mesquiteproject.org/pdap_mesquite/This source code and its compiled class files are free and modifiable under the terms of GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)*/package mesquite.pdap.lib;import mesquite.lib.*;import mesquite.lib.tree.Tree;import mesquite.cont.lib.*;public abstract class ContrastCalculator {	protected Tree tree;	protected ContinuousDistribution states;	protected int numNodes;	protected double [] contrasts = null;	protected double [] rawContrasts = null;	protected double [] x = null;	protected double [] vPrime = null;	protected double [] stdDev = null;	protected double [] height = null;    protected double [] rawHeight = null;	protected double [] weightPassThrough = null;   // needed for parents of nodes with missing data	protected boolean weightedBranches = true;	protected boolean statsOk = true;                    // maybe private    /**     * This calculates adjusted branch lengths and standard deviations of contrasts.  This is recursive, call with node == root.     * @param node specifies node to calculate from.     */	abstract protected void vCalculation(int node);    /**     * This calculates node value reconstructions and contrasts (raw and standardized).  This is recursive, call with node == root.     * @param node specifies node to calculate from.     */	abstract protected void xCalculation(int node);    public abstract double [] fillTips(int node);		public boolean Ok () {		return statsOk;	}		/*.................................................................................................................*/	/** implements the notion of weighted branches for contrast calculations. */	/** this needs to be reworked for missing data or removed */	protected double branchWeight (int node){		double weight;		if (weightedBranches){			if (node == tree.getRoot()){  /*asking for weight across root for unrooted case*///				int left = tree.firstDaughterOfNode(node);//				int right = tree.lastDaughterOfNode(node);//				if (tree.branchLengthUnassigned(left))//					weight=tree.getBranchLength(right);//				else if (tree.branchLengthUnassigned(right))//					weight=tree.getBranchLength(left);//				else//					weight=tree.getBranchLength(left) +tree.getBranchLength(right);				weight = 0.0;			}			else if (tree.branchLengthUnassigned(node))   //give warning?				weight=1.0;			else				weight=tree.getBranchLength(node,1.0); // does same as above			}		else			weight=1.0;					//if (weight==0)  //doing this breaks zero-length branch polytomies		//	weight=1.0;  //give warning?		return(weight);	}		// This pattern appears alot	private void doComponents(){        int root = tree.getRoot();        vCalculation(root);        xCalculation(root);	    	}	/** 	 * This returns the array of contrast values for those modules that need them 	 *  @return double[] - the array of contrast values calculated here 	 */	public double [] contrastCalculation() {	    doComponents();	    return contrasts;	}	    /*......................................................................................................................*/    protected void heightCalculation(double ht, int node, int mode, double[] results) {        double myHeight;        if (tree.nodeIsInternal(node)) {            if (MesquiteDouble.isCombinable(stdDev[node])) { // good data                if (node != tree.getRoot())                    if (mode == ContrastForChar.RAWHEIGHT)                        myHeight = ht + branchWeight(node);                    else                        myHeight = ht + vPrime[node];                else                        myHeight = 0;                results[node] = myHeight;            }            else {                results[node] = MesquiteDouble.unassigned;                if (mode == ContrastForChar.RAWHEIGHT)                    myHeight = ht + branchWeight(node);                else                    myHeight = ht + branchWeight(node);  //vPrime[node];            }            for (int daughter = tree.firstDaughterOfNode(node); tree.nodeExists(daughter); daughter = tree.nextSisterOfNode(daughter))                heightCalculation(myHeight, daughter,mode, results);        }        else {  // a tip            if (MesquiteDouble.isCombinable(x[node])) {                if (mode == ContrastForChar.RAWHEIGHT)                    results[node] = ht + branchWeight(node);                else                    results[node] = ht + vPrime[node];            }            else                 results[node] = MesquiteDouble.unassigned;               }    }    /**      * This also assumes things have been set up.      * @param mode indicates whether raw or corrected heights are requested     */    public double [] heightCalculation(int mode){        final int root = tree.getRoot();        vCalculation(root);        if (mode ==  ContrastForChar.RAWHEIGHT){            heightCalculation(0.0,root,mode,rawHeight);            return rawHeight;        }        else {            heightCalculation(0.0,root,mode,height);            return height;        }    }	/** 	 * This returns the array of raw contrast values for those modules that need them 	 *  @return double[] - the array of raw contrast values calculated here or null for failure 	 */	public double [] rawContrastCalculation() {	    doComponents();		return rawContrasts;	}		/** This returns the array of contrast values for those modules that need them 	@return double[] - the array of raw contrast values calculated here or null for failure */	public double [] nodeValueCalculation() {	    doComponents();		return x;	}		/** This returns the array of contrast values for those modules that need them 	@return double[] - the array of raw contrast values calculated here or null for failure */	public double [] vPrimeCalculation() {		int root = tree.getRoot();		vCalculation(root);		return vPrime;	}	/** This returns the array of contrast values for those modules that need them 	@return double[] - the array of raw contrast values calculated here or null for failure */	public double [] stdDevCalculation() {		int root = tree.getRoot();		vCalculation(root);		return stdDev;	}	/** Just returns the node values (tip and interior), without recalculating */	public double [] getNodeValues() {		return x;	}	/** Just returns the standard deviation array, without recalculating */	public double [] getStdDev() {		return stdDev;	}	/** Just returns the vPrime array, without recalculating */	public double [] getVPrime() {		return vPrime;	}}