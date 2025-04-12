/* PDAP:PDTREE package for Mesquite  copyright 2001-20089 P. Midford & W. MaddisonPDAP:PDTREE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.The web site for PDAP:PDTREE is http://mesquiteproject.org/pdap_mesquite/This source code and its compiled class files are free and modifiable under the terms of GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html) */package mesquite.pdap.FelsContrWithChar;/*~~  */import mesquite.lib.*;import mesquite.lib.characters.*;import mesquite.lib.tree.MesquiteTree;import mesquite.lib.tree.Tree;import mesquite.lib.ui.ListDialog;import mesquite.lib.ui.MesquiteMenuItemSpec;import mesquite.lib.ui.MesquiteSubmenuSpec;import mesquite.cont.lib.*;import mesquite.pdap.lib.*;/** ======================================================================== */public class FelsContrWithChar extends ContrastForChar {    private int currentItem=0;    private ContinuousDistribution observedStates;    //private ContinuousDistribution observedStates2;    //private MesquiteMenuItemSpec itemItem;    private String itemName=null;    private Tree lastUsedTree = null;    private UnivariateContrastCalculator calculator = null;    private long lastTreeVersion=-1;    private int componentShown =0;    //contrast option constants moved to ContrastForChar to avoid duplication and ensure everyone's on same page    /*.................................................................................................................*/    public boolean startJob(String arguments, Object condition, boolean hiredByName) {        return true;      }    /*.................................................................................................................*/    //Peter: I compacted these    /** returns current parameters, for logging etc..*/    public String getBriefParameters() {        String addendum = "";        if (itemName!=null)            addendum = "  (" + itemName + ")";        switch (componentShown) {        case CONTRAST:             return "Standardized contrast" + addendum;        case ABSCONTRAST:            return "Absolute value of contrast" + addendum;        case RAWCONTRAST:            return "Raw contrast" + addendum;        case SD:            return "Square root of branch length sum";        case VPRIME:            return "v Prime";        case NODEVALUE:            return "Estimated node value" + addendum;        case CORRECTEDHEIGHT:            return "Corrected Height";        case TIPS:            return "Raw tip value" + addendum;        case RESIDUAL:            return "Residual" + addendum;        case ABSRESIDUAL:            return "Absolute value of residual" + addendum;        case RAWHEIGHT:            return "Uncorrected node height" ;        }         return "Error: unknown contrast request";	    }    /*.................................................................................................................*/    /** returns current parameters, for logging etc..*/    public String getParameters() {        String addendum = "";        if (itemName!=null)            addendum = "  (" + itemName + ")";        switch (componentShown) {        case CONTRAST:             return "Standardized contrast" + addendum;        case ABSCONTRAST:            return "Absolute value of standardized contrast" + addendum;        case RAWCONTRAST:            return "Raw (uncorrected) contrast" + addendum;        case SD:            return "Square root of sum of corrected branch lengths";        case VPRIME:            return "v Prime";        case NODEVALUE:            return "Estimated value of base node" + addendum;        case CORRECTEDHEIGHT:            return "Corrected height of base node";        case TIPS:            return "Observed states of terminal taxa" + addendum;        case RESIDUAL:            return "Residual" + addendum;        case ABSRESIDUAL:            return "Absolute value of residual" + addendum;        case RAWHEIGHT:            return "Uncorrected height of base node";        }         return "Error: unknown contrast request";	    }    /** This is necessary for axis titles on screen 9 to show character names */    public String getItemName(){        if (itemName != null)            return itemName;        else return "";    }    /*.................................................................................................................*/    public Snapshot getSnapshot(MesquiteFile file) {        //TODO: allow change in assignTask, etc.        Snapshot temp = new Snapshot();        temp.addLine("setItem " + (currentItem));        return temp;    }    /*.................................................................................................................*/    public Object doCommand(String commandName, String arguments, CommandChecker checker) {        if (checker.compare(this.getClass(), "Sets which item (from a multi-item continuous matrix) is used", "[number of item]", commandName, "setItem")) {            int ic = MesquiteInteger.fromString(arguments);            if (!MesquiteInteger.isCombinable(ic) && observedStates!=null){                int numItems =observedStates.getNumItems();                String[] items = new String[numItems];                for (int i=0; i<items.length; i++){                    if (StringUtil.blank(observedStates.getItemName(i)))                        items[i] = "(unnamed)";                    else                        items[i]= observedStates.getItemName(i);                }                ic = ListDialog.queryList(containerOfModule(), "Item for contrasts", "Calculate contrasts for item:", MesquiteString.helpString, items, 0);            }            if (!MesquiteInteger.isCombinable(ic))                return null;            if (observedStates !=null && observedStates instanceof ContinuousDistribution) {                if ((ic>=0) && (ic<=observedStates.getNumItems()-1)) {                    currentItem = ic;                    parametersChanged();                }            }        }        else if (checker.compare(this.getClass(), "Sets the component to be calculated to Felsenstein's Contrasts", null, commandName, "showContrast")) {            setOptionWithChangeParameters(CONTRAST);        }        else if (checker.compare(this.getClass(), "Sets the component to be calculated to Absolute Value of Felsenstein's Contrasts", null, commandName, "showAbsContrast")) {            setOptionWithChangeParameters(ABSCONTRAST);        }        else if (checker.compare(this.getClass(), "Sets the component to be calculated to Raw Contrasts", null, commandName, "showRawContrast")) {            setOptionWithChangeParameters(RAWCONTRAST);        }        else if (checker.compare(this.getClass(), "Sets the component to be calculated to Standard Deviation", null, commandName, "showSD")) {            setOptionWithChangeParameters(SD);        }        else if (checker.compare(this.getClass(), "Sets the component to be calculated to V Prime", null, commandName, "showVPrime")) {            setOptionWithChangeParameters(VPRIME);        }        else if (checker.compare(this.getClass(), "Sets the component to be calculated to Estimated Node Values", null, commandName, "showNodeValue")) {            setOptionWithChangeParameters(NODEVALUE);        }        else if (checker.compare(this.getClass(), "Sets the component to be calculated to Corrected Height", null, commandName, "showCorrectedHeight")) {            setOptionWithChangeParameters(CORRECTEDHEIGHT);        }        else if (checker.compare(this.getClass(), "Sets the component to be calculated to Raw Height", null, commandName, "showRawHeight")) {            setOptionWithChangeParameters(RAWHEIGHT);        }        else             return  super.doCommand(commandName, arguments, checker);        return null;    }    /* Peter: Other calls would need to be aware that parametersChanged might be called and hence do other parameter settings first before parametersChanged is called. 	Wayne: I tried this, but it causes a hang in PDAPDiagnosticChart (try changing the call on line 709 to setOptionWithChangeParameters).  Not sure how to proceed.*/    public void setOption(int option){        componentShown = option;        //parametersChanged();    }    /**	@param int specifies the component (contrast related calculation) to be displayed	@param CommandRec passed on to parametersChanged     */    public void setOptionWithChangeParameters(int option){        componentShown = option;        parametersChanged();    }    /**	stub?     */    public void setXCharForReport(int xChar) {          }    public void setYCharForReport(int yChar) {    }    /*Wayne: The following is my best guess as to how to protect take control against multiple calls.  It seems to fix the problem with duplicate   	items on the submenu - which was one of the problems Ted noticed - but extra analysis is still being dumped on the text pane.    	/*Peter: this should be protected against being called more than once -- i.e., menu items should be added only if not yet exist, and should be deleted if !take */    private MesquiteMenuItemSpec showContrastItem = null;    private MesquiteMenuItemSpec showAbsContrastItem = null;    private MesquiteMenuItemSpec showRawContrastItem = null;    private MesquiteMenuItemSpec showSDItem = null;    private MesquiteMenuItemSpec showVPrimeItem = null;    private MesquiteMenuItemSpec showNodeValueItem = null;    private MesquiteMenuItemSpec showCorrectedHeightItem = null;    MesquiteSubmenuSpec mss = null;    public void takeControl(boolean take){        if (take){            MesquiteCommand itemChoiceCommand = MesquiteModule.makeCommand("setItem",  (Commandable)this);            if (mss == null)                mss = addSubmenu(null, "Contrast Component", null);            if (showContrastItem == null) {                showContrastItem = addItemToSubmenu(null, mss, "Felsenstein's Contrast", makeCommand("showContrast", this));                showAbsContrastItem = addItemToSubmenu(null, mss, "Absolute Value of Felsenstein's Contrast", makeCommand("showAbsContrast", this));		                showRawContrastItem = addItemToSubmenu(null, mss, "Raw (uncorrected) Contrast", makeCommand("showRawContrast", this));                showSDItem = addItemToSubmenu(null, mss, "Standard Deviation", makeCommand("showSD", this));                showVPrimeItem = addItemToSubmenu(null, mss, "Vprime", makeCommand("showVPrime", this));                showNodeValueItem = addItemToSubmenu(null, mss, "Estimated Node Value", makeCommand("showNodeValue", this));                showCorrectedHeightItem = addItemToSubmenu(null, mss, "Corrected Height", makeCommand("showCorrectedHeight", this));            }        }        else if ((mss != null) && (showContrastItem != null)) {            if (showContrastItem != null) {                deleteMenuItem(showContrastItem);                showContrastItem = null;                deleteMenuItem(showAbsContrastItem);                showAbsContrastItem = null;                deleteMenuItem(showRawContrastItem);                showRawContrastItem = null;                deleteMenuItem(showSDItem);                showSDItem = null;                deleteMenuItem(showVPrimeItem);                showVPrimeItem = null;                deleteMenuItem(showNodeValueItem);                showNodeValueItem = null;                deleteMenuItem(showCorrectedHeightItem);                showCorrectedHeightItem = null;            }        }	    }    /*.................................................................................................................*/    public void employeeParametersChanged(MesquiteModule employee, MesquiteModule source, Notification notification) {        observedStates = null;//to force recalculation        super.employeeParametersChanged(module, source, notification);    }    /** Called to provoke any necessary initialization.  This helps prevent the module's intialization queries to the user from   	happening at inopportune times (e.g., while a long chart calculation is in mid-progress)*/    public void initialize(Tree tree){    }    // Holds and caches a NoPolyTree expansion of the tree.    NoPolyTree localTree;    private boolean[] localDeleted;  //holds corresponding deletion array    /*.................................................................................................................*/    public double[] calculateStandardDeviations(Tree tree) {        if (tree == null)            return null;        if (tree == lastUsedTree && lastTreeVersion == tree.getVersionNumber() && calculator != null)            return calculator.getStdDev();        calculator = new UnivariateContrastCalculator(localTree,observedStates);        return calculator.stdDevCalculation();    }    /*.................................................................................................................*/    public void calculateNumbers(Tree tree, CharacterDistribution charDistribution, NumberArray result, MesquiteString resultString) {        observedStates = null;        if (result==null || tree == null)            return;        clearResultAndLastResult(result);        if (!(charDistribution instanceof ContinuousDistribution))            return;        if (tree instanceof NoPolyTree){            localTree = (NoPolyTree) tree;        }        else {            localTree = new NoPolyTree((MesquiteTree)tree);        }        observedStates = (ContinuousDistribution)charDistribution;        itemName=charDistribution.getName();        lastUsedTree = tree;        lastTreeVersion = tree.getVersionNumber();        int root = localTree.getRoot(localDeleted);        if (observedStates != null) {            calculator = new UnivariateContrastCalculator(localTree,observedStates);            if (componentShown == TIPS){  // no need to calculate contrasts, just copy values from x                double [] x = calculator.fillTips(root);                for (int i=0; i<x.length; i++) {                    result.setValue(i, x[i]);                }            }            else {                 switch (componentShown) {                case SD: {                    double [] stdDev = calculator.stdDevCalculation();                    for (int i=0; i<stdDev.length; i++)                         result.setValue(i, stdDev[i]);                    break;                }                case CONTRAST: {                    double [] contrasts = calculator.contrastCalculation();                    for (int i=0; i<contrasts.length; i++)                         result.setValue(i, contrasts[i]);                    break;                }                case ABSCONTRAST: {                    double [] contrasts = calculator.contrastCalculation();                    for (int i=0; i<contrasts.length; i++) {                        if (contrasts[i] < 0)                            result.setValue(i, -1*contrasts[i]);                        else                            result.setValue(i, contrasts[i]);                    }                    break;                }                case  RAWCONTRAST: {                    double [] raw = calculator.rawContrastCalculation();                    for (int i=0; i<raw.length; i++)                         result.setValue(i, raw[i]);                    break;                }                case VPRIME: {                    double [] vPrime = calculator.vPrimeCalculation();                    for (int i=0; i<vPrime.length; i++)                         result.setValue(i, vPrime[i]);                    break;                }                case CORRECTEDHEIGHT:                case	RAWHEIGHT: {                    double [] height = calculator.heightCalculation(componentShown);                       // If a node has a reconstructed value, but no contrast,                     // its value is really a pass through and should not be                    // used in calculations...                    for (int i=0; i<height.length; i++)                         result.setValue(i, height[i]);                    break;                }                case NODEVALUE: {                    double [] x = calculator.nodeValueCalculation();                    // If a node has a reconstructed value, but no contrast,                    // its value is really a pass through and should not be                    // used in calculations...                    for (int i=0; i<x.length; i++) {                        if (localTree.nodeIsInternal(i)) {                            if (MesquiteDouble.isCombinable(x[i]))  { // a value was set, so i specifies a node                                int left = tree.firstDaughterOfNode(i);                                int right = tree.lastDaughterOfNode(i);                                if (MesquiteDouble.isCombinable(x[left]) && MesquiteDouble.isCombinable(x[right]))                                    result.setValue(i, x[i]);                            }                        }                        else                            result.setValue(i,x[i]);  // it's a tip so either it does or it doesn't have a value.                    }                    break;                }                }            }        }        saveLastResult(result);        saveLastResultString(resultString);    }    /*.................................................................................................................*/    public void calculateBiVariateNumbers(Tree tree,             ContinuousDistribution charDistribution,             ContinuousDistribution charDistribution2,             NumberArray result,             MesquiteString resultString) {        observedStates = null;        //observedStates2 = null;        if (result==null || tree == null)            return;        result.deassignArray();        if (!(charDistribution instanceof ContinuousDistribution) ||                !(charDistribution2 instanceof ContinuousDistribution))            return;        if (tree instanceof NoPolyTree)            localTree = (NoPolyTree) tree;        else             localTree = new NoPolyTree((MesquiteTree)tree);        observedStates = (ContinuousDistribution)charDistribution;        //observedStates2 = (ContinuousDistribution)charDistribution2;        itemName=null;        lastUsedTree = tree;        lastTreeVersion = tree.getVersionNumber();        final int root = localTree.getRoot();        if (observedStates != null) {            calculator = new UnivariateContrastCalculator(localTree,observedStates);            if (componentShown == TIPS){  // no need to calculate contrasts, just copy values from x                final double [] x = calculator.fillTips(root);                for (int i=0; i<x.length; i++) {                    result.setValue(i, x[i]);                }            }            else {                 switch (componentShown) {                case SD: {                    final double [] stdDev = calculator.stdDevCalculation();                    for (int i=0; i<stdDev.length; i++)                         result.setValue(i, stdDev[i]);                    break;                }                case CONTRAST: {                    final double [] contrasts = calculator.contrastCalculation();                    for (int i=0; i<contrasts.length; i++)                         result.setValue(i, contrasts[i]);                    break;                }                case ABSCONTRAST: {                    final double [] contrasts = calculator.contrastCalculation();                    for (int i=0; i<contrasts.length; i++) {                        if (contrasts[i] < 0)                            result.setValue(i, -1*contrasts[i]);                        else                            result.setValue(i, contrasts[i]);                    }                    break;                }                case  RAWCONTRAST: {                    final double [] raw = calculator.rawContrastCalculation();                    for (int i=0; i<raw.length; i++)                         result.setValue(i, raw[i]);                    break;                }                case VPRIME: {                    final double [] vPrime = calculator.vPrimeCalculation();                    for (int i=0; i<vPrime.length; i++)                         result.setValue(i, vPrime[i]);                    break;                }                case CORRECTEDHEIGHT:                case	RAWHEIGHT: {                    final double [] height = calculator.heightCalculation(componentShown);                       // If a node has a reconstructed value, but no contrast,                     // its value is really a pass through and should not be                    // used in calculations...                    for (int i=0; i<height.length; i++)                         result.setValue(i, height[i]);                    break;                }                case NODEVALUE: {                    final double [] x = calculator.nodeValueCalculation();                    // If a node has a reconstructed value, but no contrast,                    // its value is really a pass through and should not be                    // used in calculations...                    for (int i=0; i<x.length; i++) {                        if (localTree.nodeIsInternal(i)) {                            if (MesquiteDouble.isCombinable(x[i]))  { // a value was set, so i specifies a node                                int left = tree.firstDaughterOfNode(i);                                int right = tree.lastDaughterOfNode(i);                                if (MesquiteDouble.isCombinable(x[left]) && MesquiteDouble.isCombinable(x[right]))                                    result.setValue(i, x[i]);                            }                        }                        else                            result.setValue(i,x[i]);  // it's a tip so either it does or it doesn't have a value.                    }                    break;                }                }            }        }    }    /*.................................................................................................................*/    public String getName() {        return "PDAP contrast calculations";    }    /*.................................................................................................................*/    public String getVersion() {        return "1.15";    }    /*.................................................................................................................*/    public boolean isPrerelease() {        return false;    }        /**     * This module is substantive only because the univariate calculator it uses is a simple java object, not a MesquiteModule,     * so the calculator can't be substantive (and be subject to blame), so the blame goes here.       *      */    public boolean isSubstantive(){        return true;  //TODO make the univariatecalculator a module so this can return false    }    /** returns an explanation of what the module does.*/    public String getExplanation() {        return "Calculates Felsenstein's contrasts and other related statistics for continuous-valued characters on a tree.";    }    /*.................................................................................................................*/    public String getAuthors() {        return "Peter E. Midford, Ted Garland Jr., and Wayne P. Maddison";    }}