package org.spongycastle.crypto.test;

import java.security.SecureRandom;

import org.spongycastle.crypto.engines.DESEngine;
import org.spongycastle.crypto.paddings.BlockCipherPadding;
import org.spongycastle.crypto.paddings.ISO10126d2Padding;
import org.spongycastle.crypto.paddings.ISO7816d4Padding;
import org.spongycastle.crypto.paddings.PKCS7Padding;
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.spongycastle.crypto.paddings.TBCPadding;
import org.spongycastle.crypto.paddings.X923Padding;
import org.spongycastle.crypto.paddings.ZeroBytePadding;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;
import org.spongycastle.util.test.SimpleTest;

/**
 * General Padding tests.
 */
public class PaddingTest
    extends SimpleTest
{
    public PaddingTest()
    {
    }

    private void blockCheck(
        PaddedBufferedBlockCipher   cipher,
        BlockCipherPadding          padding,
        KeyParameter                key,
        byte[]                      data)
    {
        byte[]  out = new byte[data.length + 8];
        byte[]  dec = new byte[data.length];
        
        try
        {                
            cipher.init(true, key);
            
            int    len = cipher.processBytes(data, 0, data.length, out, 0);
            
            len += cipher.doFinal(out, len);
            
            cipher.init(false, key);
            
            int    decLen = cipher.processBytes(out, 0, len, dec, 0);
            
            decLen += cipher.doFinal(dec, decLen);
            
            if (!areEqual(data, dec))
            {
                fail("failed to decrypt - i = " + data.length + ", padding = " + padding.getPaddingName());
            }
        }
        catch (Exception e)
        {
            fail("Exception - " + e.toString(), e);
        }
    }
    
    public void testPadding(
        BlockCipherPadding  padding,
        SecureRandom        rand,
        byte[]              ffVector,
        byte[]              ZeroVector)
    {
        PaddedBufferedBlockCipher    cipher = new PaddedBufferedBlockCipher(new DESEngine(), padding);
        KeyParameter                 key = new KeyParameter(Hex.decode("0011223344556677"));
        
        //
        // ff test
        //
        byte[]    data = { (byte)0xff, (byte)0xff, (byte)0xff, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0 };
        
        if (ffVector != null)
        {
            padding.addPadding(data, 3);
            
            if (!areEqual(data, ffVector))
            {
                fail("failed ff test for " + padding.getPaddingName());
            }
        }
        
        //
        // zero test
        //
        if (ZeroVector != null)
        {
            data = new byte[8];
            padding.addPadding(data, 4);
            
            if (!areEqual(data, ZeroVector))
            {
                fail("failed zero test for " + padding.getPaddingName());
            }
        }
        
        for (int i = 1; i != 200; i++)
        {
            data = new byte[i];
            
            rand.nextBytes(data);

            blockCheck(cipher, padding, key, data);
        }
    }
    
    public void performTest()
    {
        SecureRandom    rand = new SecureRandom(new byte[20]);
        
        rand.setSeed(System.currentTimeMillis());
        
        testPadding(new PKCS7Padding(), rand,
                                    Hex.decode("ffffff0505050505"),
                                    Hex.decode("0000000004040404"));

        PKCS7Padding padder = new PKCS7Padding();
        try
        {
            padder.padCount(new byte[8]);

            fail("invalid padding not detected");
        }
        catch (InvalidCipherTextException e)
        {
            if (!"pad block corrupted".equals(e.getMessage()))
            {
                fail("wrong exception for corrupt padding: " + e);
            }
        } 

        testPadding(new ISO10126d2Padding(), rand,
                                    null,
                                    null);
        
        testPadding(new X923Padding(), rand,
                                    null,
                                    null);

        testPadding(new TBCPadding(), rand,
                                    Hex.decode("ffffff0000000000"),
                                    Hex.decode("00000000ffffffff"));

        testPadding(new ZeroBytePadding(), rand,
                                    Hex.decode("ffffff0000000000"),
                                    null);
        
        testPadding(new ISO7816d4Padding(), rand,
                                    Hex.decode("ffffff8000000000"),
                                    Hex.decode("0000000080000000"));
    }

    public String getName()
    {
        return "PaddingTest";
    }

    public static void main(
        String[]    args)
    {
        runTest(new PaddingTest());
    }
}
