/* PDAP:PDTREE package for Mesquite  copyright 2001-2009 P. Midford & W. Maddison
PDAP:PDTREE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
The web site for PDAP:PDTREE is http://mesquiteproject.org/pdap_mesquite/

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/
package mesquite.pdap.lib;

import mesquite.lib.MesquiteModule;

public abstract class PDAP2CTStatPakSource extends MesquiteModule {

  	 public Class getDutyClass() {
    	 	return PDAP2CTStatPakSource.class;
    	 }
  	public String getDutyName() {
  		return "Supplies a StatPak for doing scattergram calculations with tree context";
    	 }
 	
    public abstract PDAP2CTStatPak getStatPak();


}
