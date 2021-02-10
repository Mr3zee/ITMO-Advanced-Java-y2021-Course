public class Hash {
    private static final long first8Bits = 0xFF00_0000_0000_0000L;
    private static final int last8Bits = 0xFF;

    static long pjw(final byte[] bytes, int size, long hash) {
        for (int i = 0; i < size; i++) {
            hash = (hash << 8) + (bytes[i] & last8Bits);
            final long high = hash & first8Bits;
            if (high != 0) {
                hash ^= high >> 48;
                hash &= ~high;
            }
        }
        return hash;
    }
}
