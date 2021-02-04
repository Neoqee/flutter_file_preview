package com.neoqee.flutter_file_preview

import android.text.TextUtils
import okhttp3.ResponseBody
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.Executors

class DownloadModel {

    companion object{
        fun downloadFile(url: String, callback: Callback<ResponseBody>){
            val retrofit = Retrofit.Builder()
                .baseUrl("https://baidu.com")
                .addConverterFactory(GsonConverterFactory.create())
                .callbackExecutor(Executors.newSingleThreadExecutor())
                .build()
            val downloadApi = retrofit.create(DownloadApi::class.java)
            if (!TextUtils.isEmpty(url)){
                val call = downloadApi.downloadFile(url)
                call.enqueue(callback)
            }
        }
    }

}