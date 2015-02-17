/* PDAP:PDTREE package for Mesquite  copyright 2001-2009 P. Midford & W. Maddison
PDAP:PDTREE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
The web site for PDAP:PDTREE is http://mesquiteproject.org/pdap_mesquite/

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */
package mesquite.pdap.RootReconstructionCIs;

import mesquite.cont.lib.ContinuousDistribution;
import mesquite.cont.lib.ContinuousStateTest;
import mesquite.lib.CommandChecker;
import mesquite.lib.CompatibilityTest;
import mesquite.lib.EmployeeNeed;
import mesquite.lib.MesquiteDouble;
import mesquite.lib.MesquiteFile;
import mesquite.lib.MesquiteInteger;
import mesquite.lib.MesquiteMessage;
import mesquite.lib.MesquiteModule;
import mesquite.lib.MesquiteNumber;
import mesquite.lib.MesquiteString;
import mesquite.lib.MesquiteSubmenuSpec;
import mesquite.lib.MesquiteTree;
import mesquite.lib.NumberArray;
import mesquite.lib.Snapshot;
import mesquite.lib.StringArray;
import mesquite.lib.StringUtil;
import mesquite.lib.Tree;
import mesquite.lib.characters.CharacterDistribution;
import mesquite.lib.duties.NumberFor2CharAndTree;
import mesquite.pdap.SPRootReconstruction.SPRootReconstruction;
import mesquite.pdap.lib.ContrastForChar;
import mesquite.pdap.lib.NoPolyTree;
import mesquite.pdap.lib.PDAPRootReconStatPak;

public class RootReconstructionCIs extends NumberFor2CharAndTree {

    public void getEmployeeNeeds(){  //This gets called on startup to harvest information; override this and inside, call registerEmployeeNeed
        EmployeeNeed e = registerEmployeeNeed(ContrastForChar.class, getName() + "  needs a method to calculate contrasts.",
        "The method to write FIC reports is selected initially");
        EmployeeNeed e1 = registerEmployeeNeed(SPRootReconstruction.class, getName() + "  needs a method to calculate correlations.",
        "The method to calculate correlations is selected initially");
    }



    static final int REPORT_upper_x = 0;
    static final int REPORT_lower_x = 1;
    static final int REPORT_upper_y = 2;
    static final int REPORT_lower_y = 3;

    private ContrastForChar contrastsTask;
    private SPRootReconstruction statPakTask;
    private NumberArray xArray, yArray;
    private StringArray modes;
    private MesquiteString modeName;
    private int reportMode = REPORT_upper_x;
    private double ciWidth = 0.95;
    private int dfReduce = 0;
    private boolean ignoreRootTritomies = false;

    public boolean startJob(String arguments, Object condition, boolean hiredByName) {
        contrastsTask = (ContrastForChar)hireNamedEmployee(ContrastForChar.class, "#FelsContrWithChar");
        if (contrastsTask == null)
            return sorry(getName() + " couldn't start because no calculator of Felsenstein's contrasts obtained.");
        contrastsTask.setOption(ContrastForChar.CONTRAST);
        statPakTask = (SPRootReconstruction)hireNamedEmployee(SPRootReconstruction.class, "#SPRootReconstruction");
        if (statPakTask == null)
            return sorry(getName() + " couldn't start because no calculator of correlations obtained.");
        xArray = new NumberArray(3);
        yArray = new NumberArray(3);
        modes = new StringArray(4);  
        modes.setValue(REPORT_upper_x, "upper-CI-X");  //the strings passed will be the menu item labels
        modes.setValue(REPORT_lower_x, "lower-CI-X");
        modes.setValue(REPORT_upper_y, "upper-CI-Y");
        modes.setValue(REPORT_lower_y, "lower-CI-Y");
        modeName = new MesquiteString(modes.getValue(reportMode));  //this helps the menu keep track of checkmenuitems
        final MesquiteSubmenuSpec mss = addSubmenu(null, "Select CI", makeCommand("setReportMode", this), modes); 
        mss.setSelected(modeName);
        return true; //false if no appropriate employees!
    }

    public CompatibilityTest getCompatibilityTest(){
        return new ContinuousStateTest();
    }
    public void employeeQuit(MesquiteModule m){
        if (m==contrastsTask)
            iQuit();
    }

    public Snapshot getSnapshot(MesquiteFile file) {
        final Snapshot temp = new Snapshot();
        temp.addLine("setReportMode " + StringUtil.tokenize(modes.getValue(reportMode)));
        return temp;
    }


    public Object doCommand(String commandName, String arguments, CommandChecker checker) {
        final Class thisClass = getClass();
        if (checker.compare(thisClass, "Sets the report mode", "[One of 'upper-CI-X','lower-CI-X', 'upper-CI-Y', 'lower-CI-Y']", commandName, "setReportMode")) {
            final String name = parser.getFirstToken(arguments); //get argument passed of option chosen
            int newMode = modes.indexOf(name); //see if the option is recognized by its name
            if (newMode >=0 && newMode!=reportMode){
                reportMode = newMode; //change mode
                modeName.setValue(modes.getValue(reportMode)); //so that menu item knows to become checked
                parametersChanged(); //this tells employer module that things changed, and recalculation should be requested
            }
        }
        else if (checker.compare(thisClass,"Sets the CI width", "[double]",commandName, "setWidth")) {
            final double w1 = MesquiteDouble.fromString(arguments);
            if (!MesquiteDouble.isCombinable(w1))  //not a menu command..
                MesquiteMessage.warnProgrammer("setWidth invoked without parameter");
            else
                ciWidth = w1;
        }  
        else if (checker.compare(thisClass,"Sets the degrees of freedom adjustment", "[integer]",commandName, "setDFReduction")){
            final int dfr = MesquiteInteger.fromString(arguments);
            if (!MesquiteInteger.isCombinable(dfr))
                MesquiteMessage.warnProgrammer("setDFReduction invoked without parameter");
            else
                dfReduce = dfr;
        }
        else if (checker.compare(thisClass,"Sets whether to ignore a tritomy at the root ", "[boolean]",commandName, "toggleIgnoreRootTritomies")){
            if (StringUtil.blank(arguments)) //not a menu command..
                MesquiteMessage.warnProgrammer("toggleIgnoreRootTritomies invoked without parameter");
            else{
                final String s = parser.getFirstToken(arguments);
                if ("on".equalsIgnoreCase(s))
                    ignoreRootTritomies = true;
                else if  ("off".equalsIgnoreCase(s))
                    ignoreRootTritomies = false;
                else
                    ignoreRootTritomies = !ignoreRootTritomies;
            }   
        }
        else
            return super.doCommand(commandName, arguments, checker);
        return null;
    }

    /** Called to provoke any necessary initialization.  This helps prevent the module's intialization queries to the user from
     happening at inopportune times (e.g., while a long chart calculation is in mid-progress)*/
    public void initialize(Tree tree, CharacterDistribution charStatesX, CharacterDistribution charStatesY){
    }


    public void calculateNumber(Tree tree, CharacterDistribution charStatesX, CharacterDistribution charStatesY, MesquiteNumber result, MesquiteString resultString) {
        if (result==null)
            return;
        clearResultAndLastResult(result);
        if (tree==null || charStatesX ==null || charStatesY ==null) {
            if (resultString!=null)
                resultString.setValue("Root reconstruction confidence interval can't be calculated because tree or characters not supplied");
        }
        else if (!(charStatesX instanceof ContinuousDistribution) || !(charStatesY instanceof ContinuousDistribution)) {
            if (resultString!=null)
                resultString.setValue("Root reconstruction confidence interval can't be calculated because characters supplied aren't continuous");
        }
        else {
            final int numNodes = tree.getNumNodeSpaces();
            xArray.resetSize(numNodes);
            yArray.resetSize(numNodes);
            MesquiteNumber resultX = new MesquiteNumber();
            MesquiteNumber resultY = new MesquiteNumber();
            contrastsTask.calculateNumbers(tree, charStatesX, xArray, null);
            contrastsTask.calculateNumbers(tree, charStatesY, yArray, null);
            for (int i=0; i<numNodes; i++) {
                if (tree.nodeInTree(i) && tree.nodeIsInternal(i)) { //todo: have toggle for internal only
                    xArray.placeValue(i, resultX);
                    yArray.placeValue(i, resultY);
                    if (resultX.isNegative()) {
                        resultY.changeSign();
                        resultX.abs();
                        xArray.setValue(i, resultX);
                        yArray.setValue(i, resultY);
                    }
                }
            }
            NoPolyTree localTree;
            if (tree instanceof NoPolyTree){
                localTree = (NoPolyTree) tree;
            }
            else {
                localTree = new NoPolyTree((MesquiteTree)tree);
            }
            final PDAPRootReconStatPak myStats = (PDAPRootReconStatPak)statPakTask.getStatPak();
            myStats.setDFReduction(dfReduce);
            myStats.setTree(localTree);
            myStats.setObserved1((ContinuousDistribution) charStatesX);
            myStats.setObserved2((ContinuousDistribution) charStatesY);
            myStats.setWidth1(ciWidth);
            myStats.setIgnoreRootTritomies(ignoreRootTritomies);
            myStats.doCalculations(xArray, yArray, null);
            if (reportMode == REPORT_upper_x){
                result.setValue((myStats.getMean(0)+myStats.getRootCI(0)));
                resultString.setValue("Root reconstruction x upper CI: " + result);
            }
            else if (reportMode == REPORT_lower_x){
                result.setValue((myStats.getMean(0)-myStats.getRootCI(0)));
                resultString.setValue("Root reconstruction x upper CI: " + result);
            }
            else if (reportMode == REPORT_upper_y){
                result.setValue((myStats.getMean(1)+myStats.getRootCI(1)));
                resultString.setValue("Root reconstruction y upper CI: " + result);
            }
            else if (reportMode == REPORT_lower_y){
                result.setValue((myStats.getMean(1)-myStats.getRootCI(1)));
                resultString.setValue("Root reconstruction y upper CI: " + result);
            }
        }
        saveLastResult(result);
        saveLastResultString(resultString);
    }

    /*.................................................................................................................*/
    public String getParameters(){
        return "Root Reconstruction Confidence Interval, reporting " + modeName.getValue();
    }
    /*.................................................................................................................*/
    public String getName() {
        return "Root Reconstruction CI";
    }
    /*.................................................................................................................*/
    /** Returns the name of the module in very short form.  For use for column headings and other constrained places.  Unless overridden returns getName()*/
    public String getVeryShortName(){
        return "Correlation";
    }
    /*.................................................................................................................*/
    public String getVersion() {
        return "1.15";
    }
    /*.................................................................................................................*/
    public boolean isPrerelease() {
        return false;
    }



}
