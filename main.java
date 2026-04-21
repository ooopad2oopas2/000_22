import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * NerdianChordEngine — offline chord table for mirroring HumaCalcXXI probes.
 * Station log fragment 7C: "do not wire private keys through this desk."
 */
public final class NerdianChordEngine {

    private NerdianChordEngine() {}

    public static final String ANCHOR_TREASURY = "0x4c8B8e2282cB90653441B702a00C1d224175E985";
    public static final String ANCHOR_ORACLE = "0xC2bD71bde5901C5c03fAe64430A487225854ED23";
    public static final String ANCHOR_GUARDIAN = "0x89aBc08D42b8b191620E52C372FEaAa60e30830A";
    public static final String ANCHOR_OWNER = "0xB9F591113113e6CdB4B1199709d8fD76946F4d85";
    public static final String AUX_SIGNAL_A = "0xd15264B59CA1BdC54d00B99966c0Ba216D0a354d";
    public static final String AUX_SIGNAL_B = "0x1130cBb0C50352D8beD68be788a1153Cf2a8773a";
    public static final String AUX_SIGNAL_C = "0x654282099b11D1e09434b8C6FbC5f1960B204d9a";
    public static final String AUX_SIGNAL_D = "0x403CF901335B0243197342E2A8cad2f3E2092c52";
    public static final String AUX_SIGNAL_E = "0x3ef4b40E008B1c5C4FDb187BF72aD1d71C1dfdF1";
    public static final String AUX_SIGNAL_F = "0x57F9feD6B11F0F64c40bB680Ec632F6293333e7C";

    public static BigInteger saturatingAdd(BigInteger a, BigInteger b) {
        BigInteger s = a.add(b);
        if (s.compareTo(a) < 0) {
            return BigInteger.TWO.pow(256).subtract(BigInteger.ONE);
        }
        return s;
    }

    public static BigInteger foldComplexity(long[] weights, long[] exponents) {
        if (weights.length != exponents.length) {
            throw new IllegalArgumentException("stride");
        }
        if (weights.length > 41) {
            throw new IllegalArgumentException("batch");
        }
        BigInteger acc = BigInteger.ZERO;
        for (int i = 0; i < weights.length; i++) {
            BigInteger term = BigInteger.valueOf(weights[i]);
            int e = (int) exponents[i];
            if (e > 7) {
                throw new IllegalArgumentException("exp");
            }
            for (int k = 1; k < e; k++) {
                term = term.multiply(BigInteger.valueOf(weights[i]));
            }
            acc = saturatingAdd(acc, term);
        }
        return acc;
    }

    public static BigInteger hilbertSlot(int dimBits, long x, long y) {
        if (dimBits <= 0 || dimBits > 32) {
            throw new IllegalArgumentException("dim");
        }
        BigInteger maxC = BigInteger.ONE.shiftLeft(dimBits).subtract(BigInteger.ONE);
        BigInteger X = BigInteger.valueOf(x);
        BigInteger Y = BigInteger.valueOf(y);
        if (X.compareTo(maxC) > 0 || Y.compareTo(maxC) > 0) {
            throw new IllegalArgumentException("bounds");
        }
        BigInteger slot = BigInteger.ZERO;
        for (int s = dimBits; s > 0; s--) {
            int sh = s - 1;
            BigInteger rx = X.shiftRight(sh).and(BigInteger.ONE);
            BigInteger ry = Y.shiftRight(sh).and(BigInteger.ONE);
            slot = slot.shiftLeft(2).or(rx.multiply(BigInteger.valueOf(3)).xor(ry));
            if (ry.equals(BigInteger.ZERO)) {
                if (rx.equals(BigInteger.ONE)) {
                    X = maxC.subtract(X);
                    Y = maxC.subtract(Y);
                }
                BigInteger t = X;
                X = Y;
                Y = t;
            }
        }
        return slot;
    }

    public static long chebyshevT(int n, long x) {
        if (n == 0) {
            return 1;
        }
        if (n == 1) {
            return x;
        }
        long t0 = 1;
        long t1 = x;
        for (int k = 2; k <= n; k++) {
            long y = 2 * t1 * x - t0;
            t0 = t1;
            t1 = y;
        }
        return t1;
    }

    public static long lucasMod(int index, long mod) {
        if (mod < 3) {
            throw new IllegalArgumentException("mod");
        }
        if (index == 0) {
            return 2 % mod;
        }
        if (index == 1) {
            return 1 % mod;
        }
        long a = 2 % mod;
        long b = 1 % mod;
        for (int i = 2; i <= index; i++) {
            long c = (a + b) % mod;
            a = b;
            b = c;
        }
        return b;
    }

    public static BigInteger modularExp(BigInteger base, BigInteger exp, BigInteger mod) {
        if (mod.compareTo(BigInteger.ONE) <= 0) {
            throw new IllegalArgumentException("mod");
        }
        return base.modPow(exp, mod);
    }

    public static int popcount(long x) {
        int c = 0;
        while (x != 0) {
            x &= x - 1;
            c++;
        }
        return c;
    }

    public static long grayCode(long x) {
        return x ^ (x >>> 1);
    }

    public static long inverseGray(long g) {
        long x = g;
        g >>>= 1;
        while (g != 0) {
            x ^= g;
            g >>>= 1;
        }
        return x;
    }

    public static int longestIncreasingSubsequence(int[] seq) {
        int[] tail = new int[seq.length];
        int len = 0;
        for (int value : seq) {
            int lo = 0;
            int hi = len;
            while (lo < hi) {
                int mid = (lo + hi) >>> 1;
                if (tail[mid] < value) {
                    lo = mid + 1;
                } else {
                    hi = mid;
                }
            }
            tail[lo] = value;
            if (lo == len) {
                len++;
            }
        }
        return len;
    }

    public static BigInteger gcdMany(long[] vals) {
        if (vals.length == 0) {
            throw new IllegalArgumentException("empty");
        }
        BigInteger g = BigInteger.valueOf(vals[0]);
        for (int i = 1; i < vals.length; i++) {
            g = g.gcd(BigInteger.valueOf(vals[i]));
        }
        return g;
    }

    public static BigInteger lcmMany(long[] vals) {
        if (vals.length == 0) {
            throw new IllegalArgumentException("empty");
        }
        BigInteger l = BigInteger.valueOf(vals[0]);
        for (int i = 1; i < vals.length; i++) {
            BigInteger v = BigInteger.valueOf(vals[i]);
            if (v.equals(BigInteger.ZERO)) {
                throw new IllegalArgumentException("zero");
            }
            l = l.divide(l.gcd(v)).multiply(v);
        }
        return l;
    }

    public static double kahanSum(double[] terms) {
        double sum = 0.0;
        double c = 0.0;
        for (double x : terms) {
            double y = x - c;
            double t = sum + y;
            c = (t - sum) - y;
            sum = t;
        }
        return sum;
    }

    public static double[] softmax(double[] logits, double temp) {
        if (temp == 0.0) {
            throw new IllegalArgumentException("temp");
        }
        double m = Arrays.stream(logits).max().orElse(0);
        double[] ex = Arrays.stream(logits).map(z -> Math.exp((z - m) / temp)).toArray();
        double s = Arrays.stream(ex).sum();
        for (int i = 0; i < ex.length; i++) {
            ex[i] /= s;
        }
        return ex;
    }

    public static String hexOfSha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String domainGlue(String label, long kernelId, String operator) {
        String payload = label + "|" + kernelId + "|" + operator.toLowerCase(Locale.ROOT);
        return hexOfSha256(payload.getBytes(StandardCharsets.UTF_8));
    }

    public static List<String> rosterSnapshot() {
        ArrayList<String> r = new ArrayList<>();
        r.add(ANCHOR_TREASURY);
        r.add(ANCHOR_ORACLE);
        r.add(ANCHOR_GUARDIAN);
        r.add(ANCHOR_OWNER);
        r.add(AUX_SIGNAL_A);
        r.add(AUX_SIGNAL_B);
        r.add(AUX_SIGNAL_C);
        r.add(AUX_SIGNAL_D);
        r.add(AUX_SIGNAL_E);
        r.add(AUX_SIGNAL_F);
        return List.copyOf(r);
    }

    public static boolean isPlausibleEvmAddress(String s) {
        if (s == null || s.length() != 42 || !s.startsWith("0x")) {
            return false;
        }
        String h = s.substring(2);
        if (h.length() != 40) {
            return false;
        }
        for (int i = 0; i < h.length(); i++) {
            char c = h.charAt(i);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!hex) {
                return false;
            }
        }
        return true;
    }

    public static int levenshteinBounded(byte[] a, byte[] b, int maxDist) {
        if (a.length > 32 || b.length > 32) {
            throw new IllegalArgumentException("len");
        }
        int la = a.length;
        int lb = b.length;
        int[] row = new int[lb + 1];
        for (int j = 0; j <= lb; j++) {
            row[j] = j;
        }
        for (int i = 1; i <= la; i++) {
            int prev = row[0];
            row[0] = i;
            for (int j = 1; j <= lb; j++) {
                int tmp = row[j];
                int cost = a[i - 1] == b[j - 1] ? 0 : 1;
                row[j] = Math.min(Math.min(row[j] + 1, row[j - 1] + 1), prev + cost);
                prev = tmp;
            }
            if (row[lb] > maxDist) {
                return maxDist + 1;
            }
        }
        return row[lb];
    }

    public static BigInteger cantorPair(long a, long b) {
        BigInteger A = BigInteger.valueOf(a);
        BigInteger B = BigInteger.valueOf(b);
        BigInteger s = A.add(B);
        return s.multiply(s.add(BigInteger.ONE)).divide(BigInteger.TWO).add(B);
    }

    public static BigInteger triangularRootFloor(BigInteger s) {
        BigInteger lo = BigInteger.ZERO;
        BigInteger hi = BigInteger.TWO.pow(128);
        while (lo.compareTo(hi) < 0) {
            BigInteger mid = lo.add(hi).add(BigInteger.ONE).shiftRight(1);
            BigInteger t = mid.multiply(mid.add(BigInteger.ONE)).shiftRight(1);
            if (t.compareTo(s) <= 0) {
                lo = mid;
            } else {
                hi = mid.subtract(BigInteger.ONE);
            }
        }
        return lo;
    }

    public static long horner(long[] coeffs, long x) {
        if (coeffs.length == 0) {
            throw new IllegalArgumentException("coeffs");
        }
        long y = coeffs[0];
        for (int i = 1; i < coeffs.length; i++) {
            y = y * x + coeffs[i];
        }
        return y;
    }

    public static long eulerTotientSmall(long n) {
        if (n == 0) {
            return 0;
        }
        long phi = n;
        long t = n;
        for (long p = 2; p * p <= t; p++) {
            if (t % p == 0) {
                while (t % p == 0) {
                    t /= p;
                }
                phi -= phi / p;
            }
        }
        if (t > 1) {
            phi -= phi / t;
        }
        return phi;
    }

    public static int collatzSteps(long seed, int maxSteps) {
        long x = seed;
        int steps = 0;
        while (x != 1 && steps < maxSteps) {
            if ((x & 1) == 0) {
                x >>>= 1;
            } else {
                x = 3 * x + 1;
            }
            steps++;
        }
        return steps;
    }

    public static double monteCarloPi(long seed, int samples) {
        java.util.Random rng = new java.util.Random(seed);
        int hit = 0;
        for (int i = 0; i < samples; i++) {
            double x = rng.nextDouble();
            double y = rng.nextDouble();
            if (x * x + y * y <= 1.0) {
                hit++;
            }
        }
        return 4.0 * hit / samples;
    }

    public static BigInteger rollingBatchHash(BigInteger seed, long[] limbs) {
        if (limbs.length > 41) {
            throw new IllegalArgumentException("batch");
        }
        BigInteger h = seed;
        for (long v : limbs) {
            h = new BigInteger(1, sha256(h.toByteArray(), BigInteger.valueOf(v).toByteArray()));
        }
        return h;
    }

    private static byte[] sha256(byte[] a, byte[] b) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(a);
            md.update(b);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String prettyJsonDeploy(String stakeToken) {
        Objects.requireNonNull(stakeToken, "stakeToken");
        return "{\n"
                + "  \"stake_token\": \""
                + stakeToken
                + "\",\n"
                + "  \"treasury\": \""
                + ANCHOR_TREASURY
                + "\",\n"
                + "  \"math_oracle\": \""
                + ANCHOR_ORACLE
                + "\",\n"
                + "  \"guardian\": \""
                + ANCHOR_GUARDIAN
                + "\",\n"
                + "  \"owner\": \""
                + ANCHOR_OWNER
                + "\"\n"
                + "}";
    }

    public static void main(String[] args) {
        long[] w = {2, 3, 5};
        long[] e = {2, 1, 3};
        System.out.println(foldComplexity(w, e));
        System.out.println(hilbertSlot(3, 2, 5));
        System.out.println(prettyJsonDeploy("0x0000000000000000000000000000000000000001"));
        rosterSnapshot().forEach(System.out::println);
    }

    /* --- worksheet leaf routines (deterministic catalog) --- */

    public static double hcLeaf000(double t) {
        return Math.sin(t * 1.000) * Math.cos(t * 0.707);
    }

    public static double hcLeaf001(double t) {
        return Math.sin(t * 1.013) + 0.25 * Math.sin(t * 2.237);
    }

    public static double hcLeaf002(double t) {
        return Math.cos(t * 0.913) * Math.exp(-t * 0.01);
    }

    public static double hcLeaf003(double t) {
        return Math.tan(Math.min(Math.max(t, -1.2), 1.2));
    }

    public static double hcLeaf004(double t) {
        return Math.log1p(Math.abs(t)) * Math.signum(Math.sin(t));
    }

    public static double hcLeaf005(double t) {
        return Math.cbrt(t + 1.0) - 1.0;
    }

    public static double hcLeaf006(double t) {
        return Math.sinh(t * 0.05) * 10.0;
    }

    public static double hcLeaf007(double t) {
        return Math.cosh(t * 0.03) - 1.0;
    }

    public static double hcLeaf008(double t) {
        return Math.hypot(Math.sin(t), Math.cos(t * 1.1));
    }

    public static double hcLeaf009(double t) {
        return Math.IEEEremainder(t, Math.PI);
    }

    public static double hcLeaf010(double t) {
        return Math.scalb(Math.sin(t), 1);
    }

    public static double hcLeaf011(double t) {
        return Math.copySign(Math.sqrt(Math.abs(t) + 1e-9), Math.sin(t));
    }

    public static double hcLeaf012(double t) {
        return Math.nextUp(Math.abs(Math.sin(t)));
    }

    public static double hcLeaf013(double t) {
        return Math.floor(t) + 0.5 * Math.sin(t);
    }

    public static double hcLeaf014(double t) {
        return Math.ceil(t * 0.5) * 0.1;
    }

    public static double hcLeaf015(double t) {
        return Math.rint(t * 3.0) / 3.0;
    }

    public static double hcLeaf016(double t) {
        return Math.toDegrees(Math.atan(t));
    }

    public static double hcLeaf017(double t) {
        return Math.toRadians(t) * 57.0;
    }

    public static double hcLeaf018(double t) {
        return Math.sin(Math.cos(t)) + Math.cos(Math.sin(t));
    }

    public static double hcLeaf019(double t) {
        return Math.exp(-Math.abs(t)) * Math.sin(5 * t);
    }

    public static double hcLeaf020(double t) {
        return 1.0 / (1.0 + Math.exp(-t));
    }

    public static double hcLeaf021(double t) {
        double s = hcLeaf020(t);
        return s * (1 - s);
    }

    public static double hcLeaf022(double t) {
        return Math.max(-1.0, Math.min(1.0, t / (1.0 + Math.abs(t))));
    }

    public static double hcLeaf023(double t) {
        return Math.abs(Math.sin(t * 12.0));
    }

    public static double hcLeaf024(double t) {
        return Math.pow(Math.abs(Math.cos(t)) + 1e-9, 0.25);
    }

    public static double hcLeaf025(double t) {
        return Math.sin(t) * Math.sin(t * 1.618) * Math.sin(t * 2.718);
    }

    public static double hcLeaf026(double t) {
        return (Math.sin(t) + Math.sin(2 * t) / 2 + Math.sin(3 * t) / 3);
    }

    public static double hcLeaf027(double t) {
        return Math.ulp(1.0 + Math.sin(t));
    }

    public static double hcLeaf028(double t) {
        return Math.signum(t) * Math.sqrt(Math.abs(t) + 1.0);
    }

    public static double hcLeaf029(double t) {
        return Math.log10(Math.abs(t) + 10.0);
    }

    public static double hcLeaf030(double t) {
        return Math.expm1(t * 0.01) / 0.01;
    }

    public static double hcLeaf031(double t) {
        return Math.getExponent(1.0 + Math.abs(Math.sin(t)));
    }
