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
