/* PDAP:PDTREE package for Mesquite  copyright 2001-2009 P. Midford & W. Maddison
PDAP:PDTREE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
The web site for PDAP:PDTREE is http://mesquiteproject.org/pdap_mesquite/

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */
package mesquite.pdap.InterpretPDDIST;

import java.awt.Checkbox;
import java.awt.Choice;

import mesquite.lib.characters.CharacterData;
import mesquite.distance.lib.*;
import mesquite.distance.TaxaDistFromMatrixSrc.TaxaDistFromMatrixSrc;
import mesquite.lib.*;
import mesquite.lib.duties.FileInterpreterI;

public class InterpretPDDIST extends FileInterpreterI{
	public void getEmployeeNeeds(){  //This gets called on startup to harvest information; override this and inside, call registerEmployeeNeed
		EmployeeNeed e = registerEmployeeNeed(IncTaxaDistanceSource.class, getName() + "  needs a method to calculate distances.",
		"The method to calculate distances is selected initially");
	}
	/*.................................................................................................................*/
	public static final int TABDELIMITER=0;
	public static final int COMMADELIMITER=1;
	public static final int SPACEDELIMITER=2;
	public static final int NEWLINEDELIMITER=3;
	
	public static final int DEFAULTWIDTH=15;     //maybe this should be user modifiable, within limits (>=8?)
	public static final int MINIMUMWIDTH=8;
	
	private IncTaxaDistanceSource distSource;
	private boolean addRowAndColumnHeaders; 
	private int columnDelimiterChoice;
	private int lineDelimiterChoice;
	private int userColumnWidth;

	
	
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		return true;
	}

	public boolean canImport(){
		return false;
	}
	
	public boolean canExport(){
		return true;
	}
	
	/**
	 * This puts up the DistanceMatrxiExporterDialog
	 * @param dataSelected
	 * @param taxaSelected
	 * @return true if dialog exited with the OK button
	 */
	private boolean getExportOptions(boolean dataSelected, boolean taxaSelected){
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		DistanceMatrixExporterDialog exportDialog = new DistanceMatrixExporterDialog(this,containerOfModule(), "Export PDDIST Distance Matrix Options", buttonPressed);
		exportDialog.completeAndShowDialog(dataSelected, taxaSelected);
		boolean ok = (exportDialog.query(dataSelected, taxaSelected)==0);
		exportDialog.dispose();
		return ok;
	}	
	
	/*.................................................................................................................*/
	private String getColumnDelimiter() {
	    switch (columnDelimiterChoice){
	    case TABDELIMITER:
	        return "\t";
	    case COMMADELIMITER: 
	        return ",";
	    case SPACEDELIMITER: 
	        return " ";
	    case NEWLINEDELIMITER: 
	        return getLineEnding();	   
	    default: 
	        return " "; // right default?
	    }
	}

	public boolean exportFile(MesquiteFile file, String arguments) { //if file is null, consider whole project open to export
		final Arguments args = new Arguments(new Parser(arguments), true);
		boolean usePrevious = args.parameterExists("usePrevious");  //Wayne: should I be using this? how?
		Taxa taxa;
		CharacterData data = null;
 		distSource =  (IncTaxaDistanceSource)hireEmployee(IncTaxaDistanceSource.class,"Source of distances to dump to file");
 		if (distSource == null) {
 			alert(getName() + " couldn't set up the Distance File generator because no distance calculator was obtained.");
 		}
 		if (distSource instanceof TaxaDistFromMatrixSrc){
 		    data = getProject().chooseData(containerOfModule(), file, null, null, "Select data to export");
 		    if (data ==null) {
 		        showLogWindow(true);
 		        logln("WARNING: No suitable data available for export to a file of format \"" + getName() + "\".  The file will not be written.\n");
 		        return false;
 		    }
 	        taxa = data.getTaxa();
 		}
 		else {
 		    taxa = getProject().chooseTaxa(containerOfModule(), "Choose taxa for distance calculation");
            if (taxa ==null) {
                showLogWindow(true);
                logln("WARNING: No suitable taxa available for export to a file of format \"" + getName() + "\".  The file will not be written.\n");
                return false;
            }
 		}
		boolean dataSelected;
		if (data == null)
		    dataSelected = false;
		else
		    dataSelected = data.anySelected();
		if (!MesquiteThread.isScripting() && !usePrevious)
			if (!getExportOptions(dataSelected, taxa.anySelected()))
				return false;
		//distSource.setCurrent(wt);
		TaxaDistance distanceSource =  distSource.getTaxaDistance(taxa);
		if (distanceSource == null)
		    return false;
		double[][] distances = distanceSource.getMatrix();
		if (distances.length != taxa.getNumTaxa())
			MesquiteMessage.warnProgrammer("Distances dimension was " + distances.length + "; numTaxa was " + taxa.getNumTaxa());
		final int numRows = Double2DArray.numFullRows(distances);
		final int numColumns = Double2DArray.numFullColumns(distances);
		final String columnDelimiter = getColumnDelimiter(); 
		final String lineDelimiter = getLineEnding();

		final StringBuffer reportString = new StringBuffer(2*numRows*numColumns);
		if (addRowAndColumnHeaders){
			for (int i=0;i<userColumnWidth;i++)
				reportString.append(' ');
			for (int i=0; i<numColumns;i++){
				String cheader = taxa.getTaxon(i).getName();
				if (cheader.length() > userColumnWidth)
					reportString.append(cheader.substring(0,userColumnWidth));
				else{
					for(int j=cheader.length()-1;j<userColumnWidth;j++)
						reportString.append(' ');
					reportString.append(cheader);
				}
				reportString.append(columnDelimiter);
			}
			reportString.append(lineDelimiter);
		}
		for (int j=0; j<numRows; j++) {
			if (addRowAndColumnHeaders){
				String rheader = taxa.getTaxon(j).getName();
				if (rheader.length() > userColumnWidth)
					reportString.append(rheader.substring(0,userColumnWidth));
				else{
					for(int k=rheader.length();k<userColumnWidth;k++)
						reportString.append(' ');
					reportString.append(rheader);
				}
				reportString.append(columnDelimiter);
			}
			for (int i=0; i<numColumns; i++) {
				double tmp = distances[i][j];
				if (!MesquiteDouble.isCombinable(tmp))
					tmp = -9.999999999E-99;
				final String myDist = MesquiteDouble.toFixedWidthString(tmp, userColumnWidth);
				if (myDist.startsWith("10.0")){
				    final String tmp1 = myDist.substring(0,4);
				    final String tmp2 = myDist.substring(5);
				    reportString.append(tmp1+tmp2);
				}
				else				    
				    reportString.append(myDist);
				reportString.append(columnDelimiter);
			}
			reportString.append(lineDelimiter);
			}
		if (reportString.length()>0) {
			saveExportedFileWithExtension(reportString, arguments, "dsc");
			return true;
		}
 		fireEmployee(distSource);
 		//fireEmployee(source);
 		return false;
	}
	
	void setAddRowAndColumnHeaders(boolean v){
	    addRowAndColumnHeaders = v;
	}

	void setColumnDelimiterChoice(int columnDelimiterChoice) {
	    this.columnDelimiterChoice = columnDelimiterChoice;
	}

	void setUserColumnWidth(int userColumnWidth) {
	    this.userColumnWidth = userColumnWidth;
	}
			
	
    boolean isAddRowAndColumnHeaders() {
        return addRowAndColumnHeaders;
    }

    int getColumnDelimiterChoice() {
        return columnDelimiterChoice;
    }

    int getLineDelimiterChoice() {
        return lineDelimiterChoice;
    }

    int getUserColumnWidth() {
        return userColumnWidth;
    }

    /*.................................................................................................................*/
    public String getAuthors() {
        return "Peter Midford and Wayne Maddison";
    }

    /*.................................................................................................................*/
    /** returns an explanation of what the module does.*/
    public String getExplanation() {
        return "Generates phylogenetic variance-covariance matrix in the manner of the DOS PDDIST program (termed C in Garland, T., Jr., and A. R. Ives. 2000).  " +
        "The first time you export a DSC file, please include headers to columns and rows.  " +
        "Then, check to make sure that the ordering of this matrix is the same as the ordering of any tip data matrix that you intend to analyze, e.g., via PGLS methods.  " +
        "After this, you may resave the DSC matrix without column and row headers.";
    }

  	public boolean isPrerelease() {
	    return true;
   	}

  	public boolean isSubstantive(){
  		return true;
  	}
	/*.................................................................................................................*/
  	
	public String getName() {
		return "Export PDDIST Distance Matrix (dsc)";
	}
	


	public void readFile(MesquiteProject mf, MesquiteFile mNF, String arguments) {
		// TODO Auto-generated method stub
		
	}

}





 class DistanceMatrixExporterDialog extends ExporterDialog {
	Checkbox writeHeadersCheckBox;
	Choice columnDelimiterDropDown;
	IntegerField columnWidthField;
	InterpretPDDIST exporter;
	
	public DistanceMatrixExporterDialog (InterpretPDDIST module, MesquiteWindow parent, String title, MesquiteInteger buttonPressed) {
		super(module, parent, title, buttonPressed);
		this.exporter = module;
	}
	
	/*.................................................................................................................*/
	public void completeAndShowDialog (boolean dataSelected, boolean taxaSelected){
	    columnWidthField = this.addIntegerField("Column width", InterpretPDDIST.DEFAULTWIDTH, 2, 8, 15);
		addColumnDelimiterPopUpPanel();
		writeHeadersCheckBox = this.addCheckBox("add headers to columns and rows", exporter.isAddRowAndColumnHeaders());
	    super.completeAndShowDialog(dataSelected, taxaSelected);
	}
	
	public int query(boolean dataSelected, boolean taxaSelected) {
		super.query(dataSelected,taxaSelected);
		exporter.setColumnDelimiterChoice(columnDelimiterDropDown.getSelectedIndex());
		exporter.setAddRowAndColumnHeaders(writeHeadersCheckBox.getState());
		exporter.setUserColumnWidth(columnWidthField.getValue());
		return buttonPressed.getValue();
	}

	
	public boolean getRowAndColumnHeadersCheckBox(){
		return writeHeadersCheckBox.getState();
	}
	
	public void addColumnDelimiterPopUpPanel () {
		columnDelimiterDropDown = addPopUpMenu("Table entry delimiters:", "Tab", "Comma", "Space", "NewLine",exporter.getLineDelimiterChoice());
	}
	



}

