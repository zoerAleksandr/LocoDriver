package com.z_company.use_case

import android.os.Build
import androidx.annotation.RequiresApi
import com.z_company.repository.ru_store_api.DTO.JWEAnswerDTO
import com.z_company.repository.ru_store_api.DTO.SubscriptionAnswerDTO
import com.z_company.repository.ru_store_api.RetrofitClient
import com.z_company.repository.ru_store_api.RetrofitServices
import com.z_company.repository.ru_store_api.RuStoreRepositoryKtor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Call
import retrofit2.Callback
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.Signature
import java.security.SignatureException
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Base64

class RuStoreUseCase(val ruStoreRepositoryKtor: RuStoreRepositoryKtor) {
    private val retrofit = RetrofitClient.getClient()
    private val ruStoreApi = retrofit.create(RetrofitServices::class.java)

    private val keyId = "2351022324"
    private val privateKey =
        "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDYU5f8kBkaGZh4nPM9aB8dk3re1jnD+M5o5rxVsMnu7+eotJsS3+SgLPH0WH5QWraEZbZHqnxor2XJpyCPFpBtYUbCjP4TG4TnY7rziA/wL4CJgbwlMleuwIwR4INp4S4mg1M79m7l1P92ISKF9etxtNIW0EbZoptbVsKPOKOt466Q5iOge1bohdtMTdG2zFBdVj0jhj19v9197DLoiyAT614kcPEuxyY5rImdpBHvMPeNOyp0tOlQNzoBW+EdmYUiyWm3XcxJ+sOrRlDvasFVU6/v4KxJvLMI9CjQL9WzuogVMOhlyKV/dPLEwZMlYcYFWWjppRsQe16AwBRD4zyXAgMBAAECggEADR80+Zr8CtCWpmpOzuDBUm1JvLlIk7yZ41RrrtRPIMaTShvPyZtPJkV+VRCxHiAChjwX4cz+SD4Ax0nzTaVUFBkceQdNuiGnNCn2gyMzuaFdWKSdTFxQuUbwuCeNkR9ESg3D6hU5W4j0s1kF275eY8KmI8Af1r+9phK4hoYsV4slemA6tMF1zN2qAtC9rfPYVQHW2/KbAa6SRu37IzVtAVVD5gomuKqU8wT7IgB+HgGYxfp+CSAzkVZ6GnPls1MgrXisJR0bvEVTnGUoY2LnDpbG1X55y8qbfiwlFVU/vTxUisB0AWLY/5344M7+oiLvs9Kcch7Xg3pAUWsv5g0N0QKBgQD0am+AxuIMPRWHaVpkbmAPFyGnPdWDWxUqx9eCJYO4B1+DH4Q1VWFsh4Q95Zj1PN327d8PFl98qW0N2EJiK0s2yv4sCmhfWq9a+ler1qKLNDOMPUoyaxiNl37FAoh4NDiDzbr06qUnTvdt/jCUQHJOlsCnHwfiYNdC2oZWwVWzKwKBgQDilFf/0aVR0XeyxulDYU+O99g0q9pwGmiAIJO4XrRy/Za5Rug4TDKpaFzgf7LWYr9wnW6nAYssPbvTk3vMh95XO+4rOpg1rdd1da4nh8Mz7gFEF+hOXOtfZLA1/Hh1QDj2Nr1BHIF86hssgFa7E186NLR5MLU+YBqyng65WgzWRQKBgQDmbi5fr6HQPgq9FpN1GiaQM5Oz0/UnmUi0g6JnfizX5IOk4KLJkYx/QKhpnrv9gXwW4J6q38H4itdY9Slo2j5YEztclBdgxuOKF3ludbXbHcT3k5UPQa7tVwXagY/eHAWoJd78Jvi7vZRC7CIqszPRagmJxhSt1fU8fz+mLTI57wKBgEaJC8QVzbNk1DCRf7h8KLpHKcVr4nqXngSVH6d7xA0wKKXRDyXHgtX+KTuyRUg8QYCbYgEXl+3T4g8BkL6hZXQesgw/F3dOgQ6N7gNcXkZiR36dOrJ6dsOhosGLsSw/K/xqGgyVBTKP5pm58kYWx8Rk9/HqWeHrJSSJ0+ebwvdJAoGAdj/bi0s2R9zTwLkaeZKkQjTP9GAOjIX/Vjrcy8DeG3wOG+T2nTrnruGrf5m0RrcJjzQBByOM1QeA5p/sw2wjBbhd58zdeQ+4YtvFjQ9kYPmfa82K6fyk9duApu3uGN7h24vt5x5zucTQchFEp3E3mR+Fqa5lqAGfSDm1ZN2J5Kw="

    @RequiresApi(Build.VERSION_CODES.O)
    private var signature: String = generateSignature(keyId = keyId, privateKeyContent = privateKey)

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getJWE(): Result<JWEAnswerDTO> {
        return ruStoreRepositoryKtor.service.createJWEToken(signature)
    }
//
//    suspend fun getSubscriptionDetail(
//        jweToken: String,
//        subscriptionId: String,
//        subscriptionToken: String
//    ): Result<SubscriptionAnswerDTO> {
//        return ruStoreRepositoryKtor.service.getSubscriptionDetails(
//            jweToken,
//            subscriptionId,
//            subscriptionToken
//        )
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun testJWE(){
//        userApi.getJWE(body).enqueue(callbackJWE)
//    }
//
//    val callbackJWE = object : Callback<JWEAnswerDTO> {
//        override fun onResponse(p0: Call<JWEAnswerDTO>, p1: Response<JWEAnswerDTO>) {
//            Log.d("ZZZ", "jwe callback onResponse ${p1}")
//        }
//
//        override fun onFailure(p0: Call<JWEAnswerDTO>, p1: Throwable) {
//            Log.d("ZZZ", "jwe callback onFailure $p1")
//        }
//
//    }

    fun test(
        jweToken: String,
        subscriptionId: String,
        subscriptionToken: String,
        callback: Callback<SubscriptionAnswerDTO>
    ) {
       return ruStoreApi.getDetails(
            jweToken = jweToken,
            packageName = "com.z_company.loco_driver",
            subscriptionId,
            subscriptionToken
        ).enqueue(callback)

//        val callback = object : Callback<SubscriptionAnswerDTO> {
//            override fun onResponse(
//                p0: Call<SubscriptionAnswerDTO>,
//                p1: Response<SubscriptionAnswerDTO>
//            ) {
//                onResponse(p1)
//            }
//
//            override fun onFailure(p0: Call<SubscriptionAnswerDTO>, p1: Throwable) {
//                onFailure(p1)
//            }
//        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Throws(
        NoSuchAlgorithmException::class,
        InvalidKeySpecException::class,
        InvalidKeyException::class,
        SignatureException::class
    )
    private fun generateSignature(keyId: String, privateKeyContent: String): String {
        val kf = KeyFactory.getInstance("RSA")
        val keySpecPKCS8 = PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyContent))
        val privateKey = kf.generatePrivate(keySpecPKCS8)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        val timestamp = dateFormat.format(Date())
        val messageToSign = keyId + timestamp
        println("Message to sign: $messageToSign")
        val signature: Signature = Signature.getInstance("SHA512withRSA")
        signature.initSign(privateKey)
        signature.update(messageToSign.toByteArray())
        val signatureBytes: ByteArray = signature.sign()
        val signatureValue: String = Base64.getEncoder().encodeToString(signatureBytes)
        return String.format(
            "{\n  \"keyId\":\"%s\",\n  \"timestamp\":\"%s\",\n  \"signature\":\"%s\"\n}\n",
            keyId,
            timestamp,
            signatureValue
        )
    }
}

sealed class Response<out T> {
    object Loading : Response<Nothing>()

    data class Success<out T>(
        val data: T
    ) : Response<T>()

    data class Failure(
        val e: Exception
    ) : Response<Nothing>()
}