/* PDAP:PDTREE package for Mesquite  copyright 2001-2009 P. Midford & W. MaddisonPDAP:PDTREE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.The web site for PDAP:PDTREE is http://mesquiteproject.org/pdap_mesquite/This source code and its compiled class files are free and modifiable under the terms of GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)*/package mesquite.pdap.lib;/* ======================================================================== */public abstract class ScatterRegisterableAsst extends PDAPScatterAsst {    public Class getDutyClass() {        return ScatterRegisterableAsst.class;    }    public String getDutyName() {        return "Calculates and draws regression & prediction for PDAP tips chart";    }        /**     * Attaches this to the main PDAP chart (or equivalent) for display and user queries     * @param module the PDAP scattergram used to display results and prompt the user     */    public abstract void setChartModule(PDAPTreeWinAsstC module);}