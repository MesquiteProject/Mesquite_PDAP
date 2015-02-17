/* PDAP:PDTREE package for Mesquite  copyright 2001-2009 P. Midford & W. Maddison
PDAP:PDTREE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
The web site for PDAP:PDTREE is http://mesquiteproject.org/pdap_mesquite/

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */
package mesquite.pdap.lib;

/**
 * @author peter
 * created 2002
 *
 * Class for calculating the sign test from binomial terms (not a normal approximation)
 */

public class SignTest {

    final static double log_half = Math.log(0.5);

    /** 
     * This does a signTest calculation 
     * @param N1 first number
     * @param N2 second number
     * @return 
     * */
    public static double signTest(int N1, int N2) {
        if (N1 > N2)     //calculate the smaller tail value
            return binomial050(N1+N2,N2);
        else
            return binomial050(N1+N2,N1);
    }

    /**
     * This method calculates symmetric (p=0.5) binomial terms.  Note that
     * this symmetry simplifies two-tailed calculations and the pq power
     * expressions, which are constant over the loop.  There are probably more
     * expression optimizations in the C(n,r) code, but this is a little better 
     * than calculating all three factorials outright.
     * @param N total
     * @param R smaller number
     * @return  binomial p-value for N,R with p=0.5
     */
    public static double binomial050(int N, int R) {
        double result = 0;
        final double pwr = Math.exp(N*log_half);
        for (int count = 0;count <= R; count++) {
            double pnc = perm(N,count);
            double f = perm(N-count,1);
            double comb = pnc/f;
            double bino = pwr*comb;
            result += bino;
        }
        return result;
    }


    /** This helper function calculates P(n,r) as a truncated factorial */
    private static double perm(int N, int R) {
        double result = 1.0;
        for (int i = N; i >= R+1; i--)
            result *= i;
        return result;
    }	



}
