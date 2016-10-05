/* PDAP:PDTREE package for Mesquite  copyright 2001-2010 P. Midford & W. Maddison
PDAP:PDTREE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
The web site for PDAP:PDTREE is http://mesquiteproject.org/pdap_mesquite/

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */
package mesquite.pdap.NodeValueNodeHeightCorrelation;

import mesquite.cont.lib.ContinuousData;
import mesquite.cont.lib.ContinuousDistribution;
import mesquite.cont.lib.ContinuousHistory;
import mesquite.cont.lib.ContinuousState;
import mesquite.cont.lib.ContinuousStateTest;
import mesquite.lib.CommandChecker;
import mesquite.lib.CompatibilityTest;
import mesquite.lib.EmployeeNeed;
import mesquite.lib.ExtensibleDialog;
import mesquite.lib.MesquiteFile;
import mesquite.lib.MesquiteInteger;
import mesquite.lib.MesquiteModule;
import mesquite.lib.MesquiteNumber;
import mesquite.lib.MesquiteString;
import mesquite.lib.MesquiteSubmenuSpec;
import mesquite.lib.MesquiteThread;
import mesquite.lib.MesquiteTree;
import mesquite.lib.NumberArray;
import mesquite.lib.RadioButtons;
import mesquite.lib.Snapshot;
import mesquite.lib.StringArray;
import mesquite.lib.StringUtil;
import mesquite.lib.Tree;
import mesquite.lib.characters.CharacterDistribution;
import mesquite.lib.characters.CharacterHistory;
import mesquite.lib.duties.NumberForCharAndTree;
import mesquite.parsimony.ParsimonySquared.ParsimonySquared;
import mesquite.parsimony.lib.ParsimonyModel;
import mesquite.parsimony.lib.SquaredModel;
import mesquite.pdap.lib.ContrastForChar;
import mesquite.pdap.lib.NoPolyTree;
import mesquite.pdap.lib.PDAPStatPak;
import mesquite.pdap.lib.StatPakSource;

public class NodeValueNodeHeightCorrelation extends NumberForCharAndTree {

    public void getEmployeeNeeds(){  //This gets called on startup to harvest information; override this and inside, call registerEmployeeNeed
        EmployeeNeed e = registerEmployeeNeed(StatPakSource.class, getName() + "  needs a method to provide PDAP StatPaks.",
        "The method to supply PDAP Regression STATPAKs is selected initially");
        EmployeeNeed e1 = registerEmployeeNeed(ContrastForChar.class, getName() + "  needs a method to calculate contrasts.",
        "The method to calculate contrasts is selected initially");
        EmployeeNeed e2 = registerEmployeeNeed(ParsimonySquared.class, getName() + " needs a method to calculate node values using squared-change parsimony",
         "The method to calculate squared-change parsimony reconstructions is selected initially"   );
    }
    
    final static String SETNODERECONSTRUCTORSTR = "setNodeReconstructor";

    ContrastForChar contrastsTask;
    StatPakSource statPakTask;
    ParsimonySquared parsimonyTask;
    ParsimonyModel squaredModel;
    MesquiteNumber stepsNumber = new MesquiteNumber(0);  // not sure what this is for
    NumberArray xArray;
    NumberArray yArray;
    StringArray modes;
    MesquiteString modeName;
    static final int REPORT_ContrastValue = 0;
    static final int REPORT_SQCPValue = 1;
    int calculationMode = REPORT_ContrastValue;
    
    public boolean startJob(String arguments, Object condition, boolean hiredByName) {

        contrastsTask = (ContrastForChar)hireNamedEmployee(ContrastForChar.class, "#FelsContrWithChar");
        if (contrastsTask == null)
            return sorry(getName() + " couldn't start because no calculator of Felsenstein's contrasts obtained.");
        contrastsTask.setOption(ContrastForChar.CORRECTEDHEIGHT);
        statPakTask = (StatPakSource)hireNamedEmployee(StatPakSource.class, "#SPRegDiagnostics");
        if (statPakTask == null)
            return sorry(getName() + " couldn't start because no calculator of correlation obtained.");
        parsimonyTask = (ParsimonySquared)hireNamedEmployee(ParsimonySquared.class, "#ParsimonySquared");
        if (parsimonyTask == null)
            return sorry(getName() + " couldn't start because no calculator of squared-change parsimony obtained.");
        squaredModel = new SquaredModel("Squared", ContinuousState.class);
        xArray = new NumberArray(3);
        yArray = new NumberArray(3);
        modes = new StringArray(3);  
        modes.setValue(REPORT_ContrastValue, "Node value used by Contrast calculation (downpass)");  //the strings passed will be the menu item labels
        modes.setValue(REPORT_SQCPValue, "Node value calculated by Squared-Change Parsimony");
        modeName = new MesquiteString(modes.getValue(calculationMode));  //this helps the menu keep track of checkmenuitems
        MesquiteSubmenuSpec mss = addSubmenu(null, "Reconstruct node values using", makeCommand(SETNODERECONSTRUCTORSTR, this), modes); 
        mss.setSelected(modeName);
        if (!MesquiteThread.isScripting())
            showNodeReconstructorDialog();
        return true; //false if no appropriate employees!
    }

    
    private void showNodeReconstructorDialog(){
        MesquiteInteger buttonPressed = new MesquiteInteger(1);
        ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), "How to Report Correlation?",  buttonPressed);
        dialog.addLabel("How should the contrast correlation be reported?");
        String[] labels = new String[]{modes.getValue(REPORT_ContrastValue), modes.getValue(REPORT_SQCPValue)};

        dialog.addBlankLine();
        RadioButtons radio = dialog.addRadioButtons(labels, 0);
        dialog.addBlankLine();

        dialog.completeAndShowDialog(true);

        boolean ok = (dialog.query()==0);
        if (ok){
            calculationMode = radio.getValue(); //change mode
            modeName.setValue(modes.getValue(calculationMode)); //so that menu item knows to become checked
        }
    }

    public boolean requestPrimaryChoice(){
        return true;
    }
    public CompatibilityTest getCompatibilityTest(){
        return new ContinuousStateTest();
    }
    public void employeeQuit(MesquiteModule m){
        if (m==contrastsTask)
            iQuit();
    }

    /**
     * add this method, which is used on file saving to remember what option was being used by this module.  
     * This is not a global preference, but is used during file reading when
     * Mesquite attempts to recover the analysis as done previously
     * 
     * @param file file that will receive the snapshot (though not used here)
     * */
    public Snapshot getSnapshot(MesquiteFile file) {
        final Snapshot temp = new Snapshot();
        temp.addLine(SETNODERECONSTRUCTORSTR + " " + StringUtil.tokenize(modes.getValue(calculationMode)));
        return temp;
    }

    /**
     *  Add this method, which is the main command handling method.
     */
    public Object doCommand(String commandName, String arguments, CommandChecker checker) {
        if (checker.compare(getClass(), "Sets the calculation mode", "[One of 'Contrast Node Value', or 'Squared-change parsimony']", commandName, SETNODERECONSTRUCTORSTR)) {
            String name = parser.getFirstToken(arguments); //get argument passed of option chosen
            int newMode = modes.indexOf(name); //see if the option is recognized by its name
            if (newMode >=0 && newMode!=calculationMode){
                calculationMode = newMode; //change mode
                modeName.setValue(modes.getValue(calculationMode)); //so that menu item knows to become checked
                parametersChanged(); //this tells employer module that things changed, and recalculation should be requested
            }
        }
        else
            return  super.doCommand(commandName, arguments, checker);
        return null;
    }

    /** Called to provoke any necessary initialization.  This helps prevent the module's intialization queries to the user from
    happening at inopportune times (e.g., while a long chart calculation is in mid-progress)*/
    public void initialize(Tree tree, CharacterDistribution charStates){
    }

    
    public  void calculateNumber(Tree tree, CharacterDistribution charStates, MesquiteNumber result, MesquiteString resultString) {  
        if (result==null)
            return;
        clearResultAndLastResult(result);
        if (tree==null || charStates ==null) {
            if (resultString!=null)
                resultString.setValue("Node Value / Node Height Correlation can't be calculated because tree or characters not supplied");
        }
        else if (!(charStates instanceof ContinuousDistribution)) {
            if (resultString!=null)
                resultString.setValue("Node Value / Node Height Correlation can't be calculated because characters supplied aren't continuous");
        }
        else {
            final int numNodes = tree.getNumNodeSpaces();
            xArray.resetSize(numNodes);
            yArray.resetSize(numNodes);
                
            if (calculationMode == REPORT_ContrastValue){
                contrastsTask.setOption(ContrastForChar.CORRECTEDHEIGHT);
                contrastsTask.calculateNumbers(tree, charStates, xArray, null);
                contrastsTask.setOption(ContrastForChar.NODEVALUE);
                contrastsTask.calculateNumbers(tree,charStates, yArray,null);
            }
            else {
                contrastsTask.setOption(ContrastForChar.RAWHEIGHT);
                contrastsTask.calculateNumbers(tree,charStates, yArray,null);
                CharacterHistory statesAtNodes = new ContinuousHistory(tree.getTaxa(), tree.getNumNodeSpaces(),(ContinuousData) charStates.getParentData());
                statesAtNodes = charStates.adjustHistorySize(tree, statesAtNodes);

               // SquaredReconstructor foo = new SquaredReconstructor();
               // parsimonyTask.calculateSteps(tree, charStates, squaredModel, resultString, stepsNumber);
               // Debugg.println("Number of Steps = " + stepsNumber.toString());
                parsimonyTask.calculateStates(tree, charStates, statesAtNodes, squaredModel, resultString, stepsNumber);
                fillNodes(tree,tree.getRoot(),(ContinuousHistory)statesAtNodes,xArray);                
            }
            //because setting the df adjustment may not be possible in this context
            //this will default to the most conservative df adjustment (e.g. adjust by
            //# of polytomies).
            NoPolyTree localTree;
            if (tree instanceof NoPolyTree){
                localTree = (NoPolyTree) tree;
            }
            else {
                localTree = new NoPolyTree((MesquiteTree)tree);
            }
            //filter out unused array locations
            for (int i=0; i<numNodes; i++) {
                if (!tree.nodeInTree(i) || !tree.nodeIsInternal(i)){
                    xArray.setToUnassigned(i);
                    yArray.setToUnassigned(i);
                }
            }
            
            PDAPStatPak myStats = statPakTask.getStatPak();
            myStats.doCalculations(xArray, yArray, null);
            result.setValue(myStats.getCorrelationCoefficient());
            if (calculationMode== REPORT_ContrastValue){
                resultString.setValue("Correlation of Node Value (contrast calculation) / Node Height: " + result);
            }
            else {
                resultString.setValue("Correlation of Node Value (squared-change parsimony) / Node Height: " + result);
            }
        }
        saveLastResult(result);
        saveLastResultString(resultString);
    }

    /**
     * 
     * @param node
     * @return
     */
    private NumberArray fillNodes(Tree tree, int node, ContinuousHistory history, NumberArray x) {
        for (int daughter = tree.firstDaughterOfNode(node); tree.nodeExists(daughter); daughter = tree.nextSisterOfNode(daughter))
            fillNodes(tree, daughter, history,x);
        x.setValue(node,history.getState(tree.taxonNumberOfNode(node), 0));  
        return x;
    }

    /*.................................................................................................................*/
    public String getParameters(){
        return "Node Value / Node Height, reporting " + modeName.getValue();
    }
    /*.................................................................................................................*/
    public String getName() {
        return "Node Value / Node Height Correlation";
    }
    /*.................................................................................................................*/
    /** Returns the name of the module in very short form.  For use for column headings and other constrained places.  Unless overridden returns getName()*/
    public String getVeryShortName(){
        return "Correlation";
    }
    /*.................................................................................................................*/
    public String getVersion() {
        return "1.16";
    }
    /*.................................................................................................................*/
    public boolean isPrerelease() {
        return true;
    }
    /** marks the module as doing a substantive calculation - not decorative */
    public boolean isSubstantive(){
        return true;
    }

    /*.................................................................................................................*/
    /** returns an explanation of what the module does.*/
    public String getExplanation() {
        return "Calculates the Pearson product-moment correlation between reconstructed values of each node and its height above the root.  Construction can be as in contrast calculations or using squared-change parsimony." ;
    }

}
