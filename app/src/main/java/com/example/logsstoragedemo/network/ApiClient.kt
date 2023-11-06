package com.example.logsstoragedemo.network

import com.example.logsstoragedemo.BuildConfig
import com.example.logsstoragedemo.utils.Constants
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object ApiClient {
    private fun getInstance(okHttpClient: OkHttpClient): Retrofit {

        val gson = GsonBuilder()
            .setLenient()
            .create()

        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(okHttpClient)
            .build()
    }

    fun getApiService(): ApiService {
        val interceptor = HttpLoggingInterceptor()
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)

        val okHttpClientBuilder =
            OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS) // Set connection timeout
                .readTimeout(300, TimeUnit.SECONDS) // Read timeout
                .writeTimeout(300, TimeUnit.SECONDS) // Write timeout
                .addInterceptor(Interceptor { chain ->
                    val request = chain.request()
                    val newRequest: Request = request.newBuilder().build()
                    chain.proceed(newRequest)
                })

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor()
            logging.setLevel(HttpLoggingInterceptor.Level.BODY)
            okHttpClientBuilder.addInterceptor(logging)
        }

        val okHttpClient = okHttpClientBuilder.build()

        return getInstance(okHttpClient).create(ApiService::class.java)
    }

}