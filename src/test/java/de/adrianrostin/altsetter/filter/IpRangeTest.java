package de.adrianrostin.altsetter.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class IpRangeTest {

    @ParameterizedTest(name = "{0} enthält {1}: {2}")
    @CsvSource(textBlock = """
            172.64.0.0/13,  172.64.0.1,      true
            172.64.0.0/13,  172.71.255.255,  true
            172.64.0.0/13,  172.72.0.0,      false
            104.16.0.0/13,  104.23.1.1,      true
            10.0.0.0/8,     10.42.0.7,       true
            0.0.0.0/0,      8.8.8.8,         true
            2606:4700::/32, 2606:4700:10::1, true
            2606:4700::/32, 2606:4701::1,    false
            2a06:98c0::/29, 2a06:98c7::1,    true
            172.64.0.0/13,  2606:4700::1,    false
            172.64.0.0/13,  example.com,     false
            172.64.0.0/13,  deadbeef,        false
            """)
    void matchesAddressesInsideTheRange(String cidr, String address, boolean expected) {
        assertThat(IpRange.parse(cidr).contains(address)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"172.64.0.0", "172.64.0.0/33", "cloudflare/8", "::/129"})
    void rejectsInvalidRanges(String cidr) {
        assertThatIllegalArgumentException().isThrownBy(() -> IpRange.parse(cidr));
    }
}
