package org.example.paymentservice.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Hex;

public class RazorpayWebhookUtils {

    /**
     * Verifies the webhook signature using HMAC SHA256.
     */
    public static boolean verifyWebhookSignature(String payload, String receivedSignature, String secret) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hash = sha256_HMAC.doFinal(payload.getBytes());
            String computedSignature = Hex.encodeHexString(hash);
            return computedSignature.equals(receivedSignature);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

