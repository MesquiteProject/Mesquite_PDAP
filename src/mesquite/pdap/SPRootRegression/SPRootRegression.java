/* PDAP:PDTREE package for Mesquite  copyright 2001-2008 P. Midford & W. Maddison
PDAP:PDTREE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
The web site for PDAP:PDTREE is http://mesquiteproject.org/pdap_mesquite/

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)

*/
package mesquite.pdap.SPRootRegression;

import mesquite.cont.lib.ContinuousDistribution;
import mesquite.lib.MesquiteDouble;
import mesquite.lib.MesquiteInteger;
import mesquite.lib.MesquiteMessage;
import mesquite.lib.MesquiteNumber;
import mesquite.lib.NumberArray;
import mesquite.lib.ui.MesquiteChart;
import mesquite.pdap.lib.BivariateContrastCalculator;
import mesquite.pdap.lib.NoPolyTree;
import mesquite.pdap.lib.PDAPRootRegStatPak;
import mesquite.pdap.lib.PDAPStatPak;
import mesquite.pdap.lib.PDAPTreeWinAsstC;
import mesquite.pdap.lib.StatPakSource;
import JSci.maths.statistics.TDistribution;

public class SPRootRegression extends StatPakSource{
    
    public boolean startJob(String arguments, Object condition, boolean hiredByName) {
        return true;
    }


    public PDAPStatPak getStatPak(){
        return new RegStatPak();
    }
    
    public String getName(){
        return "Source of statPaks for root regression";
    }
        
    /*.................................................................................................................*/
    public String getAuthors() {
        return "Peter E. Midford, Ted Garland Jr., and Wayne P. Maddison" ;
    }
    /*.................................................................................................................*/
    /** returns module version */
    public String getVersion() {
        return "1.12";
    }
    /*.................................................................................................................*/
     public boolean isPrerelease() {
        return false;
    }
    /*.................................................................................................................*/
    public boolean isSubstantive() {
        return false;
    }    
    /*.................................................................................................................*/
    public String getExplanation() {
        return "Calculates summary statistics and regression coefficients for a scatterplot.";
    }

    public void setChartModule(PDAPTreeWinAsstC module) {
        // TODO Auto-generated method stub
        
    }

    public void setChart(MesquiteChart chart) {
        // TODO Auto-generated method stub
        
    }

}

class RegStatPak extends PDAPRootRegStatPak {

    /*-----------------------------------------*/
    /** 
    @return double - value of the second CI/PI width, which is also used in the
    plots in CI/PI plot (screen 9B) */
    public double getWidth2(){
        return width2;
    }
    
    /*-----------------------------------------*/
    /** @param w - the value assigned to the second CI/PI width, also used in plots for
    screens 9A and 9B. */
    public void setWidth2(double w){
        width2 = w;         
    }
    
    /** Note: in order to get this method to work with these arguments, the RootStatPak needs to have
    had tree and obs1 & obs2 previously set.  After this runs, all the values will be in place for a
    root reconstruction plot or a CI/PI plot, though if Vh is not set, the Vh=0 default makes the CI
    and PI widths identical. 
    @return boolean: true if the chart needs to remunch.*/
    
    public boolean doCalculations(NumberArray xArray, NumberArray yArray, MesquiteChart chart){

        final BivariateContrastCalculator calculator = new BivariateContrastCalculator(tree, obs1, obs2);
        //boolean xOk = false;
        boolean result = false;
        min[0] = MesquiteDouble.infinite;
        min[1] = MesquiteDouble.infinite;
        max[0] = MesquiteDouble.negInfinite;
        max[1] = MesquiteDouble.negInfinite;
        
        contrasts1 = calculator.contrastCalculation();
        //xOk = calculator.Ok();
        contrasts2 = calculator.getContrasts2();
        x1 = calculator.getNodeValues();
        x2 = calculator.getNodeValues2();
        vPrime1 = calculator.getVPrime();
        countContrasts(tree);
        tStats = new TDistribution(getNumberMinus1()-getDFReduction());
        double conSlope = contrastSlope(tree,contrasts1,contrasts2,tree.getRoot());
        calcReconstructions(tree,x1,x2,contrasts1,contrasts2,vPrime1,conSlope);

        // finally, set some minimum and maximum limits for the confidence interval plot (8-20-02 PEM)
        // calculate some maximum and minimum y values, so PI lines fit on the display
        if (MesquiteDouble.isCombinable(Vh)) {
            final double min0 = min[0];
            final double max0 = max[0];
            final double incr = (max0-min0)/10.0;
            double minX = min0 - incr;
            double maxX = max0 + incr;
            for(int i=0; i < CIPIValues.size(); i++){
                if (maxX<((MesquiteDouble)CIPIValues.get(i)).getValue())
                    maxX = ((MesquiteDouble)CIPIValues.get(i)).getValue();
                if (minX>((MesquiteDouble)CIPIValues.get(i)).getValue())
                    minX = ((MesquiteDouble)CIPIValues.get(i)).getValue();
            }
            double minCIPIY1;
            double maxCIPIY1;
            double minCIPIY2;
            double maxCIPIY2;
            if (ls > 0){
                minCIPIY1 = ls*min0+lsy-calcPI(min0,0,width1);
                maxCIPIY1 = ls*max0+lsy+calcPI(max0,0,width1);
                minCIPIY2 = ls*min0+lsy-calcPI(min0,0,width2);
                maxCIPIY2 = ls*max0+lsy+calcPI(max0,0,width2);
            }
            else {
                minCIPIY1 = ls*max0+lsy-calcPI(max0,0,width1);
                maxCIPIY1 = ls*min0+lsy+calcPI(min0,0,width1);
                minCIPIY2 = ls*max0+lsy-calcPI(max0,0,width2);
                maxCIPIY2 = ls*min0+lsy+calcPI(min0,0,width2);
            }
            result = setRegressionChartLimits(minX,maxX,MesquiteDouble.minimum(minCIPIY1,minCIPIY2),
                    MesquiteDouble.maximum(maxCIPIY1,maxCIPIY2),
                    chart);                
        }
        else result = false;
        return result;
    }

    private boolean setRegressionChartLimits(double minX, double maxX, double minY, double maxY, MesquiteChart chart) {
        boolean needsRedraw = false;
        if (chart != null){    //otherwise nothing to work with...
            if (chart.getMinimumX()==null || outsideLowTolerance(chart.getMinimumX(),minX)) { 
                chart.constrainMinimumX(new MesquiteNumber(minX));
                needsRedraw = true;
            }
            if (chart.getMaximumX() == null || outsideHighTolerance(chart.getMaximumX(),maxX)) {
                chart.constrainMaximumX(new MesquiteNumber(maxX));  // 1% should overwhelm any conversion foo
                needsRedraw = true;
            }
            if (chart.getMinimumY()==null || outsideLowTolerance(chart.getMinimumY(),minY)) { 
                chart.constrainMinimumY(new MesquiteNumber(minY));
                needsRedraw = true;
            }
            if (chart.getMaximumY() == null || outsideHighTolerance(chart.getMaximumY(),maxY)) {
                chart.constrainMaximumY(new MesquiteNumber(maxY));  // 1% should overwhelm any conversion foo
                needsRedraw = true;
            }
        }
        return needsRedraw;      
    }

    
    /*-----------------------------------------*/
    /** This method returns a string describing the contents of the statPak.  This string is
    displayed at the bottom of the text pane of the diagnostic screen. */
    public String flst() {
        if (!MesquiteDouble.isCombinable(ls))
            return "";
        final StringBuffer results = new StringBuffer(1000);
        final String width100 = MesquiteInteger.toString((int)(getWidth1()*100));
        final String width200 = MesquiteInteger.toString((int)(getWidth2()*100));
        if (MesquiteDouble.isCombinable(Vh)) {
            results.append("CI/PI file columns\n\n");
            results.append("1.  Tip Name\n");
            results.append("2.  X Value\n");
            results.append("3.  Observed Y Value\n");
            results.append("4.  Predicted Y Value (Yhat)\n");
            results.append("5.  Lower " + width100 +"% Confidence Interval\n");
            results.append("6.  Upper " + width100 + "% Confidence Interval\n");
            results.append("7.  Lower " + width100 + "% Prediction Interval\n");
            results.append("8.  Upper " + width100 + "% Prediction Interval\n");
            results.append("9.  Lower " + width200 + "% Confidence Interval\n");
            results.append("10.  Upper " + width200 + "% Confidence Interval\n");
            results.append("11.  Lower " + width200 + "% Prediction Interval\n");
            results.append("12.  Upper " + width200 + "% Prediction Interval\n\n\n");
            CIPIvalues(tree,obs1,obs2,tree.getRoot(),results,"\n"," ",true);
        }
        else results.append("No CI/PI values calculated - \n One or more undefined branch lengths\n\n");
        return results.toString();
    }

    /**
    @param lineEnding the end of line character sequence
    @param tableDelimiter the end of (table) column sequence
    @return String contains the CI table
    Entry point for generating the CI data as a string to write somewhere
    */
    public String CIString(String lineEnding, String tableDelimiter, boolean convertSpaces) {
        if (!MesquiteDouble.isCombinable(ls)){
            return "";
        }
        final StringBuffer sb = new StringBuffer(1000);
        CIPIvalues(tree,obs1,obs2,tree.getRoot(),sb,lineEnding,tableDelimiter,convertSpaces);
        return sb.toString();
    }


    
    
    
    // CI/PI file columns
    
    //   1.  Tip Name
    //   2.  X Value
    //   3.  Observed Y Value
    //   4.  Predicted Y Value (Yhat)
    //   5.  Lower ',w1:2,'% Confidence Interval');
    //   6.  Upper ',w1:2,'% Confidence Interval');
    //   7.  Lower ',w1:2,'% Prediction Interval');
    //   8.  Upper ',w1:2,'% Prediction Interval');
    //   9.  Lower ',w2:2,'% Confidence Interval');
    // 10.  Upper ',w2:2,'% Confidence Interval');
    // 11.  Lower ',w2:2,'% Prediction Interval');
    // 12.  Upper ',w2:2,'% Prediction Interval');

   /** This writes one line of the CIPI table */
    private void CIPILine(double x, String yString, String label, String lineDelimiter, String tableDelimiter, StringBuffer result){
        final double yhat = ls*x + lsy;
        final double CIWidth1 = calcCI(x,0,width1);
        final double PIWidth1 = calcPI(x,0,width1);
        final double CIWidth2 = calcCI(x,0,width2);
        final double PIWidth2 = calcPI(x,0,width2);
        result.append(label);
        result.append(tableDelimiter);  
        result.append(formatForCIPI(x,tableDelimiter));   
        result.append(yString);    
        result.append(formatForCIPI(yhat,tableDelimiter)); 
        result.append(formatForCIPI(yhat-CIWidth1,tableDelimiter));  
        result.append(formatForCIPI(yhat+CIWidth1,tableDelimiter)); 
        result.append(formatForCIPI(yhat - PIWidth1,tableDelimiter));
        result.append(formatForCIPI(yhat + PIWidth1,tableDelimiter));
        result.append(formatForCIPI(yhat-CIWidth2,tableDelimiter)); 
        result.append(formatForCIPI(yhat+CIWidth2,tableDelimiter));
        result.append(formatForCIPI(yhat - PIWidth2,tableDelimiter));
        result.append(formatForCIPI(yhat + PIWidth2,tableDelimiter));
        result.append(lineDelimiter);
    }
    
    /** This handles the formating problem that MesquiteDouble.toFixedWidthString almost, but doesn't
    quite handle correctly. */
    private String formatForCIPI(double value,String tableDelimiter) {
        String tmp = MesquiteDouble.toFixedWidthString(value,15,false);
        if (value <0)
            tmp += " " + tableDelimiter;
        else 
            tmp += "  " + tableDelimiter;
        return tmp;
    }   

    /** Formats CIPI table entries for bad points */
    private void CIPIBadPoint(double x, double y, String label, String lineDelimiter, String tableDelimiter, StringBuffer result) {
        String xStr;
        String yStr;
        String yhatStr;
            
        if (MesquiteDouble.isCombinable(x)) {
            xStr = formatForCIPI(x,tableDelimiter);
            yhatStr = formatForCIPI(ls*x+lsy,tableDelimiter);
        }
        else {
            xStr = "?                " + tableDelimiter;
            yhatStr = "?                " + tableDelimiter;
        } 
        if(MesquiteDouble.isCombinable(y))
            yStr = formatForCIPI(y,tableDelimiter);
        else
            yStr = "?                " + tableDelimiter;
            result.append(label + tableDelimiter + xStr + yStr + yhatStr + " No CI/PI values calculated for missing data" + lineDelimiter);
    }

    /** This generates the rows for CI/PI values for nodes in the tree*/
    private void CIPITreevalues(NoPolyTree tree, 
                                ContinuousDistribution observed, 
                                ContinuousDistribution observed2, 
                                int node, 
                                String lineDelimiter, 
                                String tableDelimiter,
                                boolean convertSpaces,
                                StringBuffer result) {
        if (!tree.nodeIsInternal(node)) {
           double x = observed.getState(tree.taxonNumberOfNode(node), 0);
           double y = observed2.getState(tree.taxonNumberOfNode(node), 0);
           String label;
           if (convertSpaces)
               label = spaceConvertString(tree.getNodeLabel(node));
           else
               label = tree.getNodeLabel(node);
           if (MesquiteDouble.isCombinable(x) & MesquiteDouble.isCombinable(y))
              CIPILine(x,formatForCIPI(y,tableDelimiter),label,lineDelimiter,tableDelimiter,result);
           else 
              CIPIBadPoint(x, y,label,lineDelimiter,tableDelimiter,result);
        }
        else
            for (int daughter = tree.firstDaughterOfNode(node); tree.nodeExists(daughter); daughter = tree.nextSisterOfNode(daughter))
                CIPITreevalues(tree, observed, observed2, daughter,lineDelimiter,tableDelimiter,convertSpaces,result);
    }

    /** This is the main method for generating the CIPI table. */
    private void CIPIvalues(NoPolyTree tree, ContinuousDistribution observed, ContinuousDistribution observed2, int rt,StringBuffer result,String lineDelimiter, String tableDelimiter,boolean convertSpaces) {
        CIPITreevalues(tree, observed,observed2,rt,lineDelimiter,tableDelimiter,convertSpaces,result);
        for(int i = -10; i <= -1; i++){
            double x = min[0] + min[0]*i/100.0;
            String iStr = MesquiteInteger.toString(i);  // random column straightening stuff
            if ((i <0) && (i > -10))  
                iStr += " ";   
            CIPILine(x,"-99999            ",iStr,lineDelimiter,tableDelimiter,result);
        } 
        for(int i = 1; i <= 10; i++){
            double x = max[0]+max[0]*i/100.0;
            String iStr = MesquiteInteger.toString(i);  // random column straightening stuff
            if ((i >= 0) && (i < 10)) 
                iStr += "  ";
            else if (i == 10)
               iStr += " ";   
            CIPILine(x,"-99999            ",iStr,lineDelimiter,tableDelimiter,result);
        } 
        CIPILine(0.0,"-99999            ","00 ",lineDelimiter,tableDelimiter,result);
        for (int i=0; i < CIPIValues.size(); i++)
           CIPILine(CIPIValueAt(i).getValue(),"-99999            ","-99",lineDelimiter,tableDelimiter,result);
    }

    /*-----------------------------------------*/
    /** This method calculates the confidence interval of the regression estimate about a single
    point.  This is used to generate the confidence interval lines and to generate the .CI file. */
    public double calcCI(double Xi,int index1,double width) {
            double SEy;
            double rawCI;
            double deltaX = Xi-mean[index1];

            SEy = Math.sqrt(sigma2y*(((deltaX*deltaX)/x2Sum)+v_adjust));
            rawCI = CICalc.CI(tStats,(1-(1-width)/2));
            return rawCI*SEy;
    }
    
    /*-----------------------------------------*/
    /** This method calculates the prediction interval of the regression estimate about a single
    point.  This is used to generate the confidence interval lines and to generate the .CI file. */
    public double calcPI(double Xi,int index1, double width) {
            double SEyNew;
            double rawCI;
            double deltaX = Xi-mean[index1];

            SEyNew = Math.sqrt(sigma2y*(Vh+((deltaX*deltaX)/x2Sum)+v_adjust));
            rawCI = CICalc.CI(tStats, (1-(1-width)/2));
            return rawCI*SEyNew;
    }

    /**
     * nothing usual to return, this shouldn't be called
     */
    public MesquiteNumber[] getWritableResults() {
        MesquiteMessage.warnProgrammer("RootRegression doesn't support Step Through Trees...");
        return null;
    }

    
}

