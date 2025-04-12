/* PDAP:PDTREE package for Mesquite  copyright 2001-2009 P. Midford & W. MaddisonPDAP:PDTREE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.The web site for PDAP:PDTREE is http://mesquiteproject.org/pdap_mesquite/This source code and its compiled class files are free and modifiable under the terms of GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html) */package mesquite.pdap.SPRegDiagnostics;/*~~  */import mesquite.lib.*;import mesquite.lib.ui.MesquiteChart;import mesquite.pdap.lib.*;import JSci.maths.statistics.*;    // for p-value calculations/** * @author Peter E. Midford */public class SPRegDiagnostics extends StatPakSource  {    public boolean startJob(String arguments, Object condition, boolean hiredByName) {        return true;    }    public PDAPStatPak getStatPak(){        return new regStatPak();    }    public String getName(){        return "Regression Diagnostics StatPak Source";    }    /*.................................................................................................................*/    public String getAuthors() {        return "Peter E. Midford, Ted Garland Jr., and Wayne P. Maddison" ;    }    /*.................................................................................................................*/    /** returns module version */    public String getVersion() {        return "1.15";    }    /*.................................................................................................................*/    public boolean isPrerelease() {        return false;    }    /*.................................................................................................................*/    public boolean isSubstantive() {        return true;  // the statPak is substantive, but I don't think this module is...    }    public String getExplanation() {        return "Calculates summary statistics and regression coefficients for a scatterplot.";    }}/** * This statPak is for standard (not through the origin or based on residuals) regressions * @author Peter E. Midford * created 2002 * */class regStatPak extends PDAPStatPak {    double     r2 = MesquiteDouble.unassigned;    		//r squared value for least squares regression    double     ts = MesquiteDouble.unassigned;    		//T statistic    double     f = MesquiteDouble.unassigned;    		//F statistic    double 	   pvalue = MesquiteDouble.unassigned;    public regStatPak(){        super();    }    /**     * @param xValues values for first trait     * @param yValues values for second trait     * @param chart the boundaries of the chart are reset     * @return false     */    public boolean doCalculations(NumberArray xValues, NumberArray yValues, MesquiteChart chart){        // here you can put the main calculations.          resetChartBounds(chart);        MesquiteDouble sx = new MesquiteDouble(0.0);        MesquiteDouble sx2 = new MesquiteDouble(0.0);        MesquiteDouble sy = new MesquiteDouble(0.0);        MesquiteDouble sxy = new MesquiteDouble(0.0);        mean[0] = 0;        mean[1] = 0;        variance[0] = 0;        variance[1] = 0;        corr = 0;        calcBounds(xValues,yValues);        getMeans(xValues,yValues);        mean[0] /= getNumber();        mean[1] /= getNumber();		        getVars(xValues,yValues);        variance[0] /= getNumberMinus1();        variance[1] /= getNumberMinus1();        corr /= getNumberMinus1();        cov = corr;        if ((variance[0] > 0)  && (variance[1] > 0))            corr = corr/Math.sqrt(variance[0]*variance[1]);        else            corr = 0;        getSums(xValues,yValues,sx,sx2,sy,sxy);        if ((getNumber()*sx2.getValue()-sx.getValue()*sx.getValue()) != 0) {            lsregok = true;             double d = 1/(getNumber()*sx2.getValue()-sx.getValue()*sx.getValue());            ls = d*(getNumber()*sxy.getValue()-sx.getValue()*sy.getValue());            lsy = d*(sx2.getValue()*sy.getValue()-sx.getValue()*sxy.getValue());        }        else            lsregok = false;        finishStat();			        if (Math.abs(corr) <1 && df > 0)            if (ts<0)                pvalue= tStats.cumulative(ts);            else                pvalue = 1-tStats.cumulative(ts);        else            pvalue = MesquiteDouble.unassigned;        return false;    }    /**     * @return p-value saved in the statPak     */    public double getPValue(){        return pvalue;    }        private void getMeans(NumberArray data0, NumberArray data1){        for (int i = 0; i < data0.getSize(); i++)            if (data0.isCombinable(i) && data1.isCombinable(i)) {                mean[0] += data0.getDouble(i);                mean[1] += data1.getDouble(i);            }    }    private void getVars(NumberArray data0, NumberArray data1){        for (int i = 0; i < data0.getSize(); i++)            if (data0.isCombinable(i) && data1.isCombinable(i)) {                double dev0 = mean[0] - data0.getDouble(i);                double dev1 = mean[1] - data1.getDouble(i);                variance[0] += dev0*dev0;                variance[1] += dev1*dev1;                corr += dev0*dev1;            }    }    // This calculates the rma and the y intercept for rma called rmay, ma and    // the y intercept for the ma called may, r squared called r2, t statistic    // called ts, the f statistic, and the degrees of freedom called df.    private void finishStat() {	        if (variance[0]>0) {            rma = Math.sqrt(variance[1]/variance[0]);            if (corr<0) // make sure the sign of the reduced major axis slope agrees with                 rma *= -1;  // that of the correlation            rmay = mean[1]-rma*mean[0];        }        double d = Math.sqrt(((variance[0]+variance[1])*(variance[0]+variance[1])) - 4*(variance[0]*variance[1]-cov*cov));        double lambda1 = (variance[0]+variance[1]+d)/2;        // Sokal and Rolf 1981 set Y1 for vertical axis, for example on 595-596.        if (lambda1 == variance[1]) {            if (cov >0)                ma =  MesquiteDouble.infinite;            else ma = MesquiteDouble.negInfinite;        }        else ma = cov/(lambda1-variance[1]);	// This is correct variance[1] is vertical axis        may = mean[1]-ma*mean[0];        r2 = corr*corr;        if (r2<1.0) {            ts = corr*Math.sqrt((getNumber()-2)/(1-r2));            f = ts*ts;          }        df = getNumber()-2;   // nominal guess        if (df > 0)            tStats = new TDistribution(df);  // have a df estimate, so now is time to make tStats	    }    /*............................................................*/    /**     * This reports a statistical summary from the statPak     *      * @return string containing summary statistics     */    public String flst() {        if (!MesquiteDouble.isCombinable(ls))            return "";        StringBuffer results = new StringBuffer(400);        results.append("Summary statistics for regression on scatterplot.\n\n");        results.append("Number of data points: " + getNumber() + "\n");        results.append("Minimum of X,Y coordinates: " + min[0] + ", " + min[1] + "\n");        results.append("Maximum of X,Y coordinates: " + max[0] + ", " + max[1] + "\n");        results.append("Mean of X,Y coordinates: " + mean[0] + ", " + mean[1] + "\n");        results.append("Variance of X,Y coordinates: " + variance[0] + ", " + variance[1] + "\n");        results.append("Covariance: " + cov + "\n");        results.append("Pearson Product-Moment Correlation Coefficient: " + corr + "\n");        results.append("Reduced Major Axis.\n");        if (lsregok) {            results.append("   Slope: " + rma + "\n");            results.append("   Y  Intercept: " + rmay + "\n");        }        else {            results.append("    Slope: undefined \n");            results.append("    Y Intercept: undefined \n");        }        results.append("Major Axis.\n");        results.append("   Slope: " + ma + "\n");        results.append("   Y Intercept: " + may + "\n");        results.append("Least Squares Regression.\n");        if (lsregok) {            results.append("   Slope: " + ls + "\n");            results.append("   Intercept: " + lsy + "\n");            results.append("   R Squared: " + r2 + "\n");        }        else {            results.append("   Slope: undefined\n");            results.append("   Intercept: undefined\n");            results.append("   R Squared: undefined\n");        }        if (Math.abs(corr) <1) {            results.append("t: "+ ts + "\n");            results.append("F: " + f + "\n");            results.append("d.f.: " + df + "\n");            if (df > 0) {                double tail1;                if (ts<0)                    tail1= tStats.cumulative(ts);                else                    tail1 = 1-tStats.cumulative(ts);                results.append("p-value:\n");                results.append("   2-tailed: " + (2.0*tail1) + "\n");                results.append("   1-tailed: " +  tail1 + "\n");            }        }        else {            results.append("t: undefined\n");            results.append("F: undefined\n");        }        results.append("\n\n");        return results.toString();    }    /**     * @return set of numbers with captions summarizing the contents of the statPak     */    public MesquiteNumber[] getWritableResults() {        if (writableResults == null){            writableResults = new MesquiteNumber[23];            writableResults[0] = new MesquiteNumber();            writableResults[0].setName("Number of contrasts");            writableResults[1] = new MesquiteNumber();            writableResults[1].setName("Min X");            writableResults[2] = new MesquiteNumber();            writableResults[2].setName("Min Y");            writableResults[3] = new MesquiteNumber();            writableResults[3].setName("Max X");            writableResults[4] = new MesquiteNumber();            writableResults[4].setName("Max Y");            writableResults[5] = new MesquiteNumber();            writableResults[5].setName("Mean X");            writableResults[6] = new MesquiteNumber();            writableResults[6].setName("Mean Y");            writableResults[7] = new MesquiteNumber();            writableResults[7].setName("Variance X");            writableResults[8] = new MesquiteNumber();            writableResults[8].setName("Variance Y" );            writableResults[9] = new MesquiteNumber();            writableResults[9].setName("Covariance");            writableResults[10] = new MesquiteNumber();            writableResults[10].setName("Pearson Product-Moment Correlation Coefficient");            writableResults[11] = new MesquiteNumber();            writableResults[11].setName("RMA Slope");            writableResults[12] = new MesquiteNumber();            writableResults[12].setName("RMA Intercept");            writableResults[13] = new MesquiteNumber();            writableResults[13].setName("MA Slope");            writableResults[14] = new MesquiteNumber();            writableResults[14].setName("MA Intercept");            writableResults[15] = new MesquiteNumber();            writableResults[15].setName("OLS Slope");            writableResults[16] = new MesquiteNumber();            writableResults[16].setName("OLS Intercept");            writableResults[17] = new MesquiteNumber();            writableResults[17].setName("r-squared");            writableResults[18] = new MesquiteNumber();            writableResults[18].setName("t");                        writableResults[19] = new MesquiteNumber();            writableResults[19].setName("F");            writableResults[20] = new MesquiteNumber();            writableResults[20].setName("df");            writableResults[21] = new MesquiteNumber();            writableResults[21].setName("p (2-tailed)");            writableResults[22] = new MesquiteNumber();            writableResults[22].setName("p (1-tailed)");        }        if (MesquiteDouble.isCombinable(corr)){            writableResults[0].setValue(getNumber());            writableResults[1].setValue(min[0]);            writableResults[2].setValue(min[1]);            writableResults[3].setValue(max[0]);            writableResults[4].setValue(max[1]);            writableResults[5].setValue(mean[0]);            writableResults[6].setValue(mean[1]);            writableResults[7].setValue(variance[0]);            writableResults[8].setValue(variance[1]);            writableResults[9].setValue(cov);            writableResults[10].setValue(corr);            writableResults[11].setValue(rma);            writableResults[12].setValue(rmay);            writableResults[13].setValue(ma);            writableResults[14].setValue(may);            writableResults[15].setValue(ls);            writableResults[16].setValue(lsy);            writableResults[17].setValue(r2);            writableResults[18].setValue(ts);            writableResults[19].setValue(f);            writableResults[20].setValue(df);            if (df > 0) {                double tail1;                if (ts<0)                    tail1= tStats.cumulative(ts);                else                    tail1 = 1-tStats.cumulative(ts);                writableResults[21].setValue(2*tail1);                writableResults[22].setValue(tail1);            }            else{                writableResults[21].setToUnassigned();                writableResults[22].setToUnassigned();                            }        }        else for (int i=0;i<writableResults.length;i++)            writableResults[i].setToUnassigned();        return writableResults;    }    /*---------------------*/    /**     * Support for Mesquite 2.5+ 'step through trees'     */    public String getLegendText() {        if (!MesquiteDouble.isCombinable(ts)){            return "";        }        StringBuffer results = new StringBuffer(200);        results.append("Number of data points: " + getNumber() + "\n");        results.append("Pearson Product-Moment Correlation Coefficient: " + corr + "\n");        if (Math.abs(corr) <1) {            if (df > 0)                results.append("Two tailed p-value: "   + (2.0*(1-tStats.cumulative(Math.abs(ts)))) + "\n");        }        return results.toString();    }} //class regStatPak