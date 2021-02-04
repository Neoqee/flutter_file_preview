package com.neoqee.flutter_file_preview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.tencent.smtt.sdk.TbsReaderView
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class FilePreviewActivity : AppCompatActivity() {

    private val TAG: String = "flutter_file_preview"

    private lateinit var fileReaderContainer: RelativeLayout
    private lateinit var photoView: PhotoView
    private lateinit var toolbar: Toolbar
    private lateinit var progressBar: ProgressBar
    private var mTbsReaderView: TbsReaderView? = null

    private val imgTypes: Array<String> = arrayOf("png","jpg","jpeg")

    private val readerCallback =
        TbsReaderView.ReaderCallback { integer, o, o1 -> }

    private val handler = @SuppressLint("HandlerLeak")
    object : Handler(){
        override fun handleMessage(msg: Message) {
            when(msg.what){
                1 -> {
                    progressBar.visibility = View.VISIBLE
                }
                2 -> {
                    progressBar.visibility = View.GONE
                }
                3 -> {
                    Log.i(TAG,"下载完毕")
                    runOnUiThread {
                        Log.i(TAG,"准备打开文件")
                        val file = msg.obj as File
                        progressBar.visibility = View.GONE
                        openFile(file)
                    }
                }
            }
        }
    }


    companion object{
        fun show(context: Context, url: String?,title: String){
            val intent = Intent(context, FilePreviewActivity::class.java).apply {
                val bundle = Bundle()
                bundle.putSerializable("url", url)
                bundle.putSerializable("title", title)
                putExtras(bundle)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_preview)

        toolbar = findViewById(R.id.toolbar)
        fileReaderContainer = findViewById(R.id.fileReaderContainer)
        photoView = findViewById(R.id.photoView)
        progressBar = findViewById(R.id.progressBar)

        val url = intent.getStringExtra("url")
        val title = intent.getStringExtra("title")

        toolbar.title = title

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (!url.isNullOrBlank()){
            if (url.startsWith("http")){
                downloadFromNet(url)
//                Thread(Runnable {
//
//                }).start()

            }else{
                openFile(File(url))
            }
        }

    }

    override fun onDestroy() {
        if (mTbsReaderView != null){
            mTbsReaderView!!.onStop()
        }
        super.onDestroy()
    }

    private fun downloadFromNet(url: String){
        var cacheFile = getCacheFile(url)
        if (cacheFile.exists()){
            if (cacheFile.length() <= 0){
                Log.i(TAG,"删除空文件！！")
                cacheFile.delete()
                return
            }
        }
        handler.sendEmptyMessage(1)
//        runOnUiThread {
//            progressBar.visibility = View.VISIBLE
//        }
        DownloadModel.downloadFile(url, object : Callback<ResponseBody>{
            override fun onFailure(call: Call<ResponseBody>?, t: Throwable?) {
                Log.d(TAG, "文件下载失败")
//                progressBar.visibility = View.GONE
                handler.sendEmptyMessage(2)
                val file = getCacheFile(url)
                if (!file.exists()) {
                    Log.d(TAG, "删除下载失败文件")
                    file.delete()
                }
            }

            override fun onResponse(call: Call<ResponseBody>?, response: Response<ResponseBody>?) {
                Log.i(TAG,"下载文件 --> onResponse")
                var flag: Boolean
                var inputStream: InputStream? = null
                val buf = ByteArray(2048)
                var len = 0
                var fileOutputStream: FileOutputStream? = null
                try {
                    response?.apply {
                        val responseBody = body()
                        inputStream = responseBody.byteStream()
                        val total = responseBody.contentLength()

                        val file1 = getPreviewCacheDir()
                        if (!file1.exists()){
                            file1.mkdirs()
                            Log.d(TAG, "创建缓存目录： $file1")
                        }

                        val fileN = getCacheFile(url)
                        Log.d(TAG, "创建缓存文件： $fileN")
                        if (!fileN.exists()){
                            fileN.createNewFile()
                        }
                        fileOutputStream = FileOutputStream(fileN)
                        var sum: Long = 0
                        while (true){
                            len = inputStream?.read(buf)!!
                            if (len == -1){
                                break
                            }
                            fileOutputStream?.write(buf, 0, len)
                            sum += len
                            val progress =  (sum * 1.0f / total * 100).toInt()
                            Log.d(TAG, "写入缓存文件" + fileN.name + "进度: " + progress)
                        }
                        fileOutputStream?.flush()
                        Log.d(TAG, "文件下载成功,准备展示文件。")
                        handler.sendEmptyMessage(2)
                        runOnUiThread {
                            openFile(fileN)
                        }
//                        runOnUiThread {
//                            progressBar.visibility = View.GONE
//                            openFile(fileN)
//                        }
                    }
                }catch (e: IOException){
                    Log.d(TAG, "文件下载异常 = $e")
//                    runOnUiThread {
//                        progressBar.visibility = View.GONE
//                    }
                    handler.sendEmptyMessage(2)
                }finally {
//                    runOnUiThread {
//                        progressBar.visibility = View.GONE
//                    }
                    handler.sendEmptyMessage(2)
                    if (inputStream != null){
                        inputStream!!.close()
                    }
                    if (fileOutputStream != null){
                        fileOutputStream!!.close()
                    }
                }
            }

        })
    }

    private fun openFile(file: File?){
        Log.i(TAG,"打开文件")
        if (file != null && !TextUtils.isEmpty("$file")){
            if (isImgType(getFileType(file.path))){
                photoView.visibility = View.VISIBLE
                Glide.with(this).load(file).into(photoView)
            }else{
                fileReaderContainer.visibility = View.VISIBLE
                mTbsReaderView = TbsReaderView(this,readerCallback)
                fileReaderContainer.addView(
                    mTbsReaderView,
                    RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT
                    )
                )
                val preOpen = mTbsReaderView!!.preOpen(getFileType(file.absolutePath), false)
                val bundle = Bundle()
                bundle.putString(TbsReaderView.KEY_FILE_PATH,file.path)
                bundle.putString(TbsReaderView.KEY_TEMP_PATH,cacheDir.path)
                if (preOpen){
                    mTbsReaderView!!.openFile(bundle)
                }else{
                    Log.i(TAG,"无法预览文件")
                }
            }
        }else{
            Log.e(TAG, "文件路径无效！")
        }
    }

    private fun isImgType(type: String): Boolean{
        return imgTypes.contains(type)
    }

    private fun getPreviewCacheDir(): File{
        return File(this.cacheDir.absolutePath + "/file/")
    }

    private fun getCacheFile(url: String): File{
        val file = File(getPreviewCacheDir(), getFileName(url))
        Log.i(TAG,"缓存文件 = $file")
        return file
    }

    private fun getFileName(url: String): String{
        return Md5Tool.hashKey(url) + "." + getFileType(url)
    }

    private fun getFileType(url: String): String{
        var str = ""
        if (TextUtils.isEmpty(url)){
            Log.i(TAG,"url ----> null")
            return str
        }

        Log.i(TAG,"url --> $url")
        val i = url.lastIndexOf(".")
        if (i <= -1){
            Log.i(TAG,"i <= -1")
            return str
        }

        str = url.substring(i + 1)
        Log.i(TAG,"file type --> $str")
        return str
    }

}
