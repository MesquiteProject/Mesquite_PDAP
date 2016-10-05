/* PDAP:PDTREE package for Mesquite  copyright 2001-2010 P. Midford & W. Maddison
PDAP:PDTREE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
The web site for PDAP:PDTREE is http://mesquiteproject.org/pdap_mesquite/

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/
package mesquite.pdap.PDAPReRootingSet;

import mesquite.lib.MesquiteTree;
import mesquite.lib.Tree;
import mesquite.treefarm.lib.DetTreeModifier;

public class PDAPReRootingSet extends DetTreeModifier {

   
    //The Turbo Pascal code ... for reference
    /*
     *       {*********************************************************************}
      {This procedure allows the user to reroot the tree.                   }
      {*********************************************************************}
      Procedure reRoot(Var place,rt,rrt :nodePtr;
                       Var lmult :extended;
                       mx,my :integer);
         Var newRoot : nodePtr;
             dummyRoot : nodePtr;
             prev : nodePtr;
             ancestor : nodePtr;
             next : nodePtr;
             base : extended;
             temp : extended;
             savedLength : extended;
             ready : integer;
             userChar : char;
             doBranch : boolean;
         {****************************************************************}
         Begin
            rt := rrt;    {always do a pop operation to be sure!}
            doBranch := branchOrNode(place);
            If (place <> rt) Then Begin
               If (place^.ancestor = rt) Then Begin
                  If place^.length > 0 Then Begin
                        If (rt^.sibR = place)
                        Then
                          rt^.sibL^.length := rt^.sibL^.length+place^.length
                        Else
                          rt^.sibR^.length := rt^.sibR^.length+place^.length;
                        place^.length := 0;
                  End
                  Else Begin {shuffle the polytomy}
                     If (rt^.sibR = place) Then Begin
                        rt^.sibR := place^.sibL;
                        rt^.sibR^.ancestor := rt;
                        place^.sibL := rt;
                     End
                     Else Begin {rt^.sibL = place}
                        rt^.sibL := place^.sibR;
                        rt^.sibL^.ancestor := rt;
                        place^.sibR := rt;
                     End; { ~ rt^.sibR = place}
                     rt^.ancestor := place;
                     place^.ancestor := nil;
                     rt := place;
                     rrt := rt;
                  End { ~ place^.length > 0}
               End {place^.ancestor = rt}
               Else Begin
                  dummyRoot := new(nodePtr);
                  dummyRoot^.sibL := rt^.sibL;
                  dummyRoot^.sibR := rt^.sibR;
                  dummyRoot^.sibL^.ancestor := dummyRoot;
                  dummyRoot^.sibR^.ancestor := dummyRoot;
                  dummyRoot^.ancestor := NIL;
                  dummyRoot^.name := '**';
                  dummyRoot^.length := 0.0;   {safety}
                  newRoot := rt;
                  If (place = place^.ancestor^.sibR)
                  Then Begin
                     newRoot^.sibR := place;
                     newRoot^.sibL := place^.ancestor;
                     place^.ancestor^.sibR := newRoot;
                  End
                  Else Begin
                     newRoot^.sibL := place;
                     newRoot^.sibR := place^.ancestor;
                     place^.ancestor^.sibL := newRoot;
                  End;
                  savedLength := place^.ancestor^.length;
                  Begin
                     place^.ancestor^.length := place^.length;
                     place^.length := 0.0; {thus a polytomy}
                  End;
                  ancestor := place^.ancestor;
                  place^.ancestor := newRoot;
                  prev := newRoot;
                  next := ancestor^.ancestor;
                  Repeat
                     ancestor^.ancestor := prev;
                     {branch lengths}
                     If (ancestor^.sibL = prev) Then Begin
                        ancestor^.sibL := next;
                     End
                     Else Begin
                        ancestor^.sibR := next;
                     End; {else}
                     If (next^.ancestor <> nil) Then Begin
                        temp := next^.length;
                        next^.length := savedLength;
                        savedLength := temp;
                     End; {then}
                     prev := ancestor;
                     ancestor := next;
                     next := next^.ancestor
                  Until (next = nil); {thus ancestor = dummyRoot}
                  If (ancestor^.sibL = prev) Then Begin
                     ancestor^.sibR^.ancestor := prev;
                     ancestor^.sibR^.length :=
                             ancestor^.sibR^.length + savedLength;
                     If (prev^.sibR = ancestor)
                     Then prev^.sibR := ancestor^.sibR
                     Else prev^.sibL := ancestor^.sibR
                  End
                  Else Begin
                     ancestor^.sibL^.ancestor := prev;
                     ancestor^.sibL^.length :=
                             ancestor^.sibL^.length + savedLength ;
                     If (prev^.sibR = ancestor)
                     Then prev^.sibR := ancestor^.sibL
                     Else prev^.sibL := ancestor^.sibL
                  End; {else}
                  dispose(dummyRoot);
               End; {else (place^.ancestor <> rrt)}
               reDrawTree(rt,lengthMult,ht1,mx,my);
               changeName(rrt,rt,rt,lengthMult,mx,my,
                          'The previous root is named ',
                          'What would you like to name the new root? ');
            End; {else (place <> rrt)}
            place := rt;
            reDrawTree(rt,lengthMult,ht1,mx,my);
         End; {procedure reRoot}

     */
    
    public boolean startJob(String arguments, Object condition, boolean hiredByName) {
        return true;
     }
    public boolean isPrerelease(){
        return true;
    }
    /*.................................................................................................................*/
     public boolean showCitation(){
        return true;
     }
    
    
     /*.................................................................................................................*/
     public void modifyTree(Tree original, MesquiteTree modified, int ic){
        if (original == null || modified == null)
            return;
        int root = original.getRoot();
        int numReroots = original.numberOfNodesInClade(root)-2;
        if (ic<0 | ic>=numReroots)
            return;
        int r = findRootingNode(original, root, ic);
        
        if (!modified.nodeExists(r))
            return;
        modified.reroot(modified.nodeInTraversal(r, root), root, false);
        modified.setName("Rooting " + (ic+1) + " of " + original.getName());
        
    }
    int findRootingNode(Tree original, int cladeRoot, int target){
            int numReroots = original.numberOfNodesInClade(cladeRoot)-1;
        int count = -1;
        for (int i = 1; i<=numReroots; i++) {
            int atNode = original.nodeInTraversal(i, cladeRoot);
            if (original.nodeIsPolytomous(cladeRoot) || original.motherOfNode(atNode)!= cladeRoot) {
                count++;
                if (count == target)
                    return i;
            }
        }
        return -1;
    }
    /*.................................................................................................................*/
    public int getNumberOfTrees(Tree tree) {
        if (tree ==null)
            return 0;
        else
            return  tree.numberOfNodesInClade(tree.getRoot())-2;
    }

    
    
    
    /*.................................................................................................................*/
    public String getExplanation() {
        return "Returns rerootings of tree per Garland et al. 1999";
    }


    public String getName() {
        return "PDAP Set of tree Rerootings";
    }

    
    /**
     * 
     * @param tree
     * @param place
     */
//    private void Preroot(MesquiteTree tree, int place, MesquiteDouble lmult,int mx, int my){
//        int rt = tree.getRoot();
//        boolean doBranch = branchOrNode(tree,place);
//        if (place != rt){
//            if (tree.branchingAncestor(place) == rt){
//                if (tree.getBranchLength(place) > 0.0){
//                    int sibL = tree.firstDaughterOfNode(rt);
//                    int sibR = tree.nextSisterOfNode(sibL);
//                    double newLength = tree.getBranchLength(sibR) + tree.getBranchLength(sibL);
//                    if (sibL == place){
//                        tree.setBranchLength(sibR,newLength,false);
//                    }
//                    else{  //PDAP trees are strictly bifurcating, so place must be sibR
//                        tree.setBranchLength(sibL, newLength, false);
//                    }
//                    tree.setBranchLength(place,0.0,false);
//                }
//                else { //BL ==0 so a PDAP polytomy to shuffle
//                    If (rt^.sibR = place) Then Begin
//                    rt^.sibR := place^.sibL;
//                    rt^.sibR^.ancestor := rt;
//                    place^.sibL := rt;
//                 End
//                 Else Begin {rt^.sibL = place}
//                    rt^.sibL := place^.sibR;
//                    rt^.sibL^.ancestor := rt;
//                    place^.sibR := rt;
//                 End; { ~ rt^.sibR = place}
//                 rt^.ancestor := place;
//                 place^.ancestor := nil;
//                 rt := place;
//                 rrt := rt;
//
//                    
//                    
//                
//                }
//            }
//        }
//    }
    
    /**
     * @return true if rerooting at the midpoint of a branch, which shouldn't happen in the context of automatic rerouting for CI's
     */
    private boolean branchOrNode(Tree tree, int where){
        return false;  //Perhaps this function isn't necessary in this context
        
    }
    
}
