/* PDAP:PDTREE package for Mesquite  copyright 2001-2010 P. Midford & W. MaddisonPDAP:PDTREE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.The web site for PDAP:PDTREE is http://mesquiteproject.org/pdap_mesquite/This source code and its compiled class files are free and modifiable under the terms of GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html) */package mesquite.pdap.ContrastResiduals;/*~~  */import mesquite.lib.*;import mesquite.lib.characters.*;import mesquite.cont.lib.*;import mesquite.pdap.lib.*;/* ======================================================================== *//** This class is responsible for calculating the residuals from a regression of contrasts against         contrasts.  As with any such regression, it must go through the origin.        This is a bivariate contrast statistic.  As such, it needs to prune its tree to handle missing        data. */public class ContrastResiduals extends ResidualsCalculator {//    private Tree lastUsedTree = null;//    private long lastTreeVersion=-1;    private int absolute = ContrastForChar.CONTRAST;    /*.................................................................................................................*/    public boolean startJob(String arguments, Object condition, boolean hiredByName) {        return true;    }    /*.................................................................................................................*/    /** returns current parameters, for logging etc... 	see comments for  setOption  	@return String*/    public String getParameters() {        if (absolute == ContrastForChar.ABSCONTRAST)            return "(absolute value)";        else            return "(raw)";    }    /*.................................................................................................................*/    public void initialize(Tree tree){    }    /**     * This class can calculate either contrasts or absolute values of contrasts.  This method accepts a flag that controls     * that behavior     * @param option specifies whether to show contrasts or absolute values of contrasts     */    public void setOption(int option){        if ((option == ContrastForChar.ABSCONTRAST) || (option == ContrastForChar.CONTRAST))            absolute = option;	        else            MesquiteMessage.warnProgrammer("ContrastResiduals got a bad option: " + option);			    }    /** positivize by flipping the sign of corresponding elements of data1 and data2 if the data1 element is negative */    private void positivize(Tree tree, double[] data1,double [] data2,int node) {        for (int daughter = tree.firstDaughterOfNode(node); tree.nodeExists(daughter); daughter = tree.nextSisterOfNode(daughter))            positivize(tree, data1, data2, daughter);        if (MesquiteDouble.isCombinable(data1[node]) && MesquiteDouble.isCombinable(data2[node])) {            if (data1[node] < 0) {	                data1[node] *= -1;                data2[node] *= -1;            }        }    }    /** generate sums, sums of squares and cross product sums for statistical calculations */    private void getSums(Tree tree,             double[] data1,             double[] data2,            MesquiteDouble sx,            MesquiteDouble sx2,            MesquiteDouble sy,            MesquiteDouble sxy,            int node)  {        if (tree.nodeIsInternal(node)){   // only interior nodes            if (MesquiteDouble.isCombinable(data1[node]) && // calculate only for nodes with good data                    MesquiteDouble.isCombinable(data2[node])) {                sx.add(data1[node]);                sx2.add(data1[node]*data1[node]);                sy.add(data2[node]);                sxy.add(data1[node]*data2[node]);             }            getSums(tree,data1,data2,sx,sx2,sy,sxy,tree.firstDaughterOfNode(node));            getSums(tree,data1,data2,sx,sx2,sy,sxy,tree.lastDaughterOfNode(node));        }    }    /** Currently all this does is calculate the slope of the regression line, all the partial sums	 are tossed.  */    private void calculateSlope(Tree tree, double [] data1, double [] data2, MesquiteDouble conSlope) {        MesquiteDouble sx = new MesquiteDouble(0.0);        MesquiteDouble sx2 = new MesquiteDouble(0.0);        MesquiteDouble sy = new MesquiteDouble(0.0);        MesquiteDouble sxy = new MesquiteDouble(0.0);        int root = tree.getRoot();        positivize(tree,data1,data2,root);        getSums(tree,data1,data2,sx,sx2,sy,sxy,root);        // stash away the contrast slope for further use        if (sx2.getValue() != 0)              conSlope.setValue(sxy.getValue()/sx2.getValue());  // otherwise leave "unassigned"        //Debugg.println("Calculated slope is " + conSlope.getValue());  // This is useful for checking the process of calculating residuals for screens 10,11    }    /** This calculates residuals from the through the origin regression.  Note that these	residuals will not generally sum to zero.  The method calculates them in the obvious manner */    private void calculateResiduals(Tree tree,             double [] contrasts1,             double [] contrasts2,             NumberArray result,            MesquiteDouble slope,             int node)  {        if (tree.nodeIsInternal(node)) {  // only interior nodes            if (MesquiteDouble.isCombinable(contrasts2[node]) &&                    MesquiteDouble.isCombinable(contrasts1[node])) {                if (absolute == ContrastForChar.ABSCONTRAST)	                    result.setValue(node,Math.abs(contrasts2[node] - slope.getValue()*contrasts1[node]));                else                    result.setValue(node,contrasts2[node] - slope.getValue()*contrasts1[node]);            }            else                result.setValue(node,MesquiteDouble.unassigned);            for (int daughter = tree.firstDaughterOfNode(node); tree.nodeExists(daughter); daughter = tree.nextSisterOfNode(daughter))                calculateResiduals(tree, contrasts1, contrasts2, result, slope, daughter);        }    }    /*.................................................................................................................*/    public void calculateNumbers(Tree tree,             CharacterDistribution charDistribution1,             CharacterDistribution charDistribution2,             NumberArray result,             MesquiteString resultString){        ContinuousDistribution observedStates1 = null;        ContinuousDistribution observedStates2 = null;        MesquiteDouble conSlope = new MesquiteDouble();        int root = tree.getRoot();        double [] contrasts1;        double [] contrasts2;        BivariateContrastCalculator calculator;         Tree dataReducedTree;        if (result==null || tree == null)            return;        if (!(charDistribution1 instanceof ContinuousDistribution) ||!(charDistribution2 instanceof ContinuousDistribution))            return;        clearResultAndLastResult(result);        result.zeroArray();        observedStates1 = (ContinuousDistribution)charDistribution1;        observedStates2 = (ContinuousDistribution)charDistribution2;//        lastUsedTree = tree;//        lastTreeVersion = tree.getVersionNumber();        dataReducedTree = tree;                 // tree pruned to handle missing data        if ((observedStates1 != null) && (observedStates2 != null)) {            calculator = new BivariateContrastCalculator(dataReducedTree, observedStates1, observedStates2);            contrasts1 = calculator.contrastCalculation();            if (!calculator.Ok())                MesquiteMessage.warnProgrammer("Bad contrast calculation in ConstrastResiduals X trait");            contrasts2 = calculator.getContrasts2();            calculateSlope(dataReducedTree,contrasts1,contrasts2,conSlope);            calculateResiduals(dataReducedTree,contrasts1,contrasts2,result,conSlope,root);        }        if (resultString!=null)            resultString.setValue("Contrast residuals");        saveLastResult(result);        saveLastResultString(resultString);    }    /*.................................................................................................................*/    public Class getCharacterClass() {        return ContinuousState.class;    }    /*.................................................................................................................*/    public CompatibilityTest getCompatibilityTest() {        return new ContinuousStateTest();    }    /*.................................................................................................................*/    public String getExplanation() {        return "Calculates the regression residuals of Felsenstein's contrasts between two characters";    }    /*.................................................................................................................*/    public String getName() {        return "Contrast Residuals";    }    /*.................................................................................................................*/    public String getVersion() {        return "1.15";    }    /*................................................................................................................*/    public boolean isSubstantive(){        return true;    }            /*.................................................................................................................*/    public boolean isPrerelease() {        return false;    }    /*.................................................................................................................*/    public String getAuthors() {        return "Peter E. Midford, Ted Garland Jr., and Wayne P. Maddison";    }}