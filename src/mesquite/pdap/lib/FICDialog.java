/* PDAP:PDTREE package for Mesquite  copyright 2001-2009 P. Midford & W. Maddison
PDAP:PDTREE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
The web site for PDAP:PDTREE is http://mesquiteproject.org/pdap_mesquite/

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)

PDAP reporter dialog is based on the Mesquite Exporter Dialog, 
*/

package mesquite.pdap.lib;

import java.awt.*;

import mesquite.lib.*;
import mesquite.pdap.FICReport.FICReport;



/*===============================================*/
/** FIC Options dialog box class*/
public class FICDialog extends PDAPReporterDialog {
    Checkbox useUnivariateCheckBox;

    /**
     * 
     * @param fileReporter module that writes the report
     * @param parent window used for positioning
     * @param title title of the dialog
     * @param buttonPressed will receive a code indicating which button was pressed to close the dialog
     */
    public FICDialog (FICReport fileReporter, MesquiteWindow parent, String title, MesquiteInteger buttonPressed) {
        super(fileReporter,parent, title, buttonPressed);
        setDefaultButton("Write");
    }


    /*.................................................................................................................*/
    /** This method handles setting up the panels for the dialog box.  This is the long form, with
     * all the toggles passed in.
     * @param dataSelected
     * @param taxaSelected
     * @param useUnivariate true to indicate univariate calculations
     */
    public void addDefaultPanels (boolean dataSelected, boolean taxaSelected, boolean useUnivariate) {
        fileReporter.writeOnlySelectedTaxa = false;
        fileReporter.writeOnlySelectedData = false;
        fileReporter.convertSpaces = true;
        if (dataSelected) 
             writeOnlySelectedDataCheckBox = this.addCheckBox("write only selected data", fileReporter.writeOnlySelectedData);
        else
            writeOnlySelectedDataCheckBox = new Checkbox();
            
        if (taxaSelected) 
             writeOnlySelectedTaxaCheckBox = this.addCheckBox("write only selected taxa", fileReporter.writeOnlySelectedTaxa);
        else
            writeOnlySelectedTaxaCheckBox = new Checkbox();
        addTableDelimiterPopUpPanel();
        addLineEndPopUpPanel();
        addSpaceConversionCheckBox();
        useUnivariateCheckBox = this.addCheckBox("Treat each character's missing data independently", useUnivariate); 
        addPrimaryButtonRow(reportString, cancelString);
        prepareAndDisplayDialog();
    }
    /*.................................................................................................................*/
    //* This version has no passed toggles, so to decide whether to ask about table delimiters
    // it queries the reporter.
    public void addDefaultPanels () {
        addTableDelimiterPopUpPanel();
        addLineEndPopUpPanel();
        addSpaceConversionCheckBox();
        useUnivariateCheckBox = this.addCheckBox("Treat each character's missing data independently", ((FICReport)fileReporter).useUnivariate); 
        addPrimaryButtonRow(reportString, cancelString);
        prepareAndDisplayDialog();
    }
    
    /*.................................................................................................................*/

    /**
     * @param dataSelected true to report only selected data
     * @param taxaSelected true to report only selected taxa
     * @return button code stashed in buttonPressed.
     */
    public int query(boolean dataSelected, boolean taxaSelected) {
        if (dataSelected) 
            fileReporter.writeOnlySelectedData = writeOnlySelectedDataCheckBox.getState();
        if (taxaSelected) 
            fileReporter.writeOnlySelectedTaxa = writeOnlySelectedTaxaCheckBox.getState();
        fileReporter.convertSpaces = convertSpacesCheckBox.getState();
        ((FICReport)fileReporter).useUnivariate = useUnivariateCheckBox.getState();
        return buttonPressed.getValue();
    }


    
}
