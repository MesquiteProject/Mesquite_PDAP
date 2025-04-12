/* PDAP:PDTREE package for Mesquite  copyright 2001-2009 P. Midford & W. MaddisonPDAP:PDTREE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.The web site for PDAP:PDTREE is http://mesquiteproject.org/pdap_mesquite/This source code and its compiled class files are free and modifiable under the terms of GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html) */package mesquite.pdap.RootReconstructions;/* ~~ */import mesquite.lib.*;import mesquite.lib.characters.*;import mesquite.lib.tree.MesquiteTree;import mesquite.lib.tree.Tree;import mesquite.lib.tree.TreeReference;import mesquite.lib.ui.MesquiteChart;import mesquite.cont.lib.*;import mesquite.pdap.SPRootReconstruction.SPRootReconstruction;import mesquite.pdap.lib.*;/* ======================================================================== */public class RootReconstructions extends RootRegressionCalculator {    public void getEmployeeNeeds(){  //This gets called on startup to harvest information; override this and inside, call registerEmployeeNeed        EmployeeNeed e = registerEmployeeNeed(SPRootReconstruction.class, getName() + "  needs a method to calculate correlations.",        "The method to calculate correlations is selected initially");    }    private TreeReference lastTreeReference;    private SPRootReconstruction sps;    private PDAPRootReconStatPak myStats = null;    private MesquiteChart chart;    private PDAPTreeWinAsstC chartModule = null;    /* ................................................................................................................. */    public boolean startJob(String arguments, Object condition, boolean hiredByName) {        loadPreferences();        // verboseQueryMode.setValue(false);        lastTreeReference = null;        addMenuItem("Root Calculation Query Mode...", makeCommand("setVerboseQueryMode", (Commandable) this));        addMenuItem("Set CI/PI width...", makeCommand("setwidth",(Commandable) this));        addMenuItem("Set Polytomy DF Reduction...", makeCommand("setDFReduction", (Commandable) this));        addMenuItem("Ignore Root Tritomies...", makeCommand("setIgnoreRootTritomies", (Commandable)this));        sps = (SPRootReconstruction) hireNamedEmployee(SPRootReconstruction.class, "#SPRootReconstruction");        if (sps == null)            return sorry(getName() + " couldn't start because no calculator of correlation obtained.");        return true;    }    /*-----------------------------------------*/    public Snapshot getSnapshot(MesquiteFile file) {        final Snapshot temp = new Snapshot();        if (myStats == null || !MesquiteDouble.isCombinable(myStats.getWidth1()))            temp.addLine("setwidth1 0.95");        else            temp.addLine("setwidth1 " + myStats.getWidth1());        if (myStats == null || !MesquiteDouble.isCombinable(myStats.getWidth2()))            temp.addLine("setwidth2 0.90");        else            temp.addLine("setwidth2 " + myStats.getWidth2());        if (myStats == null || !MesquiteDouble.isCombinable(myStats.getDFReduction()))            temp.addLine("setDFReduction 0");        else                temp.addLine("setDFReduction " + myStats.getDFReduction());        return temp;    }    /*-----------------------------------------*/    final MesquiteInteger zeroPos = new MesquiteInteger(0);    /**  */    public Object doCommand(String commandName, String arguments, CommandChecker checker) {        final Class thisClass = getClass();        if (checker.compare(thisClass,"Sets the first width for the confidence and prediction intervals","[a confidence width e.g., 0.95]", commandName,"setwidth1")) {            if (myStats == null)                myStats = (PDAPRootReconStatPak)sps.getStatPak();            double newWidth = MesquiteDouble.fromString(arguments, zeroPos);            if (!MesquiteDouble.isCombinable(newWidth)){                chartModule.queryWidths();                newWidth = chartModule.getWidth1();            }            myStats.setWidth1(newWidth);            if (chartModule != null)                chartModule.setWidthQueryDone(true);            parametersChanged();        }        else if (checker.compare(thisClass,"Sets the second width for the confidence and prediction intervals","[a confidence width e.g., 0.90]", commandName,"setwidth2")) {            if (myStats == null)                myStats = (PDAPRootReconStatPak)sps.getStatPak();            double newWidth = MesquiteDouble.fromString(arguments, zeroPos);            if (!MesquiteDouble.isCombinable(newWidth)){                chartModule.queryWidths();                newWidth = chartModule.getWidth2();            }            myStats.setWidth2(newWidth);            if (chartModule != null)                chartModule.setWidthQueryDone(true);            parametersChanged();        }         else if (checker.compare(thisClass,"Sets both widths for the confidence and prediction intervals","[two confidence widths e.g., 0.95 0.90]", commandName,"setwidth")) {            if (myStats == null)                myStats = (PDAPRootReconStatPak)sps.getStatPak();            double newWidth1 = MesquiteDouble.fromString(arguments, zeroPos);            MesquiteInteger pos2 = null;            double newWidth2 = MesquiteDouble.unassigned;            if (MesquiteDouble.isCombinable(newWidth1)) {                pos2 = new MesquiteInteger(arguments.indexOf(' ', 0));                if (pos2.getValue() > 0)                    newWidth2 = MesquiteDouble.fromString(arguments, pos2);            }            if (!MesquiteDouble.isCombinable(newWidth1) || !MesquiteDouble.isCombinable(newWidth2)){                chartModule.queryWidths();                newWidth1 = chartModule.getWidth1();                newWidth2 = chartModule.getWidth2();            }            myStats.setWidth1(newWidth1);            myStats.setWidth2(newWidth2);            if (chartModule != null)                chartModule.setWidthQueryDone(true);            parametersChanged();        }         else if (checker.compare(thisClass,"Sets the number of DF's reduced because of polytomies","[number of dof]", commandName, "setDFReduction")) {            if (myStats == null)                myStats = (PDAPRootReconStatPak)sps.getStatPak();            int dfReduction = MesquiteInteger.fromFirstToken(arguments, zeroPos);            if (chartModule != null){                if (!MesquiteInteger.isCombinable(dfReduction))                    chartModule.queryDFReduce(myStats, true);                else                    chartModule.setDFReduction(dfReduction);                chartModule.setDFQueryDone(true);                myStats.setDFReduction(chartModule.getDFReduction());            }            else                if (MesquiteInteger.isCombinable(dfReduction))                    myStats.setDFReduction(dfReduction);            parametersChanged();        }         else if (checker.compare(thisClass,"Sets the query mode to verbose or not", "[on/off]",commandName, "setVerboseQueryMode")) {            if (chartModule != null)                if (StringUtil.blank(arguments))                     chartModule.queryVerboseQueryMode();                else                    chartModule.toggleVerboseQueryMode(parser.getFirstToken(arguments));        }         else if (checker.compare(thisClass,"Sets whether to ignore a tritomy at the root ", "[boolean]",commandName, "setIgnoreRootTritomies")){            if (StringUtil.blank(arguments))                 chartModule.queryIgnoreRootTritomies();            else{                chartModule.setIgnoreRootTritomies("on".equalsIgnoreCase(parser.getFirstToken(arguments)));            }             myStats.setIgnoreRootTritomies(chartModule.getIgnoreRootTritomies());        }        else            return  super.doCommand(commandName, arguments, checker);        return null;    }    /*-----------------------------------------*/    public void setChartModule(PDAPTreeWinAsstC module) {        chartModule = module;        myStats = (PDAPRootReconStatPak)sps.getStatPak();        updateStats();    }    /**     * Simply returns the statPak so the charter can access the slope and other     * interesting results.     */    public PDAPRootStatPak getStatPak() {        if (chartModule == null){            MesquiteMessage.warnProgrammer("No chartModule set for "+this);            return null;        }        if (myStats == null){            myStats = (PDAPRootReconStatPak)sps.getStatPak();            updateStats();        }        return myStats;    }        private void updateStats(){        myStats.setDFReduction(chartModule.getDFReduction());        myStats.setWidth1(chartModule.getWidth1());        myStats.setWidth2(chartModule.getWidth2());        myStats.setIgnoreRootTritomies(chartModule.getIgnoreRootTritomies());    }    /*-----------------------------------------*/    public void setChart(MesquiteChart chart) {        this.chart = chart;    }    /* ................................................................................................................. */    /**     *      */    public void calculateNumbers(Tree tree,            CharacterDistribution charDistribution1,            CharacterDistribution charDistribution2, NumberArray result,            MesquiteString resultString) {        ContinuousDistribution observedStates1 = null;        ContinuousDistribution observedStates2 = null;        if (result == null || tree == null)            return;        if (!(charDistribution1 instanceof ContinuousDistribution)                || !(charDistribution2 instanceof ContinuousDistribution))            return;        clearResultAndLastResult(result);        result.zeroArray();        int root = tree.getRoot();        observedStates1 = (ContinuousDistribution) charDistribution1;        observedStates2 = (ContinuousDistribution) charDistribution2;        //      Peter: tighter restrictions; any change will provoke query or reset        if ((lastTreeReference == null) ||                (tree instanceof MesquiteTree &&                         !((MesquiteTree) tree).sameTreeVersions(lastTreeReference, true, true))) {             // first create and initialize the new rootStatPak            if (myStats == null){                myStats = (PDAPRootReconStatPak)sps.getStatPak();                if (!MesquiteThread.isScripting()){                    updateStats();                    chartModule.setDFQueryDone(true);                }                else{                    MesquiteMessage.warnUser("Setting df and confidence intervals to defaults");                }            }            else                if (!MesquiteThread.isScripting()){                    updateStats();                    chartModule.setDFQueryDone(true);                }            // otherwise myStats exists and the widths and df corrections should already have been set            // now set the tree so doCalculations will do the right thing...            myStats.setTree(tree);            myStats.countInternalNodes(tree, root); // count the internal nodes,            // to set number            int zlb = ((NoPolyTree)tree).countZLBs(myStats.getIgnoreRootTritomies()); // Zero-Length-Branches are what            // PDAP calls "polytomies"; count to            // inform the user            // now ask the user to set df Reductions            if (!MesquiteThread.isScripting()){                if (!chartModule.getDFQueryDone() &&                         (zlb > 0 || chartModule.getVerboseQueryMode())){                    chartModule.queryDFReduce(myStats, true);                    myStats.setDFReduction(chartModule.getDFReduction());                    chartModule.setDFQueryDone(true);                }                if (chartModule.getVerboseQueryMode() && !chartModule.getWidthQueryDone()){                    chartModule.queryWidths();                    myStats.setWidth1(chartModule.getWidth1());                    myStats.setWidth2(chartModule.getWidth2());                    chartModule.setWidthQueryDone(true);                }            }        }        lastTreeReference = ((MesquiteTree) tree).getTreeReference(lastTreeReference);        if ((observedStates1 != null) || (observedStates2 != null)) {            NumberArray xArray = new NumberArray();            NumberArray yArray = new NumberArray();            myStats.setObserved1(observedStates1);            myStats.setObserved2(observedStates2);            boolean munch = myStats.doCalculations(xArray, yArray, chart);            if (resultString == null)                MesquiteMessage.warnProgrammer("RootReconstructions should NOT be called with an empty result string - uncontrolled recursion may occur");            else {                if (munch)                    resultString.setValue("Needs remunching");                else                    resultString.setValue("Reconstructed Root Values");            }        }        saveLastResult(result);        saveLastResultString(resultString);    }    /* ................................................................................................................. */    /*.................................................................................................................*/    public Class getCharacterClass() {        return ContinuousState.class;    }    /*.................................................................................................................*/    public CompatibilityTest getCompatibilityTest() {        return new ContinuousStateTest();    }    /** returns current parameters, for logging etc..*/    /*.................................................................................................................*/    public String getParameters() {        return "Reconstructed root stats";    }    /*.................................................................................................................*/    public String getAuthors() {        return "Peter E. Midford, Ted Garland Jr., and Wayne P. Maddison";    }    /*.................................................................................................................*/    public String getVersion() {        return "1.15";    }    /*.................................................................................................................*/    public boolean isPrerelease() {        return false;    }    /*.................................................................................................................*/    public String getName() {        return "Reconstructed Root Value";    }    /*.................................................................................................................*/    public String getExplanation() {        return "Calculates statistics related to a reconstruction of the character at the root.";    }}