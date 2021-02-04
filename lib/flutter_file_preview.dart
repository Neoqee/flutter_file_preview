
import 'dart:async';

import 'package:flutter/services.dart';

class FlutterFilePreview {
  static const MethodChannel _channel =
      const MethodChannel('flutter_file_preview');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<String> previewFile(String url,{String title}) async{
    Map<String, String> map = {
      "url": url,
      "title": title
    };
    final String result = await _channel.invokeMethod('previewFile',map);
    return result;
  }

}
