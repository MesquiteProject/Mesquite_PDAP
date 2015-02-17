/* PDAP:PDTREE package for Mesquite  copyright 2001-2011 P. Midford & W. MaddisonPDAP:PDTREE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.The web site for PDAP:PDTREE is http://mesquiteproject.org/pdap_mesquite/This source code and its compiled class files are free and modifiable under the terms of GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)*/package mesquite.pdap.GrafenTransform;/*~~  */import mesquite.lib.*;import mesquite.lib.duties.*;/** ======================================================================== */public class GrafenTransform extends BranchLengthsAlterer {    private int [] depth;    private int [] counts;    /*.................................................................................................................*/    public boolean startJob(String arguments, Object condition, boolean hiredByName) {		        return true;    }    /*.................................................................................................................*/    /**     * @param tree     * @param resultString passed in, never filled     * @return returns true because this assumes the calling code is always responsible for notifying listeners of tree     *     * Real entry point for the transform - a wrapper around the recursive method doTransform     *     */    public  boolean transformTree(AdjustableTree tree, MesquiteString resultString){        doTransform(tree, tree.getRoot());        return true;    }    /**     *      * @param tree     * @param node     *      * This procedure sets all the branch lengths to those dictated by     * arbitrary method.  The distance from the tips to the current node is     * equal to one less than the number of tips descending from that node.     */    private void doTransform(AdjustableTree tree, int root) {        depth = new int[tree.getNumNodeSpaces()];        counts = new int[tree.getNumNodeSpaces()];        grafenMe1(tree,root,tree.motherOfNode(root));  //this will actually work in Mesquite        grafenMe2(tree,root);    }    /**     *      * @param tree     * @param nd     * @param mothersCount     *      * This procedure computes the depths recursively.     * There are two support functions since DOS PDAP needed to handle polytomies specially.     */    private void grafenMe1(AdjustableTree tree, int nd, int mother) {         if (tree.nodeIsInternal(nd)) {            counts[nd] = 0;              for (int daughter=tree.firstDaughterOfNode(nd); tree.nodeExists(daughter); daughter = tree.nextSisterOfNode(daughter) )                 grafenMe1(tree,daughter,nd);            counts[mother] += counts[nd];            depth[nd] = counts[nd]-1; //save tips-1 as depth        }        else {            counts[mother]++;            // this is a tip, so there is 1 in the subtree            depth[nd] = 0;                //set depth to 0}        }    }    /**     *      * @param tree AdjustableTree to transform     * @param nd int specifying where we are in the tree     *      * This method finishes the transformation by assigning branch lengths.  Since this is a regular     * Mesquite tree, ZLB's shouldn't get special treatment.     */    private void grafenMe2(AdjustableTree tree, int nd) {        if (tree.nodeIsInternal(nd)) {            for (int daughter=tree.firstDaughterOfNode(nd); tree.nodeExists(daughter); daughter = tree.nextSisterOfNode(daughter) ) {                 tree.setBranchLength(daughter,depth[nd]-depth[daughter],false);                grafenMe2(tree,daughter);            }        }    }    /*.................................................................................................................*/    public String getName() {        return "Branch Lengths Method of Grafen (1989)";    }    /*.................................................................................................................*/    public String getVersion() {        return "1.16";    }    /*.................................................................................................................*/    public boolean isPrerelease() {        return false;    }    /*................................................................................................................*/    public boolean isSubstantive(){        return true;    }            /*.................................................................................................................*/    public String getAuthors() {        return "Peter E. Midford, Ted Garland Jr., and Wayne P. Maddison";    }    /*.................................................................................................................*/    /** returns an explanation of what the module does.*/    public String getExplanation() {        return "Adjusts a tree's branch lengths according the method of Grafen (1989)" ;    }}