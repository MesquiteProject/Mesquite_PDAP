/* PDAP:PDTREE package for Mesquite  copyright 2001-2009 P. Midford & W. Maddison
PDAP:PDTREE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
The web site for PDAP:PDTREE is http://mesquiteproject.org/pdap_mesquite/

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)

*/
package mesquite.pdap.lib;

import java.util.ArrayList;

import mesquite.cont.lib.ContinuousDistribution;
import mesquite.lib.MesquiteDouble;
import mesquite.lib.Tree;

public abstract class PDAPRootRegStatPak extends PDAPRootStatPak {

    // Add support for CI/PI 
    protected ArrayList CIPIValues = null;  //Non-generic as Mesquite needs to run under Java 1.4
    
    /*-----------------------------------------*/
    /** Set the tree so we can do the contrast calculations internally
     * @param tree the Tree to use 
     */
    public void setTree(Tree tree) {
        super.setTree(tree);
        clearCIPIlist();
    }
    
    /*-----------------------------------------*/
    /** 
     * Set the dataset for the first variable 
     * @param observed set of values for first ('horizontal') trait
     */
    public void setObserved1(ContinuousDistribution observed) {
        super.setObserved1(observed);
        clearCIPIlist();
    }
    
    /*-----------------------------------------*/
    /** 
     * Set the dataset for the second variable 
     * @param observed set of values for second ('vertical') trait
     */
    public void setObserved2(ContinuousDistribution observed) {
        super.setObserved2(observed);
        clearCIPIlist();
    }
    /*-----------------------------------------*/
    /**  
     * @return number of CIPI values
     */
    public int getCIPIValuesSize(){
        return CIPIValues.size();
    }
    
    /** 
     * This allows a new value to be added to the vector of x values that will be added to the end of the
     * CI/PI table that appears in the text pane of screen 9B. 
     * @param x value to add
     */       
    public void addCIPIValue(MesquiteDouble x) {
        CIPIValues.add(x);
    }

    /** This allows clearing the list of values (e.g., when a character is changed) */
    public void clearCIPIlist() {
        CIPIValues = new ArrayList();
    }

    /**
     * 
     * @param i index into the values array (which isn't really ordered).
     * @return value, packaged as a MesquiteDouble
     */
    public MesquiteDouble CIPIValueAt (int i){
        return (MesquiteDouble) CIPIValues.get(i);
    }

     
    /*-----------------------------------------*/
    /** @param w - the value assigned to the second CI/PI width, also used in plots for
    screens 9A and 9B. */
    public abstract void setWidth2(double w);
        
    
    /*-----------------------------------------*/
    /** This method calculates the confidence interval of the regression estimate about a single
    point.  This is used to generate the confidence interval lines and to generate the .CI file. */
    public abstract double calcCI(double Xi,int index1,double width);
        
    /*-----------------------------------------*/
    /** This method calculates the prediction interval of the regression estimate about a single
    point.  This is used to generate the confidence interval lines and to generate the .CI file. */
    public abstract double calcPI(double Xi,int index1, double width);
    
    
}
