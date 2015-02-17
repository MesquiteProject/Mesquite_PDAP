/* PDAP:PDTREE package for Mesquite  copyright 2001-2009 P. Midford & W. Maddison
PDAP:PDTREE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
The web site for PDAP:PDTREE is http://mesquiteproject.org/pdap_mesquite/

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)

*/
package mesquite.pdap.lib;


public abstract class PDAPRootReconStatPak extends PDAPRootStatPak{
    
    /** returns the confidence interval around the reconstructed root value */
    public abstract double getRootCI(int which);
    
    /** returns the standard error of the reconstructed root value */
    public abstract double getRootSE(int which);

    
}
