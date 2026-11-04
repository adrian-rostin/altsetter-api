package de.adrianrostin.altsetter.filter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Ein IP-Bereich in CIDR-Schreibweise, z. B. {@code 172.64.0.0/13} oder {@code 2606:4700::/32}.
 * Prüft nur Adressen, keine Hostnamen; es wird nie DNS abgefragt.
 */
public final class IpRange {

    private static final Pattern IPV4 = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}");
    private static final Pattern IPV6 = Pattern.compile("[0-9a-fA-F.]*:[0-9a-fA-F.:]*");

    private final byte[] network;
    private final int prefixLength;

    private IpRange(byte[] network, int prefixLength) {
        this.network = network;
        this.prefixLength = prefixLength;
    }

    public static IpRange parse(String cidr) {
        int slash = cidr.indexOf('/');
        if (slash < 0) {
            throw new IllegalArgumentException("Kein CIDR-Bereich: " + cidr);
        }
        byte[] network = toBytes(cidr.substring(0, slash))
                .orElseThrow(() -> new IllegalArgumentException("Keine IP-Adresse: " + cidr));
        int prefixLength = Integer.parseInt(cidr.substring(slash + 1));
        if (prefixLength < 0 || prefixLength > network.length * Byte.SIZE) {
            throw new IllegalArgumentException("Ungültige Präfixlänge: " + cidr);
        }
        return new IpRange(network, prefixLength);
    }

    /** Ob der Text eine IPv4- oder IPv6-Adresse ist (kein Hostname). */
    public static boolean isAddress(String text) {
        return toBytes(text).isPresent();
    }

    public boolean contains(String address) {
        return toBytes(address).map(this::contains).orElse(false);
    }

    private boolean contains(byte[] address) {
        if (address.length != network.length) {
            return false;
        }
        int fullBytes = prefixLength / Byte.SIZE;
        for (int i = 0; i < fullBytes; i++) {
            if (address[i] != network[i]) {
                return false;
            }
        }
        int remainingBits = prefixLength % Byte.SIZE;
        if (remainingBits == 0) {
            return true;
        }
        int mask = (0xFF << (Byte.SIZE - remainingBits)) & 0xFF;
        return (address[fullBytes] & mask) == (network[fullBytes] & mask);
    }

    /**
     * Nur IP-Literale, damit {@link InetAddress} nie DNS fragt: IPv4 mit vier Zahlen, IPv6 mit
     * Doppelpunkt (den kein Hostname enthalten kann).
     */
    private static Optional<byte[]> toBytes(String address) {
        if (address == null
                || !(IPV4.matcher(address).matches() || IPV6.matcher(address).matches())) {
            return Optional.empty();
        }
        try {
            return Optional.of(InetAddress.getByName(address).getAddress());
        } catch (UnknownHostException e) {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        try {
            return InetAddress.getByAddress(network).getHostAddress() + "/" + prefixLength;
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }
}
