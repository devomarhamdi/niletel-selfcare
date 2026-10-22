package com.niletel.bsscore.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {
    @Test
    void sha256HexReturnsExpectedDigestFor1234() {
        assertEquals(
                "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4",
                PasswordHasher.sha256Hex("1234")
        );
    }

    @Test
    void sha256HexReturnsLowercase64CharacterHex() {
        String hash = PasswordHasher.sha256Hex("test");

        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]{64}"));
    }

    @Test
    void isEqualHexComparesValidHashes() {
        String hash = PasswordHasher.sha256Hex("1234");

        assertTrue(PasswordHasher.isEqualHex(hash, hash));
        assertFalse(PasswordHasher.isEqualHex(hash, PasswordHasher.sha256Hex("4321")));
    }

    @Test
    void isEqualHexRejectsNullAndMalformedHashes() {
        assertFalse(PasswordHasher.isEqualHex(null, "00"));
        assertFalse(PasswordHasher.isEqualHex("00", null));
        assertFalse(PasswordHasher.isEqualHex("not-hex", "00"));
    }

    @Test
    void sha256HexRejectsNullInput() {
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.sha256Hex(null));
    }
}
