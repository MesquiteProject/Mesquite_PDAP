/* PDAP:PDTREE package for Mesquite  copyright 2001-2009 P. Midford & W. MaddisonPDAP:PDTREE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.The web site for PDAP:PDTREE is http://mesquiteproject.org/pdap_mesquite/This source code and its compiled class files are free and modifiable under the terms of GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html) */package mesquite.pdap.ScatterRegDiagnostics;/*~~  */import java.util.List;import java.awt.*;import java.util.*;import mesquite.lib.*;import mesquite.lib.ui.ChartExtra;import mesquite.lib.ui.Charter;import mesquite.lib.ui.MesquiteChart;import mesquite.pdap.lib.*;public class ScatterRegDiagnostics extends PDAPScatterAsst  {    public void getEmployeeNeeds(){  //This gets called on startup to harvest information; override this and inside, call registerEmployeeNeed        EmployeeNeed e = registerEmployeeNeed(StatPakSource.class, getName() + "  needs a method to calculate correlations.",        "The method to calculate correlations is selected initially");    }        private List<ChartExtra> extras;    private StatPakSource sps;    public boolean startJob(String arguments, Object condition, boolean hiredByName) {        extras = new ArrayList<ChartExtra>();        sps = (StatPakSource)hireNamedEmployee(StatPakSource.class, "#SPRegDiagnostics");        if(sps == null)            return false;        return true;    }    /** 	@return String containing legend text     */    public String getLegendText(){        return "Black line is ordinary least squares regression.";    }    public boolean suppliesWritableResults(){        return (sps.getStatPak() != null);    }    public Object getResultsHeading() {        return getName();    }    /**     * @return computed values with descriptive strings     */    public MesquiteNumber[] getWritableResults() {        if (extras.size() == 1){            SRDExtra extra = (SRDExtra)extras.get(0);            return extra.getWritableResults();        }        else             return sps.getStatPak().getWritableResults();    }    /**	@return boolean - always false     */    public boolean canHireMoreThanOnce () {        return false;    }    /**	@param String commandName	@param String argments	@param CommandChecker checker	Dispatches a script command     */    public Object doCommand(String commandName, String arguments, CommandChecker checker) {        if (checker.compare(this.getClass(), "Gets correl coef", null, commandName, "getCorrelationCoefficient")) {            if (extras.size() == 1 && extras.get(0) != null){                SRDExtra extra = (SRDExtra)extras.get(0);                return new MesquiteNumber(extra.getValue(commandName));            }        }        else if (checker.compare(this.getClass(), "Gets p value", null, commandName, "getPValue")) {            if (extras.size() == 1 && extras.get(0) != null){                SRDExtra extra = (SRDExtra)extras.get(0);                return new MesquiteNumber(extra.getValue(commandName));            }        }        else            return  super.doCommand(commandName, arguments, checker);        return null;    }    /**     */    public ChartExtra createExtra(MesquiteChart chart){        ChartExtra s = new SRDExtra(this, chart, sps);        extras.add(s);        return s;    }    /** returns module name */    public String getName(){        return "Scattergram Regression Diagnostics";    }    /*.................................................................................................................*/    public String getAuthors() {        return "Peter E. Midford, Ted Garland Jr., and Wayne P. Maddison" ;    }    /*.................................................................................................................*/    /** returns module version */    public String getVersion() {        return "1.15";    }    /*.................................................................................................................*/    public boolean isPrerelease() {        return false;    }    /*.................................................................................................................*/    public boolean isSubstantive() {        return false;    }    /** returns an explanation of what the module does.*/    public String getExplanation() {        return "Calculates statistics and regression lines for values on a scattergram." ;    }    /*.................................................................................................................*/    /** cleans up extras when the job ends */    public void endJob(){        if (extras != null) {            for (int i=0; i<extras.size(); i++){                if (extras.get(i) != null){                    ChartExtra extra = (ChartExtra)extras.get(i);                    extra.turnOff();                }                else                     MesquiteMessage.warnProgrammer("Found a null entry in the list of chartExtras of the ScatterRegDiagnostics while ending");            }        }        super.endJob();    }}/** */class SRDExtra extends ChartExtra {    PDAPStatPak myStats;    StatPakSource statPakSource;    public SRDExtra(MesquiteModule ownerModule, MesquiteChart chart, StatPakSource sps){        super(ownerModule, chart);        statPakSource = sps;    }    /**Do any calculations needed*/    public boolean doCalculations(){        if (chart!=null){            myStats = statPakSource.getStatPak();			            boolean munch = myStats.doCalculations(chart.getXArray(), chart.getYArray(), chart);            if (munch)                chart.munch();        }        return false;    }    /**     * This draws the colored line segments for the three different regression models that will     * be dumped below.  In this module, lines are drawn over the range of x values, compare with     * the method in ScatterOriDiagnostics.     * This draws the line for the OLS regression models that will     * be dumped below.  In this module, lines are drawn over the range of x values, compare with     * the method in ScatterOriDiagnostics.     * @param g     */    public void drawOnChart(Graphics g){        if (myStats==null)            return;        final double ls = myStats.getLeastSquaresSlope();        if (!MesquiteDouble.isCombinable(ls))            return;        final double min0 = myStats.getMin(0);        final double max0 = myStats.getMax(0);        final double lsy = myStats.getLeastSquaresYIntercept();        Charter c = chart.getCharter();        final int startX = c.xToPixel(min0,chart);        final int startYOLS =  c.yToPixel(lsy + ls*min0,chart);        final int endX = c.xToPixel(max0,chart);        final int endYOLS = c.yToPixel(lsy + ls*max0,chart);        // currently not showing these...        //int startYRMA =  c.yToPixel(rmay + rma*min[0],chart);         //int endYRMA = c.yToPixel(rmay + rma*max[0],chart);        //int startYMA =  c.yToPixel(may + ma*min[0],chart);        //int endYMA = c.yToPixel(may + ma*max[0],chart);        int extraLineWidth = 1;        g.setColor(Color.black);        g.drawLine(startX, startYOLS,endX,endYOLS);        if (Math.abs(endYOLS-startYOLS)>(endX-startX))   // abs(slope) > 1            g.drawLine(startX+extraLineWidth,startYOLS,endX+extraLineWidth,endYOLS);        else                g.drawLine(startX,startYOLS+extraLineWidth,endX,endYOLS+extraLineWidth);            }    /**print on the chart*/    public void printOnChart(Graphics g){        drawOnChart(g);    }    public String writeOnChart(){        return myStats.flst();		    }    /**	@param String commandName specifies the field from the statPak to return	@return double the value requested or MesquiteDouble.unassigned     */    public double getValue(String commandName) {        if (commandName.equals("getCorrelationCoefficient")) {            return myStats.getCorrelationCoefficient();        }        if (commandName.equals("getPValue")) {            return myStats.getPValue();        }        else return MesquiteDouble.unassigned;    }    protected MesquiteNumber[] getWritableResults(){        myStats = statPakSource.getStatPak();                   myStats.doCalculations(chart.getXArray(), chart.getYArray(), chart);        return myStats.getWritableResults();    }    /**to inform ChartExtra that cursor has just entered point*/    public void cursorEnterPoint(int point, int exactPoint, Graphics g){        //Debugg.println("enter point " + point);    }    /**to inform ChartExtra that cursor has just exited point*/    public void cursorExitPoint(int point, int exactPoint, Graphics g){        //Debugg.println("exit point " + point);    }    /**to inform ChartExtra that cursor has just touched point*/    public void cursorTouchPoint(int point, int exactPoint, Graphics g){        //Debugg.println("touch point " + point);    }}