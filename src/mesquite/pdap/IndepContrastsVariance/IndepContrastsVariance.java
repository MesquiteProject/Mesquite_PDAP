/* PDAP:PDTREE package for Mesquite  copyright 2001-2009 P. Midford & W. MaddisonPDAP:PDTREE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.The web site for PDAP:PDTREE is http://mesquiteproject.org/pdap_mesquite/This source code and its compiled class files are free and modifiable under the terms of GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html) */package mesquite.pdap.IndepContrastsVariance;/*~~  */import mesquite.lib.*;import mesquite.lib.characters.*;import mesquite.cont.lib.*;import mesquite.lib.duties.*;import mesquite.lib.tree.Tree;import mesquite.pdap.lib.*;/** ======================================================================== *///Peter: this is a new module (see also NumForTreeWith2Chars in Basic).public class IndepContrastsVariance extends NumberForCharAndTree {    public void getEmployeeNeeds(){  //This gets called on startup to harvest information; override this and inside, call registerEmployeeNeed        EmployeeNeed e = registerEmployeeNeed(ContrastForChar.class, getName() + "  needs a method to calculate contrasts.",        "The method to write FIC reports is selected initially");        EmployeeNeed e1 = registerEmployeeNeed(PDAP2CTStatPakSource.class, getName() + "  needs a method to calculate correlations.",        "The method to calculate correlations is selected initially");    }    ContrastForChar contrastsTask;    PDAP2CTStatPakSource statPakTask;    private NumberArray xArray;    /*.................................................................................................................*/    public boolean startJob(String arguments, Object condition, boolean hiredByName) {        contrastsTask = (ContrastForChar)hireNamedEmployee(ContrastForChar.class, "#FelsContrWithChar");        if (contrastsTask == null)            return sorry(getName() + " couldn't start because no calculator of Felsenstein's contrasts obtained.");        contrastsTask.setOption(ContrastForChar.CONTRAST);        statPakTask = (PDAP2CTStatPakSource)hireNamedEmployee(PDAP2CTStatPakSource.class, "#SPOriDiagnostics");        if (statPakTask == null)            return sorry(getName() + " couldn't start because no calculator of correlation obtained.");        xArray = new NumberArray(3);        return true; //false if no appropriate employees!    }        /**     * Quits if the contrasts calculator does.     */    public void employeeQuit(MesquiteModule m){        if (m==contrastsTask)            iQuit();    }    /** Called to provoke any necessary initialization.  This helps prevent the module's intialization queries to the user from   	happening at inopportune times (e.g., while a long chart calculation is in mid-progress)*/    public void initialize(Tree tree, CharacterDistribution charStatesX){    }        /*.................................................................................................................*/    /*in future this will farm out the calculation to the modules that deal with appropriate character model*/    public  void calculateNumber(Tree tree, CharacterDistribution charStatesX, MesquiteNumber result, MesquiteString resultString) {          if (result==null)            return;        clearResultAndLastResult(result);        if (tree==null || charStatesX ==null) {            if (resultString!=null)                resultString.setValue("Variance in Independent Contrasts can't be calculated because tree or character not supplied");        }        else if (!(charStatesX instanceof ContinuousDistribution)) {            if (resultString!=null)                resultString.setValue("Variance in Independent Contrasts can't be calculated because character supplied isn't continuous");        }        else {            int numNodes = tree.getNumNodeSpaces();            xArray.resetSize(numNodes);            contrastsTask.calculateNumbers(tree, charStatesX, xArray, null);            PDAP2CTStatPak myStats = statPakTask.getStatPak();             myStats.setTree(tree);            myStats.setObserved1((ContinuousDistribution)charStatesX);            myStats.setObserved2((ContinuousDistribution)charStatesX);            myStats.doCalculations(xArray, xArray, null);            result.setValue(myStats.getVariance(0));            resultString.setValue("Variance in independent contrasts: " + result);        }        saveLastResult(result);        saveLastResultString(resultString);    }    /*.................................................................................................................*/    /*.................................................................................................................*/    public String getName() {        return "Variance in Independent Contrasts";    }    /*.................................................................................................................*/    /** Returns the name of the module in very short form.  For use for column headings and other constrained places.  Unless overridden returns getName()*/    public String getVeryShortName(){        return "Indep. Contrasts Variance";    }    /*.................................................................................................................*/    public CompatibilityTest getCompatibilityTest(){        return new ContinuousStateTest();    }    /*.................................................................................................................*/    /** Returns a version number string for the module     * @return String     */    public String getVersion() {        return "1.15";    }    /*.................................................................................................................*/    /** Marks the module as NOT being a prerelease     * @return boolean - constant false      */    public boolean isPrerelease() {        return false;    }      /*.................................................................................................................*/    /** Returns a string identifying the authors of the module     * @return String      */    public String getAuthors() {        return "Wayne P. Maddison, Peter E. Midford, and Ted Garland Jr.";    }    /*.................................................................................................................*/    /** returns an explanation of what the module does.*/    public String getExplanation() {        return "Calculates the variance in standardized Felsenstein's independent contrasts of a character on a tree." ;    }}