/* PDAP:PDTREE package for Mesquite  copyright 2001-2009 P. Midford & W. Maddison
PDAP:PDTREE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
The web site for PDAP:PDTREE is http://mesquiteproject.org/pdap_mesquite/

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */
package mesquite.pdap.lib;

import mesquite.lib.MesquiteDouble;
import mesquite.lib.MesquiteMessage;
import mesquite.lib.MesquiteNumber;
import mesquite.lib.tree.Tree;
import JSci.maths.statistics.TDistribution;

public abstract class PDAPRootStatPak extends PDAP2CTStatPak{

    protected final static double CIPLOTOVERAGE = 0.2;

    protected double[] contrasts1;
    protected double[] contrasts2;
    protected double[] x1;
    protected double[] x2;
    protected double[] vPrime1;

    /** 
     * @return the sum of the regression residuals 
     */
    public double getResidualSum() {
        return sumresid;
    }

    /**
     *  @return the confidence interval around the regression slope 
     */
    public double getSlopeCI() {
        return slopeCI;
    }

    /** 
     * @return the confidence interval around the regression y-intercept 
     */
    public double getInterceptCI() {
        return interCI;
    }

    /** 
     * @return the standard error of the regression slope 
     */
    public double getSlopeSE() {
        return slopeSE;
    }

    /** 
     * @return the standard error of the regression y-intercept 
     */
    public double getInterceptSE() {
        return interSE;
    }

    /** 
     * @return the sum of squares for x 
     */
    public double getX2Sum() {
        return x2Sum;
    }

    /** 
     * @return the sigma-squared y value see Garland Midford and Ives 1999 
     */
    public double getSigmaSquaredY() {
        return sigma2y;
    }


    /*-----------------------------------------*/
    /** 
     * This method sets the maximum and minimum tip values in the appropriate fields of the statPak 
     * @param tree the tree to traverse
     * @param data0 array of trait values for the first (horizontal) trait
     * @param data1 array of trait values for the second (vertical) trait
     * @param node focus of calculation - method recurses on this; call with tree.getRoot();
     */
    private void calcBounds(Tree tree, double [] data0, double [] data1, int node) {
        if (tree.nodeIsInternal(node))
            for (int daughter = tree.firstDaughterOfNode(node); tree.nodeExists(daughter); daughter = tree.nextSisterOfNode(daughter)) {
                calcBounds(tree, data0, data1, daughter);
            }       
        else if (MesquiteDouble.isCombinable(data0[node]) &&
                MesquiteDouble.isCombinable(data1[node])) {
            if (data0[node] > max[0])   
                max[0] = data0[node];
            if (data1[node] > max[1])
                max[1] = data1[node];
            if (data0[node] < min[0])
                min[0] = data0[node];
            if (data1[node] < min[1])
                min[1] = data1[node];
        }
    }   



    /*-----------------------------------------*/
    /** The contempTips flag is true if all tips have the same height from the root. 
     *  @return value of comtempTips flag
     */
    public boolean getContempTips(){
        return contempTips;
    }

    /*-----------------------------------------*/
    /**
     * 
     */
    public double getPValue(){
        return MesquiteDouble.unassigned;
    }

    /**
     * Traverses the tree to count the number of valid contrasts (contrasts1/2 array may include invalid nodes)
     * number field is set
     * @param tree to traverse
     */
    protected void countContrasts(Tree tree){
        number = 0;
        countContrasts1(tree,tree.getRoot());
    }

    /** Recursive helper for countContrasts */
    private void countContrasts1(Tree tree, int node) {
        for (int daughter = tree.firstDaughterOfNode(node); tree.nodeExists(daughter); daughter = tree.nextSisterOfNode(daughter))
            countContrasts1(tree,daughter);
        if (tree.nodeIsInternal(node) && 
                MesquiteDouble.isCombinable(contrasts1[node]) &&
                MesquiteDouble.isCombinable(contrasts2[node])) {
            number++;
        }
    }

    /*-----------------------------------------*/

    /** This returns the appropriate mean, which is the reconstructed root value. */
    public void calcMeans(Tree tree, double [] data1, double [] data2) {
        mean[0] = data1[tree.getRoot()];
        mean[1] = data2[tree.getRoot()];
    }

    /*-----------------------------------------*/
    /** 
     * This calculates variances of contrasts (and cross products for later correlation calculations). 
     * @param tree to traverse
     * @param contrasts1 contrasts for first (horizontal) trait
     * @param contrasts2 contrasts for second (vertical) trait
     */
    public void calcVars(Tree tree, double[] contrasts1, double contrasts2 []) {
        variance[0] = 0.0;
        variance[1] = 0.0;
        varianceSums(tree,contrasts1,contrasts2,tree.getRoot());
        variance[0] /= getNumber();             // rate of evolution of trait 1
        variance[1] /= getNumber();             // rate of evolution of trait 2
    }

    /** This calculates the sums for getVars */
    private void varianceSums(Tree tree, double[] contrasts1, double[] contrasts2, int node) {
        for (int daughter = tree.firstDaughterOfNode(node); tree.nodeExists(daughter); daughter = tree.nextSisterOfNode(daughter))
            varianceSums(tree, contrasts1, contrasts2, daughter);
        if (tree.nodeIsInternal(node) && 
                MesquiteDouble.isCombinable(contrasts1[node]) &&
                MesquiteDouble.isCombinable(contrasts2[node])) {
            variance[0] += contrasts1[node]*contrasts1[node];
            variance[1] += contrasts2[node]*contrasts2[node];
            corr += contrasts1[node]*contrasts2[node];
        }
    }

    /*-----------------------------------------*/
    /** The "v" values for the root are calculated and cached here. */
    private void setVs(Tree tree, double[] treeVPrimes) {
        int root = tree.getRoot();
        v1 = treeVPrimes[tree.firstDaughterOfNode(root)];
        v2 = treeVPrimes[tree.nextSisterOfNode(tree.firstDaughterOfNode(root))];
        v_adjust = (v1*v2)/(v1+v2);
    }

    /*-----------------------------------------*/
    /** This calculates the sum of squared X's (redundant with a variance?) and the
    sum of squared residuals from the contrast regression (through the origin). */
    private void getResidualSums(Tree tree, double [] contrasts1,  double [] contrasts2, 
            double b, 
            int node) {
        for (int daughter = tree.firstDaughterOfNode(node); tree.nodeExists(daughter); daughter = tree.nextSisterOfNode(daughter))
            getResidualSums(tree, contrasts1, contrasts2, b, daughter);
        if (tree.nodeIsInternal(node) &&
                MesquiteDouble.isCombinable(contrasts1[node]) &&
                MesquiteDouble.isCombinable(contrasts2[node])) {
            x2Sum += contrasts1[node]*contrasts1[node];
            double temp = contrasts2[node] - b*contrasts1[node];
            sigma2y += temp*temp;
        }
    }

    /** This method calculates sums of squares for data1, and cross products for the internal nodes
     of the tree rooted at node.  In this module, this is only used to calculate regression information
     for the contrasts.*/
    private void getCSums(Tree tree, 
            double[] data1, 
            double[] data2,
            MesquiteDouble sx2,
            MesquiteDouble sxy,
            int node) {
        if (tree.nodeIsInternal(node)) {  // only interior nodes
            if (MesquiteDouble.isCombinable(data1[node]) && // with valid data
                    MesquiteDouble.isCombinable(data2[node])) { 
                sx2.setValue(sx2.getValue()+data1[node]*data1[node]);
                sxy.setValue(sxy.getValue()+data1[node]*data2[node]); 
            }
            getCSums(tree,data1,data2,sx2,sxy,tree.firstDaughterOfNode(node));
            getCSums(tree,data1,data2,sx2,sxy,tree.lastDaughterOfNode(node));
        }
    }

    /** This method returns the ordinary least squares slope of the contrasts regression in 
     * the MesquiteDouble conSlope.  As usual for contrasts, the regression is through the origin.  
     * @param tree tree to traverse (since contrasts1/2 may include invalid nodes)
     * @param contrasts1 contrasts for first (horizontal) traits
     * @param contrasts2 contrasts for second (vertical) traits
     * @param node node index used for recursion - call with tree.getRoot()
     */
    protected double contrastSlope(Tree tree,
            double [] contrasts1,
            double [] contrasts2,
            int node) {
        final MesquiteDouble sx2 = new MesquiteDouble(0.0);
        final MesquiteDouble sxy = new MesquiteDouble(0.0);
        getCSums(tree,contrasts1,contrasts2,sx2,sxy,node);
        if (sx2.getValue() != 0) 
            return sxy.getValue()/sx2.getValue();
        else
            return MesquiteDouble.unassigned;
    }

    /*-----------------------------------------*/
    /** This method calculates the CI for the intercept.  Width specifies   
     *  the level of confidence (usually 0.90 or 0.95).  This code implements 
     *  equations 14 (for slope CI) and 25 (for yintercept CI) from the      
     *  appendix to Garland, T. Jr. and A. Ives, 2000.  Using the past to 
     *  predict the present: Confidence intervals for regression equations in 
     *  phylogenetic comparative methods. American Naturalist. 
     */        
    private void slopeInterCI(Tree tree, double [] contrasts1, double [] contrasts2, double ls) {
        double Xz;
        double rawCI= CICalc.CI(tStats,(1-(1-width1)/2)); 

        x2Sum = 0.0;
        sigma2y = 0.0;
        getResidualSums(tree, contrasts1, contrasts2, ls, tree.getRoot());
        if (x2Sum != 0.0) {
            sigma2y /= getNumber();        // Note that s.number = # of tips - 1
            slopeSE = Math.sqrt(sigma2y*getNumber()/(x2Sum*getNumberMinus1()));
            Xz = mean[n1];       // Xz only, since Xi = 0
            interSE = Math.sqrt((sigma2y*getNumber()/getNumberMinus1())*(((Xz*Xz)/x2Sum)+v_adjust));
            interCI = rawCI*interSE;
            slopeCI = rawCI*slopeSE;
        }
        else {
            interCI = -99999;
            slopeCI = -99999;
        }
    }


    /*-----------------------------------------*/
    /** This method calculates the confidence intervals around the root estimates and
     *  calls slopeInterCI to get confidence intervals for the regression estimates. 
     *  Root C.I.'s are calculated using equation A9 of Garland, Midford, and Ives 1999. 
     *  Intercept C.I.'s are calculated according to equation A13 of Garland and Ives 2000.    
     *  @param tree
     *  @param contrasts1
     *  @param contrasts2
     *  @param conSlope
     */
    private void calcRootIntervals(Tree tree, 
            double contrasts1[], 
            double contrasts2[], 
            double conSlope) {
        // Note the difference in degrees of freedom between the root reconstruction and the regression statistics.
        // creating a new distribution seems cleaner than setting and restoring the df of the main tStats object.                          
        TDistribution tStats2 = new TDistribution(getNumber()-getDFReduction());
        double rootWidth = CICalc.CI(tStats2,(1-(1-width1)/2)); 

        se[0] = Math.sqrt(variance[0]*v_adjust);
        se[1] = Math.sqrt(variance[1]*v_adjust);
        ci[0] = rootWidth*se[0];
        ci[1] = rootWidth*se[1];
        ls = conSlope;
        lsy = mean[1] - ls*mean[0];
        slopeInterCI(tree, contrasts1, contrasts2,ls);  
    }

    /*-----------------------------------------*/
    /** This calculates reconstructed root values*/
    protected void calcReconstructions(Tree tree, 
            double [] x1,
            double [] x2,
            double [] contrasts1, 
            double [] contrasts2, 
            double [] treeVPrimes,
            double conSlope) {

        int root = tree.getRoot();

        n1 = 0;
        n2 = 1;
        calcBounds(tree,x1,x2,root);     // not the same as the getBounds for regular stats.
        calcMeans(tree,x1,x2);
        calcVars(tree,contrasts1,contrasts2);
        setVs(tree,treeVPrimes);
        calcRootIntervals(tree,contrasts1,contrasts2,conSlope);    
    }



    protected boolean outsideLowTolerance(MesquiteNumber constraint, double bound){
        return (constraint.getDoubleValue()> bound) || (bound-constraint.getDoubleValue()) > 0.05*Math.abs(bound);
    }

    protected boolean outsideHighTolerance(MesquiteNumber constraint, double bound){
        return (constraint.getDoubleValue()< bound) || (constraint.getDoubleValue()-bound) > 0.05*Math.abs(bound);
    }



    /**
     * Converts space characters to underscores '_'
     * @param nName String to convert, presumably the name of a node
     * @return copy of nName with spaces converted
     */
    protected String spaceConvertString(String nName){
        int spacePos = nName.indexOf(" ");
        if (spacePos == -1)
            return nName;
        else {
            StringBuffer workBuffer = new StringBuffer(nName);
            while (spacePos != -1){
                workBuffer.setCharAt(spacePos,'_');
                spacePos = nName.indexOf(" ",spacePos+1);
            }
            return workBuffer.toString();
        }
    }


    /*-----------------------------------------*/
    /** 
     * @return results as brief String for use in legends 
     */
    public String getLegendText(){
        StringBuffer results = new StringBuffer(200);
        results.append("Slope (least squares):            " + ls + "\n");
        results.append("Standard Error:                  " + slopeSE + "\n");
        results.append("Rates of Evolution:                      " + variance[n1] + " " + variance[n2] + "\n");
        results.append("Estimated Root Node Values:              " + mean[n1] + "  " + mean[n2] + "\n");
        results.append("Standard Errors of Root Node Estimates:  " + se[n1] + " " + se[n2] + "\n");
        return results.toString();
    }


    /**
     * This internal class calculates t-distribution widths for the specified width.  
     * It uses Brent's *one dimensional* root finder
     * @author peter
     * created 2001
     *
     */
    public static class CICalc{

        /**
         * Calculates a confidence interval width from an t-distribution (with already specified dof)
         * @param td the distribution
         * @param width the width (e.g., 0.95 for 95%) of the interval
         * @return the interval size
         */
        public static double CI(TDistribution td, double width){
            return brentZero(0,100,0.00000001,width,td);
        }

        /** This implements Brent's one dimensional zero finder, which differs from PAL's
         *  one dimensional minimizer.  
         */
        private static double brentZero(double bottom, double top,double tol, double ci,TDistribution td){

            double a = bottom;
            double b = top;
            double fa = td.cumulative(a)-ci; // hardcoded method call
            double fb = td.cumulative(b)-ci; // hardcoded method call

            if ((fa*fb) >= 0){
                MesquiteMessage.warnProgrammer("Error in routine RootStat.CICalc.brentZero");
                MesquiteMessage.warnProgrammer("root must be bracketed");
                return 0.0;
            }
            if (Math.abs(fa) < Math.abs(fb)){
                double tmp = a;
                a = b;
                b = tmp;
                tmp = fa;
                fa = fb;
                fb = tmp;
            }
            double c = a;
            double d = c;  //keep javac happy?
            double fc = td.cumulative(c)-ci;
            boolean mflag = true;

            while((fb != 0) && (Math.abs(b-a)>tol)){
                double s;
                if ((fa != fc) && (fb != fc))
                    s=((a*fb*fc)/((fa-fb)*(fa-fc)))+((b*fa*fc)/((fb-fa)*(fb-fc)))+((c*fa*fb)/((fc-fa)*(fc-fb)));
                else
                    s=b-fb*((b-a)/(fb-fa));  //secant rule
                if ((s<(3*a+b/4) || s>b) || 
                        (mflag && Math.abs(s-b)>(Math.abs(b-c)/2)) || 
                        (!mflag && Math.abs(s-b)>Math.abs(c-d)/2)){
                    s = (a+b)/2;
                    mflag = true;
                }
                else
                    mflag = false;
                double fs = td.cumulative(s)-ci;
                d = c;
                c = b;
                if (fa*fs<0)
                    b=s;
                else
                    a=s;
                fa = td.cumulative(a)-ci;
                fb = td.cumulative(b)-ci;
                fc = td.cumulative(c)-ci;
                if (Math.abs(fa) < Math.abs(fb)){
                    double tmp = a;
                    a = b;
                    b = tmp;
                    tmp = fa;
                    fa = fb;
                    fb = tmp;
                }
            }

            return b;
        }
    }

}

