package com.neoqee.flutter_file_preview

import android.Manifest
import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.annotation.NonNull
import com.tencent.smtt.export.external.TbsCoreSettings
import com.tencent.smtt.sdk.QbSdk
import com.tencent.smtt.sdk.TbsListener
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import pub.devrel.easypermissions.EasyPermissions

/** FlutterFilePreviewPlugin */
class FlutterFilePreviewPlugin: FlutterPlugin, MethodCallHandler, ActivityAware {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel
  private lateinit var context : Context
  private lateinit var activity : Activity

  private val TAG = "flutter_file_preview"

//  companion object{
//    @JvmStatic
//    fun registerWith(registrar: PluginRegistry.Registrar){
//      val channel = MethodChannel(registrar.messenger(), "flutter_file_preview")
//      val plugin = FlutterFilePreviewPlugin()
//      channel.setMethodCallHandler(plugin)
//      plugin.context = registrar.context()
//      plugin.activity = registrar.activity()
//      plugin.initQbSdk()
//    }
//  }

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    Log.i(TAG,"onAttachedToEngine")
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_file_preview")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
    initQbSdk()
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    Log.i(TAG,"onMethodCall")
    if (call.method == "getPlatformVersion") {
      result.success("Android ${android.os.Build.VERSION.RELEASE}")
    } else if (call.method == "previewFile") {
//      val perms = arrayOf(
//        Manifest.permission.READ_EXTERNAL_STORAGE,
//        Manifest.permission.WRITE_EXTERNAL_STORAGE
//      )
      val title = call.argument("title") ?: "文件预览"
      var url: String? = call.argument("url")
      if (!EasyPermissions.hasPermissions(context,
          Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
        EasyPermissions.requestPermissions(activity,"文件预览需要访问手机存储权限",10086,
          Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
      } else {
        FilePreviewActivity.show(context, url, title)
      }
      result.success("done")
    }
    else {
      result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    Log.i(TAG,"onDetachedFromEngine")
    channel.setMethodCallHandler(null)
  }



  private fun initQbSdk(){
    Log.i(TAG,"initQbSdk")
    val map = HashMap<String,Any>()
    map[TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER] = true
    map[TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE] = true
    QbSdk.initTbsSettings(map)
    QbSdk.setDownloadWithoutWifi(true)
    QbSdk.setTbsListener(object : TbsListener{
      override fun onInstallFinish(p0: Int) {
        Log.i("QbSdk", "onInstallFinish -->安装X5内核进度：$p0")
      }

      override fun onDownloadFinish(p0: Int) {
        Log.i("QbSdk", "onDownloadFinish -->下载X5内核完成：$p0")
      }

      override fun onDownloadProgress(p0: Int) {
        Log.i("QbSdk", "onDownloadProgress -->下载X5内核进度：$p0")
      }

    })
    QbSdk.initX5Environment(context,object : QbSdk.PreInitCallback{
      override fun onCoreInitFinished() {
      }

      override fun onViewInitFinished(p0: Boolean) {
        // x5內核初始化完成的回调，true表x5内核加载成功，否则表加载失败，会自动切换到系统内核。
        Log.d("QbSdk", " 内核加载 $p0")
      }

    })
  }

  override fun onDetachedFromActivity() {
    Log.i(TAG,"onDetachedFromActivity")
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    Log.i(TAG,"onReattachedToActivityForConfigChanges")
    activity = binding.activity
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    Log.i(TAG,"onAttachedToActivity")
    activity = binding.activity
  }

  override fun onDetachedFromActivityForConfigChanges() {
    Log.i(TAG,"onDetachedFromActivityForConfigChanges")

  }

}
